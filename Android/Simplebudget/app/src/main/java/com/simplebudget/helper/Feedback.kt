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