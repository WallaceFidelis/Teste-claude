package com.nfscan.data.llm

/**
 * Platform-specific llama.cpp inference engine (NDK on Android, Metal on iOS).
 *
 * Model file is expected at:
 *   Android – app's filesDir/models/smollm2-135m-q4_k_m.gguf
 *   iOS     – main bundle Models/smollm2-135m-q4_k_m.gguf
 */
expect class LlmEngine() {

    /** Load model into memory. Must be called once before [infer]. */
    suspend fun load(modelPath: String)

    /**
     * Run inference synchronously on the calling coroutine dispatcher.
     * Returns raw model output as a String.
     */
    suspend fun infer(prompt: String, maxTokens: Int = 256): String

    /** Release native resources. */
    fun close()
}

data class ParsedReceipt(
    val supermarket: String,
    val date: String,
    val items: List<ParsedItem>,
    val total: Double,
)

data class ParsedItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
)
