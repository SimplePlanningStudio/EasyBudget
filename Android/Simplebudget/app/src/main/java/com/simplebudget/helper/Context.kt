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
