package com.aegissoft.dolphyx.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.engine.network.DiscoveredPrinter
import com.aegissoft.dolphyx.engine.network.IppPrintClient
import com.aegissoft.dolphyx.engine.network.PrinterDiscoveryEngine
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyPrimaryPillButton
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PRINT_BYTES = 20 * 1024 * 1024

@Composable
fun PrinterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { PrinterDiscoveryEngine(context) }
    val printers by engine.printers.collectAsState()
    val discovering by engine.isDiscovering.collectAsState()
    val discoveryStatus by engine.status.collectAsState()

    var selected by remember { mutableStateOf<DiscoveredPrinter?>(null) }
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var documentName by remember { mutableStateOf<String?>(null) }
    var printing by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            documentUri = uri
            documentName = context.displayName(uri)
            printStatus = null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.printer_title),
                subtitle = stringResource(R.string.printer_subtitle),
                onBack = onBack,
            )
        }
        item {
            DolphyXCard {
                DolphyXStatusBadge(
                    label = if (discovering) stringResource(R.string.status_scanning) else stringResource(R.string.status_idle),
                    active = discovering,
                    icon = Icons.Rounded.Print,
                )
                Text(
                    text = stringResource(R.string.printer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DolphyPrimaryPillButton(
                        text = if (discovering) stringResource(R.string.action_stop) else stringResource(R.string.printer_discover),
                        icon = if (discovering) Icons.Rounded.Stop else Icons.Rounded.Radar,
                        modifier = Modifier.weight(1f),
                        onClick = { if (discovering) engine.stop() else engine.start() },
                    )
                }
                if (!discoveryStatus.isNullOrBlank()) {
                    Text(
                        text = discoveryStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (printers.isEmpty()) {
            item {
                DolphyXCard {
                    DolphyEmptyState(
                        title = stringResource(R.string.printer_empty),
                        subtitle = stringResource(R.string.printer_empty_hint),
                        icon = Icons.Rounded.Print,
                    )
                }
            }
        } else {
            items(printers, key = { "${it.host}:${it.port}" }) { printer ->
                val isSelected = selected?.let { it.host == printer.host && it.port == printer.port } == true
                Surface(
                    onClick = { selected = printer },
                    shape = RoundedCornerShape(28.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Print,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = printer.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${printer.host}:${printer.port}${printer.path}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.printer_job_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/pdf", "text/plain", "image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = documentName ?: stringResource(R.string.printer_pick_file),
                        modifier = Modifier.padding(start = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DolphyPrimaryPillButton(
                    text = stringResource(R.string.printer_print),
                    icon = Icons.Rounded.Print,
                    enabled = !printing,
                    onClick = {
                        val printer = selected
                        val uri = documentUri
                        when {
                            printer == null -> printStatus = context.getString(R.string.printer_need_printer)
                            uri == null -> printStatus = context.getString(R.string.printer_need_file)
                            else -> scope.launch {
                                printing = true
                                printStatus = null
                                val bytes = withContext(Dispatchers.IO) { context.readBytes(uri) }
                                if (bytes == null) {
                                    printing = false
                                    printStatus = context.getString(R.string.printer_file_error)
                                    return@launch
                                }
                                val result = withContext(Dispatchers.IO) {
                                    IppPrintClient.print(
                                        httpUrl = printer.httpUrl,
                                        printerUri = printer.ippUri,
                                        jobName = documentName ?: context.getString(R.string.printer_job_default),
                                        userName = context.getString(R.string.app_name),
                                        documentFormat = documentFormat(documentName),
                                        document = bytes,
                                    )
                                }
                                printing = false
                                printStatus = if (result.success) {
                                    context.getString(R.string.printer_sent)
                                } else {
                                    val codeHex = result.statusCode.toString(16).padStart(4, '0')
                                    when (result.statusCode) {
                                        -1 -> context.getString(R.string.printer_result_unreachable)
                                        in 0x0400..0x04FF -> context.getString(R.string.printer_result_client_error, codeHex)
                                        in 0x0500..0x05FF -> context.getString(R.string.printer_result_server_error, codeHex)
                                        else -> context.getString(R.string.printer_result_unknown, codeHex)
                                    }
                                }
                            }
                        }
                    },
                )
                if (!printStatus.isNullOrBlank()) {
                    Text(
                        text = printStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun Context.displayName(uri: Uri): String {
    val name = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
    return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document"
}

private fun Context.readBytes(uri: Uri): ByteArray? = runCatching {
    contentResolver.openInputStream(uri)?.use { input ->
        val buffer = input.readBytes()
        if (buffer.size > MAX_PRINT_BYTES) null else buffer
    }
}.getOrNull()

private fun documentFormat(name: String?): String = when (name?.substringAfterLast('.', "")?.lowercase()) {
    "pdf" -> "application/pdf"
    "txt", "log", "md" -> "text/plain"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    else -> "application/octet-stream"
}
