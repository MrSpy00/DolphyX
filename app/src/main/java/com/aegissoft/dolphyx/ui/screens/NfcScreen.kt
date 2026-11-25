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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.config.DolphyXConfig
import com.aegissoft.dolphyx.engine.nfc.DolphyXHceService
import com.aegissoft.dolphyx.engine.nfc.NfcWriterEngine
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphySegmentedPill
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class NfcTab { READ, WRITE, EMULATE, VAULT }
private enum class NdefKind { TEXT, URL, WIFI, VCARD }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcScreen(viewModel: DolphyXViewModel, onBack: () -> Unit) {
    val lastTag by viewModel.nfcEngine.lastTag.collectAsState()
    val lastRawTag by viewModel.nfcEngine.lastRawTag.collectAsState()
    val vaultRecords by viewModel.nfcVaultRecords.collectAsState()
    val activeHceText by DolphyXHceService.activeNdefText.collectAsState()
    val lastApduEvent by DolphyXHceService.lastApduEvent.collectAsState()
    val scope = rememberCoroutineScope()

    val nfcSupported = viewModel.nfcEngine.isNfcSupported()
    val nfcEnabled = viewModel.nfcEngine.isNfcEnabled()

    val tapFirstMessage = stringResource(R.string.nfc_tap_first)
    val writeSuccessMessage = stringResource(R.string.nfc_write_success)
    val writeFailureMessage = stringResource(R.string.nfc_write_failed)
    val eraseSuccessMessage = stringResource(R.string.nfc_erase_success)
    val eraseFailureMessage = stringResource(R.string.nfc_erase_failed)

    var tabIndex by remember { mutableIntStateOf(0) }
    var hceDraft by remember(activeHceText) { mutableStateOf(activeHceText) }
    var tagLabelDraft by remember { mutableStateOf("") }
    var writeKindIndex by remember { mutableIntStateOf(0) }
    var writeResult by remember { mutableStateOf<String?>(null) }
    var writeText by remember { mutableStateOf("") }
    var writeUrl by remember { mutableStateOf(DolphyXConfig.defaultHttpsUrl) }
    var writeSsid by remember { mutableStateOf("") }
    var writePassword by remember { mutableStateOf("") }
    var writeName by remember { mutableStateOf("") }
    var writePhone by remember { mutableStateOf("") }
    var writeEmail by remember { mutableStateOf("") }

