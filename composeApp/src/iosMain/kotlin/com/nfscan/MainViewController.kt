package com.nfscan

import androidx.compose.ui.window.ComposeUIViewController
import com.nfscan.di.appModule
import com.nfscan.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(appModule, platformModule())
        }
    },
) {
    App()
}
