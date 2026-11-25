package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.engine.network.IppPrintClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IppPrintClientTest {

    @Test
    fun buildPrintJobEncodesHeaderAndDocument() {
        val document = "hello".toByteArray()
        val payload = IppPrintClient.buildPrintJob(
            printerUri = "ipp://192.168.1.30:631/ipp/print",
            jobName = "Test",
            userName = "DolphyX",
            documentFormat = "text/plain",
            document = document,
        )
        assertEquals(0x02, payload[0].toInt())
        assertEquals(0x00, payload[1].toInt())
        assertEquals(0x00, payload[2].toInt())
        assertEquals(0x02, payload[3].toInt())
        assertEquals(0x01, payload[8].toInt())
        assertTrue(payload.last().toInt() == 'o'.code)
        val text = String(payload, Charsets.ISO_8859_1)
        assertTrue(text.contains("printer-uri"))
        assertTrue(text.contains("ipp://192.168.1.30:631/ipp/print"))
        assertTrue(text.contains("document-format"))
        assertTrue(text.endsWith("hello"))
    }

    @Test
    fun parsesStatusCode() {
        assertEquals(0x0000, IppPrintClient.parseStatusCode(byteArrayOf(0x02, 0x00, 0x00, 0x00)))
        assertEquals(0x0406, IppPrintClient.parseStatusCode(byteArrayOf(0x02, 0x00, 0x04, 0x06)))
        assertEquals(-1, IppPrintClient.parseStatusCode(byteArrayOf(0x02)))
    }

    @Test
    fun buildsIppUriWithDefaultPath() {
        assertEquals("ipp://10.0.0.5:631/ipp/print", IppPrintClient.ippUri("10.0.0.5", 631, ""))
        assertEquals("ipp://10.0.0.5:631/printers/lab", IppPrintClient.ippUri("10.0.0.5", 631, "printers/lab"))
    }
}
