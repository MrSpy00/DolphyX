package com.aegissoft.dolphyx.engine.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HidStatus(
    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
    val registered: Boolean = false,
    val connectedHost: String? = null,
    val message: String? = null,
)

/**
 * Bluetooth HID Device (keyboard + consumer + mouse) for the user's own paired host.
 * Registration only; the host must explicitly pair/connect from its Bluetooth settings.
 */
class BluetoothHidController(private val context: Context) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null

    private val _status = MutableStateFlow(HidStatus())
    val status: StateFlow<HidStatus> = _status.asStateFlow()

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _status.value = _status.value.copy(
                registered = registered,
                message = if (registered) "HID app registered" else "HID app unregistered",
            )
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val name = safeName(device)
            _status.value = when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    _status.value.copy(connectedHost = name, message = "Connected to $name")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    host = null
                    _status.value.copy(connectedHost = null, message = "Disconnected")
                }
                else -> _status.value
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            val target = device ?: return
            val reportId = id.toInt()
            val payload = when (reportId) {
                REPORT_ID_KEYBOARD -> ByteArray(8)
                REPORT_ID_CONSUMER -> ByteArray(1)
                REPORT_ID_MOUSE -> ByteArray(4)
                else -> return
            }
            runCatching { hidDevice?.replyReport(target, type, id, payload) }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray) {
            val target = device ?: return
            runCatching { hidDevice?.reportError(target, BluetoothHidDevice.ERROR_RSP_SUCCESS) }
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val device = proxy as? BluetoothHidDevice ?: return
            hidDevice = device
            register(device)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _status.value = _status.value.copy(registered = false, connectedHost = null)
            }
        }
    }

    fun start() {
        val btAdapter = adapter
        if (btAdapter == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _status.value = HidStatus(supported = false, message = "HID Device profile unavailable")
            return
        }
        if (!btAdapter.isEnabled) {
            _status.value = _status.value.copy(message = "Bluetooth is disabled")
            return
        }
        try {
            btAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        } catch (e: SecurityException) {
            _status.value = _status.value.copy(message = "Bluetooth permission denied")
        } catch (e: Exception) {
            Log.w(TAG, "HID profile proxy failed", e)
            _status.value = _status.value.copy(message = e.message)
        }
    }

    @SuppressLint("MissingPermission")
    private fun register(device: BluetoothHidDevice) {
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "DolphyX Keyboard",
                "DolphyX HID remote",
                "aegisSoft",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                COMBO_REPORT_DESCRIPTOR,
            )
            val queued = device.registerApp(
                sdp,
                null,
                null,
                ContextCompat.getMainExecutor(context),
                callback,
            )
            _status.value = _status.value.copy(message = if (queued) "Registering HID app..." else "HID registration rejected")
        } catch (e: SecurityException) {
            _status.value = _status.value.copy(message = "Bluetooth permission denied")
        } catch (e: Exception) {
            Log.w(TAG, "HID registerApp failed", e)
            _status.value = _status.value.copy(message = e.message)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectTo(device: BluetoothDevice): Boolean {
        val target = device
        if (!_status.value.registered) {
            _status.value = _status.value.copy(message = "Register HID first")
            return false
        }
        return try {
            hidDevice?.connect(target) == true
        } catch (e: SecurityException) {
            _status.value = _status.value.copy(message = "Bluetooth permission denied")
            false
        } catch (e: Exception) {
            _status.value = _status.value.copy(message = e.message)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            host?.let { hidDevice?.disconnect(it) }
        } catch (e: Exception) {
            Log.w(TAG, "HID disconnect failed", e)
        }
        host = null
        _status.value = _status.value.copy(connectedHost = null)
    }

    fun sendText(text: String): Result<Int> {
        val target = host ?: return Result.failure(IllegalStateException("No HID host connected"))
        val device = hidDevice ?: return Result.failure(IllegalStateException("HID profile is not ready"))
        var sent = 0
        text.forEach { char ->
            val mapped = HidKeyMapper.map(char)
            if (mapped != null) {
                val down = byteArrayOf(mapped.modifier, 0, mapped.keycode, 0, 0, 0, 0, 0)
                val up = ByteArray(8)
                val okDown = runCatching { device.sendReport(target, REPORT_ID_KEYBOARD, down) }.getOrDefault(false)
                val okUp = runCatching { device.sendReport(target, REPORT_ID_KEYBOARD, up) }.getOrDefault(false)
                if (okDown && okUp) sent++
            }
            Thread.sleep(12)
        }
        return Result.success(sent)
    }

    fun sendConsumer(volumeUp: Boolean): Boolean {
        val target = host ?: return false
        val device = hidDevice ?: return false
        return runCatching {
            val payload = byteArrayOf(if (volumeUp) 0x01 else 0x02)
            device.sendReport(target, REPORT_ID_CONSUMER, payload)
            device.sendReport(target, REPORT_ID_CONSUMER, ByteArray(1))
        }.isSuccess
    }

    fun sendMouse(buttons: Int, dx: Int, dy: Int, wheel: Int): Boolean {
        val target = host ?: return false
        val device = hidDevice ?: return false
        val payload = byteArrayOf(
            buttons.toByte(),
            dx.coerceIn(-127, 127).toByte(),
            dy.coerceIn(-127, 127).toByte(),
            wheel.coerceIn(-127, 127).toByte(),
        )
        return runCatching { device.sendReport(target, REPORT_ID_MOUSE, payload) }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BluetoothDevice> {
        return try {
            adapter?.bondedDevices?.toList().orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice?): String {
        if (device == null) return "Host"
        return try {
            device.name ?: device.address
        } catch (e: SecurityException) {
            device.address
        }
    }

    fun release() {
        disconnect()
        try {
            hidDevice?.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "HID unregister failed", e)
        }
        try {
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        } catch (e: Exception) {
            Log.w(TAG, "HID proxy close failed", e)
        }
        hidDevice = null
    }

    companion object {
        private const val TAG = "DolphyXHid"
        private const val REPORT_ID_KEYBOARD = 1
        private const val REPORT_ID_CONSUMER = 2
        private const val REPORT_ID_MOUSE = 3

        private val COMBO_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
            0x05, 0x01, 0x09, 0x06, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x01,
            0x05, 0x07, 0x19, 0xE0.toByte(), 0x29, 0xE7.toByte(), 0x15, 0x00,
            0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08, 0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x08, 0x81.toByte(), 0x01, 0x95.toByte(),
            0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07, 0x19,
            0x00, 0x29, 0x65, 0x81.toByte(), 0x00, 0xC0.toByte(),
            0x05, 0x0C, 0x09, 0x01, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x02,
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x02, 0x09,
            0xE9.toByte(), 0x09, 0xEA.toByte(), 0x81.toByte(), 0x02, 0x95.toByte(),
            0x06, 0x81.toByte(), 0x01, 0xC0.toByte(),
            0x05, 0x01, 0x09, 0x02, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x03,
            0x09, 0x01, 0xA1.toByte(), 0x00, 0x05, 0x09, 0x19, 0x01,
            0x29, 0x03, 0x15, 0x00, 0x25, 0x01, 0x95.toByte(), 0x03, 0x75,
            0x01, 0x81.toByte(), 0x02, 0x95.toByte(), 0x01, 0x75, 0x05, 0x81.toByte(),
            0x01, 0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x38, 0x15,
            0x81.toByte(), 0x25, 0x7F, 0x75, 0x08, 0x95.toByte(), 0x03,
            0x81.toByte(), 0x06, 0xC0.toByte(), 0xC0.toByte(),
        )
    }
}

