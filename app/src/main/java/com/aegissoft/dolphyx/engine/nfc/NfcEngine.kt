package com.aegissoft.dolphyx.engine.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

data class ScannedNfcTag(
    val uidHex: String,
    val techList: List<String>,
    val ndefText: String,
    val sizeBytes: Int,
    val isWritable: Boolean,
    val timestampMillis: Long
)

class NfcEngine(private val context: Context) {

    private val adapter: NfcAdapter? by lazy {
        try {
            NfcAdapter.getDefaultAdapter(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire NfcAdapter", e)
            null
        }
    }

    private val _lastTag = MutableStateFlow<ScannedNfcTag?>(null)
    val lastTag: StateFlow<ScannedNfcTag?> = _lastTag.asStateFlow()

    private val _lastRawTag = MutableStateFlow<Tag?>(null)
    val lastRawTag: StateFlow<Tag?> = _lastRawTag.asStateFlow()

    fun isNfcSupported(): Boolean = adapter != null

    fun isNfcEnabled(): Boolean = try {
        adapter?.isEnabled == true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to query NFC enabled state", e)
        false
    }

    fun enableReaderMode(activity: Activity) {
        val nfcAdapter = adapter ?: return
        if (!isNfcEnabled()) return
        try {
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            nfcAdapter.enableReaderMode(
                activity,
                { tag ->
                    if (tag != null) {
                        parseTag(tag, null)
                    }
                },
                flags,
                Bundle()
            )
        } catch (e: Exception) {
            Log.w(TAG, "enableReaderMode failed", e)
        }
    }

    fun disableReaderMode(activity: Activity) {
        val nfcAdapter = adapter ?: return
        try {
            nfcAdapter.disableReaderMode(activity)
        } catch (e: Exception) {
            Log.w(TAG, "disableReaderMode failed", e)
        }
    }

    fun handleIntent(intent: Intent?): ScannedNfcTag? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) {
            return null
        }
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return null
        return parseTag(tag, intent)
    }

    fun parseTag(tag: Tag, intent: Intent? = null): ScannedNfcTag {
        val uidHex = tag.id?.joinToString(":") { byte -> "%02X".format(byte) }.orEmpty()
        val techList = tag.techList?.map { it.substringAfterLast('.') }.orEmpty()

        var ndefText = ""
        var sizeBytes = 0
        var isWritable = false

        val ndef = try {
            Ndef.get(tag)
        } catch (e: Exception) {
            Log.w(TAG, "Ndef.get failed", e)
            null
        }

        if (ndef != null) {
            try {
                ndef.connect()
                isWritable = ndef.isWritable
                sizeBytes = ndef.maxSize
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                if (message != null) {
                    ndefText = extractNdefText(message)
                    if (sizeBytes == 0) {
                        sizeBytes = message.toByteArray().size
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reading NDEF from tag failed", e)
            } finally {
                try {
                    ndef.close()
                } catch (closeErr: Exception) {
                    Log.w(TAG, "Closing NDEF tag failed", closeErr)
                }
            }
        }

        if (ndefText.isBlank() && intent != null) {
            val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }
            val messages = rawMessages?.filterIsInstance<NdefMessage>().orEmpty()
            if (messages.isNotEmpty()) {
                ndefText = messages.joinToString("\n") { extractNdefText(it) }.trim()
                if (sizeBytes == 0) {
                    sizeBytes = messages.sumOf { it.toByteArray().size }
                }
            }
        }

        val scanned = ScannedNfcTag(
            uidHex = uidHex.ifBlank { "UNKNOWN" },
            techList = techList,
            ndefText = ndefText,
            sizeBytes = sizeBytes,
            isWritable = isWritable,
            timestampMillis = System.currentTimeMillis()
        )
        _lastTag.value = scanned
        _lastRawTag.value = tag
        return scanned
    }

    private fun extractNdefText(message: NdefMessage): String {
        val parts = mutableListOf<String>()
        for (record in message.records) {
            val payload = record.payload ?: continue
            if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT) &&
                payload.isNotEmpty()
            ) {
                val status = payload[0].toInt() and 0xFF
                val langLength = status and 0x3F
                val isUtf16 = (status and 0x80) != 0
                if (1 + langLength <= payload.size) {
                    val charset = if (isUtf16) StandardCharsets.UTF_16 else StandardCharsets.UTF_8
                    parts += String(payload, 1 + langLength, payload.size - 1 - langLength, charset)
                }
            } else if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(android.nfc.NdefRecord.RTD_URI) &&
                payload.isNotEmpty()
            ) {
                val prefix = URI_PREFIXES.getOrElse(payload[0].toInt() and 0xFF) { "" }
                val body = String(payload, 1, payload.size - 1, StandardCharsets.UTF_8)
                parts += prefix + body
            } else {
                val text = String(payload, StandardCharsets.UTF_8).trim()
                if (text.isNotEmpty()) {
                    parts += text
                }
            }
        }
        return parts.joinToString("\n").trim()
    }

    companion object {
        private const val TAG = "DolphyXNfcEngine"

        private val URI_PREFIXES = listOf(
            "", "http://www.", "https://www.", "http://", "https://",
            "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
            "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
            "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
            "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
            "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
            "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
        )
    }
}

