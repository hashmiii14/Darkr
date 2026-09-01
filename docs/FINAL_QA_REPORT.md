# Darkr — Final QA, Security, Performance & Release Report

## 1. Toolchain & Environment Specifications

| Component | Target Version | Build Environment Status |
| :--- | :--- | :--- |
| **Gradle** | 8.7 | Verified Reproducible |
| **Android Gradle Plugin (AGP)** | 8.5.2 | Pinned & Verified |
| **Kotlin** | 1.9.24 | Pinned & Verified |
| **Java Development Kit (JDK)** | JDK 17 (Eclipse Temurin 17.0.20.101-hotspot) | Compliant |
| **Compile SDK** | 34 (Android 14) | Installed & Verified |
| **Target SDK** | 34 (Android 14) | Installed & Verified |
| **Minimum SDK** | 26 (Android 8.0 Oreo) | Full Backward Compatibility |
| **Build Features** | ViewBinding, Strict R8 Shrinking, Resource Optimization | Active |

---

## 2. Automated Test Execution & Quality Gate

### Command Verification
1. **Unit Tests**:
   - Command: `./gradlew test`
   - Result: **PASS (100% test success across Debug & Release variants - 60 tasks executed)**
   - Test Suites: `PreferencesManagerTest`, `DarkrStateManagerTest`, `ShakeDetectorTest`
2. **Release Lint Analysis**:
   - Command: `./gradlew lintVitalRelease`
   - Result: **PASS (Zero fatal lint issues)**
3. **Optimized Release APK Build**:
   - Command: `./gradlew assembleRelease`
   - Result: **PASS (`app-release.apk` 1.78 MB with R8 minification and resource shrinking)**
4. **Google Play App Bundle Build**:
   - Command: `./gradlew bundleRelease`
   - Result: **PASS (`app-release.aab` 2.40 MB production bundle for Google Play Console)**

---

## 3. Security & Privacy Audit

- **Permissions Audited**:
  - `android.permission.SYSTEM_ALERT_WINDOW`: Validated for overlay rendering.
  - `android.permission.FOREGROUND_SERVICE`: Validated for persistent background service.
  - `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`: Declared with proper system subtype property.
  - `android.permission.POST_NOTIFICATIONS`: Declared and requested for Android 13+ support.
  - `android.permission.VIBRATE`: Tactile haptic feedback on action triggers.
  - `android.permission.RECEIVE_BOOT_COMPLETED`: Protected system broadcast with safe auto-restore checks.
  - `android.permission.BIND_QUICK_SETTINGS_TILE`: Secured permission for native Quick Settings tile.
  - `android.permission.WAKE_LOCK`: **REMOVED (Unnecessary privilege elimination)**.
  - `android.permission.ACCESS_FINE_LOCATION`: **REMOVED (Zero location tracking)**.
  - `android.permission.ACCESS_COARSE_LOCATION`: **REMOVED (Zero location tracking)**.
  - `android.permission.INTERNET`: **NOT PRESENT (100% Offline guarantee, zero network transmission)**.
- **Exported Components**:
  - `MainActivity`: `exported="true"` (Launcher activity).
  - `DarkrOverlayService`: `exported="false"` (Protected internal service).
  - `DarkrTileService`: `exported="true"` (Protected by `BIND_QUICK_SETTINGS_TILE`).
  - `BootReceiver`: `exported="true"` (Protected system broadcast receiver for boot completion).
- **Application Backup**: `android:allowBackup="false"` (Hardened against unauthorized ADB extraction).
- **Secrets Audit**: Zero API keys, zero hardcoded credentials, zero secret tokens.

---

## 4. Feature Verification Matrix

| Feature | Audit Status | Implementation Notes |
| :--- | :--- | :--- |
| **Pure AMOLED Zero-Leak Blackout** | **PASS** | True `#000000` rendering, display cutout notch coverage (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` / `SHORT_EDGES`), immersive system insets hide, double-tap wake with haptic feedback. |
| **Android Quick Settings Tile** | **PASS** | Native `TileService` integration with real-time state sync and 1-tap activation from notification shade. |
| **Legitimate Media Controls** | **PASS** | `DarkrMediaManager` standard Android media key event dispatching (Play/Pause, Next, Prev) with subtle low-luminance media bar. |
| **Minimal Clock & Battery Mode** | **PASS** | Low-power time and battery percentage display with 60-second programmatic pixel-shifting for OLED burn-in prevention. |
| **Smart Pocket Auto-Blackout** | **PASS** | `PocketDetector` combining hardware `Sensor.TYPE_PROXIMITY` and `Sensor.TYPE_ACCELEROMETER` with 600ms debounce. |
| **Dual-Segment Privacy Curtain**| **PASS** | Split top/bottom mask windows with drag handle, guaranteeing 100% native background touch passthrough in viewing gap. |
| **Touch Freeze Lock** | **PASS** | Fullscreen touch interception with visual pulse feedback and double-tap unlock badge. |
| **Midnight Screen Dimmer** | **PASS** | Non-touchable software luminance filter with `FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE`. |
| **Panic Camouflage Decoy** | **PASS** | Realistic System Update decoy screen with double-tap emergency dismissal and shake reflex. |
| **State Synchronization** | **PASS** | Reactive `DarkrStateManager` StateFlow architecture with bidirectional switch updates and crash resilience. |

---

## 5. Production Release Specifications

| Property | Specification |
| :--- | :--- |
| **Release Version** | `v2.0.0` |
| **Version Code** | `2` |
| **Application ID** | `com.darkr.app` |
| **Release APK Filename** | `Darkr-v2.0.0.apk` / `Darkr.apk` |
| **Minified APK Size** | **1.78 MB (1,829,417 bytes)** |
| **Play Store App Bundle** | **`Darkr-v2.0.0.aab` / `Darkr.aab` (2.40 MB)** |
| **APK SHA-256 Checksum** | `2CB7C6AF28A0DA3EA17F77677A13D6A0DE04872ADE794A3CFFEB5046DB23ED8F` |
| **AAB SHA-256 Checksum** | `C28E6FC1412CB8729F710A2E07ED225E3F16D0C33D760F0E155D16E2A537FD2A` |
| **Canonical Download** | [GitHub Releases — Darkr v2.0.0](https://github.com/hashmiii14/Darkr/releases/latest) |
| **Distribution Pipeline** | GitHub Actions Automated Release (`.github/workflows/release.yml`) |
