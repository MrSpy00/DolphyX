package com.aegissoft.dolphyx.core.config

import com.aegissoft.dolphyx.BuildConfig

/**
 * Centralized, typed application configuration backed by BuildConfig.
 * Runtime-overridable values can be customized by the user via SecurePreferences.
 */
object DolphyXConfig {
  val appBrand: String = BuildConfig.APP_BRAND
  val appDeveloper: String = BuildConfig.APP_DEVELOPER
  val appWebsite: String = BuildConfig.APP_WEBSITE
  val githubUrl: String = BuildConfig.APP_GITHUB_URL
  val upstreamUrl: String = BuildConfig.APP_UPSTREAM_URL
  val defaultLanguage: String = BuildConfig.DEFAULT_LANGUAGE

  val defaultProbeHost: String = ""
  val defaultProbePort: Int = BuildConfig.DEFAULT_PROBE_PORT
  val defaultHttpsUrl: String = BuildConfig.DEFAULT_HTTPS_URL
  val localBindHost: String = BuildConfig.DEFAULT_LOCAL_BIND_HOST
  val defaultHceText: String = BuildConfig.DEFAULT_HCE_TEXT

  val irdbAssetName: String = BuildConfig.IRDB_ASSET_NAME
  val ouiAssetName: String = BuildConfig.OUI_ASSET_NAME
  val nfcVaultDbName: String = BuildConfig.NFC_VAULT_DB_NAME

  val wakeLockTimeoutMs: Long = BuildConfig.WAKE_LOCK_TIMEOUT_MS
  val httpTimeoutSeconds: Long = BuildConfig.HTTP_TIMEOUT_SECONDS
}
