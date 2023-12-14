package com.simplebudget.helper.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.simplebudget.helper.ENABLE_ANALYTICS

object FirebaseAnalyticsHelper {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize(firebaseAnalytics: FirebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics
    }

    private fun logEvent(eventName: String, params: Bundle? = null) {
        if (ENABLE_ANALYTICS) {
            firebaseAnalytics.logEvent(eventName, params)
        }
    }


    /**
     * This function log screen events
     * @param screenBundle We'll get a bundle from Events class
     * @see Events
     * @see Events.screenBundle(screenName: String, screenClass: String)
     */
    fun screenEvent(screenBundle: Bundle) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenBundle)
    }

    /**
     * This function logs a login event
     * @param loginBundle We'll get a bundle from Events class
     * @see Events
     * @see Events.loginBundle(screenName: String, screenClass: String)
     */
    fun loginEvent(loginBundle: Bundle) {
        logEvent(FirebaseAnalytics.Event.LOGIN, loginBundle)
    }
}