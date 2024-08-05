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