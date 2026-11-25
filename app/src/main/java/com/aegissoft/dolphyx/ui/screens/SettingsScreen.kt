package com.aegissoft.dolphyx.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import android.security.NetworkSecurityPolicy
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegissoft.dolphyx.BuildConfig
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import com.aegissoft.dolphyx.core.security.SecurePreferences
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphySegmentedPill
import com.aegissoft.dolphyx.ui.components.DolphySwitchRow
import com.aegissoft.dolphyx.ui.components.DolphyToolRow
import com.aegissoft.dolphyx.ui.components.DolphyValueRow
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.theme.DolphyAccent
import com.aegissoft.dolphyx.ui.theme.DolphyAccentCyan
import com.aegissoft.dolphyx.ui.theme.DolphyAccentGreen
import com.aegissoft.dolphyx.ui.theme.DolphyAccentOrange
import com.aegissoft.dolphyx.ui.theme.DolphyAccentPurple
import com.aegissoft.dolphyx.ui.theme.DolphyAccentRed
import com.aegissoft.dolphyx.ui.theme.DolphyRainbowColors
import com.aegissoft.dolphyx.ui.theme.LocalDolphyReduceMotion

@Composable
fun SettingsScreen(
    viewModel: DolphyXViewModel,
    onRecreateActivity: () -> Unit,
    onOpenPlugins: () -> Unit,
) {
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val accent by viewModel.accent.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val flipperFont by viewModel.flipperFont.collectAsState()
    val fabVisible by viewModel.fabVisible.collectAsState()
    val fabAction by viewModel.fabAction.collectAsState()
    val menuView by viewModel.menuView.collectAsState()
    val menuStyle by viewModel.menuStyle.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val reduceMotionSetting by viewModel.reduceMotion.collectAsState()
    val allowBackup = remember(context) {
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0
    }
    val cleartextAllowed = remember { NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(title = stringResource(R.string.settings_title))
        }

        item {
            DolphyXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    AnimatedDolphin(reduceMotion = reduceMotionSetting)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = DolphyXConfig.appBrand,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = com.aegissoft.dolphyx.ui.theme.DolphyNumericFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionLabel(stringResource(R.string.settings_section_general)) }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DolphySegmentedPill(
                    options = listOf(
                        stringResource(R.string.settings_language_turkish),
                        stringResource(R.string.settings_language_english),
                    ),
                    selectedIndex = if (language == "en") 1 else 0,
                    onSelect = { index ->
                        val code = if (index == 0) "tr" else "en"
                        if (code != language) {
                            viewModel.setLanguage(code)
                            onRecreateActivity()
                        }
                    },
                )
            }
        }

        item {
            DolphyXCard {
                DolphySwitchRow(
                    title = stringResource(R.string.settings_performance),
                    subtitle = stringResource(R.string.settings_performance_hint),
                    checked = reduceMotion,
                    onCheckedChange = { viewModel.setReduceMotion(it) },
                )
            }
        }

        item {
            DolphyToolRow(
                title = stringResource(R.string.nav_plugins),
                subtitle = stringResource(R.string.settings_plugins_row_hint),
                icon = Icons.Rounded.Extension,
                onClick = onOpenPlugins,
            )
        }

        item { SectionLabel(stringResource(R.string.settings_section_personalisation)) }

        item {
            DolphyXCard {
                DolphySwitchRow(
                    title = stringResource(R.string.settings_flipper_font),
                    subtitle = stringResource(R.string.settings_flipper_font_hint),
                    checked = flipperFont,
                    onCheckedChange = { viewModel.setFlipperFont(it) },
                )
            }
        }

        item {
            DolphyXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_interface_scale),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(uiScale * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = uiScale,
                    onValueChange = { viewModel.setUiScale(it) },
                    valueRange = 0.85f..1.35f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.onSurface,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DolphySegmentedPill(
                    options = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_dark),
                        stringResource(R.string.settings_theme_light),
                    ),
                    selectedIndex = when (themeMode) {
                        SecurePreferences.THEME_LIGHT -> 2
                        SecurePreferences.THEME_DARK -> 1
                        else -> 0
                    },
                    onSelect = { index ->
                        viewModel.setThemeMode(
                            when (index) {
                                2 -> SecurePreferences.THEME_LIGHT
                                1 -> SecurePreferences.THEME_DARK
                                else -> SecurePreferences.THEME_SYSTEM
                            },
                        )
                    },
                )
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.settings_accent_color),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_accent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    item {
                        AccentSwatch(
                            label = stringResource(R.string.accent_rainbow),
                            selected = accent == SecurePreferences.ACCENT_RAINBOW,
                            brush = Brush.linearGradient(DolphyRainbowColors),
                            onClick = { viewModel.setAccent(SecurePreferences.ACCENT_RAINBOW) },
                        )
                    }
                    item {
                        AccentSwatch(
                            label = stringResource(R.string.accent_orange),
                            selected = accent == SecurePreferences.ACCENT_ORANGE,
                            brush = SolidColor(DolphyAccentOrange),
                            onClick = { viewModel.setAccent(SecurePreferences.ACCENT_ORANGE) },
                        )
                    }
                    item {
                        AccentSwatch(
                            label = stringResource(R.string.accent_red),
                            selected = accent == SecurePreferences.ACCENT_RED,
                            brush = SolidColor(DolphyAccentRed),
                            onClick = { viewModel.setAccent(SecurePreferences.ACCENT_RED) },
                        )
                    }
                    item {
                        AccentSwatch(
                            label = stringResource(R.string.accent_green),
                            selected = accent == SecurePreferences.ACCENT_GREEN,
                            brush = SolidColor(DolphyAccentGreen),
                            onClick = { viewModel.setAccent(SecurePreferences.ACCENT_GREEN) },
                        )
                    }
                    item {
                        AccentSwatch(
                            label = stringResource(R.string.accent_purple),
                            selected = accent == SecurePreferences.ACCENT_PURPLE,
                            brush = SolidColor(DolphyAccentPurple),
                            onClick = { viewModel.setAccent(SecurePreferences.ACCENT_PURPLE) },
                        )
                    }
                    item {
                        AccentSwatch(
                            label = stringResource(R.string.accent_cyan),
                            selected = accent == SecurePreferences.ACCENT_CYAN,
                            brush = SolidColor(DolphyAccentCyan),
                            onClick = { viewModel.setAccent(SecurePreferences.ACCENT_CYAN) },
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            AccentSwatch(
                                label = stringResource(R.string.accent_adaptive),
                                selected = accent == SecurePreferences.ACCENT_DYNAMIC,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF7BE3A0),
                                        Color(0xFF56CCF2),
                                        Color(0xFFC455F7),
                                        Color(0xFFF2545B),
                                        Color(0xFF7BE3A0),
                                    ),
                                ),
                                onClick = { viewModel.setAccent(SecurePreferences.ACCENT_DYNAMIC) },
                            )
                        }
                    }
                }
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.settings_menu_view),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DolphySegmentedPill(
                    options = listOf(
                        stringResource(R.string.settings_menu_containers),
                        stringResource(R.string.settings_menu_tiles),
                    ),
                    selectedIndex = if (menuView == SecurePreferences.MENU_VIEW_TILES) 1 else 0,
                    onSelect = { index ->
                        viewModel.setMenuView(
                            if (index == 1) SecurePreferences.MENU_VIEW_TILES else SecurePreferences.MENU_VIEW_CONTAINERS,
                        )
                    },
                )
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.settings_style),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DolphySegmentedPill(
                    options = listOf(
                        stringResource(R.string.settings_style_standard),
                        stringResource(R.string.settings_style_compact),
                    ),
                    selectedIndex = if (menuStyle == SecurePreferences.STYLE_COMPACT) 1 else 0,
                    onSelect = { index ->
                        viewModel.setMenuStyle(
                            if (index == 1) SecurePreferences.STYLE_COMPACT else SecurePreferences.STYLE_STANDARD,
                        )
                    },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_section_bottom_panel)) }

        item {
            DolphyXCard {
                DolphySwitchRow(
                    title = stringResource(R.string.settings_fab_button),
                    subtitle = stringResource(R.string.settings_fab_button_hint),
                    checked = fabVisible,
                    onCheckedChange = { viewModel.setFabVisible(it) },
                )
                Text(
                    text = stringResource(R.string.settings_fab_action),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DolphySegmentedPill(
                    options = listOf(
                        stringResource(R.string.settings_fab_functions),
                        stringResource(R.string.settings_fab_settings),
                    ),
                    selectedIndex = if (fabAction == SecurePreferences.FAB_ACTION_SETTINGS) 1 else 0,
                    onSelect = { index ->
                        viewModel.setFabAction(
                            if (index == 1) SecurePreferences.FAB_ACTION_SETTINGS else SecurePreferences.FAB_ACTION_FUNCTIONS,
                        )
                    },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.settings_section_security)) }

        item {
            DolphyXCard {
                SecurityRow(
                    label = stringResource(R.string.security_cleartext),
                    value = stringResource(
                        if (cleartextAllowed) R.string.status_allowed else R.string.status_blocked,
                    ),
                )
                SecurityRow(
                    label = stringResource(R.string.security_backup),
                    value = stringResource(
                        if (allowBackup) R.string.status_allowed else R.string.status_blocked,
                    ),
                )
                SecurityRow(
                    label = stringResource(R.string.security_hce_unlock),
                    value = stringResource(R.string.status_required),
                )
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_about_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = DolphyXConfig.appWebsite,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { uriHandler.openUri("https://${DolphyXConfig.appWebsite}") },
                )
                Text(
                    text = DolphyXConfig.githubUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { uriHandler.openUri(DolphyXConfig.githubUrl) },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun AccentSwatch(
    label: String,
    selected: Boolean,
    brush: Brush,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(brush, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF0A0A0A),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SecurityRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Two-frame pixel dolphin bob; a small piece of the Aseprite identity in the UI. */
@Composable
private fun AnimatedDolphin(reduceMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "dolphin")
    val offset = transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dolphin-bob",
    )
    Image(
        painter = painterResource(id = R.drawable.px_logo),
        contentDescription = stringResource(R.string.nav_home),
        modifier = Modifier
            .size(96.dp)
            .offset(y = if (reduceMotion) 0.dp else offset.value.dp),
    )
}
