package com.simplebudget.helper.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds

object AdSdkManager {
    private var isInitialized = false

    fun initialize(context: Context, onInitialized: () -> Unit) {
        if (!isInitialized) {
            MobileAds.initialize(context) {
                isInitialized = true
                onInitialized()
            }
        } else {
            onInitialized()
        }
    }
}
