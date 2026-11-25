package com.aegissoft.dolphyx.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegissoft.dolphyx.R

// Original Dolphy visual language: monochrome OLED canvas, gray cards, white pills.
val DolphyBlack = Color(0xFF0A0A0A)
val DolphyCard = Color(0xFF2A2A2A)
val DolphyCardSoft = Color(0xFF1E1E1E)
val DolphyCardHigh = Color(0xFF383838)
val DolphyLine = Color(0xFF3D3D3D)
val DolphyWhite = Color(0xFFF4F4F4)
val DolphyMuted = Color(0xFF9C9C9C)
val DolphyMutedDim = Color(0xFF6E6E6E)

val DolphyLightBg = Color(0xFFF3F3F0)
val DolphyLightCard = Color(0xFFFFFFFF)
val DolphyLightLine = Color(0xFFDCDCD8)
val DolphyLightText = Color(0xFF141414)
val DolphyLightMuted = Color(0xFF666663)

val DolphyNumericFont = FontFamily(Font(R.font.roboto_medium_numbers))
val DolphyDisplayFont = FontFamily(Font(R.font.born2b_sporty_v2))

val DolphyNumericStyle = TextStyle(
    fontFamily = DolphyNumericFont,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = 0.3.sp,
)

enum class DolphyAccent {
    RAINBOW,
    ORANGE,
    RED,
    GREEN,
    PURPLE,
    CYAN,
    DYNAMIC,
}

val DolphyAccentOrange = Color(0xFFFF6B1A)
val DolphyAccentRed = Color(0xFFF2545B)
val DolphyAccentGreen = Color(0xFF7BE3A0)
val DolphyAccentPurple = Color(0xFFC455F7)
val DolphyAccentCyan = Color(0xFF56CCF2)

val DolphyRainbowColors = listOf(
    Color(0xFFFF6B1A),
    Color(0xFFF2C94C),
    Color(0xFF7BE3A0),
    Color(0xFF56CCF2),
    Color(0xFFC455F7),
    Color(0xFFF2545B),
)

fun DolphyAccent.solidColor(): Color = when (this) {
    DolphyAccent.RAINBOW -> DolphyWhite
    DolphyAccent.ORANGE -> DolphyAccentOrange
    DolphyAccent.RED -> DolphyAccentRed
    DolphyAccent.GREEN -> DolphyAccentGreen
    DolphyAccent.PURPLE -> DolphyAccentPurple
    DolphyAccent.CYAN -> DolphyAccentCyan
    DolphyAccent.DYNAMIC -> DolphyWhite
}

private data class AccentTokens(val primary: Color, val secondary: Color, val container: Color)

private fun darkTokens(accent: DolphyAccent): AccentTokens = when (accent) {
    DolphyAccent.RAINBOW -> AccentTokens(DolphyWhite, DolphyMuted, Color(0xFF3A3A3A))
    DolphyAccent.ORANGE -> AccentTokens(Color(0xFFFFB084), Color(0xFFFFD3BC), Color(0xFF40210E))
    DolphyAccent.RED -> AccentTokens(Color(0xFFFFB3B7), Color(0xFFFFD6D8), Color(0xFF40171A))
    DolphyAccent.GREEN -> AccentTokens(Color(0xFFA9F0C4), Color(0xFFD2F8E0), Color(0xFF133424))
    DolphyAccent.PURPLE -> AccentTokens(Color(0xFFE3B5FF), Color(0xFFF1D8FF), Color(0xFF331A40))
    DolphyAccent.CYAN -> AccentTokens(Color(0xFF8CE8F5), Color(0xFFB8F4FB), Color(0xFF0E2E33))
    DolphyAccent.DYNAMIC -> AccentTokens(DolphyWhite, DolphyMuted, Color(0xFF3A3A3A))
}

private fun lightTokens(accent: DolphyAccent): AccentTokens = when (accent) {
    DolphyAccent.RAINBOW -> AccentTokens(Color(0xFF141414), Color(0xFF666663), Color(0xFFE8E8E4))
    DolphyAccent.ORANGE -> AccentTokens(Color(0xFFB5470A), Color(0xFF8A3607), Color(0xFFFFE0D1))
    DolphyAccent.RED -> AccentTokens(Color(0xFFB3262E), Color(0xFF8B1D23), Color(0xFFFFDADC))
    DolphyAccent.GREEN -> AccentTokens(Color(0xFF146C3E), Color(0xFF0F5430), Color(0xFFD7F5E3))
    DolphyAccent.PURPLE -> AccentTokens(Color(0xFF7B2AA0), Color(0xFF5C1F79), Color(0xFFF0DCFA))
    DolphyAccent.CYAN -> AccentTokens(Color(0xFF006874), Color(0xFF004F57), Color(0xFFD0F7FF))
    DolphyAccent.DYNAMIC -> AccentTokens(Color(0xFF141414), Color(0xFF666663), Color(0xFFE8E8E4))
}

