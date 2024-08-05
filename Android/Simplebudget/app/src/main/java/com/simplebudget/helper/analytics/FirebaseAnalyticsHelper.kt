/*
 *   Copyright 2024 Waheed Nazir
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
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