package com.nfscan.data.ocr

import android.graphics.Bitmap
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

    actual suspend fun extractText(bytes: ByteArray, isImage: Boolean): String =
        withContext(Dispatchers.IO) {
            if (isImage) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: error("Cannot decode image bytes")
                recognizeText(bmp)
            } else {
                extractFromPdfBytes(bytes)
            }
        }

    private suspend fun recognizeText(bmp: Bitmap): String {
        val image = InputImage.fromBitmap(bmp, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    private suspend fun extractFromPdfBytes(bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            // PdfRenderer requires a seekable file descriptor, so we need a temp file
            val tmpFile = File.createTempFile("nfscan_pdf_", ".pdf")
            try {
                tmpFile.writeBytes(bytes)
                val fd = ParcelFileDescriptor.open(
                    tmpFile, ParcelFileDescriptor.MODE_READ_ONLY,
                )
                val renderer = PdfRenderer(fd)
                val pageTexts = buildList {
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val bmp  = Bitmap.createBitmap(
                            page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888,
                        )
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        add(recognizeText(bmp))
                    }
                }
                renderer.close()
                fd.close()
                pageTexts.joinToString("\n")
            } finally {
                tmpFile.delete()
            }
        }
}
