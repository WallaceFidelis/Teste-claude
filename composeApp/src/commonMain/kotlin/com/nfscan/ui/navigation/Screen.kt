package com.nfscan.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    // filePath removed – bytes are stored in the shared ViewModel before navigating
    @Serializable
    data object Scanning : Screen

    @Serializable
    data object Result : Screen
}
