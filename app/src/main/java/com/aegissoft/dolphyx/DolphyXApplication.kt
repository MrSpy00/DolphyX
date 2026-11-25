package com.aegissoft.dolphyx

import android.app.Application
import android.util.Log

/**
 * Application entry point for DolphyX by aegisSoft.
 */
class DolphyXApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DolphyX initialized (v${BuildConfig.VERSION_NAME})")
    }

    companion object {
        private const val TAG = "DolphyXApplication"
    }
}
