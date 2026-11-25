package com.aegissoft.dolphyx.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.engine.cast.DlnaCastEngine
import com.aegissoft.dolphyx.engine.cast.DlnaDevice
import com.aegissoft.dolphyx.engine.cast.LocalMediaServer
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyPrimaryPillButton
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CastScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { DlnaCastEngine() }
    val mediaServer = remember { LocalMediaServer() }

    var devices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var discovering by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DlnaDevice?>(null) }
    var mediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var mediaName by remember { mutableStateOf<String?>(null) }
    var casting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var localUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { mediaServer.stop() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mediaUri = uri
            mediaName = uri.lastPathSegment?.substringAfterLast('/')
            statusMessage = null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.cast_title),
                subtitle = stringResource(R.string.cast_subtitle),
                onBack = onBack,
            )
        }

        item {
            DolphyXCard {
                DolphyXStatusBadge(
                    label = if (discovering) stringResource(R.string.status_scanning) else stringResource(R.string.status_idle),
                    active = discovering,
                    icon = Icons.Rounded.Radar,
                )
                DolphyPrimaryPillButton(
                    text = stringResource(R.string.cast_discover),
                    icon = Icons.Rounded.Cast,
                    onClick = {
                        if (discovering) return@DolphyPrimaryPillButton
                        discovering = true
                        scope.launch {
                            val found = engine.discover()
                            devices = found
                            discovering = false
                        }
                    },
                )
            }
        }

        if (devices.isEmpty()) {
            item {
                DolphyXCard {
                    DolphyEmptyState(
                        title = stringResource(R.string.cast_empty),
                        subtitle = stringResource(R.string.cast_empty_hint),
                        icon = Icons.Rounded.LiveTv,
                    )
                }
            }
        } else {
            items(devices, key = { it.controlUrl }) { device ->
                val isSelected = selected?.controlUrl == device.controlUrl
                Surface(
                    onClick = { selected = device },
                    shape = RoundedCornerShape(28.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.cast_select_media),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("video/*", "audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = mediaName ?: stringResource(R.string.cast_no_media),
                        modifier = Modifier.padding(start = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DolphyPrimaryPillButton(
                        text = stringResource(R.string.cast_action),
                        icon = Icons.Rounded.Cast,
                        enabled = !casting,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = selected
                            val uri = mediaUri
                            when {
                                target == null -> statusMessage = context.getString(R.string.cast_need_device)
                                uri == null -> statusMessage = context.getString(R.string.cast_need_media)
                                else -> scope.launch {
                                    casting = true
                                    statusMessage = null
                                    val session = withContext(Dispatchers.IO) { mediaServer.start(context, uri) }
                                    if (session == null) {
                                        casting = false
                                        statusMessage = context.getString(R.string.cast_failed)
                                        return@launch
                                    }
                                    localUrl = session.url
                                    val configured = engine.setAvTransportUri(target, session.url)
                                    val playing = configured && engine.play(target)
                                    casting = playing
                                    statusMessage = if (playing) {
                                        context.getString(R.string.cast_playing, mediaName.orEmpty())
                                    } else {
                                        context.getString(R.string.cast_failed)
                                    }
                                }
                            }
                        },
                    )
                    Surface(
                        onClick = {
                            val target = selected
                            if (target != null) {
                                scope.launch {
                                    engine.stop(target)
                                    mediaServer.stop()
                                    casting = false
                                    localUrl = null
                                    statusMessage = null
                                }
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = stringResource(R.string.cast_stop), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val url = localUrl
                if (url != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("cast", url))
                                Toast.makeText(context, context.getString(R.string.cast_copied), Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.cast_copy_url),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp).size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
