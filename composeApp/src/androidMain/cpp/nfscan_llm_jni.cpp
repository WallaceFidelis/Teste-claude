#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "NFScan_LLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct LlamaHandle {
    llama_model   *model;
    llama_context *ctx;
};

extern "C" {

// ── com.nfscan.data.llm.LlmEngine ────────────────────────────────────────────

JNIEXPORT jlong JNICALL
Java_com_nfscan_data_llm_LlmEngine_nativeLoad(JNIEnv *env, jobject, jstring modelPathJ) {
    llama_backend_init();

    const char *modelPath = env->GetStringUTFChars(modelPathJ, nullptr);
    LOGI("Loading model from: %s", modelPath);

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU-only; Android GPU path requires Vulkan backend

    llama_model *model = llama_load_model_from_file(modelPath, mparams);
    env->ReleaseStringUTFChars(modelPathJ, modelPath);

    if (!model) {
        LOGE("Failed to load model");
        return 0L;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx   = 2048;
    cparams.n_batch = 512;

    llama_context *ctx = llama_new_context_with_model(model, cparams);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_free_model(model);
        return 0L;
    }

    LOGI("Model loaded successfully. Context size: %d", cparams.n_ctx);
    return reinterpret_cast<jlong>(new LlamaHandle{model, ctx});
}

JNIEXPORT jstring JNICALL
Java_com_nfscan_data_llm_LlmEngine_nativeInfer(
        JNIEnv *env, jobject, jlong handle, jstring promptJ, jint maxTokens) {

    auto *h = reinterpret_cast<LlamaHandle *>(handle);
    if (!h) return env->NewStringUTF("");

    const char *promptCStr = env->GetStringUTFChars(promptJ, nullptr);
    std::string prompt(promptCStr);
    env->ReleaseStringUTFChars(promptJ, promptCStr);

    // ── Tokenise ──────────────────────────────────────────────────────────────
    // First call returns negative count of tokens needed
    const int n_prompt = -llama_tokenize(
        h->model, prompt.c_str(), static_cast<int>(prompt.size()),
        nullptr, 0, /*add_special=*/true, /*parse_special=*/true);

    std::vector<llama_token> prompt_tokens(n_prompt);
    llama_tokenize(
        h->model, prompt.c_str(), static_cast<int>(prompt.size()),
        prompt_tokens.data(), n_prompt, true, true);

    // ── Evaluate prompt ───────────────────────────────────────────────────────
    llama_kv_cache_clear(h->ctx);

    llama_batch batch = llama_batch_get_one(prompt_tokens.data(),
                                            static_cast<int>(prompt_tokens.size()));
    if (llama_decode(h->ctx, batch) != 0) {
        LOGE("llama_decode failed for prompt");
        return env->NewStringUTF("");
    }

    // ── Sampler chain: low-temperature greedy ─────────────────────────────────
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.1f));
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    // ── Generate ──────────────────────────────────────────────────────────────
    const llama_token eos = llama_token_eos(h->model);
    std::string result;
    char piece[128];

    for (int i = 0; i < maxTokens; ++i) {
        llama_token next = llama_sampler_sample(smpl, h->ctx, -1);
        if (next == eos) break;

        const int piece_len = llama_token_to_piece(
            h->model, next, piece, sizeof(piece), /*lstrip=*/0, /*special=*/false);
        if (piece_len > 0) result.append(piece, piece_len);

        llama_batch next_batch = llama_batch_get_one(&next, 1);
        if (llama_decode(h->ctx, next_batch) != 0) break;
    }

    llama_sampler_free(smpl);
    LOGI("Generated %zu chars", result.size());
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_nfscan_data_llm_LlmEngine_nativeFree(JNIEnv *, jobject, jlong handle) {
    auto *h = reinterpret_cast<LlamaHandle *>(handle);
    if (h) {
        llama_free(h->ctx);
        llama_free_model(h->model);
        delete h;
    }
    llama_backend_free();
}

} // extern "C"