    val tabs = listOf(
        stringResource(R.string.nfc_tab_read),
        stringResource(R.string.nfc_tab_write),
        stringResource(R.string.nfc_tab_emulate),
        stringResource(R.string.nfc_tab_vault),
    )
    val kinds = listOf(
        stringResource(R.string.nfc_write_kind_text),
        stringResource(R.string.nfc_write_kind_url),
        stringResource(R.string.nfc_write_kind_wifi),
        stringResource(R.string.nfc_write_kind_vcard),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.nfc_title),
                subtitle = stringResource(R.string.nfc_subtitle),
                onBack = onBack,
            )
        }
        item {
            DolphySegmentedPill(
                options = tabs,
                selectedIndex = tabIndex,
                onSelect = { tabIndex = it },
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DolphyXStatusBadge(
                    label = if (nfcSupported) stringResource(R.string.nfc_hardware_available) else stringResource(R.string.nfc_hardware_missing),
                    active = nfcSupported,
                    icon = Icons.Rounded.Nfc,
                )
                DolphyXStatusBadge(
                    label = if (nfcEnabled) stringResource(R.string.nfc_adapter_on) else stringResource(R.string.nfc_adapter_off),
                    active = nfcEnabled,
                    icon = Icons.Rounded.Contactless,
                )
                DolphyXStatusBadge(
                    label = stringResource(R.string.nfc_hce_locked),
                    active = true,
                    icon = Icons.Rounded.Lock,
                )
            }
        }

        when (NfcTab.entries[tabIndex]) {
            NfcTab.READ -> {
                item {
                    DolphyXCard {
                        Text(
                            text = stringResource(R.string.nfc_last_tag),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val tag = lastTag
                        if (tag == null) {
                            DolphyEmptyState(
                                title = stringResource(R.string.nfc_waiting_title),
                                subtitle = stringResource(R.string.nfc_tap_prompt),
                                icon = Icons.Rounded.Contactless,
                            )
                        } else {
                            Text(
                                text = "${stringResource(R.string.nfc_uid_label)}: ${tag.uidHex}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                tag.techList.forEach { tech ->
                                    SuggestionChip(onClick = {}, label = { Text(text = tech) })
                                }
                            }
                            Text(
                                text = stringResource(
                                    R.string.nfc_tag_details,
                                    tag.sizeBytes,
                                    if (tag.isWritable) stringResource(R.string.nfc_writable) else stringResource(R.string.nfc_read_only),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (tag.ndefText.isNotBlank()) {
                                Text(
                                    text = "${stringResource(R.string.nfc_ndef_label)}: ${tag.ndefText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            OutlinedTextField(
                                value = tagLabelDraft,
                                onValueChange = { tagLabelDraft = it },
                                label = { Text(text = stringResource(R.string.nfc_vault_label_hint)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.saveLastNfcTag(tagLabelDraft.ifBlank { "NFC ${tag.uidHex.take(8)}" })
                                        tagLabelDraft = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(text = stringResource(R.string.nfc_vault_save), maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val raw = lastRawTag ?: return@OutlinedButton
                                        scope.launch(Dispatchers.IO) {
                                            val result = NfcWriterEngine.erase(raw)
                                            viewModel.nfcEngine.parseTag(raw)
                                            withContext(Dispatchers.Main) {
                                                writeResult = result.fold(
                                                    onSuccess = { eraseSuccessMessage },
                                                    onFailure = { eraseFailureMessage },
                                                )
                                            }
                                        }
                                    },
                                    enabled = lastRawTag != null && tag.isWritable,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(text = stringResource(R.string.nfc_erase_action), maxLines = 1)
                                }
                            }
                        }
                        if (!writeResult.isNullOrBlank()) {
                            Text(
                                text = writeResult.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            NfcTab.WRITE -> {
                item {
                    DolphyXCard {
                        Text(
                            text = stringResource(R.string.nfc_write_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.nfc_write_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DolphySegmentedPill(
                            options = kinds,
                            selectedIndex = writeKindIndex,
                            onSelect = { writeKindIndex = it },
                        )
                        when (NdefKind.entries[writeKindIndex]) {
                            NdefKind.TEXT -> OutlinedTextField(
                                value = writeText,
                                onValueChange = { writeText = it },
                                label = { Text(text = stringResource(R.string.nfc_write_text_label)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            NdefKind.URL -> OutlinedTextField(
                                value = writeUrl,
                                onValueChange = { writeUrl = it },
                                label = { Text(text = stringResource(R.string.nfc_write_url_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            NdefKind.WIFI -> {
                                OutlinedTextField(
                                    value = writeSsid,
                                    onValueChange = { writeSsid = it },
                                    label = { Text(text = stringResource(R.string.nfc_write_ssid)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = writePassword,
                                    onValueChange = { writePassword = it },
                                    label = { Text(text = stringResource(R.string.nfc_write_password)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            NdefKind.VCARD -> {
                                OutlinedTextField(
                                    value = writeName,
                                    onValueChange = { writeName = it },
                                    label = { Text(text = stringResource(R.string.nfc_write_name)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = writePhone,
                                        onValueChange = { writePhone = it },
                                        label = { Text(text = stringResource(R.string.nfc_write_phone)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = writeEmail,
                                        onValueChange = { writeEmail = it },
                                        label = { Text(text = stringResource(R.string.nfc_write_email)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val raw = lastRawTag
                                if (raw == null) {
                                    writeResult = tapFirstMessage
                                    return@Button
                                }
                                scope.launch(Dispatchers.IO) {
                                    val result = when (NdefKind.entries[writeKindIndex]) {
                                        NdefKind.TEXT -> NfcWriterEngine.writeText(raw, writeText)
                                        NdefKind.URL -> NfcWriterEngine.writeUri(raw, writeUrl)
                                        NdefKind.WIFI -> NfcWriterEngine.writeWifi(raw, writeSsid, writePassword)
                                        NdefKind.VCARD -> NfcWriterEngine.writeVCard(raw, writeName, writePhone, writeEmail)
                                    }
                                    viewModel.nfcEngine.parseTag(raw)
                                    withContext(Dispatchers.Main) {
                                        writeResult = result.fold(
                                            onSuccess = { writeSuccessMessage },
                                            onFailure = { writeFailureMessage },
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(imageVector = Icons.Rounded.Contactless, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.nfc_write_action))
                        }
                        if (!writeResult.isNullOrBlank()) {
                            Text(
                                text = writeResult.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            NfcTab.EMULATE -> {
                item {
                    DolphyXCard {
                        Text(
                            text = stringResource(R.string.nfc_ndef_write_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = hceDraft,
                            onValueChange = { hceDraft = it },
                            label = { Text(text = stringResource(R.string.nfc_ndef_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                if (hceDraft.isNotBlank()) {
                                    DolphyXHceService.activeNdefText.value = hceDraft.trim()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(imageVector = Icons.Rounded.Contactless, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.nfc_apply_hce))
                        }
                        if (lastApduEvent.isNotBlank()) {
                            Text(
                                text = lastApduEvent,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                item {
                    DolphyXCard {
                        Text(
                            text = stringResource(R.string.nfc_emulate_profiles),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (vaultRecords.isEmpty()) {
                            Text(
                                text = stringResource(R.string.nfc_emulate_no_profiles),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                vaultRecords.forEach { record ->
                                    FilterChip(
                                        selected = record.ndefPayload.isNotBlank() && hceDraft == record.ndefPayload,
                                        onClick = {
                                            if (record.ndefPayload.isNotBlank()) {
                                                hceDraft = record.ndefPayload
                                                DolphyXHceService.activeNdefText.value = record.ndefPayload
                                            }
                                        },
                                        label = { Text(text = record.label, maxLines = 1) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            NfcTab.VAULT -> {
                item {
                    DolphyXCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EnhancedEncryption,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.nfc_vault_title, vaultRecords.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                if (vaultRecords.isEmpty()) {
                    item {
                        DolphyXCard {
                            DolphyEmptyState(
                                title = stringResource(R.string.nfc_vault_empty),
                                subtitle = stringResource(R.string.nfc_vault_empty_hint),
                                icon = Icons.Rounded.EnhancedEncryption,
                            )
                        }
                    }
                } else {
                    items(vaultRecords, key = { it.id }) { record ->
                        DolphyXCard(contentPadding = PaddingValues(13.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${stringResource(R.string.nfc_uid_label)}: ${record.uidHex}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                    )
                                    if (record.ndefPayload.isNotBlank()) {
                                        Text(
                                            text = record.ndefPayload,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteNfcRecord(record.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.action_clear),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
