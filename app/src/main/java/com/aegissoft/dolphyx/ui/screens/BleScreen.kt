package com.aegissoft.dolphyx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.engine.ble.BleDeviceItem
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge

private enum class BleFilter {
    ALL,
    BONDED,
    LIVE,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BleScreen(
    viewModel: DolphyXViewModel,
    onRequestBlePermissions: () -> Unit,
    onBack: () -> Unit,
) {
    val isScanning by viewModel.bleEngine.isScanning.collectAsState()
    val devices by viewModel.bleEngine.devices.collectAsState()
    val gattServices by viewModel.bleEngine.gattServices.collectAsState()
    val connectedAddress by viewModel.bleEngine.connectedAddress.collectAsState()
    val statusMessage by viewModel.bleEngine.statusMessage.collectAsState()

    val adapterEnabled = viewModel.bleEngine.isBluetoothEnabled()
    var activeFilter by remember { mutableStateOf(BleFilter.ALL) }

    val filteredDevices = remember(devices, activeFilter) {
        when (activeFilter) {
            BleFilter.ALL -> devices
            BleFilter.BONDED -> devices.filter { it.isBonded }
            BleFilter.LIVE -> devices.filter { !it.isBonded || it.rssi != 0 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.ble_title),
                subtitle = statusMessage ?: if (adapterEnabled) {
                    stringResource(R.string.status_ready)
                } else {
                    stringResource(R.string.ble_adapter_disabled)
                },
                onBack = onBack,
            )
        }

        item {
            DolphyXCard {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DolphyXStatusBadge(
                        label = if (adapterEnabled) {
                            stringResource(R.string.status_active)
                        } else {
                            stringResource(R.string.ble_adapter_disabled)
                        },
                        active = adapterEnabled,
                        icon = Icons.Rounded.Bluetooth,
                    )
                    DolphyXStatusBadge(
                        label = if (isScanning) {
                            stringResource(R.string.status_scanning)
                        } else {
                            stringResource(R.string.status_idle)
                        },
                        active = isScanning,
                        icon = Icons.Rounded.BluetoothSearching,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            if (isScanning) {
                                viewModel.bleEngine.stopScan()
                            } else {
                                onRequestBlePermissions()
                                viewModel.bleEngine.refreshBondedDevices()
                                viewModel.bleEngine.startScan()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Rounded.Stop else Icons.Rounded.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScanning) {
                                stringResource(R.string.ble_stop_scan)
                            } else {
                                stringResource(R.string.ble_start_scan)
                            },
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            onRequestBlePermissions()
                            viewModel.bleEngine.refreshBondedDevices()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.ble_bonded_devices))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = activeFilter == BleFilter.ALL,
                        onClick = { activeFilter = BleFilter.ALL },
                        label = { Text(text = "${stringResource(R.string.ir_categories_all)} (${devices.size})") },
                    )
                    FilterChip(
                        selected = activeFilter == BleFilter.BONDED,
                        onClick = { activeFilter = BleFilter.BONDED },
                        label = {
                            Text(
                                text = "${stringResource(R.string.ble_bonded_devices)} (${devices.count { it.isBonded }})",
                            )
                        },
                    )
                    FilterChip(
                        selected = activeFilter == BleFilter.LIVE,
                        onClick = { activeFilter = BleFilter.LIVE },
                        label = { Text(text = stringResource(R.string.ble_discovered_devices)) },
                    )
                }
            }
        }

        if (connectedAddress != null) {
            item {
                DolphyXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ble_services_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = connectedAddress ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        OutlinedButton(onClick = { viewModel.bleEngine.disconnectGatt() }) {
                            Icon(
                                imageVector = Icons.Rounded.LinkOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.ble_disconnect_gatt))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (gattServices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.status_scanning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        gattServices.forEach { svc ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                Text(
                                    text = svc.uuid,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                svc.characteristicUuids.forEach { chUuid ->
                                    Text(
                                        text = "  $chUuid",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filteredDevices.isEmpty()) {
            item {
                DolphyXCard {
                    Text(
                        text = stringResource(R.string.ble_empty_devices),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(filteredDevices, key = { it.address }) { device ->
                BleDeviceRow(
                    device = device,
                    isConnected = connectedAddress == device.address,
                    onConnect = {
                        onRequestBlePermissions()
                        viewModel.bleEngine.connectGatt(device.address)
                    },
                    onDisconnect = { viewModel.bleEngine.disconnectGatt() },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BleDeviceRow(
    device: BleDeviceItem,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    DolphyXCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = device.vendor ?: stringResource(R.string.ble_vendor_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            if (isConnected) {
                OutlinedButton(onClick = onDisconnect) {
                    Icon(
                        imageVector = Icons.Rounded.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.ble_disconnect_gatt))
                }
            } else {
                FilledTonalButton(onClick = onConnect) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.ble_connect_gatt))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (device.isBonded) {
                DolphyXStatusBadge(
                    label = stringResource(R.string.ble_bonded_devices),
                    active = true,
                    icon = Icons.Rounded.BluetoothConnected,
                )
            }
            if (device.rssi != 0) {
                DolphyXStatusBadge(
                    label = stringResource(R.string.ble_rssi_label, device.rssi),
                    active = device.rssi > -75,
                    icon = Icons.Rounded.Memory,
                )
            }
            DolphyXStatusBadge(
                label = "TRANSPORT_LE",
                active = true,
                icon = Icons.Rounded.Security,
            )
        }
    }
}
