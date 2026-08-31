# 🌑 Darkr — Precision OLED Screen & Privacy Suite

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20OLED-black?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-black?style=for-the-badge&logo=kotlin)
![Architecture](https://img.shields.io/badge/Architecture-Service%20Overlay-black?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-white?style=for-the-badge)

**Next-Generation Hardware-Level AMOLED Blackout, Metro Privacy Curtain, and Anti-Peeping Utility for Android.**  
*Engineered for zero battery consumption during background streaming, extreme subway privacy, and pocket touch defense.*

[Live Simulator](docs/index.html) • [Key Features](#-core-protection-engines) • [Architecture](#-system-architecture) • [Play Store Strategy](#-monetization--growth-engine)

</div>

---

## ⚡ Overview

**Darkr** is a high-performance, minimalist Android utility app designed for the **10 Million+ downloads** global market. Inspired by the raw utility of apps like *Black Screen*, Darkr brings Apple-grade industrial design, hardware-level OLED power savings, and precision privacy curtains into a single, seamless floating overlay.

---

## 🛡️ Core Protection Engines

| Feature | Description |
| :--- | :--- |
| **⬛ AMOLED Blackout** | Turns all OLED subpixels completely off (`#000000`) for 0% battery consumption while playing YouTube videos, podcasts, or music in the background. Includes procedural ambient sleep generators (*Soft Rain* & *432Hz Binaural Delta Waves*). |
| **🛡️ Metro Privacy Shield** | A high-contrast, draggable laser slit curtain. Perfect for public commutes, crowded trains, and metros — hides confidential WhatsApp chats and banking OTPs from shoulder-peeking commuters. |
| **🔦 Night Screen Torch** | Instant full-screen white illuminator providing soft, glare-free ambient light without triggering harsh camera LED flashes. |
| **🖐️ Touch Freeze (Pocket Lock)** | Complete touch interceptor layer. Prevents accidental pauses, pocket dialing, and toddler screen taps while listening to media. |
| **🚨 Panic Decoy Camouflage** | Instant disguise reflex. One-tap triggers an authentic animated *"Android System Updating (42%)"* screen when privacy needs immediate camouflage. |

---

## 🔘 The 36px Translucent Glass Floating Orb

- **Subtle & Non-Intrusive:** 36px semi-transparent frosted acrylic disc (`blur: 20px`) with a minimalist Black & White Eclipse monogram.
- **Vertical HUD Architecture:** Tapping the orb opens an ergonomic vertical tool capsule alongside the screen edge.
- **Multi-App Persistence:** Stays persistently floating over YouTube, WhatsApp, Spotify, and the Home Launcher.
- **Apple Spring Physics:** Smooth magnetic edge-snapping to screen borders with zero lag.

---

## 🏗️ System Architecture

- **Foreground Service with SpecialUse:** Complies with Android 14 (API 34) overlay guidelines.
- **WindowManager System Overlays:** `TYPE_APPLICATION_OVERLAY` with `FLAG_NOT_TOUCH_MODAL` ensuring typing and underlying app interaction are never blocked.
- **Hardware Accelerometer Shake Reflex:** High-frequency event filtering for instant disguise trigger.

---

## 📂 Project Structure

```
darkr/
├── app/
│   ├── src/main/
│   │   ├── java/com/darkr/app/
│   │   │   ├── MainActivity.kt               # Master dashboard & permission management
│   │   │   ├── DarkrApplication.kt          # Global lifecycle & notification channels
│   │   │   ├── service/
│   │   │   │   └── DarkrOverlayService.kt    # Persistent WindowManager overlay engine
│   │   │   ├── overlay/
│   │   │   │   ├── FloatingPillView.kt       # 36px Translucent floating orb & vertical HUD
│   │   │   │   ├── BlackoutView.kt           # Pure OLED black screen overlay
│   │   │   │   ├── PrivacyCurtainView.kt     # Draggable privacy slit mask
│   │   │   │   ├── TouchFreezeView.kt        # Accidental touch blocker
│   │   │   │   ├── MidnightDimmerView.kt     # Ultra-low brightness filter
│   │   │   │   └── CamouflageView.kt         # Fake update decoy screen
│   │   │   ├── sensor/
│   │   │   │   └── ShakeDetector.kt          # Accelerometer shake reflex listener
│   │   │   └── util/
│   │   │       └── PreferencesManager.kt     # Shared preferences persistence
│   │   └── res/                              # Monochrome vector drawables, layouts & themes
├── docs/
│   └── index.html                            # Interactive Live Simulator (GitHub Pages ready)
├── build.gradle.kts                          # Root Gradle build configuration
└── README.md
```

---

## 🚀 Building & Local Testing

### Prerequisites
- Android Studio Hedgehog (2023.1.1+) or newer
- JDK 17+
- Android SDK 34 (Android 14)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/hashmiii14/Darkr.git
   cd Darkr
   ```
2. Open the project in Android Studio.
3. Connect an Android device with Developer Options & USB Debugging enabled.
4. Run `app` on your target device:
   - Grant the *"Display over other apps"* permission when prompted.
   - Tap **Enable Screen Overlay** to launch the 36px floating orb!

---

## 💰 Monetization & Growth Engine

Darkr is designed with a **$0 server-cost, hyper-scalable monetization funnel**:
1. **AdMob Smart Banner:** Non-intrusive bottom banner inside the main dashboard.
2. **Interstitial Ad on Blackout Exit:** Triggered once every 3-4 blackout wake sessions with a 120s cooldown.
3. **Darkr Pro (In-App Purchase):** One-time lifetime unlock to remove ads, unlock custom ambient soundscapes, and activate custom panic camouflage themes.

---

## 📄 License

This project is licensed under the MIT License.
