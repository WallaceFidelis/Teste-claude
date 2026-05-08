cmake_minimum_required(VERSION 3.22.1)
project(nfscan_llm)

# ── llama.cpp submodule ──────────────────────────────────────────────────────
# Place the llama.cpp sources at composeApp/src/androidMain/cpp/llama.cpp/
# git submodule add https://github.com/ggerganov/llama.cpp \
#     composeApp/src/androidMain/cpp/llama.cpp

set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

set(LLAMA_BUILD_TESTS   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER  OFF CACHE BOOL "" FORCE)
add_subdirectory(${LLAMA_DIR} llama.cpp_build)

# ── JNI bridge ───────────────────────────────────────────────────────────────
add_library(nfscan_llm SHARED nfscan_llm_jni.cpp)

target_include_directories(nfscan_llm PRIVATE ${LLAMA_DIR})

target_link_libraries(nfscan_llm
    llama
    android
    log
)
