package com.nfscan.data.ocr

/**
 * Platform-specific OCR / PDF-text extractor.
 *
 * Android – ML Kit Document Scanner + PdfRenderer for PDFs
 * iOS     – Vision framework (VNRecognizeTextRequest) + PDFKit
 */
expect class OcrEngine() {

    /**
     * Extract plain text from an image or PDF at [filePath].
     * Returns the full concatenated text.
     */
    suspend fun extractText(filePath: String): String
}

sealed interface OcrResult {
    data class Success(val text: String) : OcrResult
    data class Failure(val cause: Throwable) : OcrResult
}
