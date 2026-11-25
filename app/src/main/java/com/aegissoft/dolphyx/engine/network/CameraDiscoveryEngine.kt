package com.aegissoft.dolphyx.engine.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.aegissoft.dolphyx.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredCamera(
    val name: String,
    val host: String,
    val port: Int,
    val serviceType: String,
) {
    val rtspUrl: String get() = "rtsp://$host:$port/"
}

/** mDNS discovery for network cameras that advertise RTSP/ONVIF services. */
class CameraDiscoveryEngine(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val resolved = linkedSetOf<String>()
    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()

    private val _cameras = MutableStateFlow<List<DiscoveredCamera>>(emptyList())
    val cameras: StateFlow<List<DiscoveredCamera>> = _cameras.asStateFlow()

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
        _cameras.value = emptyList()
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
        activeListeners.forEach { listener -> runCatching { manager.stopServiceDiscovery(listener) } }
        activeListeners.clear()
        _isDiscovering.value = false
    }

    private fun createListener(manager: NsdManager, serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                _status.value = appContext.getString(R.string.nsd_status_searching)
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val key = "${serviceInfo.serviceName}|${serviceInfo.serviceType}"
                if (!resolved.add(key)) return
                try {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            _status.value = appContext.getString(R.string.nsd_status_resolve_failed, errorCode)
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            val camera = DiscoveredCamera(
                                name = info.serviceName,
                                host = host,
                                port = info.port,
                                serviceType = info.serviceType,
                            )
                            _cameras.value = (_cameras.value + camera).distinctBy { "${it.host}:${it.port}" }
                        }
                    })
                } catch (e: Exception) {
                    _status.value = e.message
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _cameras.value = _cameras.value.filterNot {
                    it.name == serviceInfo.serviceName && it.serviceType == serviceInfo.serviceType
                }
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
        private val SERVICE_TYPES = listOf(
            "_rtsp._tcp",
            "_onvif._tcp",
            "_axis-video._tcp",
            "_dahua._tcp",
        )
    }
}
