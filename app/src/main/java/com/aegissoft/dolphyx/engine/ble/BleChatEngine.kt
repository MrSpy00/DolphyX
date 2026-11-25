package com.aegissoft.dolphyx.engine.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.aegissoft.dolphyx.R
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatPeer(val name: String, val address: String)

data class ChatMessage(
    val id: Long,
    val from: String,
    val text: String,
    val outgoing: Boolean,
    val timeMillis: Long,
)

/** Newline framing for BLE writes: chunks are reassembled until a '\n' separator. */
object ChatFraming {

    fun chunk(text: String, maxBytes: Int): List<ByteArray> {
        val bytes = (text.replace('\n', ' ') + "\n").toByteArray(Charsets.UTF_8)
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + maxBytes, bytes.size)
            chunks += bytes.copyOfRange(offset, end)
            offset = end
        }
        return chunks
    }

    fun append(accumulator: ByteArrayOutputStream, data: ByteArray): List<String> {
        val messages = mutableListOf<String>()
        for (byte in data) {
            if (byte == '\n'.code.toByte()) {
                val line = accumulator.toString(Charsets.UTF_8.name()).trim()
                accumulator.reset()
                if (line.isNotEmpty()) messages += line
            } else {
                accumulator.write(byte.toInt())
            }
        }
        return messages
    }
}

/**
 * Nearby chat over BLE: one custom service, a write characteristic (peer -> us)
 * and a notify characteristic (us -> peer). Messages are UTF-8, newline framed.
 * No cloud, no account, no encryption on the link.
 */
