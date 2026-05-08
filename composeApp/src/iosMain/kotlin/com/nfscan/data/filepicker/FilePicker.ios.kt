package com.nfscan.data.filepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceTypeCamera
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIWindow
import platform.darwin.NSObject

@Composable
actual fun rememberFilePickerLauncher(
    type: FileType,
    onResult: (ByteArray?) -> Unit,
): FilePickerLauncher {
    val onResultRef = rememberUpdatedState(onResult)
    // Keep delegate alive for the full composition lifetime
    val delegate = remember { PickerDelegate() }
    delegate.onResult = { onResultRef.value(it) }

    return remember(type) {
        FilePickerLauncher {
            val rootVC = UIApplication.sharedApplication
                .windows
                .filterIsInstance<UIWindow>()
                .firstOrNull { it.isKeyWindow }
                ?.rootViewController
                ?: return@FilePickerLauncher

            when (type) {
                FileType.IMAGE, FileType.CAMERA -> {
                    val sourceType = if (type == FileType.CAMERA)
                        UIImagePickerControllerSourceTypeCamera
                    else UIImagePickerControllerSourceTypePhotoLibrary

                    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
                        onResultRef.value(null); return@FilePickerLauncher
                    }
                    val picker = UIImagePickerController()
                    picker.sourceType    = sourceType
                    picker.allowsEditing = false
                    picker.delegate      = delegate
                    rootVC.presentViewController(picker, animated = true, completion = null)
                }

                FileType.PDF -> {
                    val picker = UIDocumentPickerViewController(
                        documentTypes = listOf("com.adobe.pdf", "public.pdf"),
                        inMode        = UIDocumentPickerModeImport,
                    )
                    picker.allowsMultipleSelection = false
                    picker.delegate                = delegate
                    rootVC.presentViewController(picker, animated = true, completion = null)
                }
            }
        }
    }
}

// ── Shared UIKit delegate ─────────────────────────────────────────────────────

private class PickerDelegate :
    NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol,
    UIDocumentPickerDelegateProtocol {

    var onResult: (ByteArray?) -> Unit = {}

    // ── UIImagePickerController ───────────────────────────────────────────────

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val nsData = image?.let { UIImageJPEGRepresentation(it, 0.85) }
        onResult(nsData?.toByteArray())
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onResult(null)
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    // ── UIDocumentPickerViewController ────────────────────────────────────────

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url   = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val bytes = url?.let { NSData.dataWithContentsOfURL(it)?.toByteArray() }
        onResult(bytes)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    if (length == 0UL) return ByteArray(0)
    return ByteArray(length.toInt()).also { arr ->
        arr.usePinned { platform.posix.memcpy(it.addressOf(0), bytes, length) }
    }
}
