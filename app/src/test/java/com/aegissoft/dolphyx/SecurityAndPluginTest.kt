package com.aegissoft.dolphyx

import com.aegissoft.dolphyx.core.security.KeystoreCryptoManager
import com.aegissoft.dolphyx.engine.plugin.PluginVerificationResult
import com.aegissoft.dolphyx.engine.plugin.SandboxedPluginEngine
import java.io.File
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndPluginTest {

  private fun readSamplePlugin(filename: String): String {
    val candidates = listOf(
      File("src/main/assets/sample_plugins/$filename"),
      File("app/src/main/assets/sample_plugins/$filename")
    )
    val file = candidates.firstOrNull { it.exists() }
      ?: error("Sample plugin not found in ${candidates.map { it.absolutePath }}")
    return file.readText(Charsets.UTF_8)
  }

  @Test
  fun keystoreCryptoManager_roundTripAndTamperRejection() {
    val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
    val rawKey = keyGen.generateKey().encoded
    val secretKey = SecretKeySpec(rawKey, "AES")
    val crypto = KeystoreCryptoManager(keyOverride = secretKey)

    val plaintext = "UID:04A1B2C3D4|NDEF:https://aegissoft.com.tr"
    val encrypted = crypto.encryptToBase64(plaintext)
    assertNotEquals(plaintext, encrypted)

    val decrypted = crypto.decryptFromBase64(encrypted)
    assertEquals(plaintext, decrypted)

    // Tamper with ciphertext bytes
    val bytes = Base64.getDecoder().decode(encrypted)
    bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 0xFF).toByte()
    val tampered = Base64.getEncoder().withoutPadding().encodeToString(bytes)
    assertNull(crypto.decryptFromBase64(tampered))
  }

  @Test
  fun sandboxedPluginEngine_verifiesBundledPlugins() {
    val healthJson = readSamplePlugin("hardware_health_check.dolphyxplugin")
    val healthResult = SandboxedPluginEngine.verifyPluginJson(healthJson)
    assertTrue("Expected Valid for hardware_health_check, got $healthResult", healthResult is PluginVerificationResult.Valid)
    val validHealth = healthResult as PluginVerificationResult.Valid
    assertEquals("com.aegissoft.dolphyx.plugin.hardware_health", validHealth.manifest.id)

    val bleOuiJson = readSamplePlugin("ble_oui_summary.dolphyxplugin")
    val bleOuiResult = SandboxedPluginEngine.verifyPluginJson(bleOuiJson)
    assertTrue("Expected Valid for ble_oui_summary, got $bleOuiResult", bleOuiResult is PluginVerificationResult.Valid)
    val validBleOui = bleOuiResult as PluginVerificationResult.Valid
    assertEquals("com.aegissoft.dolphyx.plugin.ble_oui_summary", validBleOui.manifest.id)
  }

  @Test
  fun sandboxedPluginEngine_rejectsTamperedHash() {
    val original = readSamplePlugin("hardware_health_check.dolphyxplugin")
    val tampered = original.replace("Hardware Health Check", "Tampered Health Check")
    val result = SandboxedPluginEngine.verifyPluginJson(tampered)
    assertTrue(result is PluginVerificationResult.Rejected)
  }

  @Test
  fun sandboxedPluginEngine_rejectsMissingOrEmptyHash() {
    val original = readSamplePlugin("hardware_health_check.dolphyxplugin")
    val emptyHash = original.replace(
      "a55706fcb9afa0c1a2b4934f42b0b64866f8cce1b201b0c91016a08af186fd4d",
      ""
    )
    val result = SandboxedPluginEngine.verifyPluginJson(emptyHash)
    assertTrue(result is PluginVerificationResult.Rejected)
  }

  @Test
  fun sandboxedPluginEngine_rejectsForbiddenCapabilities() {
    val original = readSamplePlugin("hardware_health_check.dolphyxplugin")
    for (forbidden in listOf("rootExec", "shizukuExec", "dex.load")) {
      val modified = original.replace("\"hardware.status\"", "\"$forbidden\"")
      val result = SandboxedPluginEngine.verifyPluginJson(modified)
      assertTrue("Expected Rejected for capability $forbidden", result is PluginVerificationResult.Rejected)
    }
  }
}
