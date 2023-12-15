package com.simplebudget.helper

import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog

/**
 * The type Dialog util.
 * Common Dialog Alert with Title, Message, Positive & Negative callback listener to be used
 */
object DialogUtil {
    fun createDialog(
        context: Context,
        title: String? = null,
        message: String,
        positiveBtn: String,
        negativeBtn: String,
        isCancelable: Boolean,
        positiveClickListener: () -> Unit,
        negativeClickListener: () -> Unit
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        if (title != null) builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveBtn) { dialog, _ ->
            dialog.dismiss()
            positiveClickListener()
        }
        if (!TextUtils.isEmpty(negativeBtn)) {
            builder.setNegativeButton(negativeBtn) { dialog, _ ->
                dialog.dismiss()
                negativeClickListener()
            }
        }
        builder.setCancelable(isCancelable)
        return builder.create()
    }
}