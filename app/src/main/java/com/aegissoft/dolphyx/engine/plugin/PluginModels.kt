package com.aegissoft.dolphyx.engine.plugin

import kotlinx.serialization.Serializable

@Serializable
data class PluginUiBlock(
  val type: String,
  val title: String = "",
  val value: String = "",
  val action: String = ""
)

@Serializable
data class DolphyXPluginManifest(
  val id: String,
  val name: String,
  val version: String,
  val author: String,
  val description: String,
  val capabilities: List<String> = emptyList(),
  val ui: List<PluginUiBlock> = emptyList(),
  val contentSha256: String = ""
)

sealed class PluginVerificationResult {
  data class Valid(
    val manifest: DolphyXPluginManifest,
    val computedSha256: String
  ) : PluginVerificationResult()

  data class Rejected(
    val reason: String
  ) : PluginVerificationResult()
}
