package com.nfscan.di

import com.nfscan.data.llm.LlmEngine
import com.nfscan.data.ocr.OcrEngine
import com.nfscan.viewmodel.ReceiptViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { OcrEngine() }
    single { LlmEngine() }
    viewModel { ReceiptViewModel(get(), get(), get(named("modelPath"))) }
}
