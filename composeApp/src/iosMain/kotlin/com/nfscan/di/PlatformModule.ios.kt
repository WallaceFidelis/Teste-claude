package com.nfscan.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual fun platformModule(): Module = module {
    // Path to the GGUF model bundled inside the iOS app target.
    // Add SmolLM2-135M-Instruct-Q4_K_M.gguf to the Xcode project under a
    // "Models" group and ensure "Add to target: iosApp" is checked.
    single(named("modelPath")) {
        NSBundle.mainBundle.pathForResource(
            name   = "SmolLM2-135M-Instruct-Q4_K_M",
            ofType = "gguf",
        ) ?: "" // empty string → scan will emit Error("Model not found")
    }
}
