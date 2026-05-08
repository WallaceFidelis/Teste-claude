cmake_minimum_required(VERSION 3.22.1)
project(nfscan_llm)

# llama.cpp submodule lives at <project_root>/third_party/llama.cpp
# This CMakeLists.txt is at composeApp/src/androidMain/cpp/ (4 levels deep)
set(LLAMA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/llama.cpp")

# Disable targets we don't need to keep the build fast
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL           OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS    OFF CACHE BOOL "" FORCE)

# Android NDK does not provide OpenMP by default
set(GGML_OPENMP          OFF CACHE BOOL "" FORCE)
# Use Android's built-in BLAS via OpenBLAS is unavailable; rely on GGML's own SIMD
set(GGML_BLAS            OFF CACHE BOOL "" FORCE)

add_subdirectory("${LLAMA_DIR}" llama_build)

# ── JNI bridge shared library ─────────────────────────────────────────────────
add_library(nfscan_llm SHARED nfscan_llm_jni.cpp)

target_include_directories(nfscan_llm PRIVATE
    "${LLAMA_DIR}/include"
    "${LLAMA_DIR}/ggml/include"
)

target_compile_options(nfscan_llm PRIVATE
    -O3
    -ffast-math
    -DNDEBUG
)

target_link_libraries(nfscan_llm
    llama
    android
    log
)
