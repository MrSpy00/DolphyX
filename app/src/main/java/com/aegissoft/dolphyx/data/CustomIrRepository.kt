package com.aegissoft.dolphyx.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class CustomIrSignalRecord(
    val id: String,
    val name: String,
    val protocol: String,
    val address: String,
    val command: String,
    val frequency: Int,
)

/**
 * Local custom IR signal shelf. Not sensitive; protocol data only.
 */
class CustomIrRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun getAll(): List<CustomIrSignalRecord> {
        val raw = prefs.getString(KEY_SIGNALS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<CustomIrSignalRecord>>(raw)
        }.getOrDefault(emptyList())
    }

    fun save(record: CustomIrSignalRecord): List<CustomIrSignalRecord> {
        val current = getAll().filterNot { it.id == record.id }
        val updated = current + record
        persist(updated)
        return updated
    }

    fun delete(id: String): List<CustomIrSignalRecord> {
        val updated = getAll().filterNot { it.id == id }
        persist(updated)
        return updated
    }

    fun saveAll(records: List<CustomIrSignalRecord>): List<CustomIrSignalRecord> {
        if (records.isEmpty()) return getAll()
        val updated = getAll() + records
        persist(updated)
        return updated
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun persist(records: List<CustomIrSignalRecord>) {
        prefs.edit().putString(KEY_SIGNALS, json.encodeToString(records)).apply()
    }

    companion object {
        private const val PREFS_NAME = "dolphyx_custom_ir"
        private const val KEY_SIGNALS = "custom_signals"
    }
}