private fun darkScheme(accent: DolphyAccent) = darkTokens(accent).let { t ->
    darkColorScheme(
        primary = t.primary,
        onPrimary = DolphyBlack,
        primaryContainer = t.container,
        onPrimaryContainer = DolphyWhite,
        secondary = t.secondary,
        onSecondary = DolphyBlack,
        secondaryContainer = DolphyCardHigh,
        onSecondaryContainer = DolphyWhite,
        tertiary = DolphyMuted,
        onTertiary = DolphyBlack,
        background = DolphyBlack,
        onBackground = DolphyWhite,
        surface = DolphyCard,
        onSurface = DolphyWhite,
        surfaceVariant = DolphyCardHigh,
        onSurfaceVariant = DolphyMuted,
        surfaceContainer = DolphyCard,
        surfaceContainerHigh = DolphyCardHigh,
        surfaceContainerLow = DolphyCardSoft,
        outline = DolphyLine,
        outlineVariant = Color(0xFF292929),
        error = Color(0xFFFF8A8A),
        onError = Color(0xFF2B0000),
        errorContainer = Color(0xFF4A1414),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color(0xCC000000),
    )
}

private fun lightScheme(accent: DolphyAccent) = lightTokens(accent).let { t ->
    lightColorScheme(
        primary = t.primary,
        onPrimary = Color.White,
        primaryContainer = t.container,
        onPrimaryContainer = DolphyLightText,
        secondary = t.secondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFECECE8),
        onSecondaryContainer = DolphyLightText,
        background = DolphyLightBg,
        onBackground = DolphyLightText,
        surface = DolphyLightCard,
        onSurface = DolphyLightText,
        surfaceVariant = Color(0xFFECECE8),
        onSurfaceVariant = DolphyLightMuted,
        surfaceContainer = DolphyLightCard,
        surfaceContainerHigh = Color(0xFFF0F0EC),
        surfaceContainerLow = Color(0xFFF7F7F4),
        outline = DolphyLightLine,
        outlineVariant = Color(0xFFE9E9E5),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )
}

/**
 * Material You colours keep the Dolphy monochrome canvas and take only the accent
 * roles from the system wallpaper palette.
 */
private fun dynamicScheme(context: Context, dark: Boolean): ColorScheme {
    val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return if (dark) {
        base.copy(
            background = DolphyBlack,
            onBackground = DolphyWhite,
            surface = DolphyCard,
            onSurface = DolphyWhite,
            surfaceVariant = DolphyCardHigh,
            onSurfaceVariant = DolphyMuted,
            surfaceContainer = DolphyCard,
            surfaceContainerHigh = DolphyCardHigh,
            surfaceContainerLow = DolphyCardSoft,
            outline = DolphyLine,
        )
    } else {
        base.copy(
            background = DolphyLightBg,
            onBackground = DolphyLightText,
            surface = DolphyLightCard,
            onSurface = DolphyLightText,
            surfaceVariant = Color(0xFFECECE8),
            onSurfaceVariant = DolphyLightMuted,
            surfaceContainer = DolphyLightCard,
            surfaceContainerHigh = Color(0xFFF0F0EC),
            surfaceContainerLow = Color(0xFFF7F7F4),
            outline = DolphyLightLine,
        )
    }
}

/** Global motion gate used by the pixel banner and transitions. */
val LocalDolphyReduceMotion = staticCompositionLocalOf { false }

/** When enabled the display font switches to the bundled Flipper pixel font. */
val LocalDolphyFlipperFont = staticCompositionLocalOf { false }

@Composable
fun DolphyXTheme(
    darkTheme: Boolean = true,
    accent: DolphyAccent = DolphyAccent.RAINBOW,
    uiScale: Float = 1f,
    reduceMotion: Boolean = false,
    flipperFont: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        accent == DolphyAccent.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicScheme(context, darkTheme)
        darkTheme -> darkScheme(accent)
        else -> lightScheme(accent)
    }
    val displayFamily = if (flipperFont) DolphyDisplayFont else FontFamily.SansSerif

    val typography = remember(displayFamily) {
        Typography(
            displayLarge = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
            displayMedium = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp),
            displaySmall = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
            headlineLarge = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
            headlineMedium = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
            headlineSmall = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
            titleLarge = TextStyle(fontFamily = displayFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
            titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
        )
    }

    val shapes = remember {
        Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp),
        )
    }

    val density = LocalDensity.current
    val scaledDensity = remember(density, uiScale) {
        val scale = uiScale.coerceIn(0.85f, 1.35f)
        Density(density.density, density.fontScale * scale)
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalDolphyReduceMotion provides reduceMotion,
        LocalDolphyFlipperFont provides flipperFont,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
