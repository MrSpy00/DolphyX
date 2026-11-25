package com.aegissoft.dolphyx.engine.ir

import com.aegissoft.dolphyx.data.IrSignal
import java.util.Locale

data class CarrierPattern(
    val frequencyHz: Int,
    val patternMicros: IntArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarrierPattern) return false
        return frequencyHz == other.frequencyHz && patternMicros.contentEquals(other.patternMicros)
    }

    override fun hashCode(): Int {
        var result = frequencyHz
        result = 31 * result + patternMicros.contentHashCode()
        return result
    }
}

object IrProtocolEncoder {

    fun encodeSignal(signal: IrSignal): CarrierPattern? {
        return when (signal.sigType.lowercase(Locale.ROOT)) {
            "raw" -> encodeRaw(signal)
            "parsed" -> encodeParsed(signal)
            else -> null
        }
    }

    private fun encodeRaw(signal: IrSignal): CarrierPattern? {
        val raw = signal.rawData?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parts = raw.split(Regex("\\s+"))
        val values = ArrayList<Int>(parts.size)
        for (part in parts) {
            val parsed = part.toIntOrNull() ?: return null
            if (parsed <= 0) return null
            values.add(parsed)
        }
        if (values.isEmpty()) return null
        val freq = signal.frequency?.takeIf { it > 0 } ?: 38000
        return CarrierPattern(freq, values.toIntArray())
    }

    private fun encodeParsed(signal: IrSignal): CarrierPattern? {
        val protocol = signal.protocol?.trim()?.uppercase(Locale.ROOT) ?: return null
        val addrBytes = parseHexBytes(signal.address)
        val cmdBytes = parseHexBytes(signal.command)
        if (addrBytes.isEmpty() || cmdBytes.isEmpty()) return null

        return when (protocol) {
            "NEC" -> encodeNec(addrBytes[0], addrBytes[0].inv() and 0xFF, cmdBytes[0], cmdBytes[0].inv() and 0xFF)
            "NECEXT" -> encodeNec(
                addrBytes[0],
                addrBytes.getOrElse(1) { 0 },
                cmdBytes[0],
                cmdBytes.getOrElse(1) { 0 },
            )
            "NEC42" -> encodeNec42(addrBytes, cmdBytes)
            "SAMSUNG32" -> encodeSamsung32(addrBytes, cmdBytes)
            "SIRC" -> encodeSirc(cmdBytes[0], addrBytes[0], totalBits = 12)
            "SIRC15" -> encodeSirc(cmdBytes[0], addrBytes[0], totalBits = 15)
            "SIRC20" -> encodeSirc20(cmdBytes[0], addrBytes)
            "RC5" -> encodeRc5(addrBytes[0], cmdBytes[0], extended = false)
            "RC5X" -> encodeRc5(addrBytes[0], cmdBytes[0], extended = true)
            "RC6" -> encodeRc6(addrBytes[0], cmdBytes[0])
            else -> null
        }
    }

    private fun parseHexBytes(hexStr: String?): IntArray {
        val clean = hexStr?.trim().orEmpty()
        if (clean.isEmpty()) return IntArray(0)
        val tokens = clean.split(Regex("\\s+"))
        val out = IntArray(tokens.size)
        for (i in tokens.indices) {
            val v = tokens[i].toIntOrNull(16) ?: return IntArray(0)
            out[i] = v and 0xFF
        }
        return out
    }

    private fun encodeNec(a0: Int, a1: Int, c0: Int, c1: Int): CarrierPattern {
        val pattern = ArrayList<Int>(68)
        pattern.add(9000)
        pattern.add(4500)
        appendLsbByte(pattern, a0, 562, 562, 1687)
        appendLsbByte(pattern, a1, 562, 562, 1687)
        appendLsbByte(pattern, c0, 562, 562, 1687)
        appendLsbByte(pattern, c1, 562, 562, 1687)
        pattern.add(562)
        return CarrierPattern(38000, pattern.toIntArray())
    }

    private fun encodeNec42(addrBytes: IntArray, cmdBytes: IntArray): CarrierPattern {
        val addr13 = (addrBytes[0] or (addrBytes.getOrElse(1) { 0 } shl 8)) and 0x1FFF
        val addrInv13 = addr13.inv() and 0x1FFF
        val cmd8 = cmdBytes[0] and 0xFF
        val cmdInv8 = cmd8.inv() and 0xFF
        val pattern = ArrayList<Int>(88)
        pattern.add(9000)
        pattern.add(4500)
        appendLsbBits(pattern, addr13, 13, 562, 562, 1687)
        appendLsbBits(pattern, addrInv13, 13, 562, 562, 1687)
        appendLsbBits(pattern, cmd8, 8, 562, 562, 1687)
        appendLsbBits(pattern, cmdInv8, 8, 562, 562, 1687)
        pattern.add(562)
        return CarrierPattern(38000, pattern.toIntArray())
    }

