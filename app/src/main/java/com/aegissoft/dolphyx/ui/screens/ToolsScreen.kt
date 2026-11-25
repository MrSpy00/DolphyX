package com.aegissoft.dolphyx.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegissoft.dolphyx.DolphyRoutes
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.security.SecurePreferences
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphySearchBar
import com.aegissoft.dolphyx.ui.components.DolphyToolRow

private enum class ToolSection(val labelRes: Int) {
    INFRARED(R.string.tools_section_infrared),
    BLUETOOTH(R.string.tools_section_bluetooth),
    NFC(R.string.tools_section_nfc),
    NETWORK(R.string.tools_section_network),
    SYSTEM(R.string.tools_section_system),
}

private data class ToolEntry(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val route: String,
    val section: ToolSection,
)

private val toolEntries = listOf(
    ToolEntry(R.string.tool_ir_title, R.string.tool_ir_desc, Icons.Rounded.SettingsRemote, DolphyRoutes.IR, ToolSection.INFRARED),
    ToolEntry(R.string.tool_ble_title, R.string.tool_ble_desc, Icons.Rounded.Bluetooth, DolphyRoutes.BLE, ToolSection.BLUETOOTH),
    ToolEntry(R.string.chat_title, R.string.chat_subtitle, Icons.Rounded.Forum, DolphyRoutes.CHAT, ToolSection.BLUETOOTH),
    ToolEntry(R.string.tool_hid_title, R.string.tool_hid_desc, Icons.Rounded.Keyboard, DolphyRoutes.HID, ToolSection.BLUETOOTH),
    ToolEntry(R.string.tool_nfc_title, R.string.tool_nfc_desc, Icons.Rounded.Nfc, DolphyRoutes.NFC, ToolSection.NFC),
    ToolEntry(R.string.tool_network_title, R.string.tool_network_desc, Icons.Rounded.Lan, DolphyRoutes.NETWORK, ToolSection.NETWORK),
    ToolEntry(R.string.tool_lan_title, R.string.tool_lan_desc, Icons.Rounded.Lan, DolphyRoutes.LAN, ToolSection.NETWORK),
    ToolEntry(R.string.tool_printer_title, R.string.tool_printer_desc, Icons.Rounded.Print, DolphyRoutes.PRINTER, ToolSection.NETWORK),
    ToolEntry(R.string.camera_title, R.string.camera_subtitle, Icons.Rounded.Videocam, DolphyRoutes.CAMERA, ToolSection.NETWORK),
    ToolEntry(R.string.cast_title, R.string.cast_subtitle, Icons.Rounded.Cast, DolphyRoutes.CAST, ToolSection.NETWORK),
    ToolEntry(R.string.tool_plugins_title, R.string.tool_plugins_desc, Icons.Rounded.Extension, DolphyRoutes.PLUGINS, ToolSection.SYSTEM),
)

@Composable
fun ToolsScreen(
    viewModel: DolphyXViewModel,
    onNavigate: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val menuView by viewModel.menuView.collectAsState()
    val menuStyle by viewModel.menuStyle.collectAsState()

    val localized = toolEntries.map { entry ->
        entry to (stringResource(entry.titleRes) to stringResource(entry.descRes))
    }
    val filtered = localized.filter { (_, texts) ->
        query.isBlank() ||
            texts.first.contains(query, ignoreCase = true) ||
            texts.second.contains(query, ignoreCase = true)
    }
    val compact = menuStyle == SecurePreferences.STYLE_COMPACT

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        item {
            DolphySearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.tools_search_hint),
                icon = Icons.Rounded.Search,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        if (filtered.isEmpty()) {
            item {
                DolphyEmptyState(
                    title = stringResource(R.string.tools_title),
                    subtitle = stringResource(R.string.tools_empty),
                    icon = Icons.Rounded.Extension,
                )
            }
        } else if (menuView == SecurePreferences.MENU_VIEW_TILES) {
            val grouped = filtered.groupBy { it.first.section }
            ToolSection.entries.forEach { section ->
                val entries = grouped[section] ?: return@forEach
                item(key = "label_${section.name}") {
                    Text(
                        text = stringResource(section.labelRes).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(entries.chunked(2), key = { row -> row.joinToString("_") { it.first.route } }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { (entry, texts) ->
                            ToolTile(
                                title = texts.first,
                                icon = entry.icon,
                                onClick = { onNavigate(entry.route) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            val grouped = filtered.groupBy { it.first.section }
            ToolSection.entries.forEach { section ->
                val entries = grouped[section] ?: return@forEach
                item(key = "label_${section.name}") {
                    Text(
                        text = stringResource(section.labelRes).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(entries, key = { it.first.route }) { (entry, texts) ->
                    if (compact) {
                        CompactRow(
                            title = texts.first,
                            icon = entry.icon,
                            onClick = { onNavigate(entry.route) },
                        )
                    } else {
                        DolphyToolRow(
                            title = texts.first,
                            subtitle = texts.second,
                            icon = entry.icon,
                            onClick = { onNavigate(entry.route) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
