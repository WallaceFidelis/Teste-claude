package com.nfscan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfscan.data.llm.LlmEngine
import com.nfscan.data.llm.ParsedReceipt
import com.nfscan.data.llm.parseLlmOutput
import com.nfscan.data.ocr.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Idle : ScanState
    data object LoadingModel : ScanState
    data object ExtractingText : ScanState
    data object RunningLlm : ScanState
    data class Success(val receipt: ParsedReceipt) : ScanState
    data class Error(val message: String) : ScanState
}

// plain class avoids ByteArray reference-equality pitfall of data class
private class ScanSource(val bytes: ByteArray, val isImage: Boolean)

class ReceiptViewModel(
    private val ocrEngine: OcrEngine,
    private val llmEngine: LlmEngine,
    private val modelPath: String,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private var pendingSource: ScanSource? = null
    private var modelReady = false

    fun setPendingSource(bytes: ByteArray, isImage: Boolean) {
        pendingSource = ScanSource(bytes, isImage)
    }

    fun scan() {
        // Guard against double-scan if recomposition triggers this again
        if (_state.value !is ScanState.Idle) return

        val source = pendingSource ?: run {
            _state.update { ScanState.Error("Nenhum arquivo selecionado") }
            return
        }

        viewModelScope.launch {
            // ── Step 0: lazy-load model on first use ──────────────────────────
            if (!modelReady) {
                _state.update { ScanState.LoadingModel }

                if (modelPath.isEmpty()) {
                    emitError(IllegalStateException(
                        "Modelo não encontrado. Consulte models/README.md para instruções de instalação."
                    ))
                    return@launch
                }

                runCatching { llmEngine.load(modelPath) }
                    .getOrElse { return@launch emitError(it) }

                modelReady = true
            }

            // ── Step 1: OCR ───────────────────────────────────────────────────
            _state.update { ScanState.ExtractingText }

            val rawText = runCatching {
                ocrEngine.extractText(source.bytes, source.isImage)
            }.getOrElse { return@launch emitError(it) }

            // ── Step 2: LLM ───────────────────────────────────────────────────
            _state.update { ScanState.RunningLlm }

            val llmOutput = runCatching {
                llmEngine.infer(buildPrompt(rawText))
            }.getOrElse { return@launch emitError(it) }

            // ── Step 3: Parse ─────────────────────────────────────────────────
            val receipt = runCatching {
                parseLlmOutput(llmOutput)
            }.getOrElse { return@launch emitError(it) }

            _state.update { ScanState.Success(receipt) }
        }
    }

    fun reset() {
        pendingSource = null
        _state.update { ScanState.Idle }
    }

    private fun emitError(cause: Throwable) {
        _state.update { ScanState.Error(cause.message ?: "Erro desconhecido") }
    }

    private fun buildPrompt(rawText: String): String = """
        |Extraia as informações da nota fiscal abaixo.
        |Retorne APENAS JSON válido com as chaves:
        |  supermarket (string), date (string DD/MM/YYYY), total (number),
        |  items (array of {name, qty, unit_price, total_price}).
        |Não inclua texto fora do JSON.
        |
        |Nota fiscal:
        |$rawText
        |
        |JSON:
    """.trimMargin()
}
