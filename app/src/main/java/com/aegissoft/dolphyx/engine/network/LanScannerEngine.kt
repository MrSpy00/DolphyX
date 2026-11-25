package com.aegissoft.dolphyx.engine.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import java.security.cert.X509Certificate

data class LanHost(
    val ip: String,
    val hostname: String?,
    val openPorts: List<Int>,
    val latencyMs: Long,
)

data class DnsLookupResult(
    val host: String,
    val addresses: List<String>,
    val error: String? = null,
)

data class TlsCertificateInfo(
    val host: String,
    val subject: String,
    val issuer: String,
    val notAfter: String,
    val protocol: String,
    val cipher: String,
    val error: String? = null,
)

class LanScannerEngine {

    fun localIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { address ->
                    !address.isLoopbackAddress &&
                        address.address.size == 4 &&
                        address.isSiteLocalAddress
                }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    fun localSubnetBase(): String? {
        val ip = localIpv4() ?: return null
        val parts = ip.split('.')
        if (parts.size != 4) return null
        return parts.take(3).joinToString(".")
    }

    suspend fun scan(
        onHost: (LanHost) -> Unit = {},
        onProgress: (Int) -> Unit = {},
    ): List<LanHost> = withContext(Dispatchers.IO) {
        val base = localSubnetBase() ?: return@withContext emptyList()
        val semaphore = Semaphore(40)
        val found = mutableListOf<LanHost>()
        val lock = Any()
        coroutineScope {
            (1..254).map { octet ->
                async {
                    semaphore.withPermit {
                        val ip = "$base.$octet"
                        val open = probePorts(ip)
                        if (open.isNotEmpty()) {
                            val host = LanHost(
                                ip = ip,
                                hostname = resolveHostname(ip),
                                openPorts = open,
                                latencyMs = measureConnect(ip, open.first()),
                            )
                            synchronized(lock) {
                                found.add(host)
                                onHost(host)
                            }
                        }
                        onProgress(octet)
                    }
                }
            }.awaitAll()
        }
        found.sortedWith(compareBy({ it.ip.substringAfterLast('.').toIntOrNull() ?: 0 }))
    }

    private fun probePorts(ip: String): List<Int> {
        val open = mutableListOf<Int>()
        for (port in SCAN_PORTS) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS)
                    open.add(port)
                }
            } catch (_: Exception) {
                // Port closed or filtered.
            }
        }
        return open
    }

    private fun measureConnect(ip: String, port: Int): Long {
        val start = System.nanoTime()
        return try {
            Socket().use { it.connect(InetSocketAddress(ip, port), TIMEOUT_MS) }
            (System.nanoTime() - start) / 1_000_000
        } catch (e: Exception) {
            -1
        }
    }

    private fun resolveHostname(ip: String): String? {
        return try {
            val address = InetAddress.getByName(ip)
            address.canonicalHostName.takeIf { it != ip && it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun resolveDns(host: String): DnsLookupResult {
        return try {
            val addresses = InetAddress.getAllByName(host).mapNotNull { it.hostAddress }
            DnsLookupResult(host, addresses)
        } catch (e: Exception) {
            DnsLookupResult(host, emptyList(), e.message ?: "DNS lookup failed")
        }
    }

    fun inspectTls(host: String, port: Int = 443): TlsCertificateInfo {
        var socket: SSLSocket? = null
        return try {
            socket = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket() as SSLSocket
            socket.soTimeout = 6000
            val params = socket.sslParameters
            params.serverNames = listOf(SNIHostName(host))
            socket.sslParameters = params
            socket.connect(InetSocketAddress(host, port), 6000)
            socket.startHandshake()
            val session = socket.session
            val cert = session.peerCertificates.firstOrNull() as? X509Certificate
            TlsCertificateInfo(
                host = host,
                subject = cert?.subjectDN?.name ?: "unknown",
                issuer = cert?.issuerDN?.name ?: "unknown",
                notAfter = cert?.notAfter?.toString() ?: "unknown",
                protocol = session.protocol,
                cipher = session.cipherSuite,
            )
        } catch (e: Exception) {
            TlsCertificateInfo(host, "", "", "", "", "", e.message ?: "TLS handshake failed")
        } finally {
            runCatching { socket?.close() }
        }
    }

    companion object {
        private const val TIMEOUT_MS = 180
        private val SCAN_PORTS = listOf(80, 443, 22, 445, 139, 8080, 8443, 631, 9100, 554, 8009, 53, 62078)
    }
}
