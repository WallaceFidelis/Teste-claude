package com.nfscan.data.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Internal DTOs that mirror the JSON the LLM is prompted to return ─────────

@Serializable
private data class LlmReceiptDto(
    val supermarket: String = "",
    val date: String = "",
    val total: Double = 0.0,
    val items: List<LlmItemDto> = emptyList(),
)

@Serializable
private data class LlmItemDto(
    val name: String = "",
    @SerialName("qty")         val quantity:   Double = 1.0,
    @SerialName("unit_price")  val unitPrice:  Double = 0.0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
)

// Lenient parser – LLMs may emit extra keys, trailing commas, or slightly
// malformed numbers; ignoreUnknownKeys + isLenient + coerceInputValues handle
// the most common deviations without failing the whole parse.
private val json = Json {
    ignoreUnknownKeys  = true
    isLenient          = true
    coerceInputValues  = true
}

/**
 * Parse raw LLM output into a [ParsedReceipt].
 *
 * The function first scans for the outermost `{…}` block so that any preamble
 * text ("Here is the JSON:") or trailing explanation is ignored.
 *
 * @throws IllegalArgumentException if no JSON object is found in [raw].
 * @throws kotlinx.serialization.SerializationException on truly malformed JSON.
 */
fun parseLlmOutput(raw: String): ParsedReceipt {
    val jsonStr = extractJsonObject(raw)
    val dto     = json.decodeFromString<LlmReceiptDto>(jsonStr)
    return dto.toDomain()
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun LlmReceiptDto.toDomain() = ParsedReceipt(
    supermarket = supermarket.trim().ifEmpty { "Desconhecido" },
    date        = date.trim(),
    total       = total,
    items       = items.map { it.toDomain() },
)

private fun LlmItemDto.toDomain() = ParsedItem(
    name       = name.trim(),
    quantity   = quantity,
    unitPrice  = unitPrice,
    // Derive total when the LLM omits or zeros it
    totalPrice = if (totalPrice > 0.0) totalPrice else quantity * unitPrice,
)

// ── JSON extraction ───────────────────────────────────────────────────────────

/**
 * Returns the substring of [text] that forms the first well-balanced `{…}`
 * object, skipping any characters before the opening brace.
 */
private fun extractJsonObject(text: String): String {
    val start = text.indexOf('{')
    require(start >= 0) { "LLM output contains no JSON object:\n${text.take(300)}" }

    var depth = 0
    var inString = false
    var escape = false

    for (i in start until text.length) {
        val c = text[i]
        when {
            escape          -> escape = false
            c == '\\'       -> if (inString) escape = true
            c == '"'        -> inString = !inString
            inString        -> Unit
            c == '{'        -> depth++
            c == '}' && --depth == 0 -> return text.substring(start, i + 1)
        }
    }
    error("Malformed JSON in LLM output (unclosed brace):\n${text.take(300)}")
}
