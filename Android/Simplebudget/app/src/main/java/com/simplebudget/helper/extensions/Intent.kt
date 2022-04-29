package com.simplebudget.helper.extensions

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
fun Intent.getTelegramIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$TELEGRAM_SUPPORT_PAGE_ID"))
}