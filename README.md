<p align="center">
  <img src="assets/logo.png" width="128" height="128" alt="DolphyX logo" />
</p>

<h1 align="center">DolphyX</h1>

<p align="center">
  <strong>Monochrome hardware &amp; RF diagnostic suite for Android â€” BLE, IR, NFC, HID, LAN, IPP, DLNA.</strong><br>
  <em>Android iÃ§in monokrom donanÄ±m ve RF tanÄ± paketi â€” BLE, IR, NFC, HID, LAN, IPP, DLNA.</em>
</p>

<p align="center">
  <a href="https://github.com/MrSpy00/DolphyX/releases"><img src="https://img.shields.io/badge/Release-v2.2.1-F4F4F4?style=for-the-badge&logo=android&logoColor=0A0A0A" alt="Release v2.2.1"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-2A2A2A?style=for-the-badge" alt="License GPL-3.0"></a>
  <a href="https://developer.android.com/about/versions/16"><img src="https://img.shields.io/badge/Android-8.0_to_16+_(API_36)-3D3D3D?style=for-the-badge&logo=android&logoColor=F4F4F4" alt="Android API 26-36"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.2"></a>
  <a href="https://aegissoft.com.tr"><img src="https://img.shields.io/badge/aegisSoft-181717?style=for-the-badge" alt="aegisSoft"></a>
  <a href="https://github.com/MrSpy00"><img src="https://img.shields.io/badge/Author-mrSpy-181717?style=for-the-badge&logo=github" alt="mrSpy"></a>
</p>

<p align="center">
  <a href="#-tÃ¼rkÃ§e"><img src="assets/flags/tr.svg" width="26" height="26" alt="TÃ¼rkÃ§e bayraÄŸÄ±"> <strong>TÃ¼rkÃ§e</strong></a>
  &nbsp;&nbsp;&nbsp;
  <a href="#-english"><img src="assets/flags/gb.svg" width="26" height="26" alt="English flag"> <strong>English</strong></a>
</p>

<p align="center">
  <img src="docs/screenshots/home.png" width="24%" alt="Bluetooth home">
  <img src="docs/screenshots/tools.png" width="24%" alt="Functions">
  <img src="docs/screenshots/ir.png" width="24%" alt="IR remotes">
  <img src="docs/screenshots/chat.png" width="24%" alt="Dolphy Chat">
</p>

---

<h1 align="left"><img src="assets/flags/tr.svg" width="30" height="30" alt="TÃ¼rkÃ§e bayraÄŸÄ±"> TÃ¼rkÃ§e</h1>

## Genel BakÄ±ÅŸ

