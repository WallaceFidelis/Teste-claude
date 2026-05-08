#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.cpp/llama.h"

#define TAG "NFScan_LLM"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_nfscan_data_llm_LlmEngine_nativeLoad(JNIEnv *env, jobject, jstring modelPathJ) {
    const char *modelPath = env->GetStringUTFChars(modelPathJ, nullptr);

    llama_backend_init();

    llama_model_params mparams = llama_model_default_params();
    llama_model *model = llama_load_model_from_file(modelPath, mparams);
    env->ReleaseStringUTFChars(modelPathJ, modelPath);

    if (!model) {
        LOGE("Failed to load model");
        return 0L;
    }

    // Pack model + context into a heap struct; return pointer as handle
    struct Handle { llama_model *model; llama_context *ctx; };
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = 2048;
    llama_context *ctx = llama_new_context_with_model(model, cparams);

    auto *h = new Handle{model, ctx};
    return reinterpret_cast<jlong>(h);
}

JNIEXPORT jstring JNICALL
Java_com_nfscan_data_llm_LlmEngine_nativeInfer(
        JNIEnv *env, jobject, jlong handle, jstring promptJ, jint maxTokens) {

    struct Handle { llama_model *model; llama_context *ctx; };
    auto *h = reinterpret_cast<Handle *>(handle);
    if (!h) return env->NewStringUTF("");

    const char *prompt = env->GetStringUTFChars(promptJ, nullptr);

    std::vector<llama_token> tokens(maxTokens);
    int n = llama_tokenize(h->model, prompt, -1, tokens.data(), maxTokens, true, false);
    env->ReleaseStringUTFChars(promptJ, prompt);
    tokens.resize(n);

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    llama_decode(h->ctx, batch);

    std::string result;
    for (int i = 0; i < maxTokens; ++i) {
        llama_token next = llama_sampler_sample(nullptr, h->ctx, -1); // simplified
        if (next == llama_token_eos(h->model)) break;
        char buf[64];
        int len = llama_token_to_piece(h->model, next, buf, sizeof(buf), 0, false);
        if (len > 0) result.append(buf, len);
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_nfscan_data_llm_LlmEngine_nativeFree(JNIEnv *, jobject, jlong handle) {
    struct Handle { llama_model *model; llama_context *ctx; };
    auto *h = reinterpret_cast<Handle *>(handle);
    if (h) {
        llama_free(h->ctx);
        llama_free_model(h->model);
        delete h;
    }
    llama_backend_free();
}

} // extern "C"
