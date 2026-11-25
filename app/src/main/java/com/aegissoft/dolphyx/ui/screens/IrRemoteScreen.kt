package com.aegissoft.dolphyx.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.data.CustomIrSignalRecord
import com.aegissoft.dolphyx.data.IrRemoteSummary
import com.aegissoft.dolphyx.engine.ir.IrFileImporter
import com.aegissoft.dolphyx.data.IrSignal
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyEmptyState
import com.aegissoft.dolphyx.ui.components.DolphyScreenHeader
import com.aegissoft.dolphyx.ui.components.DolphySegmentedPill
import com.aegissoft.dolphyx.ui.components.DolphySearchBar
import com.aegissoft.dolphyx.ui.components.DolphyXCard
import com.aegissoft.dolphyx.ui.components.DolphyXSectionHeader
import com.aegissoft.dolphyx.ui.components.DolphyXStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class IrTab { BROWSE, UNIVERSAL, FAVORITES, CUSTOM }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IrRemoteScreen(viewModel: DolphyXViewModel, onBack: () -> Unit) {
    val categories by viewModel.irCategories.collectAsState()
    val brands by viewModel.irBrands.collectAsState()
    val remotes by viewModel.irRemotes.collectAsState()
    val signals by viewModel.irSignals.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedRemote by viewModel.selectedRemote.collectAsState()
    val irStatusMessage by viewModel.irStatusMessage.collectAsState()
    val favoriteIds by viewModel.irFavorites.collectAsState()
    val favoriteSignals by viewModel.favoriteSignals.collectAsState()

    val hasEmitter = viewModel.irEngine.hasIrEmitter()
    var tabIndex by remember { mutableIntStateOf(0) }
    var brandQuery by remember { mutableStateOf("") }
    val tab = IrTab.entries[tabIndex]

    val tabLabels = listOf(
        stringResource(R.string.ir_tab_browse),
        stringResource(R.string.ir_tab_universal),
        stringResource(R.string.ir_tab_favorites),
        stringResource(R.string.ir_tab_custom),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DolphyScreenHeader(
                title = stringResource(R.string.ir_title),
                subtitle = irStatusMessage ?: if (hasEmitter) {
                    stringResource(R.string.status_supported)
                } else {
                    stringResource(R.string.ir_no_hardware)
                },
                onBack = onBack,
            )
        }
        item {
            DolphySegmentedPill(
                options = tabLabels,
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
                    label = stringResource(R.string.ir_transmitter_status),
                    active = hasEmitter,
                    icon = Icons.Rounded.SettingsRemote,
                )
                DolphyXStatusBadge(
                    label = "${stringResource(R.string.ir_tab_favorites)}: ${favoriteIds.size}",
                    active = favoriteIds.isNotEmpty(),
                    icon = Icons.Rounded.Star,
                )
            }
        }

        when (tab) {
            IrTab.BROWSE -> browseContent(
                viewModel = viewModel,
                categories = categories,
                brands = brands,
                remotes = remotes,
                signals = signals,
                selectedCategory = selectedCategory,
                selectedBrand = selectedBrand,
                selectedRemote = selectedRemote,
                favoriteIds = favoriteIds,
                brandQuery = brandQuery,
                onBrandQueryChange = {
                    brandQuery = it
                    viewModel.searchIrBrands(it)
                },
            )

            IrTab.UNIVERSAL -> {
                item {
                    UniversalContent(
                        viewModel = viewModel,
                        categories = categories,
                        hasEmitter = hasEmitter,
                    )
                }
            }

            IrTab.FAVORITES -> {
                if (favoriteSignals.isEmpty()) {
                    item {
                        DolphyXCard {
                            DolphyEmptyState(
                                title = stringResource(R.string.ir_favorites_empty),
                                subtitle = stringResource(R.string.ir_favorites_empty_hint),
                                icon = Icons.Rounded.StarBorder,
                            )
                        }
                    }
                } else {
                    items(favoriteSignals, key = { it.id }) { signal ->
                        FavoriteSignalRow(
                            signal = signal,
                            onTransmit = { viewModel.transmitIrSignal(signal) },
                            onRemove = { viewModel.toggleIrFavorite(signal.id) },
                        )
                    }
                }
            }

            IrTab.CUSTOM -> {
                item {
                    CustomContent(viewModel = viewModel, hasEmitter = hasEmitter)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.browseContent(
    viewModel: DolphyXViewModel,
    categories: List<String>,
    brands: List<String>,
    remotes: List<IrRemoteSummary>,
    signals: List<IrSignal>,
    selectedCategory: String?,
    selectedBrand: String?,
    selectedRemote: IrRemoteSummary?,
    favoriteIds: Set<Long>,
    brandQuery: String,
    onBrandQueryChange: (String) -> Unit,
) {
    item {
        DolphySearchBar(
            value = brandQuery,
            onValueChange = onBrandQueryChange,
            placeholder = stringResource(R.string.ir_search_hint),
            icon = Icons.Rounded.Search,
        )
    }

    item {
        DolphyXCard {
            Text(
                text = stringResource(R.string.ir_categories_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectIrCategory(null) },
                        label = { Text(text = stringResource(R.string.ir_categories_all)) },
                        colors = irChipColors(),
                        border = null,
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.selectIrCategory(cat) },
                        label = { Text(text = irCategoryLabel(cat)) },
                        colors = irChipColors(),
                        border = null,
                    )
                }
            }
            if (brands.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.ir_brands_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(brands) { brand ->
                        FilterChip(
                            selected = selectedBrand == brand,
                            onClick = { viewModel.selectIrBrand(brand) },
                            label = { Text(text = brand) },
                            colors = irChipColors(),
                            border = null,
                        )
                    }
                }
            }
        }
    }

    if (remotes.isEmpty()) {
        item {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.ir_empty_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        item {
            DolphyXCard {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(remotes, key = { it.id }) { remote ->
                        FilterChip(
                            selected = selectedRemote?.id == remote.id,
                            onClick = { viewModel.selectIrRemote(remote) },
                            label = { Text(text = "${remote.model} (${remote.signalCount})") },
                            colors = irChipColors(),
                            border = null,
                        )
                    }
                }
            }
        }
    }

    val currentRemote = selectedRemote
    if (currentRemote != null && signals.isNotEmpty()) {
        item {
            IrKeypadCard(
                remoteTitle = "${currentRemote.brand} ${currentRemote.model}",
                signals = signals,
                onTransmit = { viewModel.transmitIrSignal(it) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UniversalContent(
    viewModel: DolphyXViewModel,
    categories: List<String>,
    hasEmitter: Boolean,
) {
    var category by remember { mutableStateOf<String?>(null) }
    var remote by remember { mutableStateOf<IrRemoteSummary?>(null) }
    var signals by remember { mutableStateOf<List<IrSignal>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (category == null) category = categories.firstOrNull() ?: "TV"
    }
    LaunchedEffect(category) {
        val target = category ?: return@LaunchedEffect
        loading = true
        withContext(Dispatchers.IO) {
            val pair = viewModel.irDb.getFirstRemoteWithSignal(target, "power")
            remote = pair?.first
            signals = pair?.second.orEmpty()
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DolphyXCard {
            Text(
                text = stringResource(R.string.ir_universal_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.take(14).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(text = irCategoryLabel(cat)) },
                    )
                }
            }
            Text(
                text = if (loading) {
                    stringResource(R.string.status_scanning)
                } else {
                    remote?.let { "${it.brand} ${it.model}" } ?: stringResource(R.string.ir_universal_no_signal)
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (signals.isNotEmpty()) {
            UniversalKeypad(
                signals = signals,
                enabled = hasEmitter,
                onTransmit = { viewModel.transmitIrSignal(it) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomContent(
    viewModel: DolphyXViewModel,
    hasEmitter: Boolean,
) {
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("NEC") }
    var address by remember { mutableStateOf("04") }
    var command by remember { mutableStateOf("08") }
    var frequency by remember { mutableStateOf("38000") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val savedSignals by viewModel.customIrSignals.collectAsState()
    val customDefaultName = stringResource(R.string.ir_custom_default_name)
    val context = LocalContext.current
    var importMessage by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val parsed = text?.let { IrFileImporter.parse(it) }.orEmpty()
            val label = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "IR"
            val count = viewModel.importCustomIrSignals(parsed, label)
            importMessage = if (count > 0) {
                context.getString(R.string.ir_import_success, count)
            } else {
                context.getString(R.string.ir_import_empty)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DolphyXCard {
            Text(
                text = stringResource(R.string.ir_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.ir_import))
            }
            if (!importMessage.isNullOrBlank()) {
                Text(
                    text = importMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = stringResource(R.string.ir_custom_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("NEC", "NECext", "NEC42", "Samsung32", "SIRC", "RC5", "RC6", "RAW").forEach { option ->
                    FilterChip(
                        selected = protocol == option,
                        onClick = { protocol = option },
                        label = { Text(text = option) },
                        colors = irChipColors(),
                        border = null,
                    )
                }
            }
            if (protocol != "RAW") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(text = stringResource(R.string.ir_custom_address)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text(text = stringResource(R.string.ir_custom_command)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(text = stringResource(R.string.ir_custom_raw)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it },
                label = { Text(text = stringResource(R.string.ir_custom_frequency)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!validationError.isNullOrBlank()) {
                // The localized message is rendered below the action button.
            }
            Button(
                onClick = {
                    validationError = null
                    val freq = frequency.toIntOrNull()
                    if (freq == null || freq !in 20000..60000) {
                        validationError = "frequency"
                        return@Button
                    }
                    if (protocol != "RAW" && (address.isBlank() || command.isBlank())) {
                        validationError = "address"
                        return@Button
                    }
                    if (protocol == "RAW" && command.isBlank()) {
                        validationError = "raw"
                        return@Button
                    }
                    val signal = IrSignal(
                        id = 0,
                        remoteId = 0,
                        name = name.ifBlank { customDefaultName },
                        sigType = if (protocol == "RAW") "raw" else "parsed",
                        protocol = if (protocol == "RAW") null else protocol,
                        address = if (protocol == "RAW") null else address,
                        command = if (protocol == "RAW") null else command,
                        frequency = freq,
                        dutyCycle = 0.33,
                        rawData = if (protocol == "RAW") command else null,
                    )
                    viewModel.transmitIrSignal(signal)
                },
                enabled = hasEmitter,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.ir_custom_send))
            }
            FilledTonalButton(
                onClick = {
                    validationError = null
                    val freq = frequency.toIntOrNull()
                    if (freq == null || freq !in 20000..60000) {
                        validationError = "frequency"
                        return@FilledTonalButton
                    }
                    if (protocol != "RAW" && (address.isBlank() || command.isBlank())) {
                        validationError = "address"
                        return@FilledTonalButton
                    }
                    if (protocol == "RAW" && command.isBlank()) {
                        validationError = "raw"
                        return@FilledTonalButton
                    }
                    viewModel.saveCustomIrSignal(
                        CustomIrSignalRecord(
                            id = viewModel.customIr.newId(),
                            name = name.ifBlank { customDefaultName },
                            protocol = protocol,
                            address = if (protocol == "RAW") "" else address,
                            command = command,
                            frequency = freq,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.ir_custom_save))
            }
            val message = when (validationError) {
                "frequency" -> stringResource(R.string.ir_custom_error_frequency)
                "address" -> stringResource(R.string.ir_custom_error_address)
                "raw" -> stringResource(R.string.ir_custom_error_raw)
                else -> null
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val status = viewModel.irStatusMessage.collectAsState().value
            if (!status.isNullOrBlank()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (savedSignals.isEmpty()) {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.ir_custom_saved_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DolphyXCard {
                Text(
                    text = stringResource(R.string.ir_custom_saved_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                savedSignals.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${record.protocol}  ${record.frequency} Hz",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        IconButton(onClick = { viewModel.transmitIrSignal(customRecordToSignal(record)) }) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = stringResource(R.string.ir_transmit),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { viewModel.deleteCustomIrSignal(record.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.ir_custom_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun customRecordToSignal(record: CustomIrSignalRecord): IrSignal {
    return IrSignal(
        id = 0,
        remoteId = 0,
        name = record.name,
        sigType = if (record.protocol == "RAW") "raw" else "parsed",
        protocol = if (record.protocol == "RAW") null else record.protocol,
        address = if (record.protocol == "RAW") null else record.address,
        command = if (record.protocol == "RAW") null else record.command,
        frequency = record.frequency,
        dutyCycle = 0.33,
        rawData = if (record.protocol == "RAW") record.command else null,
    )
}

@Composable
private fun FavoriteSignalRow(
    signal: IrSignal,
    onTransmit: () -> Unit,
    onRemove: () -> Unit,
) {
    DolphyXCard(contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = signal.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${signal.protocol ?: signal.sigType}  ${signal.frequency ?: 38000} Hz",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onTransmit) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = stringResource(R.string.ir_transmit),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.ir_favorite_toggle),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UniversalKeypad(
    signals: List<IrSignal>,
    enabled: Boolean,
    onTransmit: (IrSignal) -> Unit,
) {
    fun find(vararg aliases: String): IrSignal? = signals.firstOrNull { sig ->
        val n = sig.name.lowercase()
        aliases.any { n.contains(it) }
    }

    val keys = listOf(
        find("power", "pwr", "on_off") to stringResource(R.string.ir_key_power),
        find("mute") to stringResource(R.string.ir_key_mute),
        find("vol_up", "vol+", "volume_up") to stringResource(R.string.ir_key_vol_up),
        find("vol_dn", "vol_down", "vol-", "volume_down") to stringResource(R.string.ir_key_vol_down),
        find("ch_next", "ch_up", "ch+", "channel_up") to stringResource(R.string.ir_key_ch_up),
        find("ch_prev", "ch_down", "ch-", "channel_down") to stringResource(R.string.ir_key_ch_down),
    )

    DolphyXCard {
        Text(
            text = stringResource(R.string.ir_universal_keypad),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            keys.forEach { (signal, label) ->
                FilledTonalButton(
                    onClick = { signal?.let(onTransmit) },
                    enabled = enabled && signal != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = label, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun irCategoryLabel(category: String): String {
    return when (category) {
        "TVs" -> stringResource(R.string.ir_cat_tv)
        "ACs" -> stringResource(R.string.ir_cat_ac)
        "Audio_and_Video_Receivers" -> stringResource(R.string.ir_cat_avr)
        "SoundBars" -> stringResource(R.string.ir_cat_soundbar)
        "Streaming_Devices" -> stringResource(R.string.ir_cat_streaming)
        "LED_Lighting" -> stringResource(R.string.ir_cat_led)
        "Fans" -> stringResource(R.string.ir_cat_fans)
        "Heaters" -> stringResource(R.string.ir_cat_heaters)
        "Projectors" -> stringResource(R.string.ir_cat_projectors)
        "Cameras" -> stringResource(R.string.ir_cat_cameras)
        "Consoles" -> stringResource(R.string.ir_cat_consoles)
        "Universal_TV_Remotes" -> stringResource(R.string.ir_cat_universal)
        "Air_Purifiers" -> stringResource(R.string.ir_cat_air_purifiers)
        "Banners" -> stringResource(R.string.ir_cat_banners)
        "Bidet" -> stringResource(R.string.ir_cat_bidet)
        "Blu-Ray" -> stringResource(R.string.ir_cat_bluray)
        "CCTV" -> stringResource(R.string.ir_cat_cctv)
        "CD_Players" -> stringResource(R.string.ir_cat_cd_players)
        "Cable_Boxes" -> stringResource(R.string.ir_cat_cable_boxes)
        "Car_Multimedia" -> stringResource(R.string.ir_cat_car_multimedia)
        "Clocks" -> stringResource(R.string.ir_cat_clocks)
        "Computers" -> stringResource(R.string.ir_cat_computers)
        "Converters" -> stringResource(R.string.ir_cat_converters)
        "DVB-T" -> stringResource(R.string.ir_cat_dvbt)
        "DVD_Players" -> stringResource(R.string.ir_cat_dvd_players)
        "Digital_Signs" -> stringResource(R.string.ir_cat_digital_signs)
        "Dust_Collectors" -> stringResource(R.string.ir_cat_dust_collectors)
        "Fireplaces" -> stringResource(R.string.ir_cat_fireplaces)
        "Head_Units" -> stringResource(R.string.ir_cat_head_units)
        "Humidifiers" -> stringResource(R.string.ir_cat_humidifiers)
        "KVM" -> stringResource(R.string.ir_cat_kvm)
        "Laserdisc" -> stringResource(R.string.ir_cat_laserdisc)
        "MiniDisc" -> stringResource(R.string.ir_cat_minidisc)
        "Miscellaneous" -> stringResource(R.string.ir_cat_misc)
        "Monitors" -> stringResource(R.string.ir_cat_monitors)
        "Multimedia" -> stringResource(R.string.ir_cat_multimedia)
        "Picture_Frames" -> stringResource(R.string.ir_cat_picture_frames)
        "Speakers" -> stringResource(R.string.ir_cat_speakers)
        "TV_Tuner" -> stringResource(R.string.ir_cat_tv_tuner)
        "Touchscreen_Displays" -> stringResource(R.string.ir_cat_touchscreens)
        "Toys" -> stringResource(R.string.ir_cat_toys)
        "VCR" -> stringResource(R.string.ir_cat_vcr)
        "Vacuum_Cleaners" -> stringResource(R.string.ir_cat_vacuums)
        "Videoconferencing" -> stringResource(R.string.ir_cat_videoconferencing)
        "Whiteboards" -> stringResource(R.string.ir_cat_whiteboards)
        "Window_cleaners" -> stringResource(R.string.ir_cat_window_cleaners)
        else -> category.replace('_', ' ')
    }
}

@Composable
private fun irChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
    selectedLabelColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun IrKeypadCard(
    remoteTitle: String,
    signals: List<IrSignal>,
    onTransmit: (IrSignal) -> Unit,
) {
    fun findSignal(vararg aliases: String): IrSignal? = signals.firstOrNull { sig ->
        val n = sig.name.lowercase()
        aliases.any { n.contains(it) }
    }

    val powerSig = findSignal("power", "pwr", "on_off")
    val muteSig = findSignal("mute")
    val volUpSig = findSignal("vol_up", "vol+", "volume_up")
    val volDownSig = findSignal("vol_dn", "vol_down", "vol-", "volume_down")
    val chUpSig = findSignal("ch_next", "ch_up", "ch+", "channel_up")
    val chDownSig = findSignal("ch_prev", "ch_down", "ch-", "channel_down")

    DolphyXCard {
        Text(
            text = remoteTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.ir_signals_title, signals.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { powerSig?.let(onTransmit) },
                enabled = powerSig != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = powerSig?.name ?: stringResource(R.string.ir_key_power), maxLines = 1)
            }
            OutlinedButton(
                onClick = { muteSig?.let(onTransmit) },
                enabled = muteSig != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Rounded.VolumeOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = muteSig?.name ?: stringResource(R.string.ir_key_mute), maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { volUpSig?.let(onTransmit) },
                    enabled = volUpSig != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.ir_key_vol_up), maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { volDownSig?.let(onTransmit) },
                    enabled = volDownSig != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.ir_key_vol_down), maxLines = 1)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { chUpSig?.let(onTransmit) },
                    enabled = chUpSig != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.ir_key_ch_up), maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { chDownSig?.let(onTransmit) },
                    enabled = chDownSig != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(imageVector = Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.ir_key_ch_down), maxLines = 1)
                }
            }
        }
    }
}
