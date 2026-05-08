package com.nfscan.di

import org.koin.core.module.Module

/** Returns a Koin module that provides platform-specific bindings (e.g. model path). */
expect fun platformModule(): Module