/** ASCII to HID usage/keycode, with shift when needed. */
object HidKeyMapper {

    data class KeyStroke(val modifier: Byte, val keycode: Byte)

    fun map(char: Char): KeyStroke? {
        val lower = char.lowercaseChar()
        val shift = char.isUpperCase()
        val shifted = if (shift) 0x02 else 0x00
        return when (lower) {
            in 'a'..'z' -> KeyStroke(shifted.toByte(), (0x04 + (lower - 'a')).toByte())
            in '1'..'9' -> KeyStroke(shifted.toByte(), (0x1E + (lower - '1')).toByte())
            '0' -> KeyStroke(shifted.toByte(), 0x27)
            ' ' -> KeyStroke(0, 0x2C)
            '\n' -> KeyStroke(0, 0x28)
            '\t' -> KeyStroke(0, 0x2B)
            '\b' -> KeyStroke(0, 0x2A)
            '-' -> KeyStroke(shifted.toByte(), 0x2D)
            '_' -> KeyStroke(0x02, 0x2D)
            '=' -> KeyStroke(shifted.toByte(), 0x2E)
            '+' -> KeyStroke(0x02, 0x2E)
            '[' -> KeyStroke(shifted.toByte(), 0x2F)
            '{' -> KeyStroke(0x02, 0x2F)
            ']' -> KeyStroke(shifted.toByte(), 0x30)
            '}' -> KeyStroke(0x02, 0x30)
            '\\' -> KeyStroke(shifted.toByte(), 0x31)
            '|' -> KeyStroke(0x02, 0x31)
            ';' -> KeyStroke(shifted.toByte(), 0x33)
            ':' -> KeyStroke(0x02, 0x33)
            '\'' -> KeyStroke(shifted.toByte(), 0x34)
            '"' -> KeyStroke(0x02, 0x34)
            '`' -> KeyStroke(shifted.toByte(), 0x35)
            '~' -> KeyStroke(0x02, 0x35)
            ',' -> KeyStroke(shifted.toByte(), 0x36)
            '<' -> KeyStroke(0x02, 0x36)
            '.' -> KeyStroke(shifted.toByte(), 0x37)
            '>' -> KeyStroke(0x02, 0x37)
            '/' -> KeyStroke(shifted.toByte(), 0x38)
            '?' -> KeyStroke(0x02, 0x38)
            '!' -> KeyStroke(0x02, 0x1E)
            '@' -> KeyStroke(0x02, 0x1F)
            '#' -> KeyStroke(0x02, 0x20)
            '$' -> KeyStroke(0x02, 0x21)
            '%' -> KeyStroke(0x02, 0x22)
            '^' -> KeyStroke(0x02, 0x23)
            '&' -> KeyStroke(0x02, 0x24)
            '*' -> KeyStroke(0x02, 0x25)
            '(' -> KeyStroke(0x02, 0x26)
            ')' -> KeyStroke(0x02, 0x27)
            else -> null
        }
    }
}
