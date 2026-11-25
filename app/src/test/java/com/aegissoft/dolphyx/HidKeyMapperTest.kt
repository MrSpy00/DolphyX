package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.engine.hid.HidKeyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HidKeyMapperTest {

    @Test
    fun lettersUseCorrectUsageAndShift() {
        assertEquals(HidKeyMapper.KeyStroke(0, 0x04), HidKeyMapper.map('a'))
        assertEquals(HidKeyMapper.KeyStroke(0x02, 0x04), HidKeyMapper.map('A'))
        assertEquals(HidKeyMapper.KeyStroke(0, 0x1D), HidKeyMapper.map('z'))
    }

    @Test
    fun digitsAndSpaceAreMapped() {
        assertEquals(HidKeyMapper.KeyStroke(0, 0x1E), HidKeyMapper.map('1'))
        assertEquals(HidKeyMapper.KeyStroke(0, 0x27), HidKeyMapper.map('0'))
        assertEquals(HidKeyMapper.KeyStroke(0, 0x2C), HidKeyMapper.map(' '))
        assertEquals(HidKeyMapper.KeyStroke(0, 0x28), HidKeyMapper.map('\n'))
    }

    @Test
    fun punctuationUsesShiftWhereRequired() {
        assertEquals(HidKeyMapper.KeyStroke(0, 0x36), HidKeyMapper.map(','))
        assertEquals(HidKeyMapper.KeyStroke(0x02, 0x36), HidKeyMapper.map('<'))
        assertEquals(HidKeyMapper.KeyStroke(0x02, 0x1E), HidKeyMapper.map('!'))
    }

    @Test
    fun unsupportedCharactersAreRejected() {
        assertNull(HidKeyMapper.map('ç'))
        assertNull(HidKeyMapper.map('€'))
    }
}
