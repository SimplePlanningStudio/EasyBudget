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