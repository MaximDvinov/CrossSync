# CrossSync

[![Status](https://img.shields.io/badge/status-WIP-orange)](https://github.com/MaximDvinov/CrossSync)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose](https://img.shields.io/badge/compose-1.10.0-blue.svg?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform)
![macOS](https://img.shields.io/badge/platform-macOS-blue)
![Android](https://img.shields.io/badge/platform-android-green)

CrossSync is a Kotlin Multiplatform project for clipboard synchronization between macOS and Android devices over a local network, without cloud services.

## Current Features
- macOS desktop app (Compose Desktop + system tray + quick-access window).
- Android client (Compose) with QR-based pairing to Mac.
- Local clipboard history with categories.
- Support for text, formatted text (HTML/RTF), images, and files in local clipboard history.
- LAN synchronization with encryption (text and formatted text).
- Settings: desktop auto-start, history auto-cleanup, quick history size, excluded applications.

## Tech Stack
- Kotlin Multiplatform, Compose Multiplatform
- Ktor (server/client, WebSocket)
- Koin
- Room + SQLite
- Gradle Wrapper 9.1.0

## Module Structure
- `desktopApp` - macOS desktop client.
- `androidApp` - Android client.
- `landing` - Kobweb landing page (GitHub Pages ready).
- `shared` - shared business logic and feature/data/domain modules.
- `docs` - screenshots and supporting materials.

## Project Checklist
- [x] macOS desktop app
- [x] Android client (MVP)
- [x] QR pairing Android -> Mac
- [x] Local clipboard history and categories
- [x] Text/formatted-text synchronization over LAN
- [x] macOS quick-access window hotkey (`Meta + Shift + V`)
- [x] Basic sync and history cleanup settings
- [ ] Image and file synchronization across devices
- [ ] Windows/Linux desktop support
- [ ] Public release artifacts (.dmg/.apk) and release docs

## Build Instructions

### Requirements
- JDK 17
- Android SDK installed (`local.properties` with `sdk.dir=...`)
- macOS for running the desktop target

### Build and Run for macOS
1. Run desktop app:
   ```bash
   ./gradlew :desktopApp:run
   ```
2. Run in detached mode (Gradle process exits immediately):
   ```bash
   ./gradlew :desktopApp:runDetached
   ```
3. Build DMG:
   ```bash
   ./gradlew :desktopApp:packageDmg
   ```
   Output: `desktopApp/build/compose/binaries/main/dmg/`.

### Build for Android
1. Debug APK:
   ```bash
   ./gradlew :androidApp:assembleDebug
   ```
2. Install debug build to a connected device:
   ```bash
   ./gradlew :androidApp:installDebug
   ```
3. Release APK:
   ```bash
   ./gradlew :androidApp:assembleRelease
   ```
4. Release AAB:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```
   Artifacts: `androidApp/build/outputs/`.

## Screenshot
![CrossSync macOS window](docs/mac_clipboard.png)

## Status
The project is actively developed and currently in WIP stage.

## Author
Developed by [Maxim](https://github.com/MaximDvinov)  
License: MIT