@SuppressLint("MissingPermission")
class BleChatEngine(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter get() = manager?.adapter
    private val localName = appContext.getString(R.string.app_name)

    private val _peers = MutableStateFlow<List<ChatPeer>>(emptyList())
    val peers: StateFlow<List<ChatPeer>> = _peers.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _hosting = MutableStateFlow(false)
    val hosting: StateFlow<Boolean> = _hosting.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _connectedName = MutableStateFlow<String?>(null)
    val connectedName: StateFlow<String?> = _connectedName.asStateFlow()

    private var server: BluetoothGattServer? = null
    private var serverDevice: BluetoothDevice? = null
    private var serverRx: BluetoothGattCharacteristic? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var gatt: BluetoothGatt? = null
    private var clientRx: BluetoothGattCharacteristic? = null
    private var clientTx: BluetoothGattCharacteristic? = null
    private val clientAccumulator = ByteArrayOutputStream()
    private val serverAccumulator = ByteArrayOutputStream()
    private var nextId = 1L

    val hasPermissions: Boolean
        get() {
            val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            return required.all {
                ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
            }
        }

    fun startHosting() {
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _status.value = appContext.getString(R.string.chat_status_bt_off)
            return
        }
        if (!hasPermissions) {
            _status.value = appContext.getString(R.string.chat_status_permission)
            return
        }
        if (_hosting.value) return

        val rx = BluetoothGattCharacteristic(
            RX_CHARACTERISTIC,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        rx.addDescriptor(
            BluetoothGattDescriptor(
                CCCD,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            ),
        )
        val tx = BluetoothGattCharacteristic(
            TX_CHARACTERISTIC,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
            addCharacteristic(rx)
            addCharacteristic(tx)
        }

        server = manager?.openGattServer(appContext, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    serverDevice = device
                    _connectedName.value = safeName(device)
                    _status.value = appContext.getString(R.string.chat_status_peer_connected)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (serverDevice?.address == device.address) {
                        serverDevice = null
                        _connectedName.value = null
                    }
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (characteristic.uuid == TX_CHARACTERISTIC) {
                    ChatFraming.append(serverAccumulator, value).forEach { line ->
                        addMessage(from = safeName(device), text = line, outgoing = false)
                    }
                }
                if (responseNeeded) {
                    runCatching { server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null) }
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (responseNeeded) {
                    runCatching { server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null) }
                }
            }
        })?.apply {
            runCatching { addService(service) }
                .onFailure { _status.value = it.message }
        }
        serverRx = rx

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val scanResponse = AdvertiseData.Builder().setIncludeDeviceName(true).build()
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                _hosting.value = true
                _status.value = appContext.getString(R.string.chat_status_hosting, localName)
            }

            override fun onStartFailure(errorCode: Int) {
                _hosting.value = false
                _status.value = appContext.getString(R.string.chat_status_advertise_failed, errorCode)
            }
        }
        runCatching {
            bluetoothAdapter.bluetoothLeAdvertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
        }.onFailure { _status.value = it.message }
    }

    fun stopHosting() {
        runCatching { advertiseCallback?.let { adapter?.bluetoothLeAdvertiser?.stopAdvertising(it) } }
        advertiseCallback = null
        runCatching { server?.close() }
        server = null
        serverDevice = null
        serverRx = null
        serverAccumulator.reset()
        _hosting.value = false
    }

    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null || !hasPermissions) {
            _status.value = appContext.getString(R.string.chat_status_scan_unavailable)
            return
        }
        if (_scanning.value) return
        _peers.value = emptyList()
        _scanning.value = true
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(listOf(filter), settings, scanCallback) }
            .onFailure {
                _scanning.value = false
                _status.value = it.message
            }
    }

    fun stopScan() {
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        _scanning.value = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            if (device.address == adapter?.address) return
            val peer = ChatPeer(name = safeName(device), address = device.address)
            _peers.value = (_peers.value + peer).distinctBy { it.address }
        }
    }

    fun connect(address: String) {
        val bluetoothAdapter = adapter ?: return
        if (!hasPermissions) {
            _status.value = appContext.getString(R.string.chat_status_permission)
            return
        }
        val device = runCatching { bluetoothAdapter.getRemoteDevice(address) }.getOrNull() ?: return
        disconnectClient()
        _status.value = appContext.getString(R.string.chat_status_connecting, safeName(device))
        gatt = runCatching {
            device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }.getOrNull()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectedName.value = safeName(gatt.device)
                runCatching { gatt.requestMtu(247) }
                runCatching { gatt.discoverServices() }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                clientRx = null
                clientTx = null
                _connectedName.value = null
                runCatching { gatt.close() }
                if (this@BleChatEngine.gatt === gatt) this@BleChatEngine.gatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID)
            if (status != BluetoothGatt.GATT_SUCCESS || service == null) {
                _status.value = appContext.getString(R.string.chat_status_service_missing)
                return
            }
            val rx = service.getCharacteristic(RX_CHARACTERISTIC)
            val tx = service.getCharacteristic(TX_CHARACTERISTIC)
            if (rx == null || tx == null) {
                _status.value = appContext.getString(R.string.chat_status_chars_missing)
                return
            }
            clientRx = rx
            clientTx = tx
            runCatching { gatt.setCharacteristicNotification(rx, true) }
            val cccd = rx.getDescriptor(CCCD)
            if (cccd != null) {
                runCatching {
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(cccd)
                }
            }
            _status.value = appContext.getString(R.string.chat_status_connected, safeName(gatt.device))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid != RX_CHARACTERISTIC) return
            ChatFraming.append(clientAccumulator, value).forEach { line ->
                addMessage(from = safeName(gatt.device), text = line, outgoing = false)
            }
        }
    }

    fun send(text: String) {
        val clean = text.trim().replace('\n', ' ')
        if (clean.isEmpty()) return
        val chunks = ChatFraming.chunk(clean, CHUNK_BYTES)
        val target = _connectedName.value ?: run {
            _status.value = appContext.getString(R.string.chat_status_no_peer)
            return
        }
        var sent = false
        serverDevice?.let { device ->
            val rx = serverRx
            if (rx != null) {
                chunks.forEach { bytes ->
                    runCatching {
                        rx.value = bytes
                        server?.notifyCharacteristicChanged(device, rx, false)
                    }
                }
                sent = true
            }
        }
        if (!sent) {
            val gatt = gatt
            val tx = clientTx
            if (gatt != null && tx != null) {
                chunks.forEach { bytes ->
                    runCatching {
                        tx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        tx.value = bytes
                        gatt.writeCharacteristic(tx)
                    }
                }
                sent = true
            }
        }
        if (sent) {
            addMessage(from = localName, text = clean, outgoing = true)
        } else {
            _status.value = appContext.getString(R.string.chat_status_send_failed, target)
        }
    }

    private fun addMessage(from: String, text: String, outgoing: Boolean) {
        val message = ChatMessage(
            id = nextId++,
            from = from,
            text = text,
            outgoing = outgoing,
            timeMillis = System.currentTimeMillis(),
        )
        _messages.value = (_messages.value + message).takeLast(MAX_MESSAGES)
    }

    private fun disconnectClient() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        clientRx = null
        clientTx = null
        clientAccumulator.reset()
        _connectedName.value = null
    }

    private fun safeName(device: BluetoothDevice): String {
        val name = runCatching { device.name }.getOrNull()
        return name?.takeIf { it.isNotBlank() } ?: device.address
    }

    fun release() {
        stopScan()
        stopHosting()
        disconnectClient()
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6b1f0001-8c3d-4a55-9c2f-d0d1e2f3a4b5")
        val TX_CHARACTERISTIC: UUID = UUID.fromString("6b1f0002-8c3d-4a55-9c2f-d0d1e2f3a4b5")
        val RX_CHARACTERISTIC: UUID = UUID.fromString("6b1f0003-8c3d-4a55-9c2f-d0d1e2f3a4b5")
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val CHUNK_BYTES = 180
        private const val MAX_MESSAGES = 200
    }
}
