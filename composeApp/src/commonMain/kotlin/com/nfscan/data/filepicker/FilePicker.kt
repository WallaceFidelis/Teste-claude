package com.nfscan.data.filepicker

import androidx.compose.runtime.Composable

enum class FileType {
    /** Photo from the device gallery / photo library. */
    IMAGE,
    /** Live camera capture. */
    CAMERA,
    /** PDF document from the file system. */
    PDF,
}

/** Thin wrapper that lets the UI trigger the picker without holding platform types. */
class FilePickerLauncher(val launch: () -> Unit)

/**
 * Returns a [FilePickerLauncher] whose [FilePickerLauncher.launch] opens the
 * platform picker for [type].  The selected file is delivered as a [ByteArray]
 * (or `null` on cancellation / error) via [onResult].
 *
 * Must be called at Composable scope (same rules as `rememberLauncherForActivityResult`).
 */
@Composable
expect fun rememberFilePickerLauncher(
    type: FileType,
    onResult: (ByteArray?) -> Unit,
): FilePickerLauncher
