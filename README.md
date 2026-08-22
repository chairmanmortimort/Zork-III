# Zork III for Light Phone III

A port of the classic interactive fiction game **Zork III: The Dungeon Master** to the
Light Phone III SDK. Runs entirely offline — the Zork story file is embedded in the APK,
so no internet is needed.

## Download APK

`zork3-release.apk` in the root of this repo is a signed release build, arm64-v8a only,
~6.3MB. Install it on a Light Phone III:

```bash
adb install -r zork3-release.apk
```

## Build from source

Requires the Light Phone III SDK and Android SDK (OpenJDK 21).

```bash
# Debug
JAVA_HOME=/tmp/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ANDROID_SDK_ROOT=~/Library/Android/sdk \
  ./gradlew assembleDebug

# Release (arm64 only, resource-shrunk, ML Kit barcode stripped)
JAVA_HOME=/tmp/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
  ANDROID_SDK_ROOT=~/Library/Android/sdk \
  ./gradlew assembleRelease
```

This module is built from the `light-sdk-fresh` parent project, which provides the `sdk`
dependency and shared Gradle configuration.

## Architecture

```
zork3/
├── build.gradle.kts      # Android app module — depends on :sdk:client
├── lighttool.toml        # SDK tool manifest (appId, label, permissions)
├── proguard-rules.zork.pro  # R8 keep rules for Zork III engine & UI
├── consumer-rules.pro    # R8 keep rules for SDK-generated classes
├── zork3-release.apk     # Prebuilt signed release APK
└── src/main/
    ├── assets/story/zork3.z3   # Embedded Z-machine story file (from historicalsource)
    ├── kotlin/com/thelightphone/zork3/
    │   ├── ZorkHomeScreen.kt   # Main game UI + About page
    │   ├── ZorkViewModel.kt    # State: transcript, status line, engine wrapper
    │   ├── ZorkSavesScreen.kt  # Save-slot browser (load/delete)
    │   └── engine/             # Vendored Z-machine v3 interpreter (Kotlin)
    │       ├── ZStory.kt
    │       ├── ZMachine.kt
    │       ├── ZOpcodes.kt
    │       ├── ZObjects.kt
    │       ├── ZText.kt
    │       └── ZOutputTypes.kt
    └── AndroidManifest.xml
```

## APK size optimization

The full SDK pulls in ML Kit barcode scanning (libbarhopper_v3.so, ~20MB across 4 ABIs,
plus 3 .tflite models) and CameraX — all useless for a text adventure. This build strips
those dependencies and targets arm64-v8a only, reducing the release APK from ~67MB to
~6.3MB.

## Project status

- [x] Z-machine v3 engine embedded in-package
- [x] Zork III story file embedded (88 KB)
- [x] Main screen with scrollable transcript + command input
- [x] About page (title card, copyright, Microsoft MIT release note, SDK credit)
- [x] Three-slot save game system
- [x] Release APK built and included (`zork3-release.apk`, 6.3MB arm64)

## Credits & licensing

- **Zork III story & source:** Originally by Infocom (1982). In 2025, Microsoft released
  the Zork III source code and Z-Machine specification under the MIT License.
- **Interpreter:** Vendored Kotlin Z-machine v3 interpreter.
- **Light Phone III SDK:** com.thelightphone.sdk (thelightphone.com).

This is an independent, non-commercial fan project. Zork and Infocom are trademarks
of their respective owners.
