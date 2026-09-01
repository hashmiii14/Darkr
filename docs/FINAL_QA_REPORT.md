# Darkr — Final QA, Security & Release Report

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
   - Result: **PASS (100% test success across Debug & Release variants)**
   - Test Suites: `PreferencesManagerTest`, `DarkrStateManagerTest`, `ShakeDetectorTest`
2. **Release Lint Analysis**:
   - Command: `./gradlew lintVitalRelease`
   - Result: **PASS (Zero fatal lint issues)**
3. **Debug Artifact Build**:
   - Command: `./gradlew assembleDebug`
   - Result: **PASS (`app-debug.apk` built)**
4. **Optimized Release Artifact Build**:
   - Command: `./gradlew assembleRelease`
   - Result: **PASS (`app-release.apk` with R8 minification and resource shrinking)**

---

## 3. Security & Privacy Audit

- **Permissions Audited**:
  - `android.permission.SYSTEM_ALERT_WINDOW`: Validated for overlay rendering.
  - `android.permission.FOREGROUND_SERVICE`: Validated for persistent background service.
  - `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`: Declared with proper system subtype property.
  - `android.permission.POST_NOTIFICATIONS`: Declared and requested for Android 13+ support.
  - `android.permission.VIBRATE`: Tactile haptic feedback on action triggers.
  - `android.permission.RECEIVE_BOOT_COMPLETED`: Protected system broadcast with safe auto-restore checks.
  - `android.permission.WAKE_LOCK`: **REMOVED (Unnecessary privilege elimination)**.
  - `android.permission.INTERNET`: **NOT PRESENT (100% Offline guarantee, zero network transmission)**.
- **Exported Components**:
  - `MainActivity`: `exported="true"` (Launcher activity).
  - `DarkrOverlayService`: `exported="false"` (Protected internal service).
  - `BootReceiver`: `exported="true"` (Protected system broadcast receiver for boot completion).
- **Application Backup**: `android:allowBackup="false"` (Hardened against unauthorized ADB extraction).
- **Secrets Audit**: Zero API keys, zero hardcoded credentials, zero secret tokens.

---

## 4. Feature Verification Matrix

| Feature | Audit Status | Implementation Notes |
| :--- | :--- | :--- |
| **Floating Action Orb & HUD** | **PASS** | Touch slop calculation (`ViewConfiguration.scaledTouchSlop`), safe boundary clamping, smooth spring snapping, Outside Touch dismiss. |
| **Pure AMOLED Blackout** | **PASS** | True `#000000` rendering, display cutout notch coverage (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`), double-tap wake with auto-fading prompt. |
| **Dual-Segment Privacy Curtain**| **PASS** | Split top/bottom mask windows with drag handle, guaranteeing 100% native background touch passthrough in viewing gap. |
| **Touch Freeze Lock** | **PASS** | Fullscreen touch interception with visual pulse feedback and double-tap unlock badge. |
| **Midnight Screen Dimmer** | **PASS** | Non-touchable software luminance filter with `FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE`. |
| **Panic Camouflage Decoy** | **PASS** | Realistic System Update decoy screen with double-tap emergency dismissal and shake reflex. |
| **Shake Detector Sensor** | **PASS** | Sliding window acceleration filter, startup gravity bias filtration, and strict 1500ms cooldown debounce. |
| **State Synchronization** | **PASS** | Reactive `DarkrStateManager` StateFlow architecture with bidirectional switch updates and crash resilience. |

---

## 5. Production Release Specifications

| Property | Specification |
| :--- | :--- |
| **Release Version** | `v1.0.0` |
| **Version Code** | `1` |
| **Application ID** | `com.darkr.app` |
| **Release APK Filename** | `Darkr-v1.0.0.apk` |
| **Minified APK Size** | **1.77 MB (1,813,790 bytes)** |
| **Debug APK Size** | **5.77 MB** |
| **SHA-256 Checksum** | `3F9013C43076D269BE05ED28F428A07C687EBF4E3EF6F4C5FA9B14572B0CCA89` |
| **Canonical Download** | [GitHub Releases — Darkr v1.0.0](https://github.com/hashmiii14/Darkr/releases/latest) |
| **Distribution Pipeline** | GitHub Actions Automated Release (`.github/workflows/release.yml`) |
