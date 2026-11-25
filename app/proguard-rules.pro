# DolphyX (aegisSoft) ProGuard / R8 Rules
-keepattributes SourceFile,LineNumberTable
-keep class com.aegissoft.dolphyx.engine.plugin.PluginManifest { *; }
-keep class com.aegissoft.dolphyx.engine.plugin.PluginUiNode { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
