package com.aegissoft.dolphyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.service.DolphyXHardwareService
import com.aegissoft.dolphyx.engine.ble.BleDeviceItem
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyPixelBanner
import com.aegissoft.dolphyx.ui.components.DolphyPrimaryPillButton
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphySegmentedPill
import com.aegissoft.dolphyx.ui.components.DolphySignalBars
import com.aegissoft.dolphyx.ui.components.DolphyToolRow
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge
import com.aegissoft.dolphyx.ui.theme.DolphyNumericFont

private val homeFilters = listOf(
    R.string.home_filter_overview,
    R.string.home_filter_bonded,
    R.string.home_filter_nearby,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: DolphyXViewModel,
    onNavigate: (String) -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val isScanning by viewModel.bleEngine.isScanning.collectAsState()
    val devices by viewModel.bleEngine.devices.collectAsState()
    val connectedAddress by viewModel.bleEngine.connectedAddress.collectAsState()
    val serviceRunning by DolphyXHardwareService.isRunning.collectAsState()

    var filterIndex by remember { mutableIntStateOf(0) }
    val filteredDevices = remember(devices, filterIndex) {
        when (filterIndex) {
            1 -> devices.filter { it.isBonded }
            2 -> devices.filter { it.rssi != 0 }
            else -> devices
        }
    }
    val strongest = devices.filter { it.rssi != 0 }.maxByOrNull { it.rssi }
    val batteryExempt = DolphyXHardwareService.isBatteryOptimizationIgnored(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.nav_home),
                subtitle = stringResource(R.string.app_subtitle),
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }

        item {
            DolphySegmentedPill(
                options = homeFilters.map { stringResource(it) },
                selectedIndex = filterIndex,
                onSelect = { filterIndex = it },
            )
        }

        item {
            DolphyPixelBanner(
                imageRes = R.drawable.px_scene,
                contentDescription = stringResource(R.string.app_subtitle),
            )
        }

        item {
            DolphyXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${filteredDevices.size}",
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = DolphyNumericFont,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.home_lcd_devices),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strongest?.rssi?.toString() ?: "--",
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = DolphyNumericFont,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.home_lcd_rssi),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DolphyPrimaryPillButton(
                    text = if (isScanning) stringResource(R.string.action_stop) else stringResource(R.string.home_scan_button),
                    icon = if (isScanning) Icons.Rounded.Stop else Icons.Rounded.BluetoothSearching,
                    onClick = {
                        if (isScanning) {
                            viewModel.bleEngine.stopScan()
                        } else {
                            onRequestPermissions()
                            viewModel.bleEngine.refreshBondedDevices()
                            viewModel.bleEngine.startScan()
                        }
                    },
                )
                Text(
                    text = strongest?.let { "${it.name}  ${it.address}" }
                        ?: stringResource(R.string.home_lcd_idle_hint),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DolphyXStatusBadge(
                    label = if (serviceRunning) stringResource(R.string.status_active) else stringResource(R.string.status_idle),
                    active = serviceRunning,
                    icon = Icons.Rounded.Memory,
                )
                SmallPill(
                    text = if (serviceRunning) stringResource(R.string.dashboard_stop_service) else stringResource(R.string.dashboard_start_service),
                    icon = if (serviceRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    onClick = {
                        if (serviceRunning) DolphyXHardwareService.stop(context) else DolphyXHardwareService.start(context)
                    },
                )
                SmallPill(
                    text = if (batteryExempt) stringResource(R.string.dashboard_battery_exempt) else stringResource(R.string.dashboard_battery_opt),
                    icon = Icons.Rounded.BatteryChargingFull,
                    onClick = { context.startActivity(DolphyXHardwareService.createBatteryOptimizationIntent(context)) },
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_devices_hint).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (filteredDevices.isEmpty()) {
            item {
                DolphyXCard {
                    DolphyEmptyState(
                        title = stringResource(R.string.ble_empty_devices),
                        subtitle = stringResource(R.string.home_devices_empty_hint),
                        icon = Icons.Rounded.BluetoothSearching,
                    )
                }
            }
        } else {
            items(filteredDevices.take(8), key = { it.address }) { device ->
                HomeDeviceRow(
                    device = device,
                    isConnected = connectedAddress == device.address,
                    onConnect = {
                        onRequestPermissions()
                        viewModel.bleEngine.connectGatt(device.address)
                    },
                    onDisconnect = { viewModel.bleEngine.disconnectGatt() },
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.home_quick_actions).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        item {
            DolphyToolRow(
                title = stringResource(R.string.nav_ir),
                subtitle = stringResource(R.string.tool_ir_desc),
                icon = Icons.Rounded.SettingsRemote,
                onClick = { onNavigate("ir") },
            )
        }
        item {
            DolphyToolRow(
                title = stringResource(R.string.nav_nfc),
                subtitle = stringResource(R.string.tool_nfc_desc),
                icon = Icons.Rounded.Nfc,
                onClick = { onNavigate("nfc") },
            )
        }
        item {
            DolphyToolRow(
                title = stringResource(R.string.nav_network),
                subtitle = stringResource(R.string.tool_network_desc),
                icon = Icons.Rounded.Lan,
                onClick = { onNavigate("network") },
            )
        }
        item {
            DolphyToolRow(
                title = stringResource(R.string.nav_plugins),
                subtitle = stringResource(R.string.tool_plugins_desc),
                icon = Icons.Rounded.Extension,
                onClick = { onNavigate("plugins") },
            )
        }
    }
}

@Composable
private fun SmallPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeDeviceRow(
    device: BleDeviceItem,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (device.isBonded) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                    contentDescription = device.name,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.vendor ?: device.address,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DolphySignalBars(rssi = device.rssi)
            Surface(
                onClick = if (isConnected) onDisconnect else onConnect,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isConnected) Icons.Rounded.Stop else Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}
