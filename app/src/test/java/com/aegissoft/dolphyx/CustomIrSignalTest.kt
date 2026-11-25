package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.data.IrSignal
import com.aegissoft.dolphyx.engine.ir.IrProtocolEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CustomIrSignalTest {

    @Test
    fun customNecSignalEncodes() {
        val signal = IrSignal(
            id = 0,
            remoteId = 0,
            name = "Test power",
            sigType = "parsed",
            protocol = "NEC",
            address = "04",
            command = "08",
            frequency = 38000,
            dutyCycle = 0.33,
            rawData = null,
        )
        val pattern = IrProtocolEncoder.encodeSignal(signal)
        assertNotNull(pattern)
        assertEquals(38000, pattern!!.frequencyHz)
        assertEquals(67, pattern.patternMicros.size)
    }

    @Test
    fun customRawSignalEncodes() {
        val signal = IrSignal(
            id = 0,
            remoteId = 0,
            name = "Raw",
            sigType = "raw",
            protocol = null,
            address = null,
            command = null,
            frequency = 40000,
            dutyCycle = 0.5,
            rawData = "9000 4500 560 560 560 1690",
        )
        val pattern = IrProtocolEncoder.encodeSignal(signal)
        assertNotNull(pattern)
        assertEquals(40000, pattern!!.frequencyHz)
        assertEquals(6, pattern.patternMicros.size)
    }
}
