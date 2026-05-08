#!/usr/bin/env bash
# setup.sh – Bootstrap NFScan for first-time development
#
# Run once after cloning:
#   chmod +x setup.sh && ./setup.sh
#
# What it does:
#   1. Downloads gradle-wrapper.jar if absent
#   2. Initialises the llama.cpp git submodule
#   3. Prints the next steps (model download, platform builds)

set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}▶ $*${NC}"; }
warning() { echo -e "${YELLOW}⚠ $*${NC}"; }
error()   { echo -e "${RED}✗ $*${NC}" >&2; exit 1; }

# ── 1. Gradle wrapper JAR ─────────────────────────────────────────────────────
WRAPPER_JAR="${REPO_ROOT}/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "${WRAPPER_JAR}" ]; then
    info "Downloading gradle-wrapper.jar (Gradle 8.9)…"
    if command -v curl &>/dev/null; then
        curl -fsSL \
          "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" \
          -o "${WRAPPER_JAR}"
    elif command -v wget &>/dev/null; then
        wget -q \
          "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" \
          -O "${WRAPPER_JAR}"
    else
        error "Neither curl nor wget found. Install one and re-run, or download manually:
  URL: https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
  Destination: ${WRAPPER_JAR}"
    fi
    info "gradle-wrapper.jar downloaded."
else
    info "gradle-wrapper.jar already present — skipping."
fi

chmod +x "${REPO_ROOT}/gradlew"

# ── 2. Git submodule (llama.cpp) ──────────────────────────────────────────────
if [ -f "${REPO_ROOT}/.gitmodules" ]; then
    if [ -z "$(ls -A "${REPO_ROOT}/third_party/llama.cpp" 2>/dev/null)" ]; then
        info "Initialising llama.cpp submodule (shallow clone, may take a moment)…"
        git -C "${REPO_ROOT}" submodule update --init --depth 1 third_party/llama.cpp
        info "llama.cpp submodule ready."
    else
        info "llama.cpp submodule already initialised — skipping."
    fi
fi

# ── 3. Next steps ─────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}✅ Bootstrap complete!${NC}"
echo ""
echo "Next steps:"
echo ""
echo "  Android:"
echo "    1. Open this folder in Android Studio"
echo "    2. Download the model (see models/README.md)"
echo "    3. adb push SmolLM2-135M-Instruct-Q4_K_M.gguf \\"
echo "           /data/data/com.nfscan/files/models/smollm2-135m-q4_k_m.gguf"
echo "    4. Run ▶ on a device / emulator"
echo ""
echo "  iOS (macOS + Xcode required):"
echo "    1. Build the llama.cpp XCFramework:"
echo "       ./scripts/build_llama_ios.sh"
echo "    2. Download the model (see models/README.md)"
echo "    3. Open iosApp/iosApp.xcodeproj in Xcode"
echo "    4. Add the .gguf file to the project (see docs/SETUP_IOS.md)"
echo "    5. Run ▶ on a simulator or device"
echo ""
echo "  Full docs: docs/SETUP_ANDROID.md  |  docs/SETUP_IOS.md"
