package com.simplebudget.notif

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import com.simplebudget.R
import com.simplebudget.push.MyFirebaseMessagingService.Companion.DAILY_REMINDER_NOTIFICATION_ID
import com.simplebudget.view.main.MainActivity
import kotlin.random.Random

object DailyReminderNotification {
    fun send(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            androidx.core.app.NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDERS)
                .setSmallIcon(R.drawable.ic_push)
                .setContentTitle(context.getString(R.string.notification_add_expenses_title))
                .setStyle(
                    androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notification_add_expenses_description))
                ) // Multi line support
                .setContentText(context.getString(R.string.notification_add_expenses_description))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(DAILY_REMINDER_NOTIFICATION_ID, notificationBuilder.build())
    }
}