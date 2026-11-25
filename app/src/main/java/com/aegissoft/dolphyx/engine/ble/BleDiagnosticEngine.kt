package com.aegissoft.dolphyx.engine.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.data.OuiDatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BleDeviceItem(
    val address: String,
    val name: String,
    val vendor: String?,
    val rssi: Int,
    val isBonded: Boolean,
    val bondState: Int,
    val lastSeenMillis: Long,
)

data class GattServiceInfo(
    val uuid: String,
    val characteristicUuids: List<String>,
)

class BleDiagnosticEngine(
    context: Context,
    private val ouiDb: OuiDatabaseHelper,
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    private val discoveredMap = ConcurrentHashMap<String, BleDeviceItem>()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _devices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val devices: StateFlow<List<BleDeviceItem>> = _devices.asStateFlow()

    private val _gattServices = MutableStateFlow<List<GattServiceInfo>>(emptyList())
    val gattServices: StateFlow<List<GattServiceInfo>> = _gattServices.asStateFlow()

    private val _connectedAddress = MutableStateFlow<String?>(null)
    val connectedAddress: StateFlow<String?> = _connectedAddress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var activeGatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val existing = discoveredMap[address]
            val deviceName = safeDeviceName(device) ?: existing?.name ?: "Unknown"
            val bondState = safeBondState(device)
            val isBonded = bondState == BluetoothDevice.BOND_BONDED || (existing?.isBonded == true)
            val vendor = existing?.vendor ?: ouiDb.lookupVendor(address)
            discoveredMap[address] = BleDeviceItem(
                address = address,
                name = deviceName,
                vendor = vendor,
                rssi = result.rssi,
                isBonded = isBonded,
                bondState = bondState,
                lastSeenMillis = System.currentTimeMillis(),
            )
            publishDevices()
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _statusMessage.value = appContext.getString(R.string.ble_status_scan_failed, errorCode)
            Log.w(TAG, "BLE scan failed with errorCode=$errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val addr = gatt.device?.address
            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_CONNECTED) {
                Log.w(TAG, "GATT connection state error status=$status newState=$newState addr=$addr")
                _connectedAddress.value = null
                _gattServices.value = emptyList()
                _statusMessage.value = appContext.getString(R.string.ble_status_gatt_disconnected, status)
                gatt.close()
                if (activeGatt === gatt) {
                    activeGatt = null
                }
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedAddress.value = addr
                    _statusMessage.value = appContext.getString(R.string.ble_status_connecting_discovering, addr)
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        _statusMessage.value = appContext.getString(R.string.ble_status_discovery_permission, e.message.orEmpty())
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectedAddress.value = null
                    _gattServices.value = emptyList()
                    _statusMessage.value = appContext.getString(R.string.ble_status_disconnected, addr)
                    gatt.close()
                    if (activeGatt === gatt) {
                        activeGatt = null
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val infos = gatt.services.orEmpty().map { svc ->
                    GattServiceInfo(
                        uuid = svc.uuid.toString(),
                        characteristicUuids = svc.characteristics.orEmpty().map { it.uuid.toString() },
                    )
                }
                _gattServices.value = infos
                _statusMessage.value = appContext.getString(R.string.ble_status_services_found, infos.size)
            } else {
                _statusMessage.value = appContext.getString(R.string.ble_status_discovery_failed, status)
            }
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val adapter = bluetoothAdapter ?: run {
            _statusMessage.value = appContext.getString(R.string.ble_status_adapter_unavailable)
            return
        }
        try {
            val now = System.currentTimeMillis()
            val bonded = adapter.bondedDevices.orEmpty()
            for (device in bonded) {
                val address = device.address ?: continue
                val existing = discoveredMap[address]
                val name = safeDeviceName(device) ?: existing?.name ?: "Bonded Device"
                val vendor = existing?.vendor ?: ouiDb.lookupVendor(address)
                discoveredMap[address] = BleDeviceItem(
                    address = address,
                    name = name,
                    vendor = vendor,
                    rssi = existing?.rssi ?: 0,
                    isBonded = true,
                    bondState = device.bondState,
                    lastSeenMillis = now,
                )
            }
            publishDevices()
        } catch (e: SecurityException) {
            _statusMessage.value = appContext.getString(R.string.ble_status_bonded_permission, e.message.orEmpty())
            Log.w(TAG, "SecurityException reading bondedDevices", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _statusMessage.value = appContext.getString(R.string.ble_status_bt_disabled)
            return
        }
        // Always populate bonded devices first so connected A2DP/HFP audio/IoT devices appear
        refreshBondedDevices()

        val scanner = bleScanner ?: run {
            _statusMessage.value = appContext.getString(R.string.ble_status_scanner_unavailable)
            return
        }
        if (_isScanning.value) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
            _statusMessage.value = appContext.getString(R.string.ble_status_scan_active)
        } catch (e: SecurityException) {
            _isScanning.value = false
            _statusMessage.value = appContext.getString(R.string.ble_status_scan_permission, e.message.orEmpty())
            Log.w(TAG, "SecurityException in startScan", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException in stopScan", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "IllegalStateException in stopScan", e)
        } finally {
            _isScanning.value = false
            _statusMessage.value = appContext.getString(R.string.ble_status_scan_stopped)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectGatt(address: String) {
        val adapter = bluetoothAdapter ?: run {
            _statusMessage.value = appContext.getString(R.string.ble_status_adapter_unavailable)
            return
        }
        disconnectGatt()
        try {
            val device = adapter.getRemoteDevice(address)
            _statusMessage.value = appContext.getString(R.string.ble_status_connecting, address)
            activeGatt = device.connectGatt(
                appContext,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
            )
        } catch (e: SecurityException) {
            _statusMessage.value = appContext.getString(R.string.ble_status_connect_permission, e.message.orEmpty())
            Log.w(TAG, "SecurityException in connectGatt", e)
        } catch (e: IllegalArgumentException) {
            _statusMessage.value = appContext.getString(R.string.ble_status_invalid_address, address)
            Log.w(TAG, "Invalid address in connectGatt: $address", e)
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun writeCharacteristic(serviceUuid: UUID, charUuid: UUID, payload: ByteArray): Boolean {
        val gatt = activeGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(charUuid) ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(
                    characteristic,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                )
                status == BluetoothGatt.GATT_SUCCESS
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                characteristic.value = payload
                gatt.writeCharacteristic(characteristic)
            }
        } catch (e: SecurityException) {
            _statusMessage.value = appContext.getString(R.string.ble_status_write_permission, e.message.orEmpty())
            Log.w(TAG, "SecurityException in writeCharacteristic", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        val gatt = activeGatt ?: return
        activeGatt = null
        try {
            gatt.disconnect()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException in gatt.disconnect", e)
        } finally {
            try {
                gatt.close()
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException in gatt.close", e)
            }
            _connectedAddress.value = null
            _gattServices.value = emptyList()
        }
    }

    fun release() {
        stopScan()
        disconnectGatt()
    }

    private fun publishDevices() {
        _devices.value = discoveredMap.values.sortedWith(
            compareByDescending<BleDeviceItem> { it.isBonded }
                .thenByDescending { it.rssi },
        )
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return try {
            device.name?.takeIf { it.isNotBlank() }
        } catch (e: SecurityException) {
            Log.d(TAG, "Unable to read device name: ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeBondState(device: BluetoothDevice): Int {
        return try {
            device.bondState
        } catch (e: SecurityException) {
            Log.d(TAG, "Unable to read bond state: ${e.message}")
            BluetoothDevice.BOND_NONE
        }
    }

    companion object {
        private const val TAG = "BleDiagnosticEngine"
    }
}
