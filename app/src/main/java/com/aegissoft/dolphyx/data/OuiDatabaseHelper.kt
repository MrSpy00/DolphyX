package com.aegissoft.dolphyx.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class OuiDatabaseHelper(context: Context) {

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

    fun lookupVendor(macAddress: String): String? {
        val hexOnly = macAddress
            .uppercase(Locale.ROOT)
            .filter { it in '0'..'9' || it in 'A'..'F' }
        if (hexOnly.length < 6) return null
        val prefix = hexOnly.substring(0, 6)
        val db = ensureDatabase()
        return db.rawQuery("SELECT vendor FROM oui WHERE prefix = ? LIMIT 1", arrayOf(prefix)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    fun getCount(): Int {
        val db = ensureDatabase()
        return db.rawQuery("SELECT COUNT(*) FROM oui", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun close() {
        synchronized(dbLock) {
            database?.close()
            database = null
        }
    }

    companion object {
        private const val TAG = "OuiDatabaseHelper"
        private val DB_NAME: String = DolphyXConfig.ouiAssetName
    }
}
