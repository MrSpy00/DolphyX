package com.aegissoft.dolphyx.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegissoft.dolphyx.R
import com.aegissoft.dolphyx.core.security.KeystoreCryptoManager
import com.aegissoft.dolphyx.core.security.SecurePreferences
import com.aegissoft.dolphyx.data.IrDatabaseHelper
import com.aegissoft.dolphyx.data.CustomIrRepository
import com.aegissoft.dolphyx.data.CustomIrSignalRecord
import com.aegissoft.dolphyx.data.IrRemoteSummary
import com.aegissoft.dolphyx.data.IrSignal
import com.aegissoft.dolphyx.data.NfcTagRecord
import com.aegissoft.dolphyx.data.NfcVaultRepository
import com.aegissoft.dolphyx.data.OuiDatabaseHelper
import com.aegissoft.dolphyx.engine.ble.BleDiagnosticEngine
import com.aegissoft.dolphyx.engine.ir.IrTransmitterEngine
import com.aegissoft.dolphyx.engine.network.NetworkDiagnosticEngine
import com.aegissoft.dolphyx.engine.network.NetworkSnapshot
import com.aegissoft.dolphyx.engine.network.SecureLocalQrServer
import com.aegissoft.dolphyx.engine.network.TcpProbeResult
import com.aegissoft.dolphyx.engine.nfc.DolphyXHceService
import com.aegissoft.dolphyx.engine.nfc.NfcEngine
import com.aegissoft.dolphyx.engine.plugin.PluginVerificationResult
import com.aegissoft.dolphyx.engine.plugin.SandboxedPluginEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central ViewModel coordinating DolphyX hardware, database, security, and plugin engines.
 */
class DolphyXViewModel(application: Application) : AndroidViewModel(application) {

    val crypto: KeystoreCryptoManager = KeystoreCryptoManager()
    val prefs: SecurePreferences = SecurePreferences(application, crypto)
    val irDb: IrDatabaseHelper = IrDatabaseHelper(application)
    val customIr: CustomIrRepository = CustomIrRepository(application)
    val ouiDb: OuiDatabaseHelper = OuiDatabaseHelper(application)
    val nfcVault: NfcVaultRepository = NfcVaultRepository(application, crypto)
    val irEngine: IrTransmitterEngine = IrTransmitterEngine(application)
    val bleEngine: BleDiagnosticEngine = BleDiagnosticEngine(application, ouiDb)
    val nfcEngine: NfcEngine = NfcEngine(application)
    val netEngine: NetworkDiagnosticEngine = NetworkDiagnosticEngine(application)
    val qrServer: SecureLocalQrServer = SecureLocalQrServer()
    val pluginEngine: SandboxedPluginEngine = SandboxedPluginEngine
    val hidController: com.aegissoft.dolphyx.engine.hid.BluetoothHidController =
        com.aegissoft.dolphyx.engine.hid.BluetoothHidController(application)

    private val _darkTheme = MutableStateFlow(prefs.isDarkTheme())
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accent = MutableStateFlow(prefs.getAccent())
    val accent: StateFlow<String> = _accent.asStateFlow()

    private val _uiScale = MutableStateFlow(prefs.getUiScale())
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()

    private val _reduceMotion = MutableStateFlow(prefs.isReduceMotion())
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _flipperFont = MutableStateFlow(prefs.isFlipperFont())
    val flipperFont: StateFlow<Boolean> = _flipperFont.asStateFlow()

    private val _fabVisible = MutableStateFlow(prefs.isFabVisible())
    val fabVisible: StateFlow<Boolean> = _fabVisible.asStateFlow()

    private val _fabAction = MutableStateFlow(prefs.getFabAction())
    val fabAction: StateFlow<String> = _fabAction.asStateFlow()

    private val _menuView = MutableStateFlow(prefs.getMenuView())
    val menuView: StateFlow<String> = _menuView.asStateFlow()

    private val _menuStyle = MutableStateFlow(prefs.getMenuStyle())
    val menuStyle: StateFlow<String> = _menuStyle.asStateFlow()

    private val _irFavorites = MutableStateFlow(prefs.getIrFavoriteIds())
    val irFavorites: StateFlow<Set<Long>> = _irFavorites.asStateFlow()

    private val _favoriteSignals = MutableStateFlow<List<IrSignal>>(emptyList())
    val favoriteSignals: StateFlow<List<IrSignal>> = _favoriteSignals.asStateFlow()

