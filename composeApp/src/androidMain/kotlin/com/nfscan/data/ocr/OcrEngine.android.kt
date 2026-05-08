package com.nfscan.data.ocr

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class OcrEngine actual constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    actual suspend fun extractText(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        when {
            filePath.endsWith(".pdf", ignoreCase = true) -> extractFromPdf(file)
            else -> extractFromImage(file)
        }
    }

    private suspend fun extractFromImage(file: File): String {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: error("Cannot decode image: ${file.name}")
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    private fun extractFromImage(file: File, pdfPage: android.graphics.Bitmap): String {
        // overload used by PDF path below
        throw UnsupportedOperationException()
    }

    private suspend fun extractFromPdf(file: File): String = withContext(Dispatchers.IO) {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val pages = (0 until renderer.pageCount).map { index ->
            val page = renderer.openPage(index)
            val bitmap = android.graphics.Bitmap.createBitmap(
                page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        }
        renderer.close()
        fd.close()

        pages.joinToString("\n") { bmp ->
            val image = InputImage.fromBitmap(bmp, 0)
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }
    }
}
