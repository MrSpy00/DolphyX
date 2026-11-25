package com.aegissoft.dolphyx.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class IrRemoteSummary(
    val id: Long,
    val category: String,
    val brand: String,
    val model: String,
    val source: String,
    val signalCount: Int,
)

data class IrSignal(
    val id: Long,
    val remoteId: Long,
    val name: String,
    val sigType: String,
    val protocol: String?,
    val address: String?,
    val command: String?,
    val frequency: Int?,
    val dutyCycle: Double?,
    val rawData: String?,
)

class IrDatabaseHelper(context: Context) {

    private val appContext = context.applicationContext
    private val dbLock = Any()

    @Volatile
    private var database: SQLiteDatabase? = null

    private fun ensureDatabase(): SQLiteDatabase {
        database?.let { existing ->
            if (existing.isOpen) return existing
        }
        synchronized(dbLock) {
            database?.let { existing ->
                if (existing.isOpen) return existing
            }
            val dbFile = appContext.getDatabasePath(DB_NAME)
            dbFile.parentFile?.let { parent ->
                if (!parent.exists()) parent.mkdirs()
            }
            val expectedAssetLength = readAssetLengthOrNull(DB_NAME)
            val needsCopy = !dbFile.exists() ||
                dbFile.length() == 0L ||
                (expectedAssetLength != null && expectedAssetLength > 0L && dbFile.length() != expectedAssetLength)

            if (needsCopy) {
                copyAssetDatabase(DB_NAME, dbFile)
            }
            val opened = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            database = opened
            return opened
        }
    }

    private fun readAssetLengthOrNull(assetName: String): Long? {
        return try {
            appContext.assets.openFd(assetName).use { afd ->
                afd.length
            }
        } catch (e: IOException) {
            Log.d(TAG, "Asset $assetName is compressed or fd unavailable: ${e.message}")
            null
        }
    }

    private fun copyAssetDatabase(assetName: String, target: File) {
        val tempFile = File(target.parentFile, "$assetName.tmp")
        appContext.assets.open(assetName).use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                }
                output.fd.sync()
            }
        }
        if (target.exists() && !target.delete()) {
            Log.w(TAG, "Failed to replace existing DB file at ${target.absolutePath}")
        }
        if (!tempFile.renameTo(target)) {
            tempFile.copyTo(target, overwrite = true)
            tempFile.delete()
        }
    }

    fun getStats(): Pair<Int, Int> {
        val db = ensureDatabase()
        val remotesCount = db.rawQuery("SELECT COUNT(*) FROM remotes", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        val signalsCount = db.rawQuery("SELECT COUNT(*) FROM signals", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        return remotesCount to signalsCount
    }

    fun getCategories(): List<String> {
        val db = ensureDatabase()
        val result = mutableListOf<String>()
        db.rawQuery(
            "SELECT DISTINCT category FROM remotes " +
                "WHERE lower(category) NOT LIKE '%jammer%' " +
                "AND category NOT LIKE '%глушилк%' " +
                "ORDER BY category COLLATE NOCASE ASC",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                result.add(c.getString(0))
            }
        }
        return result
    }

    fun getBrands(category: String? = null, query: String = ""): List<String> {
        val db = ensureDatabase()
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (!category.isNullOrBlank()) {
            conditions.add("category = ?")
            args.add(category.trim())
        }
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            conditions.add("brand LIKE ? COLLATE NOCASE")
            args.add("%$trimmedQuery%")
        }
        val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
        val sql = "SELECT DISTINCT brand FROM remotes $where ORDER BY brand COLLATE NOCASE ASC"
        val result = mutableListOf<String>()
        db.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                result.add(c.getString(0))
            }
        }
        return result
    }

    fun getRemotes(brand: String, category: String? = null, query: String = ""): List<IrRemoteSummary> {
        val db = ensureDatabase()
        val conditions = mutableListOf("brand = ?")
        val args = mutableListOf(brand)
        if (!category.isNullOrBlank()) {
            conditions.add("category = ?")
            args.add(category.trim())
        }
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            conditions.add("model LIKE ? COLLATE NOCASE")
            args.add("%$trimmedQuery%")
        }
        val sql = """
            SELECT id, category, brand, model, source, signal_count
            FROM remotes
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY model COLLATE NOCASE ASC
        """.trimIndent()
        val result = mutableListOf<IrRemoteSummary>()
        db.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                result.add(
                    IrRemoteSummary(
                        id = c.getLong(0),
                        category = c.getString(1),
                        brand = c.getString(2),
                        model = c.getString(3),
                        source = c.getString(4),
                        signalCount = c.getInt(5),
                    ),
                )
            }
        }
        return result
    }

    fun getSignals(remoteId: Long): List<IrSignal> {
        val db = ensureDatabase()
        val sql = """
            SELECT id, remote_id, name, sig_type, protocol, address, command, frequency, duty_cycle, raw_data
            FROM signals
            WHERE remote_id = ?
            ORDER BY id ASC
        """.trimIndent()
        val result = mutableListOf<IrSignal>()
        db.rawQuery(sql, arrayOf(remoteId.toString())).use { c ->
            while (c.moveToNext()) {
                result.add(readSignal(c))
            }
        }
        return result
    }

    fun getSignalsByIds(ids: Set<Long>): List<IrSignal> {
        if (ids.isEmpty()) return emptyList()
        val db = ensureDatabase()
        val placeholders = ids.joinToString(",") { "?" }
        val sql = """
            SELECT id, remote_id, name, sig_type, protocol, address, command, frequency, duty_cycle, raw_data
            FROM signals
            WHERE id IN ($placeholders)
            ORDER BY name COLLATE NOCASE ASC
        """.trimIndent()
        val result = mutableListOf<IrSignal>()
        db.rawQuery(sql, ids.map { it.toString() }.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                result.add(readSignal(c))
            }
        }
        return result
    }

    fun getFirstRemoteWithSignal(category: String, signalNameLike: String): Pair<IrRemoteSummary, List<IrSignal>>? {
        val db = ensureDatabase()
        val sql = """
            SELECT r.id, r.category, r.brand, r.model, r.source, r.signal_count
            FROM remotes r
            JOIN signals s ON s.remote_id = r.id
            WHERE r.category = ? AND lower(s.name) LIKE ?
            ORDER BY r.brand COLLATE NOCASE ASC, r.model COLLATE NOCASE ASC
            LIMIT 1
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(category, "%${signalNameLike.lowercase()}%")).use { c ->
            if (!c.moveToFirst()) return null
            val remote = IrRemoteSummary(
                id = c.getLong(0),
                category = c.getString(1),
                brand = c.getString(2),
                model = c.getString(3),
                source = c.getString(4),
                signalCount = c.getInt(5),
            )
            remote to getSignals(remote.id)
        }
    }

    private fun readSignal(c: android.database.Cursor): IrSignal {
        return IrSignal(
            id = c.getLong(0),
            remoteId = c.getLong(1),
            name = c.getString(2),
            sigType = c.getString(3),
            protocol = if (c.isNull(4)) null else c.getString(4),
            address = if (c.isNull(5)) null else c.getString(5),
            command = if (c.isNull(6)) null else c.getString(6),
            frequency = if (c.isNull(7)) null else c.getInt(7),
            dutyCycle = if (c.isNull(8)) null else c.getDouble(8),
            rawData = if (c.isNull(9)) null else c.getString(9),
        )
    }

    fun close() {
        synchronized(dbLock) {
            database?.close()
            database = null
        }
    }

    companion object {
        private const val TAG = "IrDatabaseHelper"
        private val DB_NAME: String = DolphyXConfig.irdbAssetName
    }
}
