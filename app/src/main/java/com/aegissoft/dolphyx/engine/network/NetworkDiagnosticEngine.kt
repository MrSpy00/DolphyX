package com.aegissoft.dolphyx.engine.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class NetworkSnapshot(
    val isConnected: Boolean,
    val transportType: String,
    val localIpv4: String?,
    val localIpv6: String?,
    val dnsServers: List<String>,
    val interfaceName: String?,
    val linkDownKbps: Int,
    val linkUpKbps: Int
)

data class TcpProbeResult(
    val host: String,
    val port: Int,
    val reachable: Boolean,
    val latencyMs: Long,
    val error: String?
)

class NetworkDiagnosticEngine(private val context: Context) {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(DolphyXConfig.httpTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(DolphyXConfig.httpTimeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun captureSnapshot(): NetworkSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            return NetworkSnapshot(
                isConnected = false,
                transportType = "Unavailable",
                localIpv4 = null,
                localIpv6 = null,
                dnsServers = emptyList(),
                interfaceName = null,
                linkDownKbps = 0,
                linkUpKbps = 0
            )
        }

        val activeNetwork = cm.activeNetwork
        val activeCaps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val vpnActive = activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        // Under a VPN the active link is the tunnel (10.x, tun0). Report the real
        // physical interface instead, otherwise LAN tools show a fake address.
        val physicalNetwork = if (vpnActive) {
            cm.allNetworks.firstOrNull { candidate ->
                val caps = cm.getNetworkCapabilities(candidate) ?: return@firstOrNull false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            }
        } else {
            activeNetwork
        }

        val network = physicalNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val linkProps = network?.let { cm.getLinkProperties(it) }

        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val transportType = when {
            caps == null -> "None"
            vpnActive -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
        val fallbackProps = activeNetwork?.let { cm.getLinkProperties(it) }
        val effectiveProps = if (linkProps?.interfaceName?.startsWith("tun") == true) fallbackProps else linkProps

        val addresses = effectiveProps?.linkAddresses?.mapNotNull { it.address }.orEmpty()
        val ipv4 = addresses.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
        val ipv6 = addresses.firstOrNull { it is Inet6Address && !it.isLoopbackAddress }?.hostAddress
        val dnsServers = effectiveProps?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty()

        return NetworkSnapshot(
            isConnected = isConnected,
            transportType = transportType,
            localIpv4 = ipv4,
            localIpv6 = ipv6,
            dnsServers = dnsServers,
            interfaceName = effectiveProps?.interfaceName,
            linkDownKbps = caps?.linkDownstreamBandwidthKbps ?: 0,
            linkUpKbps = caps?.linkUpstreamBandwidthKbps ?: 0
        )
    }

    fun suggestedProbeHost(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val candidates = cm.allNetworks.sortedBy { network ->
            val caps = cm.getNetworkCapabilities(network)
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 0
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> 1
                else -> 2
            }
        }
        candidates.forEach { network ->
            val linkProps = cm.getLinkProperties(network) ?: return@forEach
            val gateway = linkProps.routes?.firstOrNull { it.isDefaultRoute }?.gateway
            if (gateway is Inet4Address && !gateway.isAnyLocalAddress && !gateway.isLoopbackAddress) {
                return gateway.hostAddress
            }
        }
        val ipv4 = candidates.asSequence()
            .mapNotNull { cm.getLinkProperties(it) }
            .flatMap { it.linkAddresses.asSequence() }
            .mapNotNull { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isAnyLocalAddress && !it.isLoopbackAddress }
            ?.hostAddress
            ?: return null
        return ipv4.substringBeforeLast('.', missingDelimiterValue = ipv4) + ".1"
    }

    suspend fun probeTcpPort(host: String, port: Int, timeoutMs: Int = 1500): TcpProbeResult =
        withContext(Dispatchers.IO) {
            val cleanHost = host.trim()
            if (cleanHost.isEmpty() || port !in 1..65535) {
                return@withContext TcpProbeResult(
                    host = cleanHost,
                    port = port,
                    reachable = false,
                    latencyMs = 0L,
                    error = context.getString(R.string.network_error_invalid_host)
                )
            }
            val startNs = System.nanoTime()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(cleanHost, port), timeoutMs.coerceIn(100, 10_000))
                }
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
                TcpProbeResult(
                    host = cleanHost,
                    port = port,
                    reachable = true,
                    latencyMs = elapsedMs,
                    error = null
                )
            } catch (e: Exception) {
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
                Log.d(TAG, "TCP probe failed for $cleanHost:$port", e)
                TcpProbeResult(
                    host = cleanHost,
                    port = port,
                    reachable = false,
                    latencyMs = elapsedMs,
                    error = e.message ?: e::class.java.simpleName
                )
            }
        }

    suspend fun checkHttpsEndpoint(url: String): Result<Pair<Int, Long>> =
        withContext(Dispatchers.IO) {
            val trimmed = url.trim()
            if (!trimmed.startsWith("https://", ignoreCase = true)) {
                return@withContext Result.failure(
                    IllegalArgumentException(context.getString(R.string.network_error_https_only))
                )
            }
            val startNs = System.nanoTime()
            try {
                val request = Request.Builder()
                    .url(trimmed)
                    .get()
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
                    Result.success(response.code to elapsedMs)
                }
            } catch (e: Exception) {
                Log.w(TAG, "HTTPS check failed for $trimmed", e)
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "NetworkDiagnosticEngine"
    }
}

