package com.aegissoft.dolphyx.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption manager backed by Android Keystore.
 * Supports an optional [SecretKey] override for pure-JVM unit testing.
 */
class KeystoreCryptoManager(
  private val keyOverride: SecretKey? = null
) {
  companion object {
    private const val TAG = "KeystoreCryptoManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "dolphyx_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
  }

  private fun getOrCreateKey(): SecretKey {
    keyOverride?.let { return it }
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existingKey = (keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    if (existingKey != null) {
      return existingKey
    }
    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec = KeyGenParameterSpec.Builder(
      MASTER_KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setRandomizedEncryptionRequired(true)
      .build()
    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }

  /**
   * Encrypts [plaintext] using AES-256-GCM with a 12-byte IV prepended to the ciphertext.
   * Returns a Base64 (NO_WRAP) encoded string.
   */
  fun encryptToBase64(plaintext: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val iv = cipher.iv
    require(iv.size == GCM_IV_LENGTH_BYTES) { "Unexpected GCM IV length: ${iv.size}" }
    val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
    val combined = ByteBuffer.allocate(iv.size + ciphertext.size)
      .put(iv)
      .put(ciphertext)
      .array()
    return Base64.getEncoder().withoutPadding().encodeToString(combined)
  }

  /**
   * Decrypts a Base64 string produced by [encryptToBase64].
   * Returns null if authentication fails or input is malformed.
   */
  fun decryptFromBase64(ciphertext: String): String? {
    if (ciphertext.isBlank()) return null
    return try {
      val combined = Base64.getDecoder().decode(ciphertext)
      if (combined.size <= GCM_IV_LENGTH_BYTES) {
        return null
      }
      val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
      val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
      val plainBytes = cipher.doFinal(encryptedBytes)
      String(plainBytes, StandardCharsets.UTF_8)
    } catch (e: GeneralSecurityException) {
      Log.w(TAG, "Decryption failed: ${e.javaClass.simpleName}")
      null
    } catch (e: IllegalArgumentException) {
      Log.w(TAG, "Malformed Base64 ciphertext: ${e.message}")
      null
    }
  }
}
