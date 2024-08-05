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
package com.simplebudget.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.simplebudget.R


object Feedback {
    fun askForFeedback(context: Context) {
        try {
            val emailIntent = Intent(
                Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", context.resources.getString(R.string.bug_report_email), null
                )
            )
            val intentChooserTitle: CharSequence =
                context.resources.getString(R.string.rating_feedback_send_subject)
            emailIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                context.resources.getString(R.string.rating_feedback_send_subject)
            )
            emailIntent.putExtra(
                Intent.EXTRA_EMAIL,
                arrayListOf(context.resources.getString(R.string.bug_report_email))
            )
            emailIntent.putExtra(Intent.EXTRA_TEXT, "")
            context.startActivity(Intent.createChooser(emailIntent, intentChooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}