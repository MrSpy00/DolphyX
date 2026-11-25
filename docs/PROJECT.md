# DolphyX — Project Overview

> **Project:** DolphyX — monochrome hardware & RF diagnostic suite for Android
> **Package:** `com.aegissoft.dolphyx` · **Version:** 2.1.0 (`versionCode 3`)
> **Organization:** aegisSoft · **Lead developer:** mrSpy · **License:** GPL-3.0
> **Upstream:** [unvoiddd/Dolphy-App](https://github.com/unvoiddd/Dolphy-App)

## What it is

DolphyX is a clean, local-first Android toolkit for wireless protocol research and hardware diagnostics. It keeps the original Dolphy concept and monochrome design language, then adds fixes, new legitimate modules, and defensive hardening.

No root, no cloud account, no analytics.

## Architecture

```text
app/src/main/java/com/aegissoft/dolphyx/
├── MainActivity.kt                  # Edge-to-edge shell, capsule dock, 14 routes
├── core/
│   ├── config/                      # BuildConfig-backed central configuration
│   ├── security/                    # Keystore AES-256-GCM, encrypted preferences
│   └── service/                     # Android 16 foreground service
├── data/                            # IRDB, OUI, NFC vault, custom IR signals
├── engine/
│   ├── ble/                         # BLE scanner/GATT + Dolphy Chat
│   ├── ir/                          # Protocol encoder, transmitter, .ir importer
│   ├── nfc/                         # Reader, NDEF writer, Type-4 HCE
│   ├── hid/                         # Bluetooth HID keyboard/media/trackpad
│   ├── cast/                        # DLNA SSDP/AVTransport + local media server
│   ├── network/                     # LAN, DNS/TLS, mDNS, IPP, RTSP, QR
│   └── plugin/                      # Manifest verification, allowlist
└── ui/
    ├── theme/                       # Monochrome tokens, accents, fonts, scale
    ├── components/                  # Cards, headers, pills, dock, rows
    ├── widget/                      # Home-screen shortcut widget
    └── screens/                     # 14 screens
```

## Modules

| Module | Status | Notes |
| :--- | :--- | :--- |
| BLE scan + bonded devices + GATT | Complete | Bonded-first, `TRANSPORT_LE`, RSSI, OUI |
| Dolphy Chat | Complete | GATT server/client, newline framing, 200-message history |
| IR database | Complete | 2,460 remotes / 58,766 signals, 45 categories |
| IR favorites / universal / custom | Complete | 8 protocols + raw, local persistence |
| `.ir` import | Complete | Flipper parsed + raw signals |
| NFC read/write/vault/HCE | Complete | AES-256-GCM vault, unlock-gated HCE |
| Bluetooth HID | Complete | Paired host only |
| LAN / DNS / TLS | Complete | `/24` TCP scan, certificate view |
| IPP printing | Complete | mDNS `rp` path + binary Print-Job |
| Camera discovery | Complete | RTSP/ONVIF mDNS; external player hand-off |
| DLNA cast | Complete | SSDP + AVTransport, Range-capable media server |
| QR sharing | Complete | Loopback-first, 128-bit token |
| Plugin verification | Complete | Canonical SHA-256 + allowlist; no execution |
| Widget | Complete | `exported=false`, immutable PendingIntent |

## Defensive scope

Intentionally excluded: BLE spam/jammer, TWS hijack, BadUSB/DuckyScript, phishing, unauthorized brute force, unrestricted root/Shizuku/Rhino/Python/DEX execution, and the IR jammer category.

## Configuration

All branding, URLs, asset names, and default limits live in `gradle.properties` / `BuildConfig` and `DolphyXConfig`. UI text lives only in `values/strings.xml` and `values-tr/strings.xml`. No absolute paths, device serials, or personal data are stored in the repository.

## Build

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease --no-daemon
```

See [TESTING.md](TESTING.md) for verification details.