    private val _customIrSignals = MutableStateFlow<List<CustomIrSignalRecord>>(customIr.getAll())
    val customIrSignals: StateFlow<List<CustomIrSignalRecord>> = _customIrSignals.asStateFlow()

    private val _language = MutableStateFlow(prefs.getLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    private val _defaultProbeHost = MutableStateFlow(
        prefs.getDefaultProbeHost().ifBlank { netEngine.suggestedProbeHost().orEmpty() }
    )
    val defaultProbeHost: StateFlow<String> = _defaultProbeHost.asStateFlow()

    private val _defaultProbePort = MutableStateFlow(prefs.getDefaultProbePort())
    val defaultProbePort: StateFlow<Int> = _defaultProbePort.asStateFlow()

    private val _defaultHttpsUrl = MutableStateFlow(prefs.getDefaultHttpsUrl())
    val defaultHttpsUrl: StateFlow<String> = _defaultHttpsUrl.asStateFlow()

    private val _defaultHceText = MutableStateFlow(prefs.getDefaultHceText())
    val defaultHceText: StateFlow<String> = _defaultHceText.asStateFlow()

    private val _irdbStats = MutableStateFlow(0 to 0)
    val irdbStats: StateFlow<Pair<Int, Int>> = _irdbStats.asStateFlow()

    private val _ouiCount = MutableStateFlow(0)
    val ouiCount: StateFlow<Int> = _ouiCount.asStateFlow()

    private val _nfcVaultRecords = MutableStateFlow<List<NfcTagRecord>>(emptyList())
    val nfcVaultRecords: StateFlow<List<NfcTagRecord>> = _nfcVaultRecords.asStateFlow()

    private val _networkSnapshot = MutableStateFlow(netEngine.captureSnapshot())
    val networkSnapshot: StateFlow<NetworkSnapshot> = _networkSnapshot.asStateFlow()

    private val _tcpProbeResult = MutableStateFlow<TcpProbeResult?>(null)
    val tcpProbeResult: StateFlow<TcpProbeResult?> = _tcpProbeResult.asStateFlow()

    private val _httpsCheckStatus = MutableStateFlow<String?>(null)
    val httpsCheckStatus: StateFlow<String?> = _httpsCheckStatus.asStateFlow()

    private val _plugins = MutableStateFlow<List<PluginVerificationResult>>(emptyList())
    val plugins: StateFlow<List<PluginVerificationResult>> = _plugins.asStateFlow()

    private val _irCategories = MutableStateFlow<List<String>>(emptyList())
    val irCategories: StateFlow<List<String>> = _irCategories.asStateFlow()

    private val _irBrands = MutableStateFlow<List<String>>(emptyList())
    val irBrands: StateFlow<List<String>> = _irBrands.asStateFlow()

    private val _irRemotes = MutableStateFlow<List<IrRemoteSummary>>(emptyList())
    val irRemotes: StateFlow<List<IrRemoteSummary>> = _irRemotes.asStateFlow()

    private val _irSignals = MutableStateFlow<List<IrSignal>>(emptyList())
    val irSignals: StateFlow<List<IrSignal>> = _irSignals.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _selectedRemote = MutableStateFlow<IrRemoteSummary?>(null)
    val selectedRemote: StateFlow<IrRemoteSummary?> = _selectedRemote.asStateFlow()

    private val _irStatusMessage = MutableStateFlow<String?>(null)
    val irStatusMessage: StateFlow<String?> = _irStatusMessage.asStateFlow()

    private var irBrandQuery: String = ""

    init {
        DolphyXHceService.activeNdefText.value = _defaultHceText.value
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _irdbStats.value = irDb.getStats()
                _irCategories.value = irDb.getCategories()
                val brands = irDb.getBrands(null, "")
                _irBrands.value = brands
                if (brands.isNotEmpty()) {
                    val firstBrand = brands.first()
                    _selectedBrand.value = firstBrand
                    val remotes = irDb.getRemotes(firstBrand, null, "")
                    _irRemotes.value = remotes
                    if (remotes.isNotEmpty()) {
                        val firstRemote = remotes.first()
                        _selectedRemote.value = firstRemote
                        _irSignals.value = irDb.getSignals(firstRemote.id)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load IRDB: ${e.message}")
            }
            try {
                _ouiCount.value = ouiDb.getCount()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load OUI DB: ${e.message}")
            }
            try {
                _nfcVaultRecords.value = nfcVault.getAll()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load NFC vault: ${e.message}")
            }
            try {
                _plugins.value = pluginEngine.loadBundledPlugins(getApplication())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load bundled plugins: ${e.message}")
            }
            refreshFavoriteSignals()
        }
    }

    private suspend fun refreshFavoriteSignals() {
        try {
            _favoriteSignals.value = irDb.getSignalsByIds(_irFavorites.value)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load IR favorites: ${e.message}")
        }
    }

    fun setThemeMode(mode: String) {
        prefs.setThemeMode(mode)
        _themeMode.value = prefs.getThemeMode()
        _darkTheme.value = _themeMode.value != SecurePreferences.THEME_LIGHT
    }

    fun setAccent(accent: String) {
        prefs.setAccent(accent)
        _accent.value = prefs.getAccent()
    }

    fun setUiScale(scale: Float) {
        prefs.setUiScale(scale)
        _uiScale.value = prefs.getUiScale()
    }

    fun setReduceMotion(enabled: Boolean) {
        prefs.setReduceMotion(enabled)
        _reduceMotion.value = prefs.isReduceMotion()
    }

    fun setFlipperFont(enabled: Boolean) {
        prefs.setFlipperFont(enabled)
        _flipperFont.value = prefs.isFlipperFont()
    }

    fun setFabVisible(visible: Boolean) {
        prefs.setFabVisible(visible)
        _fabVisible.value = prefs.isFabVisible()
    }

    fun setFabAction(action: String) {
        prefs.setFabAction(action)
        _fabAction.value = prefs.getFabAction()
    }

    fun setMenuView(view: String) {
        prefs.setMenuView(view)
        _menuView.value = prefs.getMenuView()
    }

    fun setMenuStyle(style: String) {
        prefs.setMenuStyle(style)
        _menuStyle.value = prefs.getMenuStyle()
    }

    fun toggleIrFavorite(signalId: Long) {
        _irFavorites.value = prefs.toggleIrFavorite(signalId)
        viewModelScope.launch(Dispatchers.IO) { refreshFavoriteSignals() }
    }

    fun saveCustomIrSignal(record: CustomIrSignalRecord) {
        _customIrSignals.value = customIr.save(record)
    }

    fun deleteCustomIrSignal(id: String) {
        _customIrSignals.value = customIr.delete(id)
    }

    fun importCustomIrSignals(signals: List<com.aegissoft.dolphyx.engine.ir.ImportedIrSignal>, source: String): Int {
        if (signals.isEmpty()) return 0
        val records = signals.map { signal ->
            com.aegissoft.dolphyx.data.CustomIrSignalRecord(
                id = customIr.newId(),
                name = "$source · ${signal.name}",
                protocol = signal.protocol,
                address = signal.address,
                command = signal.command,
                frequency = signal.frequency,
            )
        }
        _customIrSignals.value = customIr.saveAll(records)
        return records.size
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.setDarkTheme(enabled)
        _darkTheme.value = enabled
    }

    fun setLanguage(lang: String) {
        prefs.setLanguage(lang)
        _language.value = prefs.getLanguage()
    }

    fun updateRuntimeDefaults(
        probeHost: String,
        probePort: Int,
        httpsUrl: String,
        hceText: String,
    ) {
        prefs.setDefaultProbeHost(probeHost.ifBlank { netEngine.suggestedProbeHost().orEmpty() })
        prefs.setDefaultProbePort(probePort)
        prefs.setDefaultHttpsUrl(httpsUrl)
        prefs.setDefaultHceText(hceText)
        _defaultProbeHost.value = prefs.getDefaultProbeHost()
            .ifBlank { netEngine.suggestedProbeHost().orEmpty() }
        _defaultProbePort.value = prefs.getDefaultProbePort()
        _defaultHttpsUrl.value = prefs.getDefaultHttpsUrl()
        _defaultHceText.value = prefs.getDefaultHceText()
        DolphyXHceService.activeNdefText.value = _defaultHceText.value
    }

    fun saveLastNfcTag(label: String) {
        val tag = nfcEngine.lastTag.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val cleanLabel = label.trim().ifEmpty {
                getApplication<Application>().getString(R.string.nfc_default_label, tag.uidHex.take(8))
            }
            nfcVault.insert(
                label = cleanLabel,
                uidHex = tag.uidHex,
                techList = tag.techList.joinToString(", "),
                ndefPayload = tag.ndefText,
            )
            _nfcVaultRecords.value = nfcVault.getAll()
        }
    }

