package com.aegissoft.dolphyx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.engine.plugin.PluginVerificationResult
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginsScreen(
    viewModel: DolphyXViewModel,
    onBack: () -> Unit,
) {
    val plugins by viewModel.plugins.collectAsState()
    var jsonInput by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<PluginVerificationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DolphyScreenHeader(
            title = stringResource(R.string.plugins_title),
            subtitle = stringResource(R.string.plugins_sandbox_notice),
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 0.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { viewModel.reloadPlugins() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                Text(
                    text = " ${stringResource(R.string.action_refresh)}",
                )
            }
        }

        DolphyXCard {
            DolphyXSectionHeader(
                title = stringResource(R.string.plugins_run),
                subtitle = stringResource(R.string.plugins_json_label),
                icon = Icons.Rounded.Security,
            )
            OutlinedTextField(
                value = jsonInput,
                onValueChange = { jsonInput = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = { Text(text = stringResource(R.string.plugins_json_label)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { testResult = viewModel.importPluginJson(jsonInput) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.action_confirm))
                }
                FilledTonalButton(
                    onClick = {
                        jsonInput = ""
                        testResult = null
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.action_clear))
                }
            }
            when (val res = testResult) {
                is PluginVerificationResult.Valid -> {
                    DolphyXStatusBadge(
                        label = "${stringResource(R.string.plugins_verify_badge)}: ${res.manifest.name}",
                        active = true,
                        icon = Icons.Rounded.CheckCircle,
                    )
                }
                is PluginVerificationResult.Rejected -> {
                    DolphyXStatusBadge(
                        label = "${stringResource(R.string.plugins_unverified_badge)}: ${res.reason}",
                        active = false,
                        icon = Icons.Rounded.ErrorOutline,
                    )
                }
                null -> Unit
            }
        }

        if (plugins.isEmpty()) {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.plugins_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            plugins.forEach { entry ->
                when (entry) {
                    is PluginVerificationResult.Valid -> {
                        val manifest = entry.manifest
                        DolphyXCard {
                            Text(
                                text = "${manifest.name} v${manifest.version}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${manifest.id} • ${manifest.author}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = manifest.description,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            DolphyXStatusBadge(
                                label = stringResource(R.string.plugins_verify_badge),
                                active = true,
                                icon = Icons.Rounded.VerifiedUser,
                            )
                            Text(
                                text = stringResource(R.string.plugins_sha_prefix, entry.computedSha256.take(16)),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (manifest.capabilities.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.plugins_capabilities),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    manifest.capabilities.forEach { cap ->
                                        DolphyXStatusBadge(
                                            label = cap,
                                            active = true,
                                            icon = Icons.Rounded.Security,
                                        )
                                    }
                                }
                            }
                            manifest.ui.forEach { block ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = block.title.ifBlank { block.type },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        if (block.value.isNotBlank()) {
                                            Text(
                                                text = block.value,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is PluginVerificationResult.Rejected -> {
                        DolphyXCard {
                            DolphyXStatusBadge(
                                label = stringResource(R.string.plugins_unverified_badge),
                                active = false,
                                icon = Icons.Rounded.ErrorOutline,
                            )
                            Text(
                                text = entry.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
