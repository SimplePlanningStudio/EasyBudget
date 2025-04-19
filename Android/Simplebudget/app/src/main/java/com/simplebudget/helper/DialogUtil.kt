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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.simplebudget.R
import kotlin.String

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
        neutralButton: String = "",
        isCancelable: Boolean,
        positiveClickListener: () -> Unit,
        negativeClickListener: () -> Unit,
        neutralClickListener: (() -> Unit)? = null,

        ): Dialog? {
        // ❗ Check if context is an Activity and is finishing or destroyed
        if ((context is Activity && (context.isFinishing || context.isDestroyed)) ||
            (context is FragmentActivity && context.supportFragmentManager.isDestroyed)
        ) {
            return null
        }

        val builder = AlertDialog.Builder(context)
        if (!title.isNullOrEmpty()) {
            builder.setTitle(title)
        }
        builder.setMessage(message).setPositiveButton(positiveBtn) { dialog, _ ->
            dialog.dismiss()
            positiveClickListener.invoke()
        }
        if (!TextUtils.isEmpty(negativeBtn)) {
            builder.setNegativeButton(negativeBtn) { dialog, _ ->
                dialog.dismiss()
                negativeClickListener.invoke()
            }
        }
        if (!TextUtils.isEmpty(neutralButton)) {
            builder.setNeutralButton(neutralButton) { dialog, _ ->
                dialog.dismiss()
                neutralClickListener?.invoke()
            }
        }
        builder.setCancelable(isCancelable)
        return builder.create()
    }

    /**
     * Simplified dialog for the message only
     */
    fun errorDialog(
        context: Context, message: String,
    ): Dialog? {
        return createDialog(
            context = context,
            message = message,
            positiveBtn = context.getString(R.string.ok),
            negativeBtn = "",
            isCancelable = false,
            positiveClickListener = {},
            negativeClickListener = {},
        )
    }
}
