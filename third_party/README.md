# third_party

## llama.cpp

Submodule: `https://github.com/ggerganov/llama.cpp`

After cloning the repository, initialize it with:

```bash
git submodule update --init --depth 1 third_party/llama.cpp
```

### Android
The NDK build reads from this directory automatically via `composeApp/src/androidMain/cpp/CMakeLists.txt`.

### iOS
The XCFramework must be built separately before opening the Xcode project:

```bash
./scripts/build_llama_ios.sh
```

Output is placed at `third_party/llama-ios.xcframework`.
