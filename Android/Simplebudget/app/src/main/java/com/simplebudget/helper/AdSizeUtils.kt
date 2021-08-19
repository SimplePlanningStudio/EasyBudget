package com.simplebudget.helper

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import com.google.android.gms.ads.AdSize

object AdSizeUtils {

    /**
     * Get Ad size for Adaptive Ads
     *
     * @return Ad size for Adaptive Ads
     */
    fun getAdSize(context: Context, display: Display): AdSize? {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }
}