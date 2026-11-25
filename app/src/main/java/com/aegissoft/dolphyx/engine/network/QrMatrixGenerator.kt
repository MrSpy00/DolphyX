package com.aegissoft.dolphyx.engine.network

import java.nio.charset.StandardCharsets

object QrMatrixGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignmentCenters: IntArray
    )

    private val VERSIONS = listOf(
        VersionSpec(1, 21, 19, 7, 1, intArrayOf()),
        VersionSpec(2, 25, 34, 10, 1, intArrayOf(6, 18)),
        VersionSpec(3, 29, 55, 15, 1, intArrayOf(6, 22)),
        VersionSpec(4, 33, 80, 20, 1, intArrayOf(6, 26)),
        VersionSpec(5, 37, 108, 26, 1, intArrayOf(6, 30)),
        VersionSpec(6, 41, 136, 18, 2, intArrayOf(6, 34))
    )

    // Level L (01) + Mask 0 (000) -> 15-bit BCH(15,5) masked with 0x5412 = 0x77C4
    private const val FORMAT_BITS_L_MASK0 = 0x77C4

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    fun generate(content: String): Array<BooleanArray> {
        val rawBytes = content.toByteArray(StandardCharsets.UTF_8)
        val spec = VERSIONS.firstOrNull { rawBytes.size <= it.totalDataCodewords - 2 }
            ?: throw IllegalArgumentException("Content too long (${rawBytes.size} bytes) for QR Version 1-6 Level L")

        val dataCodewords = encodeByteMode(rawBytes, spec.totalDataCodewords)
        val finalBitStream = buildFinalBitStream(dataCodewords, spec)

        val size = spec.size
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        placeFinderPattern(modules, isFunction, 0, 0)
        placeFinderPattern(modules, isFunction, size - 7, 0)
        placeFinderPattern(modules, isFunction, 0, size - 7)
        placeTimingPatterns(modules, isFunction, size)
        placeAlignmentPatterns(modules, isFunction, spec.alignmentCenters)
        reserveFormatAndDarkModule(modules, isFunction, size)

        placeDataBits(modules, isFunction, size, finalBitStream)
        applyMask0(modules, isFunction, size)
        writeFormatBits(modules, size)

        return modules
    }

    private fun encodeByteMode(data: ByteArray, capacityBytes: Int): IntArray {
        val bits = mutableListOf<Boolean>()
        appendBits(bits, 0b0100, 4)
        appendBits(bits, data.size, 8)
        for (b in data) {
            appendBits(bits, b.toInt() and 0xFF, 8)
        }
        val totalBits = capacityBytes * 8
        val terminator = minOf(4, totalBits - bits.size)
        repeat(terminator) { bits.add(false) }
        while (bits.size % 8 != 0) {
            bits.add(false)
        }
        val out = IntArray(capacityBytes)
        val byteCount = bits.size / 8
        for (i in 0 until byteCount) {
            var v = 0
            for (bit in 0 until 8) {
                v = (v shl 1) or if (bits[i * 8 + bit]) 1 else 0
            }
            out[i] = v
        }
        var padIndex = 0
        for (i in byteCount until capacityBytes) {
            out[i] = if (padIndex % 2 == 0) 0xEC else 0x11
            padIndex++
        }
        return out
    }

    private fun buildFinalBitStream(dataCodewords: IntArray, spec: VersionSpec): BooleanArray {
        val blocks = spec.numBlocks
        val blockDataLen = dataCodewords.size / blocks
        val ecLen = spec.ecCodewordsPerBlock
        val dataBlocks = Array(blocks) { b ->
            dataCodewords.copyOfRange(b * blockDataLen, (b + 1) * blockDataLen)
        }
        val ecBlocks = Array(blocks) { b ->
            computeReedSolomon(dataBlocks[b], ecLen)
        }

        val interleaved = mutableListOf<Int>()
        for (i in 0 until blockDataLen) {
            for (b in 0 until blocks) {
                interleaved += dataBlocks[b][i]
            }
        }
        for (i in 0 until ecLen) {
            for (b in 0 until blocks) {
                interleaved += ecBlocks[b][i]
            }
        }

        val remainderBits = if (spec.version in 2..6) 7 else 0
        val out = BooleanArray(interleaved.size * 8 + remainderBits)
        var idx = 0
        for (cw in interleaved) {
            for (bit in 7 downTo 0) {
                out[idx++] = ((cw ushr bit) and 1) == 1
            }
        }
        return out
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        val gen = generatorPoly(ecCount)
        val rem = IntArray(ecCount)
        for (d in data) {
            val factor = d xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecCount - 1)
            rem[ecCount - 1] = 0
            if (factor != 0) {
                for (i in 0 until ecCount) {
                    rem[i] = rem[i] xor gfMul(gen[i + 1], factor)
                }
            }
        }
        return rem
    }

    private fun generatorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun appendBits(bits: MutableList<Boolean>, value: Int, count: Int) {
        for (i in count - 1 downTo 0) {
            bits.add(((value ushr i) and 1) == 1)
        }
    }

    private fun placeFinderPattern(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>, row: Int, col: Int) {
        val size = modules.size
        for (dr in -1..7) {
            for (dc in -1..7) {
                val r = row + dr
                val c = col + dc
                if (r !in 0 until size || c !in 0 until size) continue
                val dark = dr in 0..6 && dc in 0..6 && (
                    dr == 0 || dr == 6 || dc == 0 || dc == 6 ||
                        (dr in 2..4 && dc in 2..4)
                    )
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }
    }

    private fun placeTimingPatterns(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>, size: Int) {
        for (i in 8 until size - 8) {
            val dark = (i % 2) == 0
            if (!isFunction[6][i]) {
                modules[6][i] = dark
                isFunction[6][i] = true
            }
            if (!isFunction[i][6]) {
                modules[i][6] = dark
                isFunction[i][6] = true
            }
        }
    }

    private fun placeAlignmentPatterns(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>, centers: IntArray) {
        if (centers.isEmpty()) return
        for (rCenter in centers) {
            for (cCenter in centers) {
                if (isFunction[rCenter][cCenter]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val r = rCenter + dr
                        val c = cCenter + dc
                        val dark = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc)) != 1
                        modules[r][c] = dark
                        isFunction[r][c] = true
                    }
                }
            }
        }
    }

    private fun reserveFormatAndDarkModule(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>, size: Int) {
        for (i in 0..8) {
            if (i != 6) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in size - 8 until size) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
        modules[size - 8][8] = true
        isFunction[size - 8][8] = true
    }

    private fun placeDataBits(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        size: Int,
        bits: BooleanArray
    ) {
        var bitIdx = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col--
            for (step in 0 until size) {
                val r = if (upward) size - 1 - step else step
                for (dc in 0..1) {
                    val c = col - dc
                    if (!isFunction[r][c]) {
                        modules[r][c] = if (bitIdx < bits.size) bits[bitIdx++] else false
                    }
                }
            }
            upward = !upward
            col -= 2
        }
    }

    private fun applyMask0(modules: Array<BooleanArray>, isFunction: Array<BooleanArray>, size: Int) {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c] && ((r + c) % 2 == 0)) {
                    modules[r][c] = !modules[r][c]
                }
            }
        }
    }

    private fun writeFormatBits(modules: Array<BooleanArray>, size: Int) {
        for (i in 0 until 15) {
            val bit = ((FORMAT_BITS_L_MASK0 ushr i) and 1) == 1
            when {
                i < 6 -> modules[8][i] = bit
                i < 8 -> modules[8][i + 1] = bit
                else -> modules[8][size - 15 + i] = bit
            }
            when {
                i < 8 -> modules[size - 1 - i][8] = bit
                i == 8 -> modules[7][8] = bit
                else -> modules[14 - i][8] = bit
            }
        }
    }
}

