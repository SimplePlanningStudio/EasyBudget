package com.simplebudget.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.simplebudget.R


object Feedback {
    fun askForFeedback(context: Context) {
        val uri = Uri.parse("mailto:${context.resources.getString(R.string.rating_feedback_email)}")
            .buildUpon()
            .appendQueryParameter(
                "subject",
                context.resources.getString(R.string.rating_feedback_send_subject)
            )
            .appendQueryParameter("body", "")
            .build()
        val intentChooserTitle: CharSequence =
            context.resources.getString(R.string.rating_feedback_send_subject)
        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        context.startActivity(Intent.createChooser(emailIntent, intentChooserTitle))
    }
}