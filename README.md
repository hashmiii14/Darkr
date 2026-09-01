# 🌑 Darkr — Precision Screen Suite & AMOLED Blackout Utility

[![Build & Test CI](https://github.com/hashmiii14/Darkr/actions/workflows/ci.yml/badge.svg)](https://github.com/hashmiii14/Darkr/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/hashmiii14/Darkr?color=white&label=Release&logo=github)](https://github.com/hashmiii14/Darkr/releases)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-white.svg)](https://android.com)
[![Design](https://img.shields.io/badge/Design-Strict_Monochrome_B%26W-white.svg)]()
[![License](https://img.shields.io/badge/License-Apache_2.0-white.svg)](LICENSE)

> **Darkr** is a high-performance, strictly monochrome screen blackout & privacy utility engineered for modern OLED/AMOLED Android displays. It allows users to black out their screen completely while permitted audio/media continues playing in the background.

---

## ⚡ Core Features

### 1. 🌑 Pure AMOLED Zero-Leak Blackout
- Emits pure `#000000` pixels across the entire display with complete display cutout, notch, status bar, and navigation bar coverage (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` / `SHORT_EDGES`).
- Reduces active pixel power consumption on OLED/AMOLED panels while maintaining background audio streams (YouTube, YouTube Music, podcasts, Spotify, audiobooks).
- **Wake Gesture**: Failsafe double-tap anywhere to wake with tactile haptic feedback.

### 2. 🎵 Legitimate Android Media Controls
- Built-in low-luminance media control bar (Play/Pause, Next, Previous) utilizing standard Android `AudioManager` and `MediaSession` key event dispatching.
- Control media playback without waking up the screen or violating third-party app terms.

### 3. ⚡ Android Quick Settings Tile
- Native `TileService` integration allows you to pull down the notification shade from any app and tap the **"Darkr"** tile to instantly black out the screen.

### 4. 🕒 Minimal Clock Mode & Burn-In Protection
- Low-power, low-contrast display showing current time, date, and battery percentage.
- **Pixel-Shift Algorithm**: Automatically shifts element coordinates by ±4dp every 60 seconds to prevent OLED burn-in.

### 5. 📱 Smart Pocket Auto-Blackout
- Uses hardware proximity and accelerometer sensors to automatically black out the screen when the phone is placed in a pocket or placed face-down on a desk.

### 6. 🛡️ Dual-Segment Privacy Curtain
- Splits the screen into draggable top and bottom dark masks with a customizable viewing slit.
- **100% Native Background Touch Passthrough**: Apps underneath (WhatsApp, Telegram, browsers) receive full, unobstructed touch input in the viewing slit.

### 7. 🔒 Touch Freeze Lock
- Intercepts and consumes all touch inputs across the display while keeping underlying content completely visible.
- **Unlock Gesture**: Double-tap the floating top-right lock badge.

---

## 📲 Official Releases & Downloads

### For Direct Device Installation (Sideloading):
- Download the latest **`Darkr-v2.0.0.apk`** from [**GitHub Releases**](https://github.com/hashmiii14/Darkr/releases/latest).

### For Google Play Store Deployment:
- Download the production **`Darkr-v2.0.0.aab`** Android App Bundle for Google Play Console upload.

---

## 🔒 Security & Privacy Architecture

Darkr is **100% offline**, contains **zero ads**, **zero tracking SDKs**, and requests only the minimum required Android permissions:

| Permission | API Level | Technical Purpose |
| :--- | :--- | :--- |
| `SYSTEM_ALERT_WINDOW` | API 26+ | Required to render floating orb, blackout, privacy curtains, and touch freeze. |
| `FOREGROUND_SERVICE` | API 26+ | Keeps the overlay service active while running in the background. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | API 34+ | Android 14 requirement for screen utility overlays. |
| `POST_NOTIFICATIONS` | API 33+ | Displays the persistent foreground notification required by Android 13+. |
| `BIND_QUICK_SETTINGS_TILE` | API 24+ | Allows native Quick Settings pull-down toggle tile. |
| `VIBRATE` | API 26+ | Provides tactile haptic feedback on action triggers. |
| `RECEIVE_BOOT_COMPLETED` | API 26+ | Automatically restores service on boot if explicitly enabled in preferences. |

---

## 🛠️ Build Instructions

```bash
# Run unit tests
./gradlew test

# Run Android Lint
./gradlew lintVitalRelease

# Build Minified Release APK
./gradlew assembleRelease

# Build Google Play App Bundle
./gradlew bundleRelease
```

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
