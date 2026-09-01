# 🌑 Darkr — Precision Screen Suite & AMOLED Utility

[![Build & Test CI](https://github.com/hashmiii14/Darkr/actions/workflows/ci.yml/badge.svg)](https://github.com/hashmiii14/Darkr/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/hashmiii14/Darkr?color=white&label=Release&logo=github)](https://github.com/hashmiii14/Darkr/releases)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-white.svg)](https://android.com)
[![Design](https://img.shields.io/badge/Design-Strict_Monochrome_B%26W-white.svg)]()
[![License](https://img.shields.io/badge/License-Apache_2.0-white.svg)](LICENSE)

> **Darkr** is a high-performance, strictly monochrome screen utility suite engineered for modern OLED/AMOLED Android displays. It provides true-black screen blackouts, dual-segment privacy curtains with native touch passthrough, touch freezing, sub-minimum screen dimming, emergency camouflage, and a floating quick-action HUD.

---

## ⚡ Core Features

### 1. 🌑 Pure AMOLED Blackout
- Emits pure `#000000` pixels across the entire display with display cutout/notch coverage (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`).
- Reduces active pixel power consumption on OLED/AMOLED panels while maintaining background audio streams (podcasts, videos, music).
- **Wake Gesture**: Double-tap anywhere to wake.

### 2. 🛡️ Dual-Segment Privacy Curtain
- Splits the screen into draggable top and bottom dark masks with a customizable viewing slit.
- **100% Background Touch Passthrough**: Unlike simple transparent overlays, Darkr uses split window bounds so that apps underneath (WhatsApp, Telegram, browsers) receive full, unobstructed touch events.
- **Controls**: Drag the header handle up/down to reposition or tap the close icon to dismiss.

### 3. 🔒 Touch Freeze Lock
- Intercepts and consumes all touch inputs across the display while keeping underlying content (videos, navigation maps, documents) completely visible.
- Ideal for child lock and pocket protection.
- **Unlock Gesture**: Double-tap the floating top-right lock badge.

### 4. 🌙 Midnight Screen Dimmer
- Non-interactive translucent software luminance overlay (`FLAG_NOT_TOUCHABLE`) that drops screen brightness lower than hardware minimums for eye comfort in pitch-dark environments.

### 5. 🚨 Panic Camouflage Decoy
- Instantly masks the screen with a realistic system decoy ("Android System Updating") or instant blackout when triggered via the quick action HUD or physical device shake.
- **Unlock Gesture**: Double-tap anywhere to dismiss decoy.

### 6. 🔮 Floating Action Orb & Quick Action HUD
- Compact, translucent 52dp draggable bubble with tactile edge snapping and display boundary clamping.
- Expands into a vertical/horizontal monochrome quick action HUD for one-tap mode switching.

---

## 📲 Official Release & Installation

The canonical, virus-free APK distribution is published directly via **GitHub Releases**.

### How to Download & Install
1. Navigate to [**Darkr GitHub Releases**](https://github.com/hashmiii14/Darkr/releases/latest).
2. Download the latest `Darkr-v*.apk` asset.
3. Verify the SHA-256 checksum against the published `.sha256` file.
4. Open the APK on your Android device and grant `Display over other apps` permission when prompted.

---

## 🔒 Permissions & Privacy Model

Darkr is **100% offline**, contains **zero ads**, **zero tracking SDKs**, and requests only the minimum required Android permissions:

| Permission | API Level | Technical Purpose |
| :--- | :--- | :--- |
| `SYSTEM_ALERT_WINDOW` | API 26+ | Required to render floating orb, blackout, privacy curtains, and touch freeze. |
| `FOREGROUND_SERVICE` | API 26+ | Keeps the overlay service active while running in the background. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | API 34+ | Android 14 requirement for screen utility overlays. |
| `POST_NOTIFICATIONS` | API 33+ | Displays the persistent foreground notification required by Android 13+. |
| `VIBRATE` | API 26+ | Provides tactile haptic feedback on action triggers. |
| `RECEIVE_BOOT_COMPLETED` | API 26+ | Automatically restores service on boot if explicitly enabled in preferences. |

---

## 🏗️ Architecture & State Management

- **`DarkrStateManager`**: Single source of truth using Kotlin `StateFlow` to synchronize state bidirectionally across `MainActivity`, `DarkrOverlayService`, and active overlays.
- **`OverlayManager`**: Hardened WindowManager controller that ensures idempotency, catches token exceptions gracefully, and prevents window leaks.
- **`ShakeDetector`**: Debounced accelerometer listener with startup gravity bias filtration.

---

## 🛠️ Build & Test Instructions

### Prerequisites
- JDK 17 (Eclipse Temurin or OpenJDK 17)
- Android SDK Platform 34 & Build-Tools 34.0.0

### Gradle Tasks
```bash
# Run unit tests
./gradlew test

# Run Android Lint
./gradlew lintRelease

# Build Debug APK
./gradlew assembleDebug

# Build Minified Release APK
./gradlew assembleRelease
```

---

## 📋 Compatibility Matrix

- **Minimum SDK**: Android 8.0 Oreo (API 26)
- **Target SDK**: Android 14 (API 34)
- **Tested Platforms**: Android 8.0, Android 10, Android 12, Android 13 (Tiramisu), Android 14 (UpsideDownCake), Android 15.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
