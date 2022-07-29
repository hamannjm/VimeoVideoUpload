package com.lcs.videoupload

import android.app.Application
import timber.log.Timber

class UploadApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        VimeoAuth.init(this)
    }
}