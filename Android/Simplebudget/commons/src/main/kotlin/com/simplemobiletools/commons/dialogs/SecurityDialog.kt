package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.adapters.PasswordTypesAdapter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PROTECTION_PIN
import com.simplemobiletools.commons.helpers.SHOW_PIN
import com.simplemobiletools.commons.interfaces.HashListener
import com.simplemobiletools.commons.views.MyDialogViewPager
import kotlinx.android.synthetic.main.dialog_security.view.*
import kotlinx.android.synthetic.main.dialog_security.view.dialog_scrollview

class SecurityDialog(
    val activity: Activity,
    val requiredHash: String,
    val showTabIndex: Int,
    val callback: (hash: String, type: Int, success: Boolean) -> Unit
) : HashListener {
    var dialog: AlertDialog? = null
    val view = LayoutInflater.from(activity).inflate(R.layout.dialog_security, null)
    var tabsAdapter: PasswordTypesAdapter
    var viewPager: MyDialogViewPager

    init {
        view.apply {
            viewPager = findViewById(R.id.dialog_tab_view_pager)
            viewPager.offscreenPageLimit = 1
            tabsAdapter =
                PasswordTypesAdapter(context, requiredHash, this@SecurityDialog, dialog_scrollview)
            viewPager.adapter = tabsAdapter

            viewPager.onGlobalLayout {
                updateTabVisibility()
            }

            if (showTabIndex == SHOW_PIN) {
                viewPager.currentItem = PROTECTION_PIN
                updateTabVisibility()
            } else {
                viewPager.currentItem = showTabIndex
                viewPager.allowSwiping = false
            }
        }

        dialog = AlertDialog.Builder(activity)
            .setOnCancelListener { onCancelFail() }
            .setNegativeButton(R.string.cancel) { dialog, which -> onCancelFail() }
            .create().apply {
                activity.setupDialogStuff(view, this)
            }

    }

    private fun onCancelFail() {
        callback("", 0, false)
        dialog!!.dismiss()
    }

    override fun receivedHash(hash: String, type: Int) {
        callback(hash, type, true)
        if (!activity.isFinishing) {
            dialog?.dismiss()
        }
    }

    private fun updateTabVisibility() {
        tabsAdapter.isTabVisible(0, viewPager.currentItem == 0)

    }
}
