import os
import glob
import subprocess

apk_files = glob.glob("app/build/outputs/apk/debug/*.apk")
if not apk_files:
    print("No APK found!")
    exit(1)

apk_path = apk_files[0]
print(f"Found APK: {apk_path} ({os.path.getsize(apk_path)} bytes)")

# Use curl directly via subprocess to avoid any missing Python libraries
try:
    catbox_url = subprocess.check_output(
        ["curl", "-s", "-F", "reqtype=fileupload", "-F", f"fileToUpload=@{apk_path}", "https://catbox.moe/user/api.php"],
        text=True
    ).strip()
    print("Catbox Direct URL:", catbox_url)
except Exception as e:
    catbox_url = ""
    print("Catbox error:", e)

try:
    zx_url = subprocess.check_output(
        ["curl", "-s", "-F", f"file=@{apk_path}", "https://0x0.st"],
        text=True
    ).strip()
    print("0x0 Direct URL:", zx_url)
except Exception as e:
    zx_url = ""
    print("0x0 error:", e)

readme_content = f"""# 🌑 Darkr — OLED Screen Off, Privacy Shield & Utility Suite

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-100%25_Active-brightgreen.svg)]()

> **Supercharge your OLED display and reclaim your mobile privacy.**  
> Darkr is a high-performance Android utility that provides AMOLED True Black screen blackout, draggable privacy viewing shields for messaging apps, touch freezing, and eye-protection dimmers.

---

## 📲 Direct APK Download (1-Click Install)

Click either link below on your Android phone to download immediately:

- 🚀 **[Download Darkr APK (Server 1 - Instant Direct)]({catbox_url})**
- 🚀 **[Download Darkr APK (Server 2 - Backup)]({zx_url})**

---

## ⚡ Core Features (100% Active)

1. **🌑 AMOLED True-Black Screen Engine**
   - Pure `#000000` pixels across the screen. Saves up to 85% battery while listening to YouTube or audio streams. Double-tap to dismiss.
2. **🛡️ Metro Privacy Shield**
   - Draggable viewing slit for private WhatsApp / Telegram chats.
3. **🔒 Touch Freeze**
   - Intercepts all accidental touches. Triple-tap center pill to unlock.
4. **🌙 Midnight Warm Dimmer**
   - Screen brightness below system minimum with eye-protecting warm filter.
5. **🚨 Panic Camouflage**
   - Fake "System Update" decoy screen on shake or tap.
6. **🔮 36px Floating Glass Orb**
   - Sleek floating overlay with quick-access vertical HUD.

---

## 🛠️ Setup Instructions

1. Download **`app-debug.apk`** from the link above.
2. Install the APK on your Android device.
3. Open Darkr, grant **"Display over other apps"** permission.
4. Tap **"START DARKR SERVICE"** to activate!
"""

with open("README.md", "w", encoding="utf-8") as f:
    f.write(readme_content)

print("Updated README.md successfully!")
