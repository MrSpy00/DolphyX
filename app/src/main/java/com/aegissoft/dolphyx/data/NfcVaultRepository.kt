package com.aegissoft.dolphyx.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import com.aegissoft.dolphyx.core.security.KeystoreCryptoManager

data class NfcTagRecord(
  val id: Long,
  val label: String,
  val uidHex: String,
  val techList: String,
  val ndefPayload: String,
  val createdAtMillis: Long
)

/**
 * Encrypted local vault for scanned NFC tags.
 * Encrypts [NfcTagRecord.uidHex] and [NfcTagRecord.ndefPayload] via [KeystoreCryptoManager].
 */
class NfcVaultRepository(
  context: Context,
  private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
) : SQLiteOpenHelper(context.applicationContext, DolphyXConfig.nfcVaultDbName, null, DB_VERSION) {

  companion object {
    private const val DB_VERSION = 1
    private const val TABLE_TAGS = "nfc_tags"
    private const val COL_ID = "id"
    private const val COL_LABEL = "label"
    private const val COL_UID_ENC = "uid_enc"
    private const val COL_TECH_LIST = "tech_list"
    private const val COL_NDEF_ENC = "ndef_enc"
    private const val COL_CREATED_AT = "created_at"
  }

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS $TABLE_TAGS (
        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COL_LABEL TEXT NOT NULL,
        $COL_UID_ENC TEXT NOT NULL,
        $COL_TECH_LIST TEXT NOT NULL,
        $COL_NDEF_ENC TEXT NOT NULL,
        $COL_CREATED_AT INTEGER NOT NULL
      )
      """.trimIndent()
    )
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP TABLE IF EXISTS $TABLE_TAGS")
    onCreate(db)
  }

  fun insert(label: String, uidHex: String, techList: String, ndefPayload: String): Long {
    val values = ContentValues().apply {
      put(COL_LABEL, label)
      put(COL_UID_ENC, cryptoManager.encryptToBase64(uidHex))
      put(COL_TECH_LIST, techList)
      put(COL_NDEF_ENC, cryptoManager.encryptToBase64(ndefPayload))
      put(COL_CREATED_AT, System.currentTimeMillis())
    }
    return writableDatabase.insertOrThrow(TABLE_TAGS, null, values)
  }

  fun getAll(): List<NfcTagRecord> {
    val results = mutableListOf<NfcTagRecord>()
    readableDatabase.query(
      TABLE_TAGS,
      arrayOf(COL_ID, COL_LABEL, COL_UID_ENC, COL_TECH_LIST, COL_NDEF_ENC, COL_CREATED_AT),
      null,
      null,
      null,
      null,
      "$COL_CREATED_AT DESC"
    ).use { cursor ->
      while (cursor.moveToNext()) {
        val id = cursor.getLong(0)
        val label = cursor.getString(1)
        val uidEnc = cursor.getString(2)
        val techList = cursor.getString(3)
        val ndefEnc = cursor.getString(4)
        val createdAt = cursor.getLong(5)

        val uidHex = cryptoManager.decryptFromBase64(uidEnc) ?: "[UNREADABLE]"
        val ndefPayload = cryptoManager.decryptFromBase64(ndefEnc) ?: ""
        results.add(NfcTagRecord(id, label, uidHex, techList, ndefPayload, createdAt))
      }
    }
    return results
  }

  fun delete(id: Long) {
    writableDatabase.delete(TABLE_TAGS, "$COL_ID = ?", arrayOf(id.toString()))
  }

  fun count(): Int {
    readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_TAGS", null).use { cursor ->
      return if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }
  }
}
