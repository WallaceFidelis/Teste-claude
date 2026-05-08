# iOS Setup Guide

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| macOS | Ventura 13.5 or newer |
| Xcode | 15.0 or newer |
| Command Line Tools | matching Xcode version |
| JDK | 17 (for Gradle; install via [sdkman.io](https://sdkman.io) or Homebrew) |

A Mac is required for all iOS builds. An Apple Silicon Mac (M1+) is strongly recommended — llama.cpp compiles and runs much faster with native Metal support.

## 1. Clone and bootstrap

```bash
git clone --recurse-submodules <repo-url> NFScan
cd NFScan
chmod +x setup.sh && ./setup.sh
```

`setup.sh` downloads `gradle-wrapper.jar` and initialises `third_party/llama.cpp`.

## 2. Build the llama.cpp XCFramework

```bash
chmod +x scripts/build_llama_ios.sh
./scripts/build_llama_ios.sh
```

This compiles two static library slices (device arm64 + simulator arm64/x86_64), merges them, and packages the result into `third_party/llama-ios.xcframework`. The first build takes 5–15 minutes depending on your hardware.

Expected output:

```
third_party/llama-ios.xcframework/
  ios-arm64/
    Headers/  llama.h  ggml.h  ...
    libllama.a
  ios-arm64_x86_64-simulator/
    Headers/  ...
    libllama.a
  Info.plist
```

## 3. Download the model

Follow the instructions in [models/README.md](../models/README.md) to obtain `SmolLM2-135M-Instruct-Q4_K_M.gguf`.

## 4. Add the model to the Xcode project

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. In the Project Navigator, right-click the `iosApp` group → **Add Files to "iosApp"**.
3. Select `SmolLM2-135M-Instruct-Q4_K_M.gguf`.
4. In the dialog, check **Copy items if needed** and make sure the target `iosApp` is selected under "Add to targets".
5. Click **Add**.

The file will be bundled into the app and accessed at runtime via `NSBundle.mainBundle.pathForResource(name:ofType:)`.

> **Simulator note:** Simulators run on x86_64 or arm64 (Apple Silicon) depending on your Mac. The model runs on simulator but performance is slower than a real device.

## 5. Build and run

1. In Xcode, select the `iosApp` scheme and choose a simulator or connected device.
2. Press **▶ (Cmd+R)**.

On first build, Xcode invokes Gradle to compile the Kotlin Compose framework (`embedAndSignAppleFrameworkForXcode` task) — this takes several minutes. Subsequent builds are incremental.

## Troubleshooting

**"gradlew not found" in Xcode build log**
: The Xcode shell script phase runs `./gradlew` from the repo root. Make sure you ran `./setup.sh` and the file is executable:
```bash
chmod +x gradlew
```

**`llama-ios.xcframework` not found at link time**
: Re-run `scripts/build_llama_ios.sh`. Verify that `third_party/llama-ios.xcframework/Info.plist` exists.

**`llama.cpp` submodule directory is empty**
: Run `git submodule update --init --depth 1 third_party/llama.cpp`.

**Build error: `module 'ComposeApp' not found`**
: Clean the build folder (Cmd+Shift+K) and rebuild. If it persists, run the Gradle task manually:
```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode \
    CONFIGURATION=Debug SDK_NAME=iphonesimulator
```

**App crashes immediately on launch**
: Check that `startKoin` is called in `MainViewController.kt` before `App()` is composed. Verify that the model file was added to the Xcode target (step 4 above) and its size is ~90 MB in the app bundle.

**Slow inference on simulator**
: Expected — Metal GPU acceleration is unavailable in the simulator. Use a real device for performance testing.
