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
package com.simplebudget.helper.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.simplebudget.R
import com.simplebudget.helper.toast.ToastManager


const val TELEGRAM_SUPPORT_PAGE_ID = "SimpleBudgetSupport"
const val WHATS_APP_CHANNEL = "https://whatsapp.com/channel/0029VaIvpNp8F2pFBZOKLX28"
const val YOUTUBE_CHANNEL = "https://www.youtube.com/channel/UC2HO7gZWI7rFM9da75zFWqg"

/**
 *
 */
fun Intent.addExtrasForDownloadCampaign(packageId: String): Intent {
    this.putExtra("package", packageId)
    return this
}

/**
 * Telegram channel page
 */
fun Intent.openTelegramChannel(context: Context, toastManager: ToastManager) {
    try {
        try {
            context.packageManager.getPackageInfo(
                "org.telegram.messenger", 0
            )//Check for Telegram Messenger App
        } catch (e: Exception) {
            context.packageManager.getPackageInfo(
                "org.thunderdog.challegram", 0
            )//Check for Telegram X App
        }
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("tg://resolve?domain=${TELEGRAM_SUPPORT_PAGE_ID}")
            )
        )
    } catch (e: Exception) {
        //App not found open in browser
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://www.telegram.me/$TELEGRAM_SUPPORT_PAGE_ID")
            )
        )
    }
}

/**
 * Whats app channel page
 */
fun Intent.openWhatsAppChannel(context: Context, toastManager: ToastManager) {
    val uri = Uri.parse(WHATS_APP_CHANNEL)
    return try {
        val intent = Intent("android.intent.action.MAIN")
        intent.action = Intent.ACTION_VIEW
        intent.setPackage("com.whatsapp")
        intent.data = uri
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/**
 * Youtube channel page
 */
fun Intent.openYoutubeChannel(context: Context, toastManager: ToastManager) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_CHANNEL))
    // Set the package name to the YouTube app
    intent.setPackage("com.google.android.youtube")
    // Check if the YouTube app is installed
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // If the YouTube app is not installed, open the channel in a web browser
        intent.data = Uri.parse(YOUTUBE_CHANNEL)
        context.startActivity(intent)
    }
}