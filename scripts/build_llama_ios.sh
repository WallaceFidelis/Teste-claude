#!/usr/bin/env bash
# build_llama_ios.sh
#
# Compiles llama.cpp as static libraries for all iOS targets and packages them
# into an XCFramework at third_party/llama-ios.xcframework.
#
# Prerequisites:
#   - Xcode command-line tools installed
#   - submodule initialised: git submodule update --init --depth 1 third_party/llama.cpp
#
# Usage:
#   ./scripts/build_llama_ios.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LLAMA_SRC="${REPO_ROOT}/third_party/llama.cpp"
BUILD_DIR="${REPO_ROOT}/build/llama-ios"
XCFW_OUT="${REPO_ROOT}/third_party/llama-ios.xcframework"

CMAKE_COMMON=(
  -DCMAKE_BUILD_TYPE=Release
  -DLLAMA_BUILD_TESTS=OFF
  -DLLAMA_BUILD_EXAMPLES=OFF
  -DLLAMA_BUILD_SERVER=OFF
  -DLLAMA_CURL=OFF
  -DBUILD_SHARED_LIBS=OFF
  -DGGML_METAL=ON          # Metal acceleration on Apple Silicon
  -DGGML_BLAS=OFF
)

build_slice() {
  local name="$1"; shift
  local cmake_opts=("$@")
  local out="${BUILD_DIR}/${name}"
  mkdir -p "${out}"
  cmake -S "${LLAMA_SRC}" -B "${out}" "${CMAKE_COMMON[@]}" "${cmake_opts[@]}"
  cmake --build "${out}" --config Release -- -j"$(sysctl -n hw.logicalcpu)"
  echo "${out}"
}

echo "▶ Building iosArm64 (device)…"
ARM64_DIR=$(build_slice "iosArm64" \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=16.0 \
  -DCMAKE_OSX_SYSROOT="$(xcrun --sdk iphoneos --show-sdk-path)")

echo "▶ Building iosSimulatorArm64 + iosX64 (simulator fat)…"
SIM_DIR=$(build_slice "iosSimulator" \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_ARCHITECTURES="arm64;x86_64" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=16.0 \
  -DCMAKE_OSX_SYSROOT="$(xcrun --sdk iphonesimulator --show-sdk-path)")

# Collect headers from the device slice (same for all slices)
HEADERS_DIR="${BUILD_DIR}/headers"
mkdir -p "${HEADERS_DIR}"
cp "${LLAMA_SRC}/include/llama.h"      "${HEADERS_DIR}/"
cp "${LLAMA_SRC}/ggml/include/ggml.h"  "${HEADERS_DIR}/"

# Libraries to bundle (adjust names if llama.cpp version changes)
collect_libs() {
  local slice_dir="$1"
  find "${slice_dir}" -name "libllama.a" -o -name "libggml.a" \
       -o -name "libggml-base.a" -o -name "libggml-cpu.a" \
       -o -name "libggml-metal.a" | tr '\n' ' '
}

DEVICE_LIBS=$(collect_libs "${ARM64_DIR}")
SIM_LIBS=$(collect_libs "${SIM_DIR}")

# Merge per-slice into a single fat archive per slice
merge_libs() {
  local out="$1"; shift
  libtool -static -o "${out}" "$@"
}

echo "▶ Merging device libs…"
merge_libs "${BUILD_DIR}/libllama-device.a"    ${DEVICE_LIBS}

echo "▶ Merging simulator libs…"
merge_libs "${BUILD_DIR}/libllama-simulator.a" ${SIM_LIBS}

# Package XCFramework
rm -rf "${XCFW_OUT}"
echo "▶ Creating XCFramework…"
xcodebuild -create-xcframework \
  -library "${BUILD_DIR}/libllama-device.a"    -headers "${HEADERS_DIR}" \
  -library "${BUILD_DIR}/libllama-simulator.a" -headers "${HEADERS_DIR}" \
  -output "${XCFW_OUT}"

echo "✅ XCFramework ready at: ${XCFW_OUT}"
