# Lumen Gallery 🌟

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Build](https://github.com/Dailhiso7/Lumen-Gallery/actions/workflows/build.yml/badge.svg)](https://github.com/Dailhiso7/Lumen-Gallery/actions/workflows/build.yml)

**Lumen Gallery** is a fast, lightweight, and privacy-first photo and video gallery designed for Android. Built with simplicity and user security at its core, Lumen Gallery operates 100% offline with zero internet permissions, strips sensitive metadata on share, and respects your personal memories without ads or tracking.

---

## 🔒 Privacy & Security

Your photos and videos are deeply personal. Lumen Gallery is engineered from the ground up to guarantee that your media remains completely under your control:

- **100% Offline & Network-Isolated**: The application does **not** request or possess the `android.permission.INTERNET` permission. It is physically impossible for the app to connect to remote servers, upload media, or leak data.
- **Strip Metadata When Sharing**: Share photos safely with messengers and social media apps. Lumen Gallery includes a dedicated **"Share without metadata"** action that strips camera models, device identifiers, and sensitive GPS location tags before export.
- **Zero Ads, Zero Trackers**: No Google Play Services, Firebase, telemetry, analytics SDKs, or third-party tracking scripts.
- **No Cloud, No Accounts**: Everything stays local on your device. No registration, no login, and no subscription fees.
- **Minimal Footprint**: Highly optimized and compact (~3.6 MB APK) with negligible battery and memory usage.

---

## ✨ Features

- 📅 **Dynamic Timeline**:
  - Chronological media grid organized into intuitive date groups (*Today*, *Yesterday*, and dated sections).
  - **Fluid Pinch-to-Zoom**: Effortlessly scale the grid layout between 2 and 5 columns using intuitive multi-touch gestures.
  - Video duration chips and visual favorite badges at a glance.

- 📁 **Albums & Folder Organization**:
  - Automatically detects and organizes media into albums based on your on-device storage folders.
  - Visual album cover previews with real-time media counts.

- 🖼️ **Immersive Fullscreen Viewer**:
  - Clean edge-to-edge dark viewer with smooth swiping between media items.
  - **Intuitive Touch Controls**: Pinch-to-zoom (up to 4x), quick double-tap zoom, pan navigation, and swipe-down to dismiss.
  - Built-in video player powered by AndroidX Media3 for smooth, low-latency playback.

- 🔍 **EXIF Metadata Inspector**:
  - View full photo details: capture timestamp, camera manufacturer and model, aperture, shutter speed, ISO, focal length, image resolution, and embedded GPS location coordinates.

- ⭐ **Favorites**:
  - Quickly bookmark and filter your favorite photos and videos offline with persistent local storage.

- 🗑️ **Trash & File Operations**:
  - Built-in Recycle Bin support: easily restore accidentally removed photos or permanently delete them.
  - Rename files seamlessly with full compatibility for modern Android storage systems.

- 🌐 **Multilingual Experience**:
  - Native translations for 10 languages:
    - English (`en`)
    - Русский (`ru`)
    - Deutsch (`de`)
    - Español (`es`)
    - Français (`fr`)
    - Italiano (`it`)
    - 日本語 (`ja`)
    - 简体中文 (`zh-CN`)
    - 繁體中文 (`zh-TW`)
    - 한국어 (`ko`)
  - Instant in-app language switching without having to change your system-wide language settings.

---

## 📱 System Requirements

- **Operating System**: Android 8.0 (Oreo, API level 26) or higher (fully compatible with Android 15+)
- **Permissions**: Requires only local storage access to display your photos and videos (`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` on Android 13+, or `READ_EXTERNAL_STORAGE` on older versions). **No internet permission is requested.**

---

## 📥 Installation

Grab the latest compiled APK from the [Releases](https://github.com/Dailhiso7/Lumen-Gallery/releases) page on GitHub and install it directly on your Android device.

---

## 🛠 Building from Source

For developers and power users wishing to build the project locally:

### Prerequisites
- JDK 17 or higher
- Android SDK (Platform 35 / 36)

### Build Commands
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

The compiled APK will be located at `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License

Lumen Gallery is free and open-source software licensed under the **GNU General Public License v3.0** (GPL-3.0-or-later). See the [LICENSE](LICENSE) file for more information.
