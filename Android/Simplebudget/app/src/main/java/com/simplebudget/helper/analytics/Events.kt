package com.simplebudget.helper.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object Events {
    /**
     * Provides screen event bundle
     * @param screenName Screen name for event
     * @param screenClass Screen class for event
     * @return Bundle of screen event
     */
    fun screenBundle(screenName: String, screenClass: String): Bundle {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        return bundle
    }

    /**
     * Provides screen event bundle
     * @param screenName Screen name for event
     * @return Bundle of screen event
     */
    fun loginBundle(screenName: String): Bundle {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        return bundle
    }

}