    fun deleteNfcRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            nfcVault.delete(id)
            _nfcVaultRecords.value = nfcVault.getAll()
        }
    }

    fun refreshNetwork() {
        _networkSnapshot.value = netEngine.captureSnapshot()
    }

    fun runTcpProbe(host: String, port: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _tcpProbeResult.value = netEngine.probeTcpPort(host, port)
        }
    }

    fun runHttpsCheck(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val result = netEngine.checkHttpsEndpoint(url)
            _httpsCheckStatus.value = result.fold(
                onSuccess = { (code, ms) -> app.getString(R.string.network_https_http_result, code, ms) },
                onFailure = { err -> err.message ?: app.getString(R.string.network_https_failed) },
            )
        }
    }

    fun reloadPlugins() {
        viewModelScope.launch(Dispatchers.IO) {
            _plugins.value = pluginEngine.loadBundledPlugins(getApplication())
        }
    }

    fun importPluginJson(rawJson: String): PluginVerificationResult {
        val result = pluginEngine.verifyPluginJson(rawJson)
        if (result is PluginVerificationResult.Valid) {
            _plugins.value = listOf(result) + _plugins.value.filterNot {
                it is PluginVerificationResult.Valid && it.manifest.id == result.manifest.id
            }
        }
        return result
    }

    fun selectIrCategory(category: String?) {
        _selectedCategory.value = category
        viewModelScope.launch(Dispatchers.IO) {
            val brands = irDb.getBrands(category, irBrandQuery)
            _irBrands.value = brands
            val targetBrand = _selectedBrand.value?.takeIf { it in brands } ?: brands.firstOrNull()
            _selectedBrand.value = targetBrand
            if (targetBrand != null) {
                val remotes = irDb.getRemotes(targetBrand, category, "")
                _irRemotes.value = remotes
                val firstRemote = remotes.firstOrNull()
                _selectedRemote.value = firstRemote
                _irSignals.value = if (firstRemote != null) irDb.getSignals(firstRemote.id) else emptyList()
            } else {
                _irRemotes.value = emptyList()
                _selectedRemote.value = null
                _irSignals.value = emptyList()
            }
        }
    }

    fun searchIrBrands(query: String) {
        irBrandQuery = query
        viewModelScope.launch(Dispatchers.IO) {
            val brands = irDb.getBrands(_selectedCategory.value, query)
            _irBrands.value = brands
            val targetBrand = _selectedBrand.value?.takeIf { it in brands } ?: brands.firstOrNull()
            _selectedBrand.value = targetBrand
            if (targetBrand != null) {
                val remotes = irDb.getRemotes(targetBrand, _selectedCategory.value, "")
                _irRemotes.value = remotes
                val firstRemote = remotes.firstOrNull()
                _selectedRemote.value = firstRemote
                _irSignals.value = if (firstRemote != null) irDb.getSignals(firstRemote.id) else emptyList()
            } else {
                _irRemotes.value = emptyList()
                _selectedRemote.value = null
                _irSignals.value = emptyList()
            }
        }
    }

    fun selectIrBrand(brand: String) {
        _selectedBrand.value = brand
        viewModelScope.launch(Dispatchers.IO) {
            val remotes = irDb.getRemotes(brand, _selectedCategory.value, "")
            _irRemotes.value = remotes
            val firstRemote = remotes.firstOrNull()
            _selectedRemote.value = firstRemote
            _irSignals.value = if (firstRemote != null) irDb.getSignals(firstRemote.id) else emptyList()
        }
    }

    fun selectIrRemote(remote: IrRemoteSummary) {
        _selectedRemote.value = remote
        viewModelScope.launch(Dispatchers.IO) {
            _irSignals.value = irDb.getSignals(remote.id)
        }
    }

    fun transmitIrSignal(signal: IrSignal) {
        viewModelScope.launch(Dispatchers.Default) {
            val app = getApplication<Application>()
            val result = irEngine.transmit(signal)
            _irStatusMessage.value = result.fold(
                onSuccess = { msg -> msg },
                onFailure = { err -> err.message ?: app.getString(R.string.ir_transmit_failed) },
            )
        }
    }

    override fun onCleared() {
        bleEngine.release()
        hidController.release()
        qrServer.stop()
        irDb.close()
        ouiDb.close()
        super.onCleared()
    }

    companion object {
        private const val TAG = "DolphyXViewModel"
    }
}
