<div align="center">

# 🎵 AudioFlow

**A lightweight, modern, audio-only downloader for Android built with Jetpack Compose & yt-dlp.**

[![Latest Release](https://img.shields.io/github/v/release/838notfound/audioflow?style=for-the-badge&color=blue&logo=github)](https://github.com/838notfound/audioflow/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-GPL--3.0-orange?style=for-the-badge)](LICENSE)

<br />

[Features](#-key-features) • [Installation](#-installation) • [Architecture Guide](#-architecture-guide) • [Troubleshooting](#-troubleshooting--updating-yt-dlp) • [Tech Stack](#-tech-stack)

<hr/>

</div>

## 🌟 Overview

**AudioFlow** is a sleek, distraction-free Android application focused purely on downloading high-quality audio streams. Powered under the hood by [yt-dlp](https://github.com/yt-dlp/yt-dlp) via [youtubedl-android](https://github.com/junkfood02/youtubedl-android), AudioFlow gives you reliable access to your favorite music, podcasts, and sound clips with full support for audio encoding in MP3, M4A, OPUS, FLAC and WAV.

---

## ✨ Key Features

- **Audio-First Architecture:** Tailored specifically for music & audio.
- **High-Fidelity Opus Support:** Bundled with native FFmpeg extraction for crystal-clear audio quality via Opus or FLAC.
- **Built with Jetpack Compose:** Built 100% with Jetpack Compose for smooth animations, fluid layouts, and system dark theme support.
- **In-App Binary Updater:** Update core `yt-dlp` extractor binaries directly from the app settings without needing to reinstall the APK.
- **Privacy & Offline First:** Zero tracking, no telemetry, and direct local downloads straight to your device storage.

---

## 📲 Installation

1. Navigate to the [**Latest Releases**](https://github.com/838notfound/audioflow/releases/latest) tab.
2. Choose the build that matches your device architecture.
3. Download and open the `.apk` file on your Android device.
4. If prompted, grant permission to allow installation from your browser/file manager.

---

## 🛠️ Troubleshooting & Updating yt-dlp

YouTube frequently update their APIs and extractor signatures, which may occasionally cause downloads to stall or fail with extraction errors.

> ### ⚠️ **If downloads keep failing:**
> 1. Go to the **Settings** tab (gear icon).
> 2. Tap **"Update yt-dlp Binaries"** in the yt-dlp Updater section.
> 3. Wait for the latest extraction core to download and apply.
> 4. Retry your download.

Updating the binaries regularly ensures your app always has the newest stream decoders without requiring a full app update.

---

## 🧰 Tech Stack & Credits

- **Language & Framework:** [Kotlin](https://kotlinlang.org/) + [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Extraction Core:** [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- **Android Integration:** [youtubedl-android](https://github.com/junkfood02/youtubedl-android)
- **Audio Processing:** [FFmpeg](https://ffmpeg.org/) (Bundled for native Opus, FLAC, WAV & transcode support)

---

## 📄 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
