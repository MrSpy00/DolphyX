package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.engine.ir.IrFileImporter
import org.junit.Assert.assertEquals
import org.junit.Test

class IrFileImporterTest {

    @Test
    fun parsesParsedAndRawSignals() {
        val content = """
            Filetype: IR signals file
            Version: 1
            #
            name: Power
            type: parsed
            protocol: NEC
            address: 04 00 00 00
            command: 08 00 00 00
            frequency: 38000
            duty_cycle: 0.330000
            #
            name: Volume_up
            type: raw
            frequency: 40000
            duty_cycle: 0.330000
            data: 9000 4500 560 560 560 1690
            #
        """.trimIndent()

        val signals = IrFileImporter.parse(content)
        assertEquals(2, signals.size)
        assertEquals("Power", signals[0].name)
        assertEquals("NEC", signals[0].protocol)
        assertEquals("04 00 00 00", signals[0].address)
        assertEquals("08 00 00 00", signals[0].command)
        assertEquals(38000, signals[0].frequency)
        assertEquals("RAW", signals[1].protocol)
        assertEquals("9000 4500 560 560 560 1690", signals[1].command)
        assertEquals(40000, signals[1].frequency)
    }

    @Test
    fun ignoresIncompleteSignals() {
        val content = """
            name: Broken
            type: parsed
            protocol: NEC
            #
            name: RawMissingData
            type: raw
            #
        """.trimIndent()
        assertEquals(0, IrFileImporter.parse(content).size)
    }
}
