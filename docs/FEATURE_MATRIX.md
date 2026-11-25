# Feature Matrix

Status legend: **Complete** Â· **Partial** Â· **Planned** Â· **Out of scope (defensive)**

| Area | Feature | Status | Module |
| :--- | :--- | :---: | :--- |
| Navigation | 3-tab capsule dock, expanding tab, square FAB | Complete | `MainActivity`, `DolphyFloatingDock` |
| Design | Monochrome OLED, 28dp cards, white pills, pixel art | Complete | `Theme.kt`, `DolphyXComponents.kt` |
| Personalisation | 5 accents, pixel font, UI scale, reduce motion | Complete | `SettingsScreen`, `Theme.kt` |
| Functions | Containers/Tiles, Standard/Compact | Complete | `ToolsScreen` |
| Localisation | Turkish + English, 517 keys | Complete | `values/`, `values-tr/` |
| BLE | Scan, bonded-first, RSSI, OUI, GATT | Complete | `BleDiagnosticEngine` |
| Chat | BLE GATT nearby chat | Complete | `BleChatEngine`, `ChatScreen` |
| IR | 2,460 remotes / 58,766 signals, 45 categories | Complete | `IrDatabaseHelper`, `IrRemoteScreen` |
| IR | Favorites, universal remote, custom signals | Complete | `DolphyXViewModel`, `CustomIrRepository` |
| IR | Flipper `.ir` import | Complete | `IrFileImporter` |
| IR | USB IR dongle | Planned | Hardware-dependent |
| NFC | Read, NDEF write, erase, encrypted vault | Complete | `NfcEngine`, `NfcWriterEngine`, `NfcVaultRepository` |
| NFC | Type-4 HCE profiles | Complete | `DolphyXHceService` |
| HID | Keyboard, media, trackpad | Complete | `BluetoothHidController`, `HidRemoteScreen` |
| Network | Interface summary, TCP probe, HTTPS check | Complete | `NetworkDiagnosticEngine`, `NetworkQrScreen` |
| Network | `/24` LAN scan, DNS/TLS | Complete | `LanScannerEngine` |
| Printer | IPP/AirPrint discovery | Complete | `PrinterDiscoveryEngine` |
| Printer | IPP Print-Job (PDF/text/image) | Complete | `IppPrintClient` |
| Camera | RTSP/ONVIF discovery, manual URL | Complete | `CameraDiscoveryEngine`, `CameraScreen` |
| Camera | In-app RTSP player | Planned | Media3 integration |
| Cast | DLNA SSDP + AVTransport playback | Complete | `DlnaCastEngine`, `CastScreen` |
| Cast | Range-capable local media server | Complete | `LocalMediaServer` |
| QR | Pure Kotlin QR + token server | Complete | `QrMatrixGenerator`, `SecureLocalQrServer` |
| Plugins | SHA-256 + permission allowlist, no execution | Complete | `SandboxedPluginEngine` |
| Widget | IR/NFC/app shortcuts | Complete | `DolphyXQuickWidgetProvider` |
| Smart home | ELK-BLEDOM LED strip | Planned | Protocol needs hardware validation |
| Scooter | Read-only telemetry | Planned | Protocol needs hardware validation |
| Security | BLE spam/jammer, TWS hijack, BadUSB, phishing, brute force, unrestricted code execution | Out of scope | â€” |
| Security | IR jammer category | Out of scope | Filtered from IRDB queries |

## Known limits

- Chat transport is currently unencrypted at the BLE link layer.
- IPP printing depends on the printer accepting `application/pdf`, `text/plain`, or an image format; self-signed `ipps` certificates are rejected.
- RTSP streams open in an external player; there is no in-app player yet.
- DLNA playback depends on the renderer accepting AVTransport; DRM content is unsupported.
- Hardware-dependent features (HID host pairing, NFC tags, IR blaster, USB IR dongle) require physical devices.
