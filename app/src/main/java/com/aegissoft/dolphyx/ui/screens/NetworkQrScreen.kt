package com.aegissoft.dolphyx.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Http
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.engine.network.QrMatrixGenerator
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkQrScreen(
    viewModel: DolphyXViewModel,
    onBack: () -> Unit,
) {
    val snapshot by viewModel.networkSnapshot.collectAsState()
    val tcpProbeResult by viewModel.tcpProbeResult.collectAsState()
    val httpsCheckStatus by viewModel.httpsCheckStatus.collectAsState()
    val serverState by viewModel.qrServer.state.collectAsState()

    val defaultProbeHost by viewModel.defaultProbeHost.collectAsState()
    val defaultProbePort by viewModel.defaultProbePort.collectAsState()
    val defaultHttpsUrl by viewModel.defaultHttpsUrl.collectAsState()
    val defaultShareText = stringResource(R.string.network_default_share_text)

    var probeHost by remember(defaultProbeHost) { mutableStateOf(defaultProbeHost) }
    var probePort by remember(defaultProbePort) { mutableStateOf(defaultProbePort.toString()) }
    var httpsUrl by remember(defaultHttpsUrl) { mutableStateOf(defaultHttpsUrl) }
    var shareText by remember(defaultShareText) { mutableStateOf(defaultShareText) }
    var bindLan by remember { mutableStateOf(false) }

    val qrMatrix = remember(serverState.shareUrl) {
        serverState.shareUrl?.let { url -> QrMatrixGenerator.generate(url) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.network_title),
                subtitle = stringResource(R.string.network_subtitle),
                onBack = onBack,
            )
        }

        item {
            DolphyXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.network_wifi_info),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { viewModel.refreshNetwork() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DolphyXStatusBadge(
                        label = snapshot.transportType.ifBlank { stringResource(R.string.status_unavailable) },
                        active = snapshot.isConnected,
                        icon = Icons.Rounded.Wifi,
                    )
                    DolphyXStatusBadge(
                        label = stringResource(R.string.settings_cleartext_blocked),
                        active = true,
                        icon = Icons.Rounded.Lock,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.network_ip_address)}: ${snapshot.localIpv4 ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                if (!snapshot.localIpv6.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.network_ipv6_value, snapshot.localIpv6.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.network_dns_value,
                        snapshot.dnsServers.joinToString().ifBlank { "-" },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = stringResource(
                        R.string.network_if_value,
                        snapshot.interfaceName ?: "-",
                        snapshot.linkDownKbps,
                        snapshot.linkUpKbps,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.network_subnet_scan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = probeHost,
                        onValueChange = { probeHost = it },
                        label = { Text(text = stringResource(R.string.network_host_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        value = probePort,
                        onValueChange = { probePort = it },
                        label = { Text(text = stringResource(R.string.network_port_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        val p = probePort.toIntOrNull() ?: defaultProbePort
                        viewModel.runTcpProbe(probeHost.trim(), p)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.action_scan))
                }
                tcpProbeResult?.let { res ->
                    Spacer(modifier = Modifier.height(6.dp))
                    val statusSuffix = if (res.reachable) {
                        stringResource(R.string.network_probe_open, res.latencyMs)
                    } else {
                        stringResource(R.string.network_probe_closed, res.error ?: "-")
                    }
                    Text(
                        text = stringResource(R.string.network_probe_result, res.host, res.port, statusSuffix),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (res.reachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = httpsUrl,
                    onValueChange = { httpsUrl = it },
                    label = { Text(text = stringResource(R.string.network_https_endpoint)) },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Http, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { viewModel.runHttpsCheck(httpsUrl.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.network_https_check))
                }
                httpsCheckStatus?.let { status ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        item {
            DolphyXCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.network_qr_generator),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = shareText,
                    onValueChange = { shareText = it },
                    label = { Text(text = stringResource(R.string.network_qr_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (bindLan) {
                            stringResource(R.string.network_bind_lan, snapshot.localIpv4 ?: com.aegissoft.dolphyx.core.config.DolphyXConfig.localBindHost)
                        } else {
                            stringResource(R.string.network_loopback_only, com.aegissoft.dolphyx.core.config.DolphyXConfig.localBindHost)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Switch(
                        checked = bindLan,
                        onCheckedChange = { bindLan = it },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (serverState.isRunning) {
                            viewModel.qrServer.stop()
                        } else {
                            viewModel.qrServer.startTextShare(
                                text = shareText,
                                bindLan = bindLan,
                                lanIp = snapshot.localIpv4,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (serverState.isRunning) Icons.Rounded.Stop else Icons.Rounded.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (serverState.isRunning) {
                            stringResource(R.string.action_stop)
                        } else {
                            stringResource(R.string.network_generate_qr)
                        },
                    )
                }

                if (serverState.isRunning && serverState.shareUrl != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.network_share_url, serverState.shareUrl ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.network_bearer_hits, serverState.token ?: "-", serverState.requestCount),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    qrMatrix?.let { matrix ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            QrCodeCanvas(
                                matrix = matrix,
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeCanvas(
    matrix: Array<BooleanArray>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val count = matrix.size
        if (count == 0) return@Canvas
        val cellSize = size.minDimension / count.toFloat()
        for (y in 0 until count) {
            val row = matrix[y]
            for (x in 0 until count) {
                if (row[x]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize),
                    )
                }
            }
        }
    }
}
