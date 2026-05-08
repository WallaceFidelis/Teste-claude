package com.nfscan.data.llm

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * iOS actual using llama.cpp compiled as a static XCFramework with Metal acceleration.
 * The XCFramework is added to the Xcode project and bridged via cinterop definitions
 * at composeApp/src/iosMain/cinterop/llama.def.
 */
@OptIn(ExperimentalForeignApi::class)
actual class LlmEngine actual constructor() {

    private var handle: Long = 0

    actual suspend fun load(modelPath: String) = withContext(Dispatchers.IO) {
        // llama_backend_init() + llama_load_model + llama_new_context via cinterop
        // handle = nfscan_llm_load(modelPath)
        TODO("Wire llama.cpp cinterop")
    }

    actual suspend fun infer(prompt: String, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            // nfscan_llm_infer(handle, prompt, maxTokens)?.toKString() ?: ""
            TODO("Wire llama.cpp cinterop")
        }

    actual fun close() {
        if (handle != 0L) {
            // nfscan_llm_free(handle)
            handle = 0
        }
    }
}
