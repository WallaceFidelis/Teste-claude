package com.nfscan.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Absolute path to the GGUF model inside the app's private files directory.
    // Deploy the model with:
    //   adb push SmolLM2-135M-Instruct-Q4_K_M.gguf \
    //       /data/data/com.nfscan/files/models/smollm2-135m-q4_k_m.gguf
    single(named("modelPath")) {
        androidContext()
            .filesDir
            .resolve("models/smollm2-135m-q4_k_m.gguf")
            .absolutePath
    }
}
