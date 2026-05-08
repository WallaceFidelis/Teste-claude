package com.nfscan.data.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS actual using Vision (VNRecognizeTextRequest) for images
 * and PDFKit for PDF text extraction.
 */
actual class OcrEngine actual constructor() {

    actual suspend fun extractText(filePath: String): String =
        withContext(Dispatchers.IO) {
            val url = NSURL.fileURLWithPath(filePath)
            when {
                filePath.endsWith(".pdf", ignoreCase = true) -> extractPdf(url)
                else -> extractImage(url)
            }
        }

    private fun extractImage(url: NSURL): String {
        // Use VNImageRequestHandler + VNRecognizeTextRequest
        // Results collected from VNRecognizedTextObservation.topCandidates(1)
        TODO("Wire Vision framework via platform.Vision cinterop")
    }

    private fun extractPdf(url: NSURL): String {
        // Use PDFKit: PDFDocument(url:) → iterate PDFPage.string
        TODO("Wire PDFKit via platform.PDFKit cinterop")
    }
}
