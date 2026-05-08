package com.nfscan.data.ocr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.PDFKit.PDFDocument
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizeTextRequestRevision3
import kotlin.coroutines.resume

actual class OcrEngine actual constructor() {

    actual suspend fun extractText(bytes: ByteArray, isImage: Boolean): String =
        withContext(Dispatchers.IO) {
            val nsData = bytes.toNSData()
            if (isImage) extractFromImageData(nsData)
            else extractFromPdfData(nsData)
        }

    // ── Image (Vision framework) ──────────────────────────────────────────────

    private suspend fun extractFromImageData(data: NSData): String =
        suspendCancellableCoroutine { cont ->
            val image = UIImage(data = data)
            val cgImage = image.CGImage
                ?: return@suspendCancellableCoroutine cont.resume("")

            val request = VNRecognizeTextRequest { req, error ->
                if (error != null) { cont.resume(""); return@VNRecognizeTextRequest }
                @Suppress("UNCHECKED_CAST")
                val observations = req?.results as? List<platform.Vision.VNRecognizedTextObservation>
                    ?: emptyList()
                val text = observations.mapNotNull { obs ->
                    obs.topCandidates(1u).firstOrNull()?.string
                }.joinToString("\n")
                cont.resume(text)
            }
            request.recognitionLevel = platform.Vision.VNRequestTextRecognitionLevelAccurate
            request.revision = VNRecognizeTextRequestRevision3
            request.usesLanguageCorrection = true

            val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())
            val success = handler.performRequests(listOf(request), error = null)
            if (!success) cont.resume("")
        }

    // ── PDF (PDFKit) ──────────────────────────────────────────────────────────

    private suspend fun extractFromPdfData(data: NSData): String =
        withContext(Dispatchers.IO) {
            val doc = PDFDocument(data = data)
                ?: return@withContext ""
            buildString {
                for (i in 0 until doc.pageCount) {
                    val page = doc.pageAtIndex(i) ?: continue
                    val pageText = page.string ?: continue
                    if (isNotEmpty()) append("\n")
                    append(pageText)
                }
            }
        }
}

// ── Helper ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData()
    else usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }
