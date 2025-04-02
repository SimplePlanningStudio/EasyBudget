package com.simplebudget.helper.analytics

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.helper.Logger

/**
 * To debug events
 * adb shell setprop debug.firebase.analytics.app PACKAGE_NAME
 * https://firebase.google.com/docs/analytics/debugview#android
 */
class AnalyticsManager(private val context: Context) {
    /**
     * Log an event with optional parameters.
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        if (BuildConfig.ANALYTICS_ACTIVATED) {
            try {

                //Log firebase event
                Firebase.analytics.logEvent(eventName) {
                    params?.forEach { (key, value) ->
                        when (value) {
                            is String -> param(key, value)
                            is Long -> param(key, value)
                            is Double -> param(key, value)
                            is Int -> param(key, value.toLong())
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.error(
                    AnalyticsManager::class.java.simpleName,
                    context.getString(R.string.error_logging_firebase_analytics_events),
                    e
                )
            }
        }
    }

    fun setUserProperty(propertyName: String, propertyValue: String) {
        try {
            if (BuildConfig.ANALYTICS_ACTIVATED) {
                Firebase.analytics.setUserProperty(propertyName, propertyValue)
            }
        } catch (e: Exception) {
            Logger.error(
                AnalyticsManager::class.java.simpleName,
                context.getString(R.string.error_setting_user_property_for_analytics),
                e
            )
        }
    }

}