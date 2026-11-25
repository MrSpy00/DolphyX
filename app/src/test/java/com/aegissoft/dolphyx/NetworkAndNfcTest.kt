package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.engine.network.QrMatrixGenerator
import com.aegissoft.dolphyx.engine.network.SecureLocalQrServer
import com.aegissoft.dolphyx.engine.nfc.DolphyXHceService
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class NetworkAndNfcTest {

    @Test
    fun sanitizePathSegment_acceptsSafeAndRejectsTraversal() {
        assertEquals("report_1.txt", SecureLocalQrServer.sanitizePathSegment("report_1.txt"))
        assertNull(SecureLocalQrServer.sanitizePathSegment(".."))
        assertNull(SecureLocalQrServer.sanitizePathSegment("../secret"))
        assertNull(SecureLocalQrServer.sanitizePathSegment("a/b"))
        assertNull(SecureLocalQrServer.sanitizePathSegment("a\\b"))
        assertNull(SecureLocalQrServer.sanitizePathSegment(""))
        assertNull(SecureLocalQrServer.sanitizePathSegment("bad\u0000name"))
    }

    @Test
    fun verifyToken_constantTimeChecks() {
        val expected = "0123456789abcdef0123456789abcdef"
        assertTrue(SecureLocalQrServer.verifyToken(expected, expected))
        assertFalse(SecureLocalQrServer.verifyToken("0123456789abcdef0123456789abcde0", expected))
        assertFalse(SecureLocalQrServer.verifyToken(null, expected))
        assertFalse(SecureLocalQrServer.verifyToken("", expected))
        assertFalse(SecureLocalQrServer.verifyToken("abc", ""))
    }

    @Test
    fun qrMatrixGenerator_producesValidDeterministicMatrix() {
        val url = "http://127.0.0.1:8080/share?token=abcdef123456"
        val matrix = QrMatrixGenerator.generate(url)
        val matrix2 = QrMatrixGenerator.generate(url)

        assertTrue(matrix.size >= 21)
        assertEquals(1, matrix.size % 4)
        for (row in matrix) {
            assertEquals(matrix.size, row.size)
        }
        for (i in matrix.indices) {
            assertArrayEquals(matrix[i], matrix2[i])
        }

        // Verify 7x7 finder pattern border at (0, 0) is dark
        for (i in 0..6) {
            assertTrue(matrix[0][i])
            assertTrue(matrix[6][i])
            assertTrue(matrix[i][0])
            assertTrue(matrix[i][6])
        }
        // Verify inner 3x3 finder core at (2..4, 2..4) is dark
        for (r in 2..4) {
            for (c in 2..4) {
                assertTrue(matrix[r][c])
            }
        }
    }

    @Test
    fun buildNdefTextMessage_producesValidType4NdefRecord() {
        val msg = DolphyXHceService.buildNdefTextMessage("DolphyX")
        assertTrue(msg.size > 6)

        val nlen = ((msg[0].toInt() and 0xFF) shl 8) or (msg[1].toInt() and 0xFF)
        assertEquals(msg.size - 2, nlen)

        // Short NDEF Text record: TNF=0xD1, TypeLen=1, Type='T'(0x54), Status=0x02 ("en")
        assertEquals(0xD1.toByte(), msg[2])
        assertEquals(0x01.toByte(), msg[3])
        assertEquals(0x54.toByte(), msg[5])
        assertEquals(0x02.toByte(), msg[6])

        val decoded = String(msg, 2, msg.size - 2, StandardCharsets.UTF_8)
        assertTrue(decoded.contains("DolphyX"))
    }
}
