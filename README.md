# NFScan

Offline supermarket receipt scanner powered by on-device OCR and a local LLM (SmolLM2 135M via llama.cpp). No cloud, no account, no data leaves the device.

**Platforms:** Android 8.0+ · iOS 16+

## Architecture

```
composeApp/          # Shared KMP module (UI + business logic)
  commonMain/        # Compose UI, ViewModel, domain models
  androidMain/       # ML Kit OCR, JNI llama.cpp bridge, FileProvider
  iosMain/           # Vision OCR, Kotlin/Native cinterop, UIKit pickers
iosApp/              # Xcode host project
third_party/
  llama.cpp/         # Git submodule
models/              # GGUF files go here (not tracked by git)
docs/                # Platform-specific setup guides
scripts/             # Build helpers
```

Key libraries: Compose Multiplatform 1.7.1 · SQLDelight 2.0.2 · Koin 3.6.0 · kotlinx.serialization · ML Kit · llama.cpp

## Quick start

```bash
git clone --recurse-submodules <repo-url>
chmod +x setup.sh && ./setup.sh
```

Then follow the platform guide:

- **Android** → [docs/SETUP_ANDROID.md](docs/SETUP_ANDROID.md)
- **iOS** → [docs/SETUP_IOS.md](docs/SETUP_IOS.md)

## Model

The app uses **SmolLM2-135M-Instruct-Q4_K_M.gguf** (~90 MB). It is not included in this repository. See [models/README.md](models/README.md) for download instructions.

## License

MIT
