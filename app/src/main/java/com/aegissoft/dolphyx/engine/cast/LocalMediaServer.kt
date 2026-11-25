package com.aegissoft.dolphyx.engine.cast

import android.content.Context
import android.net.Uri
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Loopback/LAN HTTP file server with byte-range support for DLNA/browser playback.
 * The selected document is copied into cache while the session is alive.
 */
class LocalMediaServer {

    data class Session(val url: String, val mime: String)

    private var serverSocket: ServerSocket? = null
    private var cacheFile: File? = null
    private var worker: Thread? = null
    private var mime: String = "video/mp4"

    fun start(context: Context, uri: Uri, mimeOverride: String? = null): Session? {
        stop()
        val copied = runCatching {
            val suffix = context.contentResolver.getType(uri)?.substringAfterLast('/')?.let { ".$it" } ?: ".mp4"
            val target = File.createTempFile("dolphyx_cast_", suffix, context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            target
        }.getOrNull() ?: return null

        cacheFile = copied
        mime = mimeOverride
            ?: context.contentResolver.getType(uri)
            ?: mimeFor(copied.name)
        val socket = runCatching { ServerSocket(0) }.getOrNull() ?: run {
            copied.delete()
            return null
        }
        serverSocket = socket
        val ip = localIpv4() ?: DolphyXConfig.localBindHost
        worker = thread(isDaemon = true, name = "dolphyx-media-server") {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                runCatching { handle(client, copied) }
            }
        }
        return Session("http://$ip:${socket.localPort}/media", mime)
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        worker = null
        cacheFile?.delete()
        cacheFile = null
    }

    private fun handle(client: Socket, file: File) {
        client.use { socket ->
            val reader = socket.getInputStream().bufferedReader()
            val requestLine = reader.readLine().orEmpty()
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
            }
            val output = socket.getOutputStream()
            if (!requestLine.contains("/media")) {
                output.write("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n".toByteArray())
                return
            }
            val length = file.length()
            val range = parseRange(headers["range"], length)
            if (range == null) {
                writeCommon(output, 200, "OK", mime, length, null)
                file.inputStream().use { it.copyTo(output) }
            } else {
                val (start, end) = range
                val count = (end - start + 1).coerceAtLeast(0)
                writeCommon(output, 206, "Partial Content", mime, count, "bytes $start-$end/$length")
                file.inputStream().use { input ->
                    input.skip(start)
                    val buffer = ByteArray(64 * 1024)
                    var remaining = count
                    while (remaining > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
            output.flush()
        }
    }

    private fun writeCommon(
        output: OutputStream,
        code: Int,
        reason: String,
        contentType: String,
        contentLength: Long,
        contentRange: String?,
    ) {
        val head = buildString {
            append("HTTP/1.1 $code $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: $contentLength\r\n")
            append("Accept-Ranges: bytes\r\n")
            if (contentRange != null) append("Content-Range: $contentRange\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(head.toByteArray())
    }

    companion object {
        /** Parses a single `bytes=start-end` range. Invalid or multi ranges return null. */
        fun parseRange(header: String?, length: Long): Pair<Long, Long>? {
            if (header == null || !header.startsWith("bytes=") || length <= 0) return null
            val spec = header.removePrefix("bytes=").substringBefore(',')
            val parts = spec.split('-')
            if (parts.size != 2) return null
            return if (parts[0].isBlank()) {
                val suffix = parts[1].toLongOrNull() ?: return null
                val start = (length - suffix).coerceAtLeast(0)
                start to (length - 1)
            } else {
                val start = parts[0].toLongOrNull() ?: return null
                val end = parts[1].toLongOrNull()?.coerceAtMost(length - 1) ?: (length - 1)
                if (start > end || start >= length) null else start to end
            }
        }

        fun localIpv4(): String? {
            return runCatching {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                    ?.hostAddress
            }.getOrNull()
        }

        private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }
}
