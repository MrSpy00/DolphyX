package com.aegissoft.dolphyx.engine.network

import android.util.Log
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

data class ServerState(
    val isRunning: Boolean,
    val bindHost: String,
    val port: Int,
    val shareUrl: String?,
    val token: String?,
    val requestCount: Int
)

class SecureLocalQrServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    @Volatile
    private var sharedText: String = ""

    @Volatile
    private var activeToken: String = ""

    private val _state = MutableStateFlow(
        ServerState(
            isRunning = false,
            bindHost = LOOPBACK_HOST,
            port = 0,
            shareUrl = null,
            token = null,
            requestCount = 0
        )
    )
    val state: StateFlow<ServerState> = _state.asStateFlow()

    fun startTextShare(text: String, bindLan: Boolean = false, lanIp: String? = null) {
        stop()
        val bindHost = if (bindLan && !lanIp.isNullOrBlank()) lanIp.trim() else LOOPBACK_HOST
        val token = generateTokenHex()
        sharedText = text
        activeToken = token

        try {
            val socket = ServerSocket(0, 16, InetAddress.getByName(bindHost))
            serverSocket = socket
            val boundPort = socket.localPort
            val shareUrl = "http://$bindHost:$boundPort/share?token=$token"
            _state.value = ServerState(
                isRunning = true,
                bindHost = bindHost,
                port = boundPort,
                shareUrl = shareUrl,
                token = token,
                requestCount = 0
            )
            acceptJob = scope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            Log.w(TAG, "Accept loop error", e)
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SecureLocalQrServer on $bindHost", e)
            stop()
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ServerSocket", e)
        }
        serverSocket = null
        activeToken = ""
        _state.value = ServerState(
            isRunning = false,
            bindHost = LOOPBACK_HOST,
            port = 0,
            shareUrl = null,
            token = null,
            requestCount = 0
        )
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            socket.soTimeout = 5000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) {
                writeResponse(socket, 400, "Bad Request", "text/plain", "Bad Request")
                return
            }

            val rawTarget = parts[1]
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] =
                        line.substring(colon + 1).trim()
                }
            }

            val expected = activeToken
            val queryToken = extractQueryToken(rawTarget)
            val bearerToken = headers["authorization"]
                ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                ?.substring(7)
                ?.trim()

            if (!verifyToken(queryToken ?: bearerToken, expected)) {
                writeResponse(socket, 401, "Unauthorized", "text/plain", "401 Unauthorized")
                return
            }

            val pathOnly = rawTarget.substringBefore('?')
            val segment = pathOnly.trimStart('/')
            if (segment.isNotEmpty() && sanitizePathSegment(segment) == null) {
                writeResponse(socket, 400, "Bad Request", "text/plain", "Invalid path")
                return
            }

            val current = _state.value
            _state.value = current.copy(requestCount = current.requestCount + 1)

            val escaped = escapeHtml(sharedText)
            val html = """
                <!doctype html>
                <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>DolphyX Local Share</title>
                <style>body{font-family:monospace;background:#09060F;color:#F3F4F6;padding:24px;}pre{background:#140E21;padding:16px;border-radius:12px;white-space:pre-wrap;word-break:break-word;}</style>
                </head><body><h3>DolphyX Local Share</h3><pre>$escaped</pre></body></html>
            """.trimIndent()
            writeResponse(socket, 200, "OK", "text/html; charset=utf-8", html)
        }
    }

    private fun extractQueryToken(rawTarget: String): String? {
        val query = rawTarget.substringAfter('?', "")
        if (query.isEmpty()) return null
        for (pair in query.split('&')) {
            val eq = pair.indexOf('=')
            if (eq > 0 && pair.substring(0, eq) == "token") {
                return pair.substring(eq + 1)
            }
        }
        return null
    }

    private fun writeResponse(
        socket: Socket,
        status: Int,
        reason: String,
        contentType: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII)
        writer.write("HTTP/1.1 $status $reason\r\n")
        writer.write("Content-Type: $contentType\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.flush()
        socket.getOutputStream().write(bodyBytes)
        socket.getOutputStream().flush()
    }

    private fun escapeHtml(input: String): String = buildString(input.length) {
        for (ch in input) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(ch)
            }
        }
    }

    companion object {
        private const val TAG = "SecureLocalQrServer"
        private val LOOPBACK_HOST: String = DolphyXConfig.localBindHost
        private val SAFE_SEGMENT_REGEX = Regex("^[A-Za-z0-9._-]+$")

        fun sanitizePathSegment(segment: String): String? {
            if (segment.isEmpty()) return null
            if (segment.contains("..") || segment.contains('/') || segment.contains('\\') || segment.contains('\u0000')) {
                return null
            }
            return if (SAFE_SEGMENT_REGEX.matches(segment)) segment else null
        }

        fun verifyToken(provided: String?, expected: String): Boolean {
            if (provided.isNullOrEmpty() || expected.isEmpty()) return false
            val a = provided.toByteArray(StandardCharsets.UTF_8)
            val b = expected.toByteArray(StandardCharsets.UTF_8)
            return MessageDigest.isEqual(a, b)
        }

        private fun generateTokenHex(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

