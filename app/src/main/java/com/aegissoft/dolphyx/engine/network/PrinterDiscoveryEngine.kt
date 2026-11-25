package com.aegissoft.dolphyx.engine.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.aegissoft.dolphyx.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredPrinter(
    val name: String,
    val host: String,
    val port: Int,
    val serviceType: String,
    val path: String,
) {
    val httpUrl: String get() = "${if (serviceType.startsWith("_ipps")) "https" else "http"}://$host:$port$path"
    val ippUri: String get() = IppPrintClient.ippUri(host, port, path)
}

/**
 * mDNS/DNS-SD discovery for IPP printers. Discovery only; no print job is sent here.
 */
class PrinterDiscoveryEngine(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val resolved = linkedSetOf<String>()
    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

    private val _printers = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val printers: StateFlow<List<DiscoveredPrinter>> = _printers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun start() {
        val manager = nsdManager ?: run {
            _status.value = appContext.getString(R.string.nsd_status_unavailable)
            return
        }
        if (_isDiscovering.value) return
        _printers.value = emptyList()
        resolved.clear()
        _isDiscovering.value = true
        SERVICE_TYPES.forEach { type ->
            val listener = createListener(manager, type)
            activeListeners.add(listener)
            try {
                manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                _status.value = e.message
            }
        }
    }

    fun stop() {
        val manager = nsdManager ?: return
        activeListeners.forEach { listener ->
            runCatching { manager.stopServiceDiscovery(listener) }
        }
        activeListeners.clear()
        _isDiscovering.value = false
    }

    private fun createListener(manager: NsdManager, serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                _status.value = appContext.getString(R.string.nsd_status_searching)
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != serviceType) return
                val key = "${serviceInfo.serviceName}|${serviceInfo.serviceType}"
                if (!resolved.add(key)) return
                try {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            _status.value = appContext.getString(R.string.nsd_status_resolve_failed, errorCode)
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            val resourcePath = info.attributes["rp"]
                                ?.toString(Charsets.UTF_8)
                                ?.trim()
                                .orEmpty()
                                .ifBlank { IppPrintClient.DEFAULT_PATH }
                            val printer = DiscoveredPrinter(
                                name = info.serviceName,
                                host = host,
                                port = info.port,
                                serviceType = info.serviceType,
                                path = if (resourcePath.startsWith("/")) resourcePath else "/$resourcePath",
                            )
                            _printers.value = (_printers.value + printer)
                                .distinctBy { "${it.host}:${it.port}" }
                        }
                    })
                } catch (e: Exception) {
                    _status.value = e.message
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _printers.value = _printers.value.filterNot { it.name == serviceInfo.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscovering.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _status.value = appContext.getString(R.string.nsd_status_discovery_failed, errorCode)
                _isDiscovering.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _status.value = appContext.getString(R.string.nsd_status_stop_failed, errorCode)
            }
        }
    }

    companion object {
        private val SERVICE_TYPES = listOf("_ipp._tcp", "_ipps._tcp", "_printer._tcp")
    }
}
