package com.aegissoft.dolphyx.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.aegissoft.dolphyx.ui.theme.DolphyAccentGreen
import com.aegissoft.dolphyx.ui.theme.DolphyAccentOrange
import com.aegissoft.dolphyx.ui.theme.DolphyAccentPurple
import com.aegissoft.dolphyx.ui.theme.DolphyAccentRed
import com.aegissoft.dolphyx.ui.theme.DolphyRainbowColors
import com.aegissoft.dolphyx.ui.theme.DolphyWhite

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
                    Image(
                        painter = painterResource(id = R.drawable.px_dolphin),
                        contentDescription = stringResource(R.string.nav_home),
                        modifier = Modifier.size(96.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.settings_passport),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.bodySmall,
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
                    valueRange = 0.85f..1.25f,
                    steps = 7,
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
                DolphySwitchRow(
                    title = stringResource(R.string.settings_cyclic_animation),
                    checked = !reduceMotion,
                    onCheckedChange = { viewModel.setReduceMotion(!it) },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AccentSwatch(
                        selected = accent == SecurePreferences.ACCENT_RAINBOW,
                        brush = Brush.linearGradient(DolphyRainbowColors),
                        onClick = { viewModel.setAccent(SecurePreferences.ACCENT_RAINBOW) },
                        modifier = Modifier.weight(1f),
                    )
                    AccentSwatch(
                        selected = accent == SecurePreferences.ACCENT_ORANGE,
                        brush = SolidColor(DolphyAccentOrange),
                        onClick = { viewModel.setAccent(SecurePreferences.ACCENT_ORANGE) },
                        modifier = Modifier.weight(1f),
                    )
                    AccentSwatch(
                        selected = accent == SecurePreferences.ACCENT_RED,
                        brush = SolidColor(DolphyAccentRed),
                        onClick = { viewModel.setAccent(SecurePreferences.ACCENT_RED) },
                        modifier = Modifier.weight(1f),
                    )
                    AccentSwatch(
                        selected = accent == SecurePreferences.ACCENT_GREEN,
                        brush = SolidColor(DolphyAccentGreen),
                        onClick = { viewModel.setAccent(SecurePreferences.ACCENT_GREEN) },
                        modifier = Modifier.weight(1f),
                    )
                    AccentSwatch(
                        selected = accent == SecurePreferences.ACCENT_PURPLE,
                        brush = SolidColor(DolphyAccentPurple),
                        onClick = { viewModel.setAccent(SecurePreferences.ACCENT_PURPLE) },
                        modifier = Modifier.weight(1f),
                    )
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
                SecurityRow(stringResource(R.string.settings_cleartext_blocked))
                SecurityRow(stringResource(R.string.settings_backup_disabled))
                SecurityRow(stringResource(R.string.settings_hce_unlock))
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
    selected: Boolean,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(brush, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = DolphyWhite,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SecurityRow(text: String) {
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
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
