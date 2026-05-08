# Android Setup Guide

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Android Studio | Hedgehog (2023.1) or newer |
| JDK | 17 |
| Android SDK | API 34 (build tools 34.0.0) |
| NDK | 26.1.10909125 (install via SDK Manager → SDK Tools → NDK) |
| CMake | 3.22.1 (install via SDK Manager → SDK Tools → CMake) |
| ADB | bundled with Android Studio |

A physical device or emulator running Android 8.0+ (API 26+) is required. The on-device LLM runs best on a device with ≥ 3 GB RAM.

## 1. Clone and bootstrap

```bash
git clone --recurse-submodules <repo-url> NFScan
cd NFScan
chmod +x setup.sh && ./setup.sh
```

`setup.sh` downloads `gradle-wrapper.jar` (if absent) and initialises the `third_party/llama.cpp` submodule. It must complete without errors before you proceed.

## 2. Open in Android Studio

1. **File → Open** → select the `NFScan` root folder.
2. Wait for Gradle sync to finish (first sync downloads ~500 MB).
3. If prompted, accept the NDK/CMake install suggestion.

## 3. Download the model

Follow the instructions in [models/README.md](../models/README.md) to obtain `SmolLM2-135M-Instruct-Q4_K_M.gguf`.

## 4. Push the model to the device

```bash
# Create the target directory on the device
adb shell mkdir -p /data/data/com.nfscan/files/models

# Push the model file
adb push SmolLM2-135M-Instruct-Q4_K_M.gguf \
    /data/data/com.nfscan/files/models/smollm2-135m-q4_k_m.gguf
```

> The app looks for the file at that exact path. The file name must be `smollm2-135m-q4_k_m.gguf` (all lowercase).

## 5. Build and run

Select the `composeApp` run configuration and press **▶**. The first build compiles the llama.cpp NDK library and will take several minutes; subsequent builds are incremental.

## Troubleshooting

**Gradle sync fails with "NDK not configured"**
: Install NDK 26.1 via SDK Manager → SDK Tools → NDK (Side by side).

**`gradlew: not found`** or **`gradle-wrapper.jar` missing**
: Run `./setup.sh` again. If curl/wget is unavailable, download `gradle-wrapper.jar` manually — see the URL printed by setup.sh.

**`llama.cpp` submodule directory is empty**
: Run `git submodule update --init --depth 1 third_party/llama.cpp`.

**App crashes with "model file not found"**
: Verify the ADB push path matches exactly. Check with:
```bash
adb shell ls /data/data/com.nfscan/files/models/
```

**Build error: `CMakeLists.txt` not found**
: Ensure the NDK and CMake versions listed above are installed and that `local.properties` contains a valid `sdk.dir`.
