package com.nfscan.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Scanning(val filePath: String) : Screen

    @Serializable
    data object Result : Screen
}
