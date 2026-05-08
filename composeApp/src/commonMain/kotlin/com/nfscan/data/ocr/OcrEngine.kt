package com.nfscan.data.ocr

/**
 * Platform-specific OCR / PDF-text extractor.
 *
 * Android – ML Kit Text Recognition for images, PdfRenderer for PDFs
 * iOS     – Vision framework (VNRecognizeTextRequest) for images, PDFKit for PDFs
 */
expect class OcrEngine() {

    /**
     * Extract plain text from raw file bytes.
     *
     * @param bytes   Raw image (JPEG / PNG) or PDF bytes.
     * @param isImage `true` for images, `false` for PDFs.
     */
    suspend fun extractText(bytes: ByteArray, isImage: Boolean): String
}
