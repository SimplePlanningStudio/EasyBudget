/*
 *   Copyright 2025 Waheed Nazir
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

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

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