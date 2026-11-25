package com.aegissoft.dolphyx.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HidRemoteScreen(viewModel: DolphyXViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val status by viewModel.hidController.status.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshTick by remember { mutableIntStateOf(0) }
    var textDraft by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    val hosts = remember(refreshTick) { viewModel.hidController.bondedDevices() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.hid_title),
                subtitle = stringResource(R.string.hid_subtitle),
                onBack = onBack,
            )
        }
        item {
            DolphyXCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DolphyXStatusBadge(
                        label = if (status.supported) stringResource(R.string.status_supported) else stringResource(R.string.hid_not_supported),
                        active = status.supported,
                        icon = Icons.Rounded.Bluetooth,
                    )
                    DolphyXStatusBadge(
                        label = if (status.registered) stringResource(R.string.hid_registered) else stringResource(R.string.hid_not_registered),
                        active = status.registered,
                        icon = Icons.Rounded.BluetoothConnected,
                    )
                    DolphyXStatusBadge(
                        label = status.connectedHost ?: stringResource(R.string.hid_no_host),
                        active = status.connectedHost != null,
                        icon = Icons.Rounded.Keyboard,
                    )
                }
                Text(
                    text = stringResource(R.string.hid_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.hidController.start()
                            feedback = context.getString(R.string.hid_registering)
                        },
                        enabled = status.supported,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = stringResource(R.string.hid_register), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            refreshTick++
                            feedback = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = stringResource(R.string.hid_refresh), maxLines = 1)
                    }
                }
            }
        }

        item {
            DolphyXSectionHeader(
                title = stringResource(R.string.hid_hosts),
                subtitle = stringResource(R.string.hid_hosts_hint),
                icon = Icons.Rounded.BluetoothConnected,
            )
        }

        if (hosts.isEmpty()) {
            item {
                DolphyXCard {
                    Text(
                        text = stringResource(R.string.hid_no_hosts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(hosts, key = { it.address }) { host ->
                HidHostRow(
                    host = host,
                    connected = status.connectedHost == safeName(host),
                    onConnect = {
                        viewModel.hidController.connectTo(host)
                        feedback = context.getString(R.string.hid_connecting, safeName(host))
                    },
                    onDisconnect = { viewModel.hidController.disconnect() },
                )
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.hid_keyboard),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    label = { Text(text = stringResource(R.string.hid_text_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val payload = textDraft
                        if (payload.isBlank()) return@Button
                        scope.launch(Dispatchers.IO) {
                            val result = viewModel.hidController.sendText(payload)
                            feedback = result.fold(
                                onSuccess = { context.getString(R.string.hid_sent_count, it) },
                                onFailure = { context.getString(R.string.hid_send_failed) },
                            )
                        }
                    },
                    enabled = status.connectedHost != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.hid_send_text))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) { viewModel.hidController.sendConsumer(true) }
                        },
                        enabled = status.connectedHost != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = stringResource(R.string.hid_volume_up), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) { viewModel.hidController.sendConsumer(false) }
                        },
                        enabled = status.connectedHost != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Rounded.VolumeDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = stringResource(R.string.hid_volume_down), maxLines = 1)
                    }
                }
            }
        }

        item {
            DolphyXCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Rounded.Mouse, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.hid_trackpad),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = stringResource(R.string.hid_trackpad_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                        .pointerInput(status.connectedHost) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                viewModel.hidController.sendMouse(0, amount.x.toInt(), amount.y.toInt(), 0)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (status.connectedHost == null) {
                            stringResource(R.string.hid_no_connection)
                        } else {
                            stringResource(R.string.hid_trackpad_active)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                viewModel.hidController.sendMouse(1, 0, 0, 0)
                                viewModel.hidController.sendMouse(0, 0, 0, 0)
                            }
                        },
                        enabled = status.connectedHost != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(text = stringResource(R.string.hid_left_click), maxLines = 1) }
                    FilledTonalButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                viewModel.hidController.sendMouse(2, 0, 0, 0)
                                viewModel.hidController.sendMouse(0, 0, 0, 0)
                            }
                        },
                        enabled = status.connectedHost != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(text = stringResource(R.string.hid_right_click), maxLines = 1) }
                    FilledTonalButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) { viewModel.hidController.sendMouse(0, 0, 0, 1) }
                        },
                        enabled = status.connectedHost != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(text = stringResource(R.string.hid_scroll), maxLines = 1) }
                }
            }
        }

        if (!feedback.isNullOrBlank()) {
            item {
                Text(
                    text = feedback.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HidHostRow(
    host: BluetoothDevice,
    connected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    DolphyXCard(contentPadding = PaddingValues(13.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = safeName(host),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = host.address,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (connected) {
                OutlinedButton(onClick = onDisconnect) {
                    Text(text = stringResource(R.string.hid_disconnect), maxLines = 1)
                }
            } else {
                FilledTonalButton(onClick = onConnect) {
                    Text(text = stringResource(R.string.hid_connect), maxLines = 1)
                }
            }
        }
    }
}

@Suppress("MissingPermission")
private fun safeName(device: BluetoothDevice): String {
    return try {
        device.name ?: device.address
    } catch (e: SecurityException) {
        device.address
    }
}
