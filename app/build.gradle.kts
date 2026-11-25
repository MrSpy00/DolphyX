import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
  if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { load(it) }
  }
}

fun configValue(key: String, fallback: String): String =
  providers.gradleProperty(key).orElse(fallback).get()

fun quoted(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val appBrand = configValue("dolphyx.brand", "aegisSoft")
val appDeveloper = configValue("dolphyx.developer", "mrSpy")
val appWebsite = configValue("dolphyx.website", "aegissoft.com.tr")
val appGithubUrl = configValue("dolphyx.githubUrl", "https://github.com/MrSpy00/DolphyX")
val appUpstreamUrl = configValue("dolphyx.upstreamUrl", "https://github.com/unvoiddd/Dolphy-App")
val appDefaultLanguage = configValue("dolphyx.defaultLanguage", "tr")

android {
  namespace = "com.aegissoft.dolphyx"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aegissoft.dolphyx"
    minSdk = 26
    targetSdk = 36
    versionCode = 3
    versionName = "2.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "APP_BRAND", quoted(appBrand))
    buildConfigField("String", "APP_DEVELOPER", quoted(appDeveloper))
    buildConfigField("String", "APP_WEBSITE", quoted(appWebsite))
    buildConfigField("String", "APP_GITHUB_URL", quoted(appGithubUrl))
    buildConfigField("String", "APP_UPSTREAM_URL", quoted(appUpstreamUrl))
    buildConfigField("String", "DEFAULT_LANGUAGE", quoted(appDefaultLanguage))
    buildConfigField("int", "DEFAULT_PROBE_PORT", "443")
    buildConfigField("String", "DEFAULT_HTTPS_URL", quoted("https://$appWebsite"))
    buildConfigField("String", "DEFAULT_LOCAL_BIND_HOST", quoted("127.0.0.1"))
    buildConfigField("String", "IRDB_ASSET_NAME", quoted("dolphyx_irdb.db"))
    buildConfigField("String", "OUI_ASSET_NAME", quoted("dolphyx_oui.db"))
    buildConfigField("String", "NFC_VAULT_DB_NAME", quoted("dolphyx_nfc_vault.db"))
    buildConfigField("String", "DEFAULT_HCE_TEXT", quoted("$appBrand DolphyX"))
    buildConfigField("long", "WAKE_LOCK_TIMEOUT_MS", "600000L")
    buildConfigField("long", "HTTP_TIMEOUT_SECONDS", "5L")
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }

  val ksPath = System.getenv("DOLPHYX_KEYSTORE_PATH") ?: keystoreProps.getProperty("storeFile")
  val ksStorePass = System.getenv("DOLPHYX_KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
  val ksAlias = System.getenv("DOLPHYX_KEYSTORE_ALIAS") ?: keystoreProps.getProperty("keyAlias")
  val ksKeyPass = System.getenv("DOLPHYX_KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
  val releaseKeystore = ksPath?.let { rootProject.file(it) }

  signingConfigs {
    if (releaseKeystore != null && releaseKeystore.exists() && !ksStorePass.isNullOrBlank() && !ksAlias.isNullOrBlank() && !ksKeyPass.isNullOrBlank()) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = ksStorePass
        keyAlias = ksAlias
        keyPassword = ksKeyPass
        enableV1Signing = false
        enableV2Signing = true
        enableV3Signing = true
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      isShrinkResources = false
      isDebuggable = false
      signingConfigs.findByName("release")?.let { signingConfig = it }
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
    debug {
      applicationIdSuffix = ".debug"
      isDebuggable = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    freeCompilerArgs.addAll(
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
      "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
      "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    )
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2025.12.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
  implementation("androidx.activity:activity-compose:1.12.2")

  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.animation:animation")
  implementation("androidx.navigation:navigation-compose:2.9.6")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  implementation("com.google.android.material:material:1.13.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
