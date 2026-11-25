package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.engine.cast.DlnaCastEngine
import com.aegissoft.dolphyx.engine.cast.LocalMediaServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CastEngineTest {

    @Test
    fun parsesSuffixAndOpenRanges() {
        assertEquals(0L to 99L, LocalMediaServer.parseRange("bytes=0-99", 1000))
        assertEquals(900L to 999L, LocalMediaServer.parseRange("bytes=-100", 1000))
        assertEquals(500L to 999L, LocalMediaServer.parseRange("bytes=500-", 1000))
    }

    @Test
    fun rejectsInvalidRanges() {
        assertNull(LocalMediaServer.parseRange(null, 1000))
        assertNull(LocalMediaServer.parseRange("bytes=1000-1200", 1000))
        assertNull(LocalMediaServer.parseRange("bytes=abc-def", 1000))
    }

    @Test
    fun extractsSsdpLocation() {
        val message = "HTTP/1.1 200 OK\r\nST: upnp:rootdevice\r\nLOCATION: http://192.168.1.20:49152/desc.xml\r\n\r\n"
        assertEquals("http://192.168.1.20:49152/desc.xml", DlnaCastEngine.parseLocation(message))
        assertNull(DlnaCastEngine.parseLocation("HTTP/1.1 200 OK\r\n\r\n"))
    }

    @Test
    fun resolvesAvTransportControlUrl() {
        val xml = """
            <root>
              <device>
                <friendlyName>Salon TV</friendlyName>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                    <controlURL>/upnp/control/avt</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()
        val (name, control) = DlnaCastEngine.parseDeviceDescription(xml, "http://192.168.1.20:49152/desc.xml")
        assertEquals("Salon TV", name)
        assertEquals("http://192.168.1.20:49152/upnp/control/avt", control)
    }
}
