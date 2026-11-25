# Testing & Verification

## Automated tests

```powershell
.\gradlew.bat testDebugUnitTest --no-daemon --console=plain
```

**Result:** 35/35 passing, 0 failures, 0 skipped.

**Device verification (25 Nov 2026, v2.2.1):** 14 routes opened on Honor Magic `PTP-N49`
(Android 16 / API 36) at 100% and 130% system font scale, in Turkish and English, dark and
light themes. No fatal exceptions.

Interaction checks run on the same build:

| Check | Method | Result |
| :--- | :--- | :--- |
| Dock navigation | Tapped each slot in order (Bluetooth → İşlevler → Ayarlar) | Correct screen every time |
| NFC segmented tabs | Tapped Oku / Yaz / Emüle / Kasa and asserted tab body | Correct body each time |
| IR segmented tabs | Tapped Gözat / Evrensel / Favoriler / Özel | Correct body each time |
| Tools catalogue | Tapped every row from the first screen and after scrolling | All rows reached their route |
| Theme switch | Tapped Sistem / Koyu / Açık in settings | Applied without restart |
| LAN under VPN | Opened scanner with an active VPN | Scan disabled, explicit warning, no fake hosts |

| Suite | Tests | Coverage |
| :--- | :---: | :--- |
| `IrProtocolEncoderTest` | 8 | NEC, NECext, NEC42, Samsung32, SIRC, RC5, RC6, raw |
| `SecurityAndPluginTest` | 5 | SHA-256, permission allowlist, keystore |
| `NetworkAndNfcTest` | 4 | LAN/QR/NDEF |
| `HidKeyMapperTest` | 4 | HID report mapping |
| `CustomIrSignalTest` | 2 | Custom IR record validation |
| `IrFileImporterTest` | 2 | `.ir` parsed/raw and incomplete blocks |
| `ChatFramingTest` | 3 | Chunking, UTF-8 reassembly, partial frame |
| `CastEngineTest` | 4 | Range parsing, SSDP LOCATION, AVTransport URL |
| `IppPrintClientTest` | 3 | IPP header, status code, URI |

## Build verification

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease --no-daemon --console=plain
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk` (signed when `keystore.properties` exists)
- Release size: 19.09 MiB (20,020,141 bytes, signed, v2.2.1)
- SDK: min 26 / target 36 / compile 36; Java 17

## Device smoke test

Device: Honor Magic `PTP-N49`, Android 16 (API 36).

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
$routes = @('home','tools','settings','ble','chat','ir','nfc','network','hid','lan','printer','camera','cast','plugins')
foreach ($route in $routes) {
  adb shell am start -n com.aegissoft.dolphyx.debug/com.aegissoft.dolphyx.MainActivity --es dolphyx_route $route
  Start-Sleep -Seconds 2
}
adb logcat -d -t 300 | Select-String "FATAL EXCEPTION"
```

**Result:** all 14 routes open; 0 fatal exceptions.

Additional live checks:

- BLE scan found 51 devices and rendered live results.
- Dolphy Chat hosting started and showed the localized "hosting" state.
- IR: category â†’ brand â†’ model â†’ key transmission exercised (AC remote).
- QR share server started and exposed a stop action.
- Printer and camera mDNS discovery started and reported localized status.
- Settings, NFC write tab, LAN, HID, plugins, and cast routes rendered correctly.

## Manual / hardware checks

| Feature | Requirement |
| :--- | :--- |
| HID | Pair the phone with a PC/TV as a Bluetooth keyboard |
| NFC write | Physical NDEF tag |
| IR | Device with a consumer IR emitter |
| IPP print | IPP/AirPrint printer on the same Wi-Fi |
| Camera | RTSP/ONVIF camera on the same Wi-Fi |
| DLNA cast | DLNA MediaRenderer on the same network |
| Chat | Two devices running DolphyX |

## Reproducible screenshots

Screenshots in [`docs/screenshots`](screenshots) were captured on the device above with the
release build at 100% font scale in the system dark theme. Use `adb shell screencap -p`;
cropping is unnecessary because the app draws edge-to-edge itself.
