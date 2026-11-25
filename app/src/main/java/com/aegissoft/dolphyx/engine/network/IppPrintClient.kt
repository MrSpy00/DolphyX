package com.aegissoft.dolphyx.engine.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Result of an IPP Print-Job request. */
data class IppPrintResult(val success: Boolean, val statusCode: Int, val detail: String)

/**
 * Minimal IPP/1.1 client that sends a single Print-Job request with an attached document.
 * Supports plain HTTP and HTTPS printer URLs; self-signed TLS certificates are rejected by the platform.
 */
object IppPrintClient {

    private const val OP_PRINT_JOB = 0x0002
    private const val REQUEST_ID = 0x00000001
    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 15000
    const val DEFAULT_PATH = "/ipp/print"
    private const val TAG_OPERATION_ATTRIBUTES = 0x01
    private const val TAG_END_OF_ATTRIBUTES = 0x03
    private const val TAG_CHARSET = 0x47
    private const val TAG_NATURAL_LANGUAGE = 0x48
    private const val TAG_URI = 0x45
    private const val TAG_NAME = 0x42
    private const val TAG_MIME = 0x49

    /** Builds a binary IPP Print-Job request followed by the document bytes. */
    fun buildPrintJob(
        printerUri: String,
        jobName: String,
        userName: String,
        documentFormat: String,
        document: ByteArray,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x02, 0x00)) // IPP version 2.0
        out.write(byteArrayOf((OP_PRINT_JOB shr 8).toByte(), OP_PRINT_JOB.toByte()))
        out.write(byteArrayOf(0x00, 0x00, 0x00, REQUEST_ID.toByte())) // request-id
        out.write(TAG_OPERATION_ATTRIBUTES)
        writeAttribute(out, TAG_CHARSET, "attributes-charset", "utf-8")
        writeAttribute(out, TAG_NATURAL_LANGUAGE, "attributes-natural-language", "en")
        writeAttribute(out, TAG_URI, "printer-uri", printerUri)
        writeAttribute(out, TAG_NAME, "requesting-user-name", userName)
        writeAttribute(out, TAG_NAME, "job-name", jobName)
        writeAttribute(out, TAG_MIME, "document-format", documentFormat)
        out.write(TAG_END_OF_ATTRIBUTES)
        out.write(document)
        return out.toByteArray()
    }

    /** Reads the status-code field of an IPP response. */
    fun parseStatusCode(response: ByteArray): Int {
        if (response.size < 4) return -1
        return ((response[2].toInt() and 0xFF) shl 8) or (response[3].toInt() and 0xFF)
    }

    fun ippUri(host: String, port: Int, path: String): String {
        val cleanPath = path.trim().ifBlank { DEFAULT_PATH }.let { if (it.startsWith("/")) it else "/$it" }
        return "ipp://$host:$port$cleanPath"
    }

    suspend fun print(
        httpUrl: String,
        printerUri: String,
        jobName: String,
        userName: String,
        documentFormat: String,
        document: ByteArray,
    ): IppPrintResult = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPrintJob(printerUri, jobName, userName, documentFormat, document)
            val connection = URL(httpUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/ipp")
                connection.setFixedLengthStreamingMode(payload.size)
                connection.outputStream.use { it.write(payload) }
                val status = parseStatusCode(connection.inputStream.use { it.readBytes() })
                IppPrintResult(
                    success = status == 0x0000,
                    statusCode = status,
                    detail = statusText(status),
                )
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            IppPrintResult(success = false, statusCode = -1, detail = error.message ?: "Print failed")
        }
    }

    private fun writeAttribute(out: ByteArrayOutputStream, tag: Int, name: String, value: String) {
        val nameBytes = name.toByteArray(Charsets.US_ASCII)
        val valueBytes = value.toByteArray(Charsets.UTF_8)
        out.write(tag)
        out.write((nameBytes.size shr 8).toByte().toInt() and 0xFF)
        out.write(nameBytes.size and 0xFF)
        out.write(nameBytes)
        out.write((valueBytes.size shr 8).toByte().toInt() and 0xFF)
        out.write(valueBytes.size and 0xFF)
        out.write(valueBytes)
    }

    private fun statusText(status: Int): String = when {
        status == 0x0000 -> "Print job accepted"
        status in 0x0400..0x04FF -> "Client error (0x${status.toString(16)})"
        status in 0x0500..0x05FF -> "Server error (0x${status.toString(16)})"
        else -> "IPP status 0x${status.toString(16)}"
    }
}
