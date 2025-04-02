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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object Rate {
    fun onPlayStore(context: Context) {
        val appPackageName = context.packageName
        try {
            val intent =
                Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri())

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$appPackageName".toUri()
            )
            context.startActivity(intent)
        }
    }

    /**
     *
     */
    fun openPlayStore(packageName: String, context: Context) {
        if (packageName.isEmpty()) return
        val uri = "market://details?id=$packageName".toUri()
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            context.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "http://play.google.com/store/apps/details?id=$packageName".toUri()
                )
            )
        }
    }
}