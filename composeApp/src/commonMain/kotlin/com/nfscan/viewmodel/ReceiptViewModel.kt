package com.nfscan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfscan.data.llm.LlmEngine
import com.nfscan.data.llm.ParsedReceipt
import com.nfscan.data.ocr.OcrEngine
import com.nfscan.data.ocr.OcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Idle : ScanState
    data object ExtractingText : ScanState
    data object RunningLlm : ScanState
    data class Success(val receipt: ParsedReceipt) : ScanState
    data class Error(val message: String) : ScanState
}

class ReceiptViewModel(
    private val ocrEngine: OcrEngine,
    private val llmEngine: LlmEngine,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun scan(filePath: String) {
        viewModelScope.launch {
            _state.update { ScanState.ExtractingText }

            val rawText = when (val ocr = runCatching { ocrEngine.extractText(filePath) }) {
                else -> ocr.getOrElse { return@launch emitError(it) }
            }

            _state.update { ScanState.RunningLlm }

            val prompt = buildPrompt(rawText)
            val llmOutput = runCatching { llmEngine.infer(prompt) }
                .getOrElse { return@launch emitError(it) }

            val receipt = runCatching { parseLlmOutput(llmOutput, rawText) }
                .getOrElse { return@launch emitError(it) }

            _state.update { ScanState.Success(receipt) }
        }
    }

    fun reset() {
        _state.update { ScanState.Idle }
    }

    private fun emitError(cause: Throwable) {
        _state.update { ScanState.Error(cause.message ?: "Unknown error") }
    }

    private fun buildPrompt(rawText: String): String = """
        |Extract the following fields from the supermarket receipt text below.
        |Return ONLY valid JSON with keys: supermarket, date, total, items (array of {name, qty, unit_price, total_price}).
        |
        |Receipt:
        |$rawText
        |
        |JSON:
    """.trimMargin()

    private fun parseLlmOutput(json: String, rawText: String): ParsedReceipt {
        // Stub: replace with kotlinx.serialization JSON parsing of LLM output
        return ParsedReceipt(
            supermarket = "Unknown",
            date = "",
            items = emptyList(),
            total = 0.0,
        )
    }
}
