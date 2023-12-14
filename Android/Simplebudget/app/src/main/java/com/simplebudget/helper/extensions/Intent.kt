package com.simplebudget.helper.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri


const val TELEGRAM_PAGE_ID = "SimpleBudget"
const val TELEGRAM_SUPPORT_PAGE_ID = "SimpleBudgetSupport"

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
fun Intent.getTelegramIntent(context: Context): Intent {
    return try {
        try {
            context.packageManager.getPackageInfo("org.telegram.messenger", 0)//Check for Telegram Messenger App
        } catch (e : Exception){
            context.packageManager.getPackageInfo("org.thunderdog.challegram", 0)//Check for Telegram X App
        }
        Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=${TELEGRAM_SUPPORT_PAGE_ID}"))
    }catch (e : Exception){ //App not found open in browser
        Intent(Intent.ACTION_VIEW, Uri.parse("http://www.telegram.me/$TELEGRAM_SUPPORT_PAGE_ID"))
    }
}