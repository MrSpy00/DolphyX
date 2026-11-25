package com.aegissoft.dolphyx.core.security

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import java.util.Locale

/**
 * Locale helper supporting Turkish ("tr") and English ("en").
 */
object AppLocaleManager {
  val supportedLocales: List<String> = listOf("tr", "en")

  fun wrapContext(base: Context, langCode: String): Context {
    val normalized = if (langCode in supportedLocales) langCode else DolphyXConfig.defaultLanguage
    val locale = Locale.forLanguageTag(normalized)
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocales(LocaleList(locale))
    return base.createConfigurationContext(config)
  }
}
