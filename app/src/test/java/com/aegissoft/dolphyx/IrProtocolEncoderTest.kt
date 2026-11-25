package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.data.IrSignal
import com.aegissoft.dolphyx.engine.ir.IrProtocolEncoder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrProtocolEncoderTest {

    private fun makeSignal(
        sigType: String,
        protocol: String? = null,
        address: String? = null,
        command: String? = null,
        frequency: Int? = null,
        rawData: String? = null,
    ): IrSignal = IrSignal(
        id = 1L,
        remoteId = 1L,
        name = "POWER",
        sigType = sigType,
        protocol = protocol,
        address = address,
        command = command,
        frequency = frequency,
        dutyCycle = 0.33,
        rawData = rawData,
    )

    @Test
    fun rawSignal_valid_returnsPatternAndFrequency() {
        val pattern = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "raw", frequency = 38000, rawData = "9000 4500 562 562 562 1687 562"),
        )
        assertNotNull(pattern)
        assertEquals(38000, pattern!!.frequencyHz)
        assertArrayEquals(intArrayOf(9000, 4500, 562, 562, 562, 1687, 562), pattern.patternMicros)
    }

    @Test
    fun rawSignal_emptyOrInvalid_returnsNull() {
        assertNull(IrProtocolEncoder.encodeSignal(makeSignal(sigType = "raw", rawData = "")))
        assertNull(IrProtocolEncoder.encodeSignal(makeSignal(sigType = "raw", rawData = "9000 abc 562")))
        assertNull(IrProtocolEncoder.encodeSignal(makeSignal(sigType = "raw", rawData = "9000 0 562")))
    }

    @Test
    fun parsedNec_encodes38kHzLeaderAnd67Pulses() {
        val pattern = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "NEC", address = "20 00 00 00", command = "02 00 00 00"),
        )
        assertNotNull(pattern)
        assertEquals(38000, pattern!!.frequencyHz)
        assertEquals(67, pattern.patternMicros.size)
        assertEquals(9000, pattern.patternMicros[0])
        assertEquals(4500, pattern.patternMicros[1])
        assertTrue(pattern.patternMicros.all { it > 0 })
    }

    @Test
    fun parsedNecExt_encodes38kHz() {
        val pattern = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "NECext", address = "04 FB 00 00", command = "08 F7 00 00"),
        )
        assertNotNull(pattern)
        assertEquals(38000, pattern!!.frequencyHz)
        assertEquals(67, pattern.patternMicros.size)
        assertTrue(pattern.patternMicros.all { it > 0 })
    }

    @Test
    fun parsedSamsung32_encodes38kHzLeader4500() {
        val pattern = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "Samsung32", address = "07 07 00 00", command = "02 FD 00 00"),
        )
        assertNotNull(pattern)
        assertEquals(38000, pattern!!.frequencyHz)
        assertEquals(67, pattern.patternMicros.size)
        assertEquals(4500, pattern.patternMicros[0])
        assertEquals(4500, pattern.patternMicros[1])
        assertTrue(pattern.patternMicros.all { it > 0 })
    }

    @Test
    fun parsedSirc_encodes40kHzLeader2400() {
        val pattern = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "SIRC", address = "01 00 00 00", command = "15 00 00 00"),
        )
        assertNotNull(pattern)
        assertEquals(40000, pattern!!.frequencyHz)
        assertEquals(26, pattern.patternMicros.size)
        assertEquals(2400, pattern.patternMicros[0])
        assertEquals(600, pattern.patternMicros[1])
        assertTrue(pattern.patternMicros.all { it > 0 })
    }

    @Test
    fun parsedRc5AndRc6_encode36kHzPositivePulses() {
        val rc5 = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "RC5", address = "00 00 00 00", command = "0C 00 00 00"),
        )
        assertNotNull(rc5)
        assertEquals(36000, rc5!!.frequencyHz)
        assertTrue(rc5.patternMicros.isNotEmpty())
        assertTrue(rc5.patternMicros.all { it > 0 })

        val rc6 = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "RC6", address = "00 00 00 00", command = "0C 00 00 00"),
        )
        assertNotNull(rc6)
        assertEquals(36000, rc6!!.frequencyHz)
        assertTrue(rc6.patternMicros.isNotEmpty())
        assertTrue(rc6.patternMicros.all { it > 0 })
    }

    @Test
    fun unknownProtocol_returnsNull() {
        val pattern = IrProtocolEncoder.encodeSignal(
            makeSignal(sigType = "parsed", protocol = "UNKNOWN_PROTO", address = "01", command = "02"),
        )
        assertNull(pattern)
    }
}
