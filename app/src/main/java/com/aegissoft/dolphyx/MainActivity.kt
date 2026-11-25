package com.aegissoft.dolphyx

import android.Manifest
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.IntentCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aegissoft.dolphyx.core.security.AppLocaleManager
import com.aegissoft.dolphyx.core.security.SecurePreferences
import com.aegissoft.dolphyx.ui.DolphyXViewModel
import com.aegissoft.dolphyx.ui.components.DolphyDockItem
import com.aegissoft.dolphyx.ui.components.DolphyFloatingDock
import com.aegissoft.dolphyx.ui.screens.BleScreen
import com.aegissoft.dolphyx.ui.screens.CameraScreen
import com.aegissoft.dolphyx.ui.screens.CastScreen
import com.aegissoft.dolphyx.ui.screens.ChatScreen
import com.aegissoft.dolphyx.ui.screens.HidRemoteScreen
import com.aegissoft.dolphyx.ui.screens.HomeScreen
import com.aegissoft.dolphyx.ui.screens.IrRemoteScreen
import com.aegissoft.dolphyx.ui.screens.LanScannerScreen
import com.aegissoft.dolphyx.ui.screens.NetworkQrScreen
import com.aegissoft.dolphyx.ui.screens.NfcScreen
import com.aegissoft.dolphyx.ui.screens.PluginsScreen
import com.aegissoft.dolphyx.ui.screens.PrinterScreen
import com.aegissoft.dolphyx.ui.screens.SettingsScreen
import com.aegissoft.dolphyx.ui.screens.ToolsScreen
import com.aegissoft.dolphyx.ui.theme.DolphyAccent
import com.aegissoft.dolphyx.ui.theme.DolphyXTheme

object DolphyRoutes {
    const val HOME = "home"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val BLE = "ble"
    const val IR = "ir"
    const val NFC = "nfc"
    const val NETWORK = "network"
    const val PLUGINS = "plugins"
    const val HID = "hid"
    const val LAN = "lan"
    const val PRINTER = "printer"
    const val CAMERA = "camera"
    const val CAST = "cast"
    const val CHAT = "chat"

    val primary = setOf(HOME, TOOLS, SETTINGS)
}

