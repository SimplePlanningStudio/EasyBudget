package com.simplebudget.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.helper.*
import com.simplebudget.notif.CHANNEL_DAILY_REMINDERS
import com.simplebudget.notif.CHANNEL_WEEKLY_REMINDERS
import com.simplebudget.notif.CHANNEL_NEW_FEATURES
import com.simplebudget.prefs.*
import com.simplebudget.view.main.MainActivity
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.random.Random


class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val parameters: AppPreferences by inject()

    /**
     *
     */
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setUpNotificationChannel()
        }
    }

    /**
     *
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            if (shouldDisplayPush(remoteMessage)) {
                when {
                    "true".equals(remoteMessage.data[DAILY_REMINDER_KEY], ignoreCase = true) -> {
                        sendNotification(it, CHANNEL_DAILY_REMINDERS)
                    }
                    "true".equals(remoteMessage.data[WEEKLY_REMINDER_KEY], ignoreCase = true) -> {
                        sendNotification(it, CHANNEL_WEEKLY_REMINDERS)
                    }
                    else -> {
                        sendNotification(it, CHANNEL_NEW_FEATURES)
                        checkIfItsDownloadCampaign(remoteMessage)
                    }
                }
            }
        }
    }

    /**
     * If it's for download promotion for other apps.
     */
    private fun checkIfItsDownloadCampaign(remoteMessage: RemoteMessage) {
        val download: String = remoteMessage.data["download"] ?: ""
        if (download.isNotEmpty()) {
            val appPackage: String = remoteMessage.data["package"] ?: ""
            appPackage.let { pkg ->
                if (pkg != "") {
                    parameters.putString(PACKAGE_TO_DOWNLOAD, pkg)
                    val localBroadcastManager = LocalBroadcastManager.getInstance(this)
                    val localIntent = Intent(ACTION_TRIGGER_DOWNLOAD)
                        .putExtra("download", download)
                        .putExtra("package", pkg)
                        .putExtra("title", remoteMessage.notification?.title ?: "Download")
                        .putExtra(
                            "body",
                            remoteMessage.notification?.body
                                ?: "Download ${remoteMessage.notification?.title ?: "Download"}  app for free!"
                        )
                    localBroadcastManager.sendBroadcast(localIntent)
                }
            }
        }
    }

    /**
     * Check if the push should be displayed
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private fun shouldDisplayPush(remoteMessage: RemoteMessage): Boolean {
        return isUserOk(remoteMessage) && isVersionCompatible(remoteMessage)
    }

    /**
     * Check if the push should be displayed according to user choice
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private fun isUserOk(remoteMessage: RemoteMessage): Boolean {
        try {
            // Check if it's a daily reminder
            if (remoteMessage.data.containsKey(DAILY_REMINDER_KEY) && "true" == remoteMessage.data[DAILY_REMINDER_KEY]) {
                // Check user choice
                // Check if the app hasn't been opened today
                val lastOpenTimestamp = parameters.getLastOpenTimestamp()
                if (lastOpenTimestamp == 0L) {
                    return false
                }

                val lastOpen = Date(lastOpenTimestamp)

                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_YEAR)
                cal.time = lastOpen
                val lastOpenDay = cal.get(Calendar.DAY_OF_YEAR)

                return currentDay != lastOpenDay
            } else if (remoteMessage.data.containsKey(WEEKLY_REMINDER_KEY) && "true" == remoteMessage.data[WEEKLY_REMINDER_KEY]) {
                return parameters.isUserAllowingMonthlyReminderPushes()
            }

            // Else it must be an update push
            return parameters.isUserAllowingUpdatePushes()
        } catch (e: Exception) {
            Logger.error("Error while checking user ok for push", e)
            return false
        }

    }

    /**
     * Check if the push should be displayed according to version constrains
     *
     * @param remoteMessage
     * @return true if should display the push, false otherwise
     */
    private fun isVersionCompatible(remoteMessage: RemoteMessage): Boolean {
        try {
            var maxVersion = BuildConfig.VERSION_CODE
            var minVersion = 1

            if (remoteMessage.data.containsKey(INTENT_MAX_VERSION_KEY)) {
                maxVersion = Integer.parseInt(remoteMessage.data[INTENT_MAX_VERSION_KEY]!!)
            }

            if (remoteMessage.data.containsKey(INTENT_MIN_VERSION_KEY)) {
                minVersion = Integer.parseInt(remoteMessage.data[INTENT_MIN_VERSION_KEY]!!)
            }
            return BuildConfig.VERSION_CODE in minVersion..maxVersion
        } catch (e: Exception) {
            Logger.error("Error while checking app version for push", e)
            return false
        }
    }

    companion object {
        /**
         * Key to retrieve the max version for a push
         */
        private const val INTENT_MAX_VERSION_KEY = "maxVersion"

        /**
         * Key to retrieve the max version for a push
         */
        private const val INTENT_MIN_VERSION_KEY = "minVersion"

        /**
         * Key to retrieve the daily reminder key for a push
         */
        const val DAILY_REMINDER_KEY = "daily"

        /**
         * Key to retrieve the monthly reminder key for a push
         */
        const val WEEKLY_REMINDER_KEY = "weekly"

        /**
         * Key to retrieve the multiple accounts key for a push
         */
        const val MULTIPLE_ACCOUNT_KEY = "multi_accounts"

        /**
         * Package to download
         */
        const val PACKAGE_TO_DOWNLOAD = "KeyPackageToDownload"

        /**
         * Action triggered to download
         */
        const val ACTION_TRIGGER_DOWNLOAD = "DownloadTrigger"

        /**
         * Action triggered to download
         */
        const val NOTIFICATION_ID_NEW_FEATURES = 124124124
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(notification: RemoteMessage.Notification, channel: String) {
        if (notification.body == null) return // Body is null no need to display notification
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_push)
            .setContentTitle(notification.title ?: getString(R.string.app_name_simple_budget))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(notification.body)
            ) // Multi line support
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(
            if (channel == CHANNEL_NEW_FEATURES) NOTIFICATION_ID_NEW_FEATURES else Random.nextInt(),
            notificationBuilder.build()
        )
    }

    /**
     * Set-up Batch SDK config + lifecycle
     */
    private fun setUpNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Monthly report channel
            val name = getString(R.string.setting_category_notifications_weekly_title)
            val description = getString(R.string.setting_category_notifications_monthly_message)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val monthlyReportChannel =
                NotificationChannel(CHANNEL_WEEKLY_REMINDERS, name, importance)
            monthlyReportChannel.description = description

            // Daily reminder channel
            val dailyName = getString(R.string.setting_category_notifications_daily_title)
            val dailyDescription = getString(R.string.setting_category_notifications_daily_message)
            val dailyImportance = NotificationManager.IMPORTANCE_DEFAULT

            val dailyReportChannel =
                NotificationChannel(CHANNEL_DAILY_REMINDERS, dailyName, dailyImportance)
            dailyReportChannel.description = dailyDescription

            // New features channel
            val newFeatureName = getString(R.string.setting_category_notifications_update_title)
            val newFeatureDescription =
                getString(R.string.setting_category_notifications_update_message)
            val newFeatureImportance = NotificationManager.IMPORTANCE_LOW

            val newFeatureChannel =
                NotificationChannel(CHANNEL_NEW_FEATURES, newFeatureName, newFeatureImportance)
            newFeatureChannel.description = newFeatureDescription

            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(newFeatureChannel)
                notificationManager.createNotificationChannel(monthlyReportChannel)
                notificationManager.createNotificationChannel(dailyReportChannel)
            }
        }
    }
}