**DolphyX**, [unvoiddd/Dolphy-App](https://github.com/unvoiddd/Dolphy-App) projesini temel alan; orijinalin monokrom tasarÄ±m dilini koruyup Ã¼zerine yeni modÃ¼ller, hata dÃ¼zeltmeleri ve gÃ¼venlik iyileÅŸtirmeleri ekleyen baÄŸÄ±msÄ±z bir **aegisSoft** geliÅŸtirmesidir. Bu depo, temel alÄ±nan projenin bir tÃ¼revi/modifiye sÃ¼rÃ¼mÃ¼dÃ¼r; orijinal geliÅŸtiriciye ve GPL-3.0 lisansÄ±na saygÄ± gÃ¶sterir.

Uygulama; kÃ¶k (root) gerektirmeden BLE, kÄ±zÄ±lÃ¶tesi, NFC, Bluetooth HID, yerel aÄŸ, IPP yazdÄ±rma ve DLNA yayÄ±nÄ±nÄ± tek bir monokrom arayÃ¼zde toplar. TÃ¼m iÅŸlemler cihaz Ã¼zerinde ve yerel aÄŸda Ã§alÄ±ÅŸÄ±r; bulut hesabÄ±, telemetri veya reklam yoktur.

**DokÃ¼mantasyon:** [Proje Ã¶zeti](docs/PROJECT.md) Â· [TasarÄ±m sistemi](docs/DESIGN_SYSTEM.md) Â· [Ã–zellik matrisi](docs/FEATURE_MATRIX.md) Â· [Test ve doÄŸrulama](docs/TESTING.md)

## Ã–ne Ã‡Ä±kanlar

| ModÃ¼l | Yetenek |
| :--- | :--- |
| **Bluetooth / BLE** | EÅŸleÅŸmiÅŸ cihaz Ã¶nceliÄŸi, RSSI, OUI Ã¼retici Ã§Ã¶zÃ¼mleme, GATT servis/karakteristik keÅŸfi |
| **Dolphy Chat** | BLE GATT Ã¼zerinden yakÄ±n sohbet; sunucu + istemci, yeni satÄ±r Ã§erÃ§eveleme |
| **IR** | 2.460 kumanda / 58.766 sinyal; 45 kategori, marka/model arama, favoriler, evrensel kumanda, 8 protokol + raw Ã¶zel sinyal, Flipper `.ir` iÃ§e aktarma |
| **NFC** | Etiket okuma (UID/tech/NDEF), metin/URL/Wi-Fi/vCard yazma, silme, AES-256-GCM kasasÄ±, Type-4 HCE |
| **HID** | EÅŸleÅŸmiÅŸ PC/TV iÃ§in Bluetooth klavye, medya tuÅŸlarÄ±, trackpad |
| **AÄŸ** | TCP port yoklama, HTTPS kontrolÃ¼, `/24` LAN taramasÄ±, DNS/TLS incelemesi |
| **YazÄ±cÄ±** | IPP/AirPrint mDNS keÅŸfi + gerÃ§ek IPP Print-Job (PDF/metin/gÃ¶rsel) |
| **Kamera** | RTSP/ONVIF mDNS keÅŸfi, elle RTSP adresi, harici oynatÄ±cÄ±ya devir |
| **Cast** | SSDP DLNA keÅŸfi, AVTransport ile TV'ye yayÄ±n, Range destekli yerel HTTP sunucusu |
| **QR** | Saf Kotlin QR Ã¼retimi, token korumalÄ± yerel paylaÅŸÄ±m sunucusu |
| **Eklenti** | Kanonik SHA-256 + yetki beyaz listesi; kod Ã§alÄ±ÅŸtÄ±rma yok |
| **Widget** | IR/NFC/uygulama kÄ±sayollarÄ±; `exported=false` |

## TasarÄ±m

- **Monokrom OLED:** `#0A0A0A` zemin, `#2A2A2A` kartlar, 28dp kÃ¶ÅŸe, beyaz 64dp pill butonlar.
- **KapsÃ¼l dock:** Aktif sekme geniÅŸler; kare FAB gÃ¶rÃ¼nÃ¼rlÃ¼ÄŸÃ¼ ve eylemi ayarlanabilir.
- **KiÅŸiselleÅŸtirme:** 5 vurgu (Rainbow varsayÄ±lan, Orange, Red, Green, Purple), Flipper pixel font, %85â€“135 arayÃ¼z Ã¶lÃ§eÄŸi, reduce motion, Functions Containers/Tiles ve Standard/Compact.
- **Pixel sanat:** Aseprite ile Ã¼retilen maskot ve banner (`px_dolphin.png`, `px_scene.png`).
- **EriÅŸilebilirlik:** Edge-to-edge + `safeDrawingPadding()`, 48dp dokunma hedefleri, kesilmeyen metinler, sÄ±fÄ±r emoji.
- **Diller:** TÃ¼rkÃ§e ve Ä°ngilizce tam string eÅŸitliÄŸi (540 anahtar).

## SavunmacÄ± Kapsam

AÅŸaÄŸÄ±daki saldÄ±rÄ±/suistimal bileÅŸenleri **bilinÃ§li olarak yoktur**: BLE spam/jammer, TWS hijack, BadUSB/DuckyScript, phishing, yetkisiz kamera/router brute-force, kÄ±sÄ±tlamasÄ±z root/Shizuku/Rhino/Python/DEX Ã§alÄ±ÅŸtÄ±rma. IR jammer kategorisi veritabanÄ±ndan filtrelenir.

## Kurulum ve Derleme

**Gereksinimler:** JDK 17, Android SDK 36, Gradle Wrapper (depoda hazÄ±r).

```bash
git clone https://github.com/MrSpy00/DolphyX.git
cd DolphyX
```

```powershell
# 35 birim testi
.\gradlew.bat testDebugUnitTest

# Debug APK
.\gradlew.bat assembleDebug

# Release APK
.\gradlew.bat assembleRelease
```

Ã‡Ä±ktÄ±lar:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Kurulum:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.aegissoft.dolphyx.debug/com.aegissoft.dolphyx.MainActivity
```

Ãœretim imzasÄ± iÃ§in `keystore.properties` veya `DOLPHYX_KEYSTORE_*` ortam deÄŸiÅŸkenleri kullanÄ±labilir; ardÄ±ndan `.\build_and_install.ps1`.

## Proje YapÄ±sÄ±

```text
DolphyX/
â”œâ”€â”€ app/src/main/java/com/aegissoft/dolphyx/
â”‚   â”œâ”€â”€ core/{config,security,service}   # BuildConfig, Keystore AES-256-GCM, FGS
â”‚   â”œâ”€â”€ data/                            # IRDB, OUI, NFC kasasÄ±, Ã¶zel IR kayÄ±tlarÄ±
â”‚   â”œâ”€â”€ engine/
â”‚   â”‚   â”œâ”€â”€ ble/                         # BLE tarayÄ±cÄ± + Dolphy Chat
â”‚   â”‚   â”œâ”€â”€ ir/                          # Protokol kodlayÄ±cÄ±, verici, .ir iÃ§e aktarma
â”‚   â”‚   â”œâ”€â”€ nfc/                         # Okuyucu, NDEF yazÄ±cÄ±, HCE
â”‚   â”‚   â”œâ”€â”€ hid/                         # Bluetooth HID
â”‚   â”‚   â”œâ”€â”€ cast/                        # DLNA + yerel medya sunucusu
â”‚   â”‚   â””â”€â”€ network/                     # LAN, mDNS, IPP, RTSP, QR
â”‚   â””â”€â”€ ui/{theme,components,screens,widget}
â”œâ”€â”€ docs/                                # TasarÄ±m, eÅŸitlik matrisi, raporlar, ekranlar
â”œâ”€â”€ tools/                               # Aseprite pixel sanat betikleri
â””â”€â”€ scripts/                             # VarlÄ±k paketleme
```

## DoÄŸrulama

- **35/35 JUnit 4 testi** baÅŸarÄ±lÄ±.
- Honor Magic `PTP-N49` (Android 16 / API 36) Ã¼zerinde tÃ¼m rotalar aÃ§Ä±ldÄ±: `home, tools, settings, ble, chat, ir, nfc, network, hid, lan, printer, camera, cast, plugins` â€” **0 fatal**.
- BLE taramasÄ± canlÄ± 51 cihaz buldu; IR kategorisi â†’ marka â†’ model â†’ tuÅŸ gÃ¶nderimi doÄŸrulandÄ±; QR sunucusu ve Chat hosting baÅŸlatÄ±ldÄ±.

## Ekran GÃ¶rÃ¼ntÃ¼leri

| Bluetooth | Functions | IR | Chat |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="220"> | <img src="docs/screenshots/tools.png" width="220"> | <img src="docs/screenshots/ir.png" width="220"> | <img src="docs/screenshots/chat.png" width="220"> |

| YazÄ±cÄ± | Kamera | Cast | Ayarlar |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/printer.png" width="220"> | <img src="docs/screenshots/camera.png" width="220"> | <img src="docs/screenshots/cast.png" width="220"> | <img src="docs/screenshots/settings.png" width="220"> |

---

<h1 align="left"><img src="assets/flags/gb.svg" width="30" height="30" alt="English flag"> English</h1>

## Overview

**DolphyX** is an independent **aegisSoft** project based on [unvoiddd/Dolphy-App](https://github.com/unvoiddd/Dolphy-App). It keeps the original monochrome design language and adds new modules, bug fixes, and security hardening on top. This repository is a modified derivative of the upstream project and credits the original author and its GPL-3.0 license.

The app brings BLE, infrared, NFC, Bluetooth HID, local networking, IPP printing, and DLNA casting into one monochrome interface without root. Everything runs on-device or on the local network; there is no cloud account, telemetry, or advertising.

**Documentation:** [Project overview](docs/PROJECT.md) Â· [Design system](docs/DESIGN_SYSTEM.md) Â· [Feature matrix](docs/FEATURE_MATRIX.md) Â· [Testing & verification](docs/TESTING.md)

## Highlights

| Module | Capability |
| :--- | :--- |
| **Bluetooth / BLE** | Bonded-device priority, RSSI, OUI vendor lookup, GATT service/characteristic discovery |
| **Dolphy Chat** | Nearby chat over BLE GATT; server + client, newline framing |
| **IR** | 2,460 remotes / 58,766 signals; 45 categories, brand/model search, favorites, universal remote, 8 protocols + raw custom signals, Flipper `.ir` import |
| **NFC** | Tag reading (UID/tech/NDEF), text/URL/Wi-Fi/vCard writing, erase, AES-256-GCM vault, Type-4 HCE |
| **HID** | Bluetooth keyboard, media keys, and trackpad for a paired PC/TV |
| **Network** | TCP port probe, HTTPS check, `/24` LAN scan, DNS/TLS inspection |
| **Printer** | IPP/AirPrint mDNS discovery + real IPP Print-Job (PDF/text/image) |
| **Camera** | RTSP/ONVIF mDNS discovery, manual RTSP address, hand-off to an external player |
| **Cast** | SSDP DLNA discovery, AVTransport playback, Range-capable local HTTP server |
| **QR** | Pure Kotlin QR generation, token-protected local share server |
| **Plugins** | Canonical SHA-256 + permission allowlist; no code execution |
| **Widget** | IR/NFC/app shortcuts; `exported=false` |

## Design

- **Monochrome OLED:** `#0A0A0A` background, `#2A2A2A` cards, 28dp corners, white 64dp pill buttons.
- **Capsule dock:** Active tab expands; square FAB visibility and action are configurable.
- **Personalisation:** 5 accents (Rainbow default, Orange, Red, Green, Purple), Flipper pixel font, 85â€“135% UI scale, reduce motion, Functions Containers/Tiles and Standard/Compact styles.
- **Pixel art:** Aseprite-generated mascot and banner (`px_dolphin.png`, `px_scene.png`).
- **Accessibility:** Edge-to-edge + `safeDrawingPadding()`, 48dp touch targets, no clipped text, zero emoji.
- **Languages:** Full Turkish and English string parity (540 keys).

## Defensive Scope

The following attack/abuse components are **intentionally absent**: BLE spam/jammer, TWS hijack, BadUSB/DuckyScript, phishing, unauthorized camera/router brute force, and unrestricted root/Shizuku/Rhino/Python/DEX execution. The IR jammer category is filtered from the database.

## Build & Install

**Requirements:** JDK 17, Android SDK 36, Gradle Wrapper (included).

```bash
git clone https://github.com/MrSpy00/DolphyX.git
cd DolphyX
```

```powershell
# 35 unit tests
.\gradlew.bat testDebugUnitTest

# Debug APK
.\gradlew.bat assembleDebug

# Release APK
.\gradlew.bat assembleRelease
```

Outputs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Install:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.aegissoft.dolphyx.debug/com.aegissoft.dolphyx.MainActivity
```

For release signing use `keystore.properties` or the `DOLPHYX_KEYSTORE_*` environment variables, then run `.\build_and_install.ps1`.

## Verification

- **35/35 JUnit 4 tests** pass.
- All routes opened on a Honor Magic `PTP-N49` (Android 16 / API 36): `home, tools, settings, ble, chat, ir, nfc, network, hid, lan, printer, camera, cast, plugins` â€” **0 fatal**.
- Live BLE scan found 51 devices; IR category â†’ brand â†’ model â†’ key transmission was exercised; QR server and Chat hosting started successfully.

## Privacy & Security

- No analytics, ads, accounts, or cloud sync.
- Sensitive preferences and the NFC vault are encrypted with AES-256-GCM via Android Keystore.
- Cleartext HTTP is blocked globally; only the loopback QR server is allowed.
- HCE requires device unlock; exported components are minimized (`RECEIVER_NOT_EXPORTED`, `exported=false` widget).
- Chat and local media stay on the device/LAN; chat transport is currently unencrypted at the BLE link layer (documented limitation).

## Screenshots

See the Turkish section above; the same images are used for both languages.

---

## Credits & License / EmeÄŸi GeÃ§enler ve Lisans

This project is based on **[unvoiddd/Dolphy-App](https://github.com/unvoiddd/Dolphy-App)** (GPL-3.0). The original project and its author are credited for the concept, feature set, and design language. DolphyX is an independent derivative with substantial modifications, new modules, and fixes by **aegisSoft / mrSpy** (2026). See [NOTICE](NOTICE) for the modification statement.

Bu proje **[unvoiddd/Dolphy-App](https://github.com/unvoiddd/Dolphy-App)** (GPL-3.0) projesini temel alÄ±r. Konsept, Ã¶zellik seti ve tasarÄ±m dili iÃ§in orijinal proje ve geliÅŸtiricisi anÄ±lÄ±r. DolphyX; **aegisSoft / mrSpy** tarafÄ±ndan kapsamlÄ± deÄŸiÅŸiklikler, yeni modÃ¼ller ve dÃ¼zeltmeler iÃ§eren baÄŸÄ±msÄ±z bir tÃ¼revdir (2026). DeÄŸiÅŸiklik beyanÄ± iÃ§in [NOTICE](NOTICE) dosyasÄ±na bakÄ±n.

- **License:** [GNU General Public License v3.0](LICENSE)
- **Upstream:** <https://github.com/unvoiddd/Dolphy-App>
- **Developer:** [aegisSoft](https://aegissoft.com.tr) Â· [mrSpy](https://github.com/MrSpy00)

<p align="center">
  <sub>DolphyX is not affiliated with or endorsed by the upstream author or any hardware vendor.<br>
  DolphyX, orijinal geliÅŸtirici veya herhangi bir donanÄ±m Ã¼reticisiyle baÄŸlantÄ±lÄ± deÄŸildir.</sub>
</p>
