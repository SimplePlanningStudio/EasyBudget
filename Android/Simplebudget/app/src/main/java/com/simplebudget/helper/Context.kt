package com.simplebudget.helper

import android.content.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplebudget.view.main.MainActivity

val Context.statusBarHeight: Int
    get() {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

val Context.actionBarHeight: Int
    get() {
        val styledAttributes =
            theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = styledAttributes.getDimension(0, 0f)
        styledAttributes.recycle()
        return actionBarHeight.toInt()
    }

fun Context.updateAccountNotifyBroadcast() {
    val intent = Intent(MainActivity.INTENT_ACCOUNT_TYPE_UPDATED)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

fun Context.editAccountNotifyBroadcast() {
    val intent = Intent(MainActivity.INTENT_ACCOUNT_TYPE_EDITED)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}
