package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.engine.ble.ChatFraming
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFramingTest {

    @Test
    fun chunksRespectLimitAndTerminateWithNewline() {
        val chunks = ChatFraming.chunk("hello world", 4)
        assertTrue(chunks.all { it.size <= 4 })
        assertEquals("hello world\n", chunks.joinToString("") { String(it, Charsets.UTF_8) })
    }

    @Test
    fun reassemblesSplitUtf8Messages() {
        val text = "merhaba dünya · dolphin"
        val chunks = ChatFraming.chunk(text, 5)
        val accumulator = ByteArrayOutputStream()
        val received = chunks.flatMap { ChatFraming.append(accumulator, it) }
        assertEquals(1, received.size)
        assertEquals(text, received[0])
    }

    @Test
    fun keepsPartialMessageUntilNewline() {
        val accumulator = ByteArrayOutputStream()
        assertTrue(ChatFraming.append(accumulator, "par".toByteArray()).isEmpty())
        assertEquals(listOf("partial"), ChatFraming.append(accumulator, "tial\n".toByteArray()))
    }
}