private val dockItems = listOf(
    Triple(DolphyRoutes.HOME, R.string.nav_home, Icons.Rounded.Bluetooth),
    Triple(DolphyRoutes.TOOLS, R.string.nav_tools, Icons.Rounded.Extension),
    Triple(DolphyRoutes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

class MainActivity : ComponentActivity() {

    private val viewModel: DolphyXViewModel by viewModels()
    private var widgetRoute by mutableStateOf<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        viewModel.bleEngine.refreshBondedDevices()
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = try {
            SecurePreferences(newBase).getLanguage()
        } catch (_: Exception) {
            "tr"
        }
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        widgetRoute = intent?.getStringExtra(com.aegissoft.dolphyx.ui.widget.DolphyXQuickWidgetProvider.EXTRA_ROUTE)
        handleNfcIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val accent by viewModel.accent.collectAsState()
            val uiScale by viewModel.uiScale.collectAsState()
            val reduceMotion by viewModel.reduceMotion.collectAsState()
            val flipperFont by viewModel.flipperFont.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                SecurePreferences.THEME_LIGHT -> false
                SecurePreferences.THEME_DARK -> true
                else -> systemDark
            }
            val accentEnum = when (accent) {
                SecurePreferences.ACCENT_ORANGE -> DolphyAccent.ORANGE
                SecurePreferences.ACCENT_RED -> DolphyAccent.RED
                SecurePreferences.ACCENT_GREEN -> DolphyAccent.GREEN
                SecurePreferences.ACCENT_PURPLE -> DolphyAccent.PURPLE
                else -> DolphyAccent.RAINBOW
            }
            DolphyXTheme(
                darkTheme = darkTheme,
                accent = accentEnum,
                uiScale = uiScale,
                reduceMotion = reduceMotion,
                flipperFont = flipperFont,
            ) {
                DolphyXAppShell(
                    viewModel = viewModel,
                    onRequestPermissions = ::requestHardwarePermissions,
                    onRecreateActivity = ::recreate,
                    initialRoute = widgetRoute,
                    onRouteConsumed = {
                        widgetRoute = null
                        intent?.removeExtra(com.aegissoft.dolphyx.ui.widget.DolphyXQuickWidgetProvider.EXTRA_ROUTE)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.nfcEngine.enableReaderMode(this)
        viewModel.refreshNetwork()
    }

    override fun onPause() {
        viewModel.nfcEngine.disableReaderMode(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
        if (tag != null) {
            viewModel.nfcEngine.parseTag(tag, intent)
        }
    }

    private fun requestHardwarePermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@Composable
private fun DolphyXAppShell(
    viewModel: DolphyXViewModel,
    onRequestPermissions: () -> Unit,
    onRecreateActivity: () -> Unit,
    initialRoute: String?,
    onRouteConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: DolphyRoutes.HOME
    val fabVisible by viewModel.fabVisible.collectAsState()
    val fabAction by viewModel.fabAction.collectAsState()

    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            navController.navigate(initialRoute) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    val back: () -> Unit = { navController.popBackStack() }
    val primarySelection = when (currentRoute) {
        DolphyRoutes.HOME -> DolphyRoutes.HOME
        DolphyRoutes.SETTINGS -> DolphyRoutes.SETTINGS
        else -> DolphyRoutes.TOOLS
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DolphyFloatingDock(
                items = dockItems.map { (route, labelRes, icon) ->
                    DolphyDockItem(route = route, label = stringResource(labelRes), icon = icon)
                },
                selectedRoute = primarySelection,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                fabIcon = if (fabAction == SecurePreferences.FAB_ACTION_SETTINGS) Icons.Rounded.Settings else Icons.Rounded.Extension,
                onFabClick = {
                    val target = if (fabAction == SecurePreferences.FAB_ACTION_SETTINGS) DolphyRoutes.SETTINGS else DolphyRoutes.TOOLS
                    navController.navigate(target) { launchSingleTop = true }
                },
                fabContentDescription = stringResource(R.string.nav_tools),
                showFab = fabVisible,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = DolphyRoutes.HOME,
            ) {
                composable(DolphyRoutes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                        onRequestPermissions = onRequestPermissions,
                    )
                }
                composable(DolphyRoutes.TOOLS) {
                    ToolsScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                    )
                }
                composable(DolphyRoutes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onRecreateActivity = onRecreateActivity,
                        onOpenPlugins = {
                            navController.navigate(DolphyRoutes.PLUGINS) { launchSingleTop = true }
                        },
                    )
                }
                composable(DolphyRoutes.BLE) {
                    BleScreen(
                        viewModel = viewModel,
                        onRequestBlePermissions = onRequestPermissions,
                        onBack = back,
                    )
                }
                composable(DolphyRoutes.IR) {
                    IrRemoteScreen(viewModel = viewModel, onBack = back)
                }
                composable(DolphyRoutes.NFC) {
                    NfcScreen(viewModel = viewModel, onBack = back)
                }
                composable(DolphyRoutes.NETWORK) {
                    NetworkQrScreen(viewModel = viewModel, onBack = back)
                }
                composable(DolphyRoutes.PLUGINS) {
                    PluginsScreen(viewModel = viewModel, onBack = back)
                }
                composable(DolphyRoutes.HID) {
                    HidRemoteScreen(viewModel = viewModel, onBack = back)
                }
                composable(DolphyRoutes.LAN) {
                    LanScannerScreen(onBack = back)
                }
                composable(DolphyRoutes.PRINTER) {
                    PrinterScreen(onBack = back)
                }
                composable(DolphyRoutes.CAMERA) {
                    CameraScreen(onBack = back)
                }
                composable(DolphyRoutes.CAST) {
                    CastScreen(onBack = back)
                }
                composable(DolphyRoutes.CHAT) {
                    ChatScreen(onBack = back)
                }
            }
        }
    }
}
