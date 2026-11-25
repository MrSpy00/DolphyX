package com.aegissoft.dolphyx.engine.ir

/** One signal parsed from a Flipper `.ir` file. */
data class ImportedIrSignal(
    val name: String,
    val protocol: String,
    val address: String,
    val command: String,
    val frequency: Int,
)

/**
 * Parser for the Flipper IR file format (Filetype: IR signals file, Version 1).
 * Signals are separated by `#` comment lines; keys are `key: value` pairs.
 */
object IrFileImporter {

    fun parse(content: String): List<ImportedIrSignal> {
        val signals = mutableListOf<ImportedIrSignal>()
        var block = mutableMapOf<String, String>()

        fun flush() {
            if (block.isEmpty()) return
            val name = block["name"]?.trim().orEmpty().ifBlank { "Signal ${signals.size + 1}" }
            val type = block["type"]?.trim()?.lowercase().orEmpty()
            val frequency = block["frequency"]?.trim()?.toIntOrNull()?.coerceIn(20_000, 60_000) ?: 38_000
            val protocol = block["protocol"]?.trim().orEmpty()
            val address = block["address"]?.trim().orEmpty()
            val command = block["command"]?.trim().orEmpty()
            val raw = block["data"]?.trim().orEmpty()
            val valid = when (type) {
                "raw" -> raw.isNotBlank()
                "parsed" -> protocol.isNotBlank() && command.isNotBlank()
                else -> false
            }
            if (valid) {
                signals += ImportedIrSignal(
                    name = name,
                    protocol = if (type == "raw") "RAW" else protocol.uppercase(),
                    address = address,
                    command = if (type == "raw") raw else command,
                    frequency = frequency,
                )
            }
            block = mutableMapOf()
        }

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("Filetype:") || line.startsWith("Version:") -> Unit
                line.startsWith("#") -> flush()
                line.isEmpty() -> Unit
                else -> {
                    val separator = line.indexOf(':')
                    if (separator > 0) {
                        val key = line.substring(0, separator).trim().lowercase()
                        val value = line.substring(separator + 1).trim()
                        if (value.isNotEmpty()) block[key] = value
                    }
                }
            }
        }
        flush()
        return signals
    }
}
