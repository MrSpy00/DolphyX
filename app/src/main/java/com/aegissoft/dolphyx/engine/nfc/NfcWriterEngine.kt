package com.aegissoft.dolphyx.engine.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.StandardCharsets

/**
 * Small, local NDEF writer. It only writes to a tag the user has physically tapped.
 */
object NfcWriterEngine {

    fun writeText(tag: Tag, text: String, language: String = "en"): Result<String> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Text is empty"))
        return write(tag, NdefMessage(NdefRecord.createTextRecord(language, text)))
    }

    fun writeUri(tag: Tag, uri: String): Result<String> {
        val clean = uri.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("URI is empty"))
        return try {
            write(tag, NdefMessage(NdefRecord.createUri(clean)))
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    fun writeWifi(tag: Tag, ssid: String, password: String, security: String = "WPA"): Result<String> {
        if (ssid.isBlank()) return Result.failure(IllegalArgumentException("SSID is empty"))
        val payload = "WIFI:T:$security;S:$ssid;P:$password;;"
        return write(tag, NdefMessage(NdefRecord.createTextRecord("en", payload)))
    }

    fun writeVCard(tag: Tag, name: String, phone: String, email: String): Result<String> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name is empty"))
        val vcard = buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:$name")
            appendLine("N:$name")
            if (phone.isNotBlank()) appendLine("TEL:$phone")
            if (email.isNotBlank()) appendLine("EMAIL:$email")
            append("END:VCARD")
        }
        return write(
            tag,
            NdefMessage(
                NdefRecord.createMime("text/vcard", vcard.toByteArray(StandardCharsets.UTF_8)),
            ),
        )
    }

    fun erase(tag: Tag): Result<String> {
        val ndef = try {
            Ndef.get(tag)
        } catch (e: Exception) {
            null
        } ?: return Result.failure(IllegalStateException("Tag does not expose NDEF"))
        return try {
            ndef.connect()
            if (!ndef.isWritable) return Result.failure(IllegalStateException("Tag is read-only"))
            ndef.writeNdefMessage(NdefMessage(NdefRecord.createTextRecord("en", "")))
            Result.success("NDEF payload cleared")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun write(tag: Tag, message: NdefMessage): Result<String> {
        val bytes = message.toByteArray()
        val ndef = try {
            Ndef.get(tag)
        } catch (e: Exception) {
            null
        }
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable) return Result.failure(IllegalStateException("Tag is read-only"))
                if (ndef.maxSize in 1 until bytes.size) {
                    return Result.failure(IllegalArgumentException("Payload exceeds tag capacity"))
                }
                ndef.writeNdefMessage(message)
                Result.success("NDEF written (${bytes.size} bytes)")
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                runCatching { ndef.close() }
            }
        }

        val formatable = try {
            NdefFormatable.get(tag)
        } catch (e: Exception) {
            null
        } ?: return Result.failure(IllegalStateException("Tag is not NDEF compatible"))
        return try {
            formatable.connect()
            formatable.format(message)
            Result.success("Tag formatted and written (${bytes.size} bytes)")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { formatable.close() }
        }
    }
}
