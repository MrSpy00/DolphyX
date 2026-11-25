package com.aegissoft.dolphyx.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import com.aegissoft.dolphyx.engine.network.DnsLookupResult
import com.aegissoft.dolphyx.engine.network.LanHost
import com.aegissoft.dolphyx.engine.network.LanScannerEngine
import com.aegissoft.dolphyx.engine.network.TlsCertificateInfo
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyPrimaryPillButton
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyValueRow
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanScannerScreen(onBack: () -> Unit) {
    val engine = remember { LanScannerEngine() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vpnActive = remember(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }
    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var hosts by remember { mutableStateOf<List<LanHost>>(emptyList()) }
    var dnsHost by remember { mutableStateOf(DolphyXConfig.appWebsite) }
    var dnsResult by remember { mutableStateOf<DnsLookupResult?>(null) }
    var tlsResult by remember { mutableStateOf<TlsCertificateInfo?>(null) }
    var resolving by remember { mutableStateOf(false) }

    val localIp = remember { engine.localIpv4() }
    val subnet = remember { engine.localSubnetBase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.lan_title),
                subtitle = stringResource(R.string.lan_subtitle),
                onBack = onBack,
            )
        }
        item {
            DolphyXCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${stringResource(R.string.lan_local_ip)}: ${localIp ?: stringResource(R.string.status_unavailable)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${stringResource(R.string.lan_subnet)}: ${subnet ?: "-"}.0/24",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DolphyPrimaryPillButton(
                    text = if (scanning) {
                        stringResource(R.string.lan_scanning, progress)
                    } else {
                        stringResource(R.string.lan_scan)
                    },
                    icon = Icons.Rounded.Radar,
                    enabled = !scanning && !vpnActive,
                    onClick = {
                        if (scanning || vpnActive) return@DolphyPrimaryPillButton
                        scanning = true
                        progress = 0
                        hosts = emptyList()
                        scope.launch {
                            val result = engine.scan(onProgress = { progress = it })
                            hosts = result
                            scanned = true
                            scanning = false
                        }
                    },
                )
                if (scanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = when {
                        vpnActive -> stringResource(R.string.lan_vpn_warning)
                        scanning -> stringResource(R.string.lan_scanning, progress)
                        scanned -> stringResource(R.string.lan_scan_hint, hosts.size)
                        else -> stringResource(R.string.lan_scan_idle_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vpnActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!scanning && scanned && hosts.isEmpty()) {
            item {
                DolphyXCard {
                    DolphyEmptyState(
                        title = stringResource(R.string.lan_no_hosts),
                        subtitle = stringResource(R.string.lan_no_hosts_hint),
                        icon = Icons.Rounded.Lan,
                    )
                }
            }
        } else {
            items(hosts, key = { it.ip }) { host ->
                LanHostRow(host)
            }
        }

        item {
            DolphyXCard {
                DolphyXSectionHeader(
                    title = stringResource(R.string.lan_dns_title),
                    subtitle = stringResource(R.string.lan_dns_hint),
                    icon = Icons.Rounded.Dns,
                )
                OutlinedTextField(
                    value = dnsHost,
                    onValueChange = { dnsHost = it },
                    label = { Text(text = stringResource(R.string.lan_dns_host)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            val target = dnsHost.trim()
                            if (target.isBlank()) return@FilledTonalButton
                            resolving = true
                            scope.launch {
                                dnsResult = withContext(Dispatchers.IO) { engine.resolveDns(target) }
                                resolving = false
                            }
                        },
                        enabled = !resolving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(R.string.lan_dns_button), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            val target = dnsHost.trim()
                            if (target.isBlank()) return@FilledTonalButton
                            resolving = true
                            scope.launch {
                                tlsResult = withContext(Dispatchers.IO) { engine.inspectTls(target) }
                                resolving = false
                            }
                        },
                        enabled = !resolving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.lan_tls_button), maxLines = 1)
                    }
                }
                val dns = dnsResult
                if (dns != null) {
                    DolphyValueRow(
                        label = stringResource(R.string.lan_dns_result),
                        value = dns.addresses.joinToString(", ").ifBlank { dns.error ?: "-" },
                    )
                }
                val tls = tlsResult
                if (tls != null) {
                    Text(
                        text = stringResource(R.string.lan_tls_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (tls.error != null) {
                        Text(
                            text = tls.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        DolphyValueRow(label = stringResource(R.string.lan_tls_subject), value = tls.subject)
                        DolphyValueRow(label = stringResource(R.string.lan_tls_issuer), value = tls.issuer)
                        DolphyValueRow(label = stringResource(R.string.lan_tls_expiry), value = tls.notAfter)
                        DolphyValueRow(label = stringResource(R.string.lan_tls_protocol), value = "${tls.protocol} / ${tls.cipher}")
                    }
                }
            }
        }
    }
}

@Composable
private fun LanHostRow(host: LanHost) {
    DolphyXCard(contentPadding = PaddingValues(13.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.hostname ?: host.ip,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = host.ip,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            Text(
                text = if (host.latencyMs >= 0) "${host.latencyMs} ms" else "-",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            host.openPorts.forEach { port ->
                Text(
                    text = ":$port",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
