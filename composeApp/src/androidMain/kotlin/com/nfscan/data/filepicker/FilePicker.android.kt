package com.nfscan.data.filepicker

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberFilePickerLauncher(
    type: FileType,
    onResult: (ByteArray?) -> Unit,
): FilePickerLauncher {
    val context      = LocalContext.current
    val onResultRef  = rememberUpdatedState(onResult)

    fun Uri.readBytes(ctx: Context): ByteArray? =
        ctx.contentResolver.openInputStream(this)?.use { it.readBytes() }

    // ── Gallery / photo library ───────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> onResultRef.value(uri?.readBytes(context)) }

    // ── Camera capture ────────────────────────────────────────────────────────
    // TakePicture needs a writable URI prepared before launch; we store it in
    // a remembered ref so the result callback can read it back.
    val cameraUriRef = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { captured ->
        onResultRef.value(
            if (captured) cameraUriRef.value?.readBytes(context) else null,
        )
    }

    // ── PDF document picker ───────────────────────────────────────────────────
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> onResultRef.value(uri?.readBytes(context)) }

    return remember(type) {
        FilePickerLauncher {
            when (type) {
                FileType.IMAGE  -> galleryLauncher.launch("image/*")

                FileType.CAMERA -> {
                    val file = File(
                        context.cacheDir,
                        "nfscan_capture_${System.currentTimeMillis()}.jpg",
                    )
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    cameraUriRef.value = uri
                    cameraLauncher.launch(uri)
                }

                FileType.PDF    -> pdfLauncher.launch("application/pdf")
            }
        }
    }
}
