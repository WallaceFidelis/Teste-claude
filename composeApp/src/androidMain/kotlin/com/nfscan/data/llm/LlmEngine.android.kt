package com.nfscan.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class LlmEngine actual constructor() {

    private var nativeHandle: Long = 0

    actual suspend fun load(modelPath: String) = withContext(Dispatchers.IO) {
        nativeHandle = nativeLoad(modelPath)
    }

    actual suspend fun infer(prompt: String, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            nativeInfer(nativeHandle, prompt, maxTokens)
        }

    actual fun close() {
        if (nativeHandle != 0L) {
            nativeFree(nativeHandle)
            nativeHandle = 0
        }
    }

    // JNI bridge to llama.cpp compiled via NDK (CMakeLists.txt in androidMain/cpp)
    private external fun nativeLoad(modelPath: String): Long
    private external fun nativeInfer(handle: Long, prompt: String, maxTokens: Int): String
    private external fun nativeFree(handle: Long)

    companion object {
        init {
            System.loadLibrary("nfscan_llm")
        }
    }
}