    private fun encodeSamsung32(addrBytes: IntArray, cmdBytes: IntArray): CarrierPattern {
        val a0 = addrBytes[0]
        val a1 = addrBytes.getOrElse(1) { a0 }
        val c0 = cmdBytes[0]
        val c1 = cmdBytes.getOrElse(1) { c0.inv() and 0xFF }
        val pattern = ArrayList<Int>(68)
        pattern.add(4500)
        pattern.add(4500)
        appendLsbByte(pattern, a0, 550, 550, 1650)
        appendLsbByte(pattern, a1, 550, 550, 1650)
        appendLsbByte(pattern, c0, 550, 550, 1650)
        appendLsbByte(pattern, c1, 550, 550, 1650)
        pattern.add(550)
        return CarrierPattern(38000, pattern.toIntArray())
    }

    private fun encodeSirc(cmd: Int, addr: Int, totalBits: Int): CarrierPattern {
        val addrBits = totalBits - 7
        val pattern = ArrayList<Int>(2 + totalBits * 2)
        pattern.add(2400)
        pattern.add(600)
        for (i in 0 until 7) {
            val bit = (cmd ushr i) and 1
            pattern.add(if (bit == 1) 1200 else 600)
            pattern.add(600)
        }
        for (i in 0 until addrBits) {
            val bit = (addr ushr i) and 1
            pattern.add(if (bit == 1) 1200 else 600)
            pattern.add(600)
        }
        return CarrierPattern(40000, pattern.toIntArray())
    }

    private fun encodeSirc20(cmd: Int, addrBytes: IntArray): CarrierPattern {
        val addr13 = (addrBytes[0] or (addrBytes.getOrElse(1) { 0 } shl 8)) and 0x1FFF
        val pattern = ArrayList<Int>(42)
        pattern.add(2400)
        pattern.add(600)
        for (i in 0 until 7) {
            val bit = (cmd ushr i) and 1
            pattern.add(if (bit == 1) 1200 else 600)
            pattern.add(600)
        }
        for (i in 0 until 13) {
            val bit = (addr13 ushr i) and 1
            pattern.add(if (bit == 1) 1200 else 600)
            pattern.add(600)
        }
        return CarrierPattern(40000, pattern.toIntArray())
    }

    private fun encodeRc5(addr: Int, cmd: Int, extended: Boolean): CarrierPattern {
        val halfBit = 889
        val s1 = 1
        val s2 = if (extended && (cmd and 0x40) != 0) 0 else 1
        val toggle = 0
        val frame = (s1 shl 13) or (s2 shl 12) or (toggle shl 11) or ((addr and 0x1F) shl 6) or (cmd and 0x3F)
        val levels = ArrayList<Pair<Boolean, Int>>(28)
        for (i in 13 downTo 0) {
            val bit = (frame ushr i) and 1
            if (bit == 1) {
                levels.add(false to halfBit)
                levels.add(true to halfBit)
            } else {
                levels.add(true to halfBit)
                levels.add(false to halfBit)
            }
        }
        return CarrierPattern(36000, mergeLevelsToPattern(levels))
    }

    private fun encodeRc6(addr: Int, cmd: Int): CarrierPattern {
        val halfBit = 444
        val levels = ArrayList<Pair<Boolean, Int>>(48)
        levels.add(true to 2666)
        levels.add(false to 889)

        fun appendRc6Bit(bit: Int, width: Int) {
            if (bit == 1) {
                levels.add(true to width)
                levels.add(false to width)
            } else {
                levels.add(false to width)
                levels.add(true to width)
            }
        }

        appendRc6Bit(1, halfBit) // Start bit
        appendRc6Bit(0, halfBit) // Mode bit 2
        appendRc6Bit(0, halfBit) // Mode bit 1
        appendRc6Bit(0, halfBit) // Mode bit 0
        appendRc6Bit(0, halfBit * 2) // Trailer bit (double width)

        val payload = ((addr and 0xFF) shl 8) or (cmd and 0xFF)
        for (i in 15 downTo 0) {
            appendRc6Bit((payload ushr i) and 1, halfBit)
        }
        return CarrierPattern(36000, mergeLevelsToPattern(levels))
    }

    private fun mergeLevelsToPattern(levels: List<Pair<Boolean, Int>>): IntArray {
        val merged = ArrayList<Pair<Boolean, Int>>(levels.size)
        for (entry in levels) {
            if (merged.isEmpty()) {
                if (!entry.first) continue
                merged.add(entry)
            } else {
                val lastIdx = merged.lastIndex
                val last = merged[lastIdx]
                if (last.first == entry.first) {
                    merged[lastIdx] = last.first to (last.second + entry.second)
                } else {
                    merged.add(entry)
                }
            }
        }
        if (merged.isNotEmpty() && !merged.last().first) {
            merged.removeAt(merged.lastIndex)
        }
        return IntArray(merged.size) { idx -> merged[idx].second }
    }

    private fun appendLsbByte(
        out: MutableList<Int>,
        value: Int,
        markUs: Int,
        zeroSpaceUs: Int,
        oneSpaceUs: Int,
    ) {
        appendLsbBits(out, value, 8, markUs, zeroSpaceUs, oneSpaceUs)
    }

    private fun appendLsbBits(
        out: MutableList<Int>,
        value: Int,
        bitCount: Int,
        markUs: Int,
        zeroSpaceUs: Int,
        oneSpaceUs: Int,
    ) {
        for (i in 0 until bitCount) {
            val bit = (value ushr i) and 1
            out.add(markUs)
            out.add(if (bit == 1) oneSpaceUs else zeroSpaceUs)
        }
    }
}
