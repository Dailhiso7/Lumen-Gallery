# Lumen Gallery 🌟

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![F-Droid](https://img.shields.io/badge/F--Droid-Ready-brightgreen.svg)](https://f-droid.org)
[![Build](https://github.com/Dailhiso7/Lumen-Gallery/actions/workflows/build.yml/badge.svg)](https://github.com/Dailhiso7/Lumen-Gallery/actions/workflows/build.yml)

**Lumen Gallery** is an ultra-lightweight, privacy-first, fully offline photo and video gallery application built from the ground up to adhere to strict F-Droid inclusion standards and Scoped Storage requirements on modern Android devices (Android 8.0 to Android 15+).

---

## 🔒 Privacy & F-Droid Standards

- **100% Offline / Network Isolated**: `android.permission.INTERNET` is **strictly excluded** from `AndroidManifest.xml`. The app cannot establish network connections under any circumstances.
- **Zero Proprietary Bloat**: Absolutely NO Google Play Services, Firebase, advertising SDKs, tracking libraries, or closed-source binary dependencies.
- **Share Without Metadata**: Strip embedded EXIF tags, GPS location coordinates, camera models, and device serial numbers before sharing media with external apps or messengers.
- **Minimal Footprint**: Aggressive ProGuard/R8 optimizations, stripping unused bytecode and logging in release builds to maintain a lean APK size (~3.6 MB).
- **F-Droid Build Compliant**: Builds cleanly with standard `./gradlew assembleRelease` using open-source toolchains without requiring private keystores.

---

## ✨ Features

- **Chronological Timeline Grid**:
  - Organized by date headers (*Today*, *Yesterday*, formatted date groups).
  - **Dynamic Multi-Touch Pinch-to-Zoom**: Fluidly scale the grid from 2 up to 5 columns with smooth micro-animations.
  - Video duration chips and favorites indicator.
- **Albums & Folders**:
  - Automatically indexes media into albums mapped directly to physical on-device directories.
  - Album covers with item counts and path info.
- **Immersive Fullscreen Viewer**:
  - Smooth `HorizontalPager` with edge-to-edge dark theme.
  - **Touch Gestures**: Pinch-to-zoom (up to 4x), double-tap zoom (1x ⟷ 2.5x), panning, and **swipe-down to dismiss**.
  - Local video player powered by lightweight Media3 ExoPlayer.
- **Comprehensive EXIF Inspector**:
  - Reads camera make, model, ISO, exposure time, aperture (f-number), focal length, resolution, and GPS coordinates via AndroidX `ExifInterface`.
- **Integrated Trash & File Operations**:
  - View and manage deleted media with restore and permanent erase capabilities.
  - MediaStore Scoped Storage file operations (Trash API / RecoverableSecurityException).
  - Rename files seamlessly.
- **Persistent Offline Favorites**:
  - Fast local bookmarking backed by an embedded AndroidX Room SQLite database.
- **Multilingual Support**:
  - 10 high-quality native localizations:
    - English (`en`)
    - Русский (`ru`)
    - Deutsch (`de`)
    - Español (`es`)
    - Français (`fr`)
    - Italiano (`it`)
    - 日本語 (`ja`)
    - 简体中文 (`zh-rCN`)
    - 繁體中文 (`zh-rTW`)
    - 한국어 (`ko`)
  - In-app language switcher with live dynamic UI updates and restart option.

---

## 🏗 Architecture & Tech Stack

- **Language**: Kotlin 2.1.10 (Coroutines + StateFlow)
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture + MVVM
- **Media Engine**: Native Android `MediaStore` ContentResolver + `ContentObserver` for real-time local sync
- **Image Pipeline**: Coil Compose + Coil Video (configured with local memory & disk caching, zero proprietary trackers)
- **Local Persistence**: AndroidX Room (Stores favorites offline)
- **Metadata**: AndroidX `ExifInterface`
- **Video Playback**: AndroidX Media3 ExoPlayer (minimal local core)

---

## 🛠 Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK Platform 35 / 36

### Build Debug APK:
```bash
./gradlew assembleDebug
```

### Build Minified Release APK (F-Droid target):
```bash
./gradlew assembleRelease
```
The output APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## 📦 F-Droid Fastlane Metadata

This repository contains the complete Fastlane directory structure required for F-Droid metadata integration:
```
fastlane/metadata/android/
├── en-US/
│   ├── title.txt
│   ├── short_description.txt
│   ├── full_description.txt
│   └── images/
│       ├── icon.png
│       └── phoneScreenshots/
└── ru-RU/
    ├── title.txt
    ├── short_description.txt
    ├── full_description.txt
    └── images/
        ├── icon.png
        └── phoneScreenshots/
```

An F-Droid build recipe template is also available in [fdroid-recipe/org.lumengallery.app.yml](fdroid-recipe/org.lumengallery.app.yml).

---

## 📄 License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0-or-later). See the [LICENSE](LICENSE) file for the full text.
