package com.aegissoft.dolphyx.core.security

import android.content.Context
import android.content.SharedPreferences
import com.aegissoft.dolphyx.core.config.DolphyXConfig

/**
 * Encrypted key-value preferences backed by [KeystoreCryptoManager] and private [SharedPreferences].
 */
class SecurePreferences(
  context: Context,
  private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
) {
  private val prefs: SharedPreferences = context.applicationContext
    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  companion object {
    private const val PREFS_NAME = "dolphyx_secure_prefs"
    private const val KEY_LANGUAGE = "enc_app_language"
    private const val KEY_DARK_THEME = "enc_dark_theme"
    private const val KEY_PROBE_HOST = "enc_default_probe_host"
    private const val KEY_PROBE_PORT = "enc_default_probe_port"
    private const val KEY_HTTPS_URL = "enc_default_https_url"
    private const val KEY_HCE_TEXT = "enc_default_hce_text"
    private const val KEY_THEME_MODE = "enc_theme_mode"
    private const val KEY_ACCENT = "enc_accent"
    private const val KEY_UI_SCALE = "enc_ui_scale"
    private const val KEY_REDUCE_MOTION = "enc_reduce_motion"
    private const val KEY_IR_FAVORITES = "enc_ir_favorites"
    private const val KEY_FLIPPER_FONT = "enc_flipper_font"
    private const val KEY_FAB_VISIBLE = "enc_fab_visible"
    private const val KEY_FAB_ACTION = "enc_fab_action"
    private const val KEY_MENU_VIEW = "enc_menu_view"
    private const val KEY_MENU_STYLE = "enc_menu_style"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    val THEME_MODES = setOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK)

    const val ACCENT_RAINBOW = "rainbow"
    const val ACCENT_ORANGE = "orange"
    const val ACCENT_RED = "red"
    const val ACCENT_GREEN = "green"
    const val ACCENT_PURPLE = "purple"
    const val ACCENT_CYAN = "cyan"
    const val ACCENT_DYNAMIC = "dynamic"
    val ACCENTS = setOf(
        ACCENT_RAINBOW,
        ACCENT_ORANGE,
        ACCENT_RED,
        ACCENT_GREEN,
        ACCENT_PURPLE,
        ACCENT_CYAN,
        ACCENT_DYNAMIC,
    )

    const val FAB_ACTION_FUNCTIONS = "functions"
    const val FAB_ACTION_SETTINGS = "settings"
    val FAB_ACTIONS = setOf(FAB_ACTION_FUNCTIONS, FAB_ACTION_SETTINGS)

    const val MENU_VIEW_CONTAINERS = "containers"
    const val MENU_VIEW_TILES = "tiles"
    val MENU_VIEWS = setOf(MENU_VIEW_CONTAINERS, MENU_VIEW_TILES)

    const val STYLE_STANDARD = "standard"
    const val STYLE_COMPACT = "compact"
    val MENU_STYLES = setOf(STYLE_STANDARD, STYLE_COMPACT)
  }

  fun getLanguage(): String {
    val encrypted = prefs.getString(KEY_LANGUAGE, null) ?: return DolphyXConfig.defaultLanguage
    val decrypted = cryptoManager.decryptFromBase64(encrypted)
    return if (decrypted in AppLocaleManager.supportedLocales) decrypted!! else DolphyXConfig.defaultLanguage
  }

  fun setLanguage(lang: String) {
    val normalized = if (lang in AppLocaleManager.supportedLocales) lang else DolphyXConfig.defaultLanguage
    prefs.edit().putString(KEY_LANGUAGE, cryptoManager.encryptToBase64(normalized)).apply()
  }

  fun isDarkTheme(): Boolean {
    val encrypted = prefs.getString(KEY_DARK_THEME, null) ?: return true
    val decrypted = cryptoManager.decryptFromBase64(encrypted) ?: return true
    return decrypted.toBooleanStrictOrNull() ?: true
  }

  fun setDarkTheme(enabled: Boolean) {
    prefs.edit().putString(KEY_DARK_THEME, cryptoManager.encryptToBase64(enabled.toString())).apply()
  }

  fun getDefaultProbeHost(): String {
    val encrypted = prefs.getString(KEY_PROBE_HOST, null) ?: return DolphyXConfig.defaultProbeHost
    return cryptoManager.decryptFromBase64(encrypted) ?: DolphyXConfig.defaultProbeHost
  }

  fun setDefaultProbeHost(host: String) {
    val clean = host.trim()
    prefs.edit().putString(KEY_PROBE_HOST, cryptoManager.encryptToBase64(clean)).apply()
  }

  fun getDefaultProbePort(): Int {
    val encrypted = prefs.getString(KEY_PROBE_PORT, null) ?: return DolphyXConfig.defaultProbePort
    val parsed = cryptoManager.decryptFromBase64(encrypted)?.toIntOrNull() ?: return DolphyXConfig.defaultProbePort
    return if (parsed in 1..65535) parsed else DolphyXConfig.defaultProbePort
  }

  fun setDefaultProbePort(port: Int) {
    val valid = if (port in 1..65535) port else DolphyXConfig.defaultProbePort
    prefs.edit().putString(KEY_PROBE_PORT, cryptoManager.encryptToBase64(valid.toString())).apply()
  }

  fun getDefaultHttpsUrl(): String {
    val encrypted = prefs.getString(KEY_HTTPS_URL, null) ?: return DolphyXConfig.defaultHttpsUrl
    return cryptoManager.decryptFromBase64(encrypted)?.takeIf { it.isNotBlank() } ?: DolphyXConfig.defaultHttpsUrl
  }

  fun setDefaultHttpsUrl(url: String) {
    val clean = url.trim().ifBlank { DolphyXConfig.defaultHttpsUrl }
    prefs.edit().putString(KEY_HTTPS_URL, cryptoManager.encryptToBase64(clean)).apply()
  }

  fun getDefaultHceText(): String {
    val encrypted = prefs.getString(KEY_HCE_TEXT, null) ?: return DolphyXConfig.defaultHceText
    return cryptoManager.decryptFromBase64(encrypted)?.takeIf { it.isNotBlank() } ?: DolphyXConfig.defaultHceText
  }

  fun setDefaultHceText(text: String) {
    val clean = text.trim().ifBlank { DolphyXConfig.defaultHceText }
    prefs.edit().putString(KEY_HCE_TEXT, cryptoManager.encryptToBase64(clean)).apply()
  }

  fun isHceUnlockRequired(): Boolean = true

  fun isPluginStrictMode(): Boolean = true

  fun getThemeMode(): String {
    val encrypted = prefs.getString(KEY_THEME_MODE, null) ?: return THEME_DARK
    val mode = cryptoManager.decryptFromBase64(encrypted) ?: return THEME_DARK
    return if (mode in THEME_MODES) mode else THEME_DARK
  }

  fun setThemeMode(mode: String) {
    val normalized = if (mode in THEME_MODES) mode else THEME_DARK
    prefs.edit().putString(KEY_THEME_MODE, cryptoManager.encryptToBase64(normalized)).apply()
  }

  fun getAccent(): String {
    val encrypted = prefs.getString(KEY_ACCENT, null) ?: return ACCENT_RAINBOW
    val accent = cryptoManager.decryptFromBase64(encrypted) ?: return ACCENT_RAINBOW
    return if (accent in ACCENTS) accent else ACCENT_RAINBOW
  }

  fun setAccent(accent: String) {
    val normalized = if (accent in ACCENTS) accent else ACCENT_RAINBOW
    prefs.edit().putString(KEY_ACCENT, cryptoManager.encryptToBase64(normalized)).apply()
  }

  fun isFlipperFont(): Boolean = readBoolean(KEY_FLIPPER_FONT, false)

  fun setFlipperFont(enabled: Boolean) = writeBoolean(KEY_FLIPPER_FONT, enabled)

  fun isFabVisible(): Boolean = readBoolean(KEY_FAB_VISIBLE, true)

  fun setFabVisible(visible: Boolean) = writeBoolean(KEY_FAB_VISIBLE, visible)

  fun getFabAction(): String {
    val raw = readString(KEY_FAB_ACTION, FAB_ACTION_FUNCTIONS)
    return if (raw in FAB_ACTIONS) raw else FAB_ACTION_FUNCTIONS
  }

  fun setFabAction(action: String) {
    val normalized = if (action in FAB_ACTIONS) action else FAB_ACTION_FUNCTIONS
    prefs.edit().putString(KEY_FAB_ACTION, cryptoManager.encryptToBase64(normalized)).apply()
  }

  fun getMenuView(): String {
    val raw = readString(KEY_MENU_VIEW, MENU_VIEW_CONTAINERS)
    return if (raw in MENU_VIEWS) raw else MENU_VIEW_CONTAINERS
  }

  fun setMenuView(view: String) {
    val normalized = if (view in MENU_VIEWS) view else MENU_VIEW_CONTAINERS
    prefs.edit().putString(KEY_MENU_VIEW, cryptoManager.encryptToBase64(normalized)).apply()
  }

  fun getMenuStyle(): String {
    val raw = readString(KEY_MENU_STYLE, STYLE_STANDARD)
    return if (raw in MENU_STYLES) raw else STYLE_STANDARD
  }

  fun setMenuStyle(style: String) {
    val normalized = if (style in MENU_STYLES) style else STYLE_STANDARD
    prefs.edit().putString(KEY_MENU_STYLE, cryptoManager.encryptToBase64(normalized)).apply()
  }

  private fun readString(key: String, fallback: String): String {
    val encrypted = prefs.getString(key, null) ?: return fallback
    return cryptoManager.decryptFromBase64(encrypted) ?: fallback
  }

  private fun readBoolean(key: String, fallback: Boolean): Boolean {
    val encrypted = prefs.getString(key, null) ?: return fallback
    return cryptoManager.decryptFromBase64(encrypted)?.toBooleanStrictOrNull() ?: fallback
  }

  private fun writeBoolean(key: String, value: Boolean) {
    prefs.edit().putString(key, cryptoManager.encryptToBase64(value.toString())).apply()
  }

  fun getUiScale(): Float {
    val encrypted = prefs.getString(KEY_UI_SCALE, null) ?: return 1f
    val scale = cryptoManager.decryptFromBase64(encrypted)?.toFloatOrNull() ?: return 1f
    return scale.coerceIn(0.85f, 1.35f)
  }

  fun setUiScale(scale: Float) {
    val valid = scale.coerceIn(0.85f, 1.35f)
    prefs.edit().putString(KEY_UI_SCALE, cryptoManager.encryptToBase64(valid.toString())).apply()
  }

  fun isReduceMotion(): Boolean {
    val encrypted = prefs.getString(KEY_REDUCE_MOTION, null) ?: return false
    val value = cryptoManager.decryptFromBase64(encrypted) ?: return false
    return value.toBooleanStrictOrNull() ?: false
  }

  fun setReduceMotion(enabled: Boolean) {
    prefs.edit().putString(KEY_REDUCE_MOTION, cryptoManager.encryptToBase64(enabled.toString())).apply()
  }

  fun getIrFavoriteIds(): Set<Long> {
    val encrypted = prefs.getString(KEY_IR_FAVORITES, null) ?: return emptySet()
    val raw = cryptoManager.decryptFromBase64(encrypted).orEmpty()
    return raw.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
  }

  fun toggleIrFavorite(signalId: Long): Set<Long> {
    val current = getIrFavoriteIds().toMutableSet()
    if (!current.add(signalId)) current.remove(signalId)
    prefs.edit().putString(KEY_IR_FAVORITES, cryptoManager.encryptToBase64(current.joinToString(","))).apply()
    return current
  }
}
