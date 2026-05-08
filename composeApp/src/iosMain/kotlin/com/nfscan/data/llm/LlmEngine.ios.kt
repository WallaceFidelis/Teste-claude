package com.nfscan.data.llm

import com.nfscan.cinterop.llama.llama_backend_free
import com.nfscan.cinterop.llama.llama_backend_init
import com.nfscan.cinterop.llama.llama_batch_get_one
import com.nfscan.cinterop.llama.llama_context
import com.nfscan.cinterop.llama.llama_context_default_params
import com.nfscan.cinterop.llama.llama_decode
import com.nfscan.cinterop.llama.llama_free
import com.nfscan.cinterop.llama.llama_free_model
import com.nfscan.cinterop.llama.llama_kv_cache_clear
import com.nfscan.cinterop.llama.llama_load_model_from_file
import com.nfscan.cinterop.llama.llama_model
import com.nfscan.cinterop.llama.llama_model_default_params
import com.nfscan.cinterop.llama.llama_new_context_with_model
import com.nfscan.cinterop.llama.llama_sampler_chain_add
import com.nfscan.cinterop.llama.llama_sampler_chain_default_params
import com.nfscan.cinterop.llama.llama_sampler_chain_init
import com.nfscan.cinterop.llama.llama_sampler_free
import com.nfscan.cinterop.llama.llama_sampler_init_greedy
import com.nfscan.cinterop.llama.llama_sampler_init_temp
import com.nfscan.cinterop.llama.llama_sampler_sample
import com.nfscan.cinterop.llama.llama_token_eos
import com.nfscan.cinterop.llama.llama_token_to_piece
import com.nfscan.cinterop.llama.llama_tokenize
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@OptIn(ExperimentalForeignApi::class)
actual class LlmEngine actual constructor() {

    private var model: CPointer<llama_model>? = null
    private var ctx: CPointer<llama_context>? = null

    actual suspend fun load(modelPath: String) = withContext(Dispatchers.IO) {
        llama_backend_init()

        val mparams = llama_model_default_params()
        model = llama_load_model_from_file(modelPath, mparams)
            ?: error("Failed to load model from $modelPath")

        val cparams = llama_context_default_params().apply {
            n_ctx   = 2048u
            n_batch = 512u
        }
        ctx = llama_new_context_with_model(model, cparams)
            ?: error("Failed to create llama context")
    }

    actual suspend fun infer(prompt: String, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val model = checkNotNull(model) { "Call load() before infer()" }
            val ctx   = checkNotNull(ctx)   { "Call load() before infer()" }

            memScoped {
                // ── Tokenise ─────────────────────────────────────────────────
                val nTokens = -llama_tokenize(
                    model, prompt, prompt.length,
                    null, 0, /*add_special=*/1, /*parse_special=*/1,
                )
                val tokenBuf = allocArray<IntVar>(nTokens)
                llama_tokenize(
                    model, prompt, prompt.length,
                    tokenBuf, nTokens, 1, 1,
                )

                // ── Eval prompt ───────────────────────────────────────────────
                llama_kv_cache_clear(ctx)
                val batch = llama_batch_get_one(tokenBuf, nTokens)
                check(llama_decode(ctx, batch) == 0) { "llama_decode failed for prompt" }

                // ── Sampler chain ─────────────────────────────────────────────
                val sparams = llama_sampler_chain_default_params()
                val sampler = llama_sampler_chain_init(sparams)
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.1f))
                llama_sampler_chain_add(sampler, llama_sampler_init_greedy())

                // ── Generate ──────────────────────────────────────────────────
                val eos      = llama_token_eos(model)
                val pieceBuf = allocArray<ByteVar>(128)
                val result   = StringBuilder()

                for (i in 0 until maxTokens) {
                    val next = llama_sampler_sample(sampler, ctx, -1)
                    if (next == eos) break

                    val pieceLen = llama_token_to_piece(
                        model, next, pieceBuf, 128, /*lstrip=*/0, /*special=*/0,
                    )
                    if (pieceLen > 0) {
                        result.append(pieceBuf.toKString().substring(0, pieceLen))
                    }

                    val nextBatch = llama_batch_get_one(
                        allocArray<IntVar>(1).also { it[0] = next }, 1,
                    )
                    if (llama_decode(ctx, nextBatch) != 0) break
                }

                llama_sampler_free(sampler)
                result.toString()
            }
        }

    actual fun close() {
        ctx?.let   { llama_free(it);       ctx   = null }
        model?.let { llama_free_model(it); model = null }
        llama_backend_free()
    }
}
