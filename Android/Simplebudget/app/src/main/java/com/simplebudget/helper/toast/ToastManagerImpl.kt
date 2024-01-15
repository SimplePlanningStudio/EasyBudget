package com.simplebudget.helper.toast

import android.content.Context
import android.widget.Toast

class ToastManagerImpl(private val context: Context) : ToastManager {

    override fun showShort(message: String?) {
        message?.let {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun showLong(message: String?) {
        message?.let {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
