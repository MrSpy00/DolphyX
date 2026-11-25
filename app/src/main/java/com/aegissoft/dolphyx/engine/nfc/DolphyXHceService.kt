package com.aegissoft.dolphyx.engine.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.charset.StandardCharsets

class DolphyXHceService : HostApduService() {

    private var selectedFile: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) {
            lastApduEvent.value = "INVALID_APDU"
            return SW_UNKNOWN
        }

        if (startsWith(commandApdu, SELECT_NDEF_APP)) {
            selectedFile = null
            lastApduEvent.value = "SELECT NDEF APP (OK)"
            return SW_OK
        }

        if (startsWith(commandApdu, SELECT_CC_FILE)) {
            selectedFile = CC_FILE
            lastApduEvent.value = "SELECT CC FILE E103 (OK)"
            return SW_OK
        }

        if (startsWith(commandApdu, SELECT_NDEF_FILE)) {
            selectedFile = buildNdefTextMessage(activeNdefText.value)
            lastApduEvent.value = "SELECT NDEF FILE E104 (OK)"
            return SW_OK
        }

        if (commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xB0.toByte()) {
            val file = selectedFile
            if (file == null) {
                lastApduEvent.value = "READ BINARY (NO FILE SELECTED)"
                return SW_FILE_NOT_FOUND
            }
            if (commandApdu.size < 5) {
                lastApduEvent.value = "READ BINARY (MALFORMED)"
                return SW_WRONG_LENGTH
            }
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            val leRaw = commandApdu[4].toInt() and 0xFF
            val le = if (leRaw == 0) 256 else leRaw

            if (offset < 0 || offset >= file.size) {
                lastApduEvent.value = "READ BINARY offset=$offset OUT_OF_BOUNDS"
                return SW_WRONG_P1P2
            }
            val available = file.size - offset
            val count = minOf(le, available)
            val slice = file.copyOfRange(offset, offset + count)
            lastApduEvent.value = "READ BINARY offset=$offset len=$count"
            return slice + SW_OK
        }

        lastApduEvent.value = "UNSUPPORTED INS=${"%02X".format(commandApdu[1])}"
        return SW_INS_NOT_SUPPORTED
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = null
        lastApduEvent.value = "DEACTIVATED reason=$reason"
        Log.d(TAG, "HCE link deactivated: $reason")
    }

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) {
            if (data[i] != prefix[i]) return false
        }
        return true
    }

    companion object {
        private const val TAG = "DolphyXHceService"

        val activeNdefText = MutableStateFlow(DolphyXConfig.defaultHceText)
        val lastApduEvent = MutableStateFlow("Idle")

        private val SELECT_NDEF_APP = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01
        )
        private val SELECT_CC_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x03
        )
        private val SELECT_NDEF_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04
        )
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,
            0x20,
            0x00, 0xFF.toByte(),
            0x00, 0xFF.toByte(),
            0x04, 0x06,
            0xE1.toByte(), 0x04,
            0x04, 0x00,
            0x00, 0x00
        )

        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        private val SW_WRONG_P1P2 = byteArrayOf(0x6B, 0x00)
        private val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
        private val SW_UNKNOWN = byteArrayOf(0x6F, 0x00)

        fun buildNdefTextMessage(text: String): ByteArray {
            val langBytes = "en".toByteArray(StandardCharsets.US_ASCII)
            val textBytes = text.toByteArray(StandardCharsets.UTF_8)
            val payload = ByteArray(1 + langBytes.size + textBytes.size)
            payload[0] = langBytes.size.toByte()
            System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
            System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

            val record: ByteArray = if (payload.size <= 255) {
                byteArrayOf(
                    0xD1.toByte(),
                    0x01,
                    payload.size.toByte(),
                    0x54
                ) + payload
            } else {
                val len = payload.size
                byteArrayOf(
                    0xC1.toByte(),
                    0x01,
                    ((len ushr 24) and 0xFF).toByte(),
                    ((len ushr 16) and 0xFF).toByte(),
                    ((len ushr 8) and 0xFF).toByte(),
                    (len and 0xFF).toByte(),
                    0x54
                ) + payload
            }

            val nlen = record.size
            return byteArrayOf(
                ((nlen ushr 8) and 0xFF).toByte(),
                (nlen and 0xFF).toByte()
            ) + record
        }
    }
}

