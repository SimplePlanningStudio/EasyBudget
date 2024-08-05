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

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.simplebudget.R
import java.net.URLEncoder

object RedeemPromo {

    fun openPromoCodeDialog(context: Activity?) {
        context?.let { activity ->
            val dialogView =
                activity.layoutInflater.inflate(R.layout.dialog_redeem_voucher, null)
            val voucherEditText = dialogView.findViewById<View>(R.id.voucher) as EditText

            val builder = AlertDialog.Builder(activity)
                .setTitle(R.string.voucher_redeem_dialog_title)
                .setMessage(R.string.voucher_redeem_dialog_message)
                .setView(dialogView)
                .setPositiveButton(R.string.voucher_redeem_dialog_cta) { dialog, _ ->
                    dialog.dismiss()

                    val promocode = voucherEditText.text.toString()
                    if (promocode.trim { it <= ' ' }.isEmpty()) {
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.oops)
                            .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                            .setPositiveButton(R.string.ok) { dialog12, _ -> dialog12.dismiss() }
                            .show()
                    }

                    if (!launch(activity, promocode)) {
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.oops)
                            .setMessage(
                                activity.getString(
                                    R.string.iab_purchase_error_message,
                                    "Error redeeming promo code"
                                )
                            )
                            .setPositiveButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                            .show()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }

            val dialog = builder.show()

            // Directly show keyboard when the dialog pops
            voucherEditText.onFocusChangeListener =
                View.OnFocusChangeListener { _, hasFocus ->
                    // Check if the device doesn't have a physical keyboard
                    if (hasFocus && activity.resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    }
                }
        }
    }

    /**
     * Launch the redeem promoCode flow
     * @param activity the activity
     * @param promoCode the promoCode to redeem
     */
    private fun launch(activity: Activity?, promoCode: String): Boolean {
        return try {
            val url = "https://play.google.com/redeem?code=" + URLEncoder.encode(promoCode, "UTF-8")
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (e: Exception) {
            Logger.error(false, "Error while redeeming promo code", e)
            false
        }
    }
}