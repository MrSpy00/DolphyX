package com.aegissoft.dolphyx.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.engine.ble.BleChatEngine
import com.aegissoft.dolphyx.engine.ble.ChatMessage
import com.aegissoft.dolphyx.engine.ble.ChatPeer
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyPrimaryPillButton
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { BleChatEngine(context) }
    val peers by engine.peers.collectAsState()
    val messages by engine.messages.collectAsState()
    val status by engine.status.collectAsState()
    val hosting by engine.hosting.collectAsState()
    val scanning by engine.scanning.collectAsState()
    val connected by engine.connectedName.collectAsState()
    var draft by remember { mutableStateOf("") }
    var permissionsGranted by remember { mutableStateOf(engine.hasPermissions) }
    val listState = rememberLazyListState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionsGranted = engine.hasPermissions }

    LaunchedEffect(messages.size, peers.size) {
        if (messages.isNotEmpty()) {
            val messageStart = 1 + if (peers.isEmpty()) 0 else 1 + peers.size
            listState.animateScrollToItem(messageStart + messages.size - 1)
        }
    }
    DisposableEffect(Unit) {
        onDispose { engine.release() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
            DolphyScreenHeader(
                title = stringResource(R.string.chat_title),
                subtitle = stringResource(R.string.chat_subtitle),
                onBack = onBack,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DolphyXCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DolphyXStatusBadge(
                            label = connected ?: if (hosting) stringResource(R.string.chat_hosting) else stringResource(R.string.status_idle),
                            active = connected != null || hosting,
                            icon = Icons.Rounded.Forum,
                        )
                    }
                    if (!permissionsGranted) {
                        DolphyPrimaryPillButton(
                            text = stringResource(R.string.chat_grant),
                            icon = Icons.Rounded.Forum,
                            onClick = {
                                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_ADVERTISE,
                                    )
                                } else {
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                                permissionLauncher.launch(permissions)
                            },
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DolphyPrimaryPillButton(
                                text = if (hosting) stringResource(R.string.chat_stop_host) else stringResource(R.string.chat_host),
                                icon = if (hosting) Icons.Rounded.Stop else Icons.Rounded.WifiTethering,
                                modifier = Modifier.weight(1f),
                                onClick = { if (hosting) engine.stopHosting() else engine.startHosting() },
                            )
                            DolphyPrimaryPillButton(
                                text = if (scanning) stringResource(R.string.action_stop) else stringResource(R.string.chat_find),
                                icon = if (scanning) Icons.Rounded.Stop else Icons.Rounded.Radar,
                                modifier = Modifier.weight(1f),
                                onClick = { if (scanning) engine.stopScan() else engine.startScan() },
                            )
                        }
                    }
                    if (!status.isNullOrBlank()) {
                        Text(
                            text = status.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (peers.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.chat_peers),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(peers, key = { it.address }) { peer ->
                    PeerRow(peer = peer, onConnect = { engine.connect(peer.address) })
                }
            }

            if (messages.isEmpty()) {
                item {
                    DolphyXCard {
                        DolphyEmptyState(
                            title = stringResource(R.string.chat_empty),
                            subtitle = stringResource(R.string.chat_empty_hint),
                            icon = Icons.Rounded.Forum,
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
            )
            Surface(
                onClick = {
                    if (draft.isNotBlank()) {
                        engine.send(draft)
                        draft = ""
                    }
                },
                enabled = connected != null,
                shape = RoundedCornerShape(999.dp),
                color = if (connected != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = if (connected != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp).size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun PeerRow(peer: ChatPeer, onConnect: () -> Unit) {
    Surface(
        onClick = onConnect,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Forum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = peer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.chat_connect),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (message.outgoing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (!message.outgoing) {
                    Text(
                        text = message.from,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.outgoing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
