package com.aegissoft.dolphyx.engine.cast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class DlnaDevice(
    val name: String,
    val location: String,
    val controlUrl: String,
)

/**
 * Minimal DLNA/UPnP MediaRenderer client: SSDP discovery, device description parsing
 * and AVTransport SetAVTransportURI / Play / Stop SOAP calls.
 */
class DlnaCastEngine {

    suspend fun discover(timeoutMs: Int = SSDP_TIMEOUT_MS): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val locations = linkedSetOf<String>()
        val search = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: $SSDP_MX\r\n")
            append("ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n")
            append("\r\n")
        }.toByteArray()

        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = SSDP_SOCKET_TIMEOUT_MS
                val address = InetAddress.getByName(SSDP_ADDRESS)
                repeat(SSDP_PROBES) {
                    socket.send(DatagramPacket(search, search.size, address, SSDP_PORT))
                }
                val deadline = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(4096)
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val message = String(packet.data, 0, packet.length)
                        parseLocation(message)?.let { locations += it }
                    } catch (_: Exception) {
                        // timeout slice; keep waiting until the deadline
                    }
                }
            }
        }

        locations.mapNotNull { location ->
            runCatching {
                val xml = fetchText(location)
                val (name, control) = parseDeviceDescription(xml, location)
                if (control == null) null else DlnaDevice(name = name, location = location, controlUrl = control)
            }.getOrNull()
        }.distinctBy { it.controlUrl }
    }

    suspend fun setAvTransportUri(device: DlnaDevice, mediaUrl: String): Boolean = withContext(Dispatchers.IO) {
        val escaped = mediaUrl.replace("&", "&amp;")
        val body = soapEnvelope(
            service = SERVICE,
            action = "SetAVTransportURI",
            inner = "<InstanceID>0</InstanceID><CurrentURI>$escaped</CurrentURI>" +
                "<CurrentURIMetaData></CurrentURIMetaData>",
        )
        soap(device.controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI", body)
    }

    suspend fun play(device: DlnaDevice): Boolean = withContext(Dispatchers.IO) {
        val body = soapEnvelope(
            service = SERVICE,
            action = "Play",
            inner = "<InstanceID>0</InstanceID><Speed>1</Speed>",
        )
        soap(device.controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#Play", body)
    }

    suspend fun stop(device: DlnaDevice): Boolean = withContext(Dispatchers.IO) {
        val body = soapEnvelope(
            service = SERVICE,
            action = "Stop",
            inner = "<InstanceID>0</InstanceID>",
        )
        soap(device.controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#Stop", body)
    }

    private fun soap(controlUrl: String, soapAction: String, body: String): Boolean {
        val connection = URL(controlUrl).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = HTTP_CONNECT_TIMEOUT_MS
            connection.readTimeout = HTTP_READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            connection.setRequestProperty("SOAPACTION", "\"$soapAction\"")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = HTTP_CONNECT_TIMEOUT_MS
            connection.readTimeout = HTTP_READ_TIMEOUT_MS
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_MX = 2
        private const val SSDP_PROBES = 2
        private const val SSDP_TIMEOUT_MS = 2500
        private const val SSDP_SOCKET_TIMEOUT_MS = 500
        private const val HTTP_CONNECT_TIMEOUT_MS = 4000
        private const val HTTP_READ_TIMEOUT_MS = 5000

        private fun soapEnvelope(service: String, action: String, inner: String): String =
            """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <s:Body>
    <u:$action xmlns:u="$service">$inner</u:$action>
  </s:Body>
</s:Envelope>"""

        /** Extracts the LOCATION header from an SSDP response. */
        fun parseLocation(message: String): String? {
            return message.lineSequence()
                .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        }

        /** Returns the friendly name and the AVTransport control URL resolved against [baseUrl]. */
        fun parseDeviceDescription(xml: String, baseUrl: String): Pair<String, String?> {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(StringReader(xml)))
            val friendlyName = document.getElementsByTagName("friendlyName")
                .item(0)?.textContent?.trim().orEmpty().ifBlank { "DLNA renderer" }

            var control: String? = null
            val services = document.getElementsByTagName("service")
            for (i in 0 until services.length) {
                val service = services.item(i) as? Element ?: continue
                val type = service.getElementsByTagName("serviceType").item(0)?.textContent.orEmpty()
                if (!type.contains("AVTransport")) continue
                val raw = service.getElementsByTagName("controlURL").item(0)?.textContent?.trim().orEmpty()
                if (raw.isNotEmpty()) {
                    control = runCatching { URI(baseUrl).resolve(raw).toString() }.getOrDefault(raw)
                    break
                }
            }
            return friendlyName to control
        }
    }
}
