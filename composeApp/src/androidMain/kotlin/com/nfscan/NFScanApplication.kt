package com.nfscan

import android.app.Application
import com.nfscan.di.appModule
import com.nfscan.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NFScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NFScanApplication)
            modules(appModule, platformModule())
        }
    }
}
