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
package com.simplebudget.view.category.manage

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.simplebudget.R
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.model.category.Category

object EditCategoryDialog {
    fun open(
        context: Activity?,
        category: Category,
        updateCategory: (newCategoryName: String) -> Unit,
        toastManager: ToastManager
    ) {
        context?.let { activity ->
            val dialogView =
                activity.layoutInflater.inflate(R.layout.dialog_edit_category, null)
            val categoryEditText = dialogView.findViewById<View>(R.id.categoryEditText) as EditText
            categoryEditText.hint = category.name

            val builder = AlertDialog.Builder(activity)
                .setTitle("Edit category '${category.name}'")
                .setMessage(R.string.edit_category_note)
                .setView(dialogView)
                .setPositiveButton(R.string.save) { dialog, _ ->
                    val newCategoryName = categoryEditText.text.toString()
                    if (newCategoryName.trim { it <= ' ' }.isEmpty()) {
                        toastManager.showShort(context.getString(R.string.category_cant_be_empty))
                    } else {
                        updateCategory.invoke(newCategoryName.uppercase())
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

            val dialog = builder.show()
            // Directly show keyboard when the dialog pops
            categoryEditText.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus ->
                    // Check if the device doesn't have a physical keyboard
                    if (hasFocus && activity.resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    }
                }
        }
    }
}