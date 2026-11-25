package com.aegissoft.dolphyx.engine.plugin

import android.content.Context
import android.util.Log
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Strict read-only sandboxed plugin verifier and loader.
 * Enforces canonical SHA-256 integrity and a closed capability allowlist.
 */
object SandboxedPluginEngine {
  private const val TAG = "SandboxedPluginEngine"
  private const val BUNDLED_DIR = "sample_plugins"

  val ALLOWED_CAPABILITIES: Set<String> = setOf(
    "hardware.status",
    "ble.summary",
    "irdb.stats",
    "oui.stats",
    "nfc.vault_count",
    "network.summary"
  )

  private val ID_REGEX = Regex("^[a-zA-Z0-9._-]{3,80}$")
  private val SHA256_REGEX = Regex("^[0-9a-f]{64}$")

  private val json = Json {
    ignoreUnknownKeys = false
    encodeDefaults = true
    prettyPrint = false
  }

  /**
   * Verifies a `.dolphyxplugin` JSON string against canonical SHA-256 and capability rules.
   */
  fun verifyPluginJson(rawJson: String): PluginVerificationResult {
    if (rawJson.isBlank()) {
      return PluginVerificationResult.Rejected("Empty plugin payload")
    }

    val rootElement = try {
      json.parseToJsonElement(rawJson)
    } catch (e: SerializationException) {
      return PluginVerificationResult.Rejected("Malformed JSON: ${e.message}")
    } catch (e: IllegalArgumentException) {
      return PluginVerificationResult.Rejected("Invalid JSON input: ${e.message}")
    }

    val rootObj = rootElement as? JsonObject
      ?: return PluginVerificationResult.Rejected("Plugin root must be a JSON object")

    val manifest = try {
      json.decodeFromJsonElement(DolphyXPluginManifest.serializer(), rootObj)
    } catch (e: SerializationException) {
      return PluginVerificationResult.Rejected("Invalid plugin manifest schema: ${e.message}")
    } catch (e: IllegalArgumentException) {
      return PluginVerificationResult.Rejected("Invalid plugin manifest fields: ${e.message}")
    }

    if (!ID_REGEX.matches(manifest.id)) {
      return PluginVerificationResult.Rejected("Invalid plugin id: '${manifest.id}'")
    }
    if (manifest.name.isBlank()) {
      return PluginVerificationResult.Rejected("Plugin name must not be blank")
    }

    val declaredHash = manifest.contentSha256.trim().lowercase()
    if (!SHA256_REGEX.matches(declaredHash)) {
      return PluginVerificationResult.Rejected("Missing or invalid contentSha256")
    }

    for (cap in manifest.capabilities) {
      if (cap !in ALLOWED_CAPABILITIES) {
        return PluginVerificationResult.Rejected("Disallowed capability: '$cap'")
      }
    }

    val canonicalObj = stripHashAndSort(rootObj)
    val canonicalJson = json.encodeToString(JsonElement.serializer(), canonicalObj)
    val computedBytes = sha256Bytes(canonicalJson.toByteArray(StandardCharsets.UTF_8))
    val computedHex = computedBytes.joinToString("") { "%02x".format(it) }
    val declaredBytes = hexToBytes(declaredHash)

    if (!MessageDigest.isEqual(computedBytes, declaredBytes)) {
      return PluginVerificationResult.Rejected(
        "SHA-256 mismatch: expected=$declaredHash computed=$computedHex"
      )
    }

    return PluginVerificationResult.Valid(manifest, computedHex)
  }

  /**
   * Loads and verifies all bundled `.dolphyxplugin` assets under `assets/sample_plugins/`.
   */
  fun loadBundledPlugins(context: Context): List<PluginVerificationResult> {
    val assetManager = context.assets
    val files = try {
      assetManager.list(BUNDLED_DIR)?.filter { it.endsWith(".dolphyxplugin") }?.sorted() ?: emptyList()
    } catch (e: IOException) {
      Log.w(TAG, "Failed to list $BUNDLED_DIR: ${e.message}")
      return emptyList()
    }

    return files.map { filename ->
      try {
        val raw = assetManager.open("$BUNDLED_DIR/$filename")
          .bufferedReader(StandardCharsets.UTF_8)
          .use { it.readText() }
        verifyPluginJson(raw)
      } catch (e: IOException) {
        PluginVerificationResult.Rejected("Failed to read $filename: ${e.message}")
      }
    }
  }

  private fun stripHashAndSort(rootObj: JsonObject): JsonObject {
    val filtered = rootObj.filterKeys { it != "contentSha256" }
    return sortJsonObject(JsonObject(filtered))
  }

  private fun sortJsonElement(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> sortJsonObject(element)
    is JsonArray -> JsonArray(element.map { sortJsonElement(it) })
    is JsonPrimitive -> element
  }

  private fun sortJsonObject(obj: JsonObject): JsonObject {
    val sortedEntries = obj.entries
      .sortedBy { it.key }
      .associate { (k, v) -> k to sortJsonElement(v) }
    return JsonObject(sortedEntries)
  }

  private fun sha256Bytes(data: ByteArray): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(data)
  }

  private fun hexToBytes(hex: String): ByteArray {
    val out = ByteArray(hex.length / 2)
    for (i in out.indices) {
      val idx = i * 2
      out[i] = hex.substring(idx, idx + 2).toInt(16).toByte()
    }
    return out
  }
}
