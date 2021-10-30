/*
 *   Copyright 2021 Benoit LETONDOR
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
package com.simplebudget.view.premium

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.simplebudget.iab.PremiumPurchaseFlowResult
import com.simplebudget.SimpleBudget
import com.simplebudget.R
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.RedeemPromo
import kotlinx.android.synthetic.main.activity_premium.*
import org.koin.android.viewmodel.ext.android.viewModel

/**
 * Activity that contains the premium onboarding screen. This activity should return with a
 * [Activity.RESULT_OK] if user has successfully purchased premium.
 *
 */
class PremiumActivity : BaseActivity() {

    private val viewModel: PremiumViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        // Cancelled by default
        setResult(Activity.RESULT_CANCELED)

        premium_not_now_button.setOnClickListener {
            finish()
        }

        premium_cta_button.setOnClickListener {
            viewModel.onBuyPremiumClicked(this)
        }

        tv_promo_code.setOnClickListener {
            RedeemPromo.openPromoCodeDialog(this)
        }

        var loadingProgressDialog: ProgressDialog? = null
        viewModel.premiumFlowErrorEventStream.observe(this, Observer { status ->
            when (status) {
                PremiumPurchaseFlowResult.Cancelled -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                }
                is PremiumPurchaseFlowResult.Error -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null

                    AlertDialog.Builder(this)
                        .setTitle(R.string.iab_purchase_error_title)
                        .setMessage(getString(R.string.iab_purchase_error_message, status.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        })

        viewModel.premiumFlowStatusLiveData.observe(this, { status ->
            when (status) {
                PremiumFlowStatus.NOT_STARTED -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                }
                PremiumFlowStatus.LOADING -> {
                    loadingProgressDialog = ProgressDialog.show(
                        this@PremiumActivity,
                        resources.getString(R.string.iab_purchase_wait_title),
                        resources.getString(R.string.iab_purchase_wait_message),
                        true, false
                    )
                }
                PremiumFlowStatus.DONE -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                    //Update Premium Status So That Can't be displayed
                    SimpleBudget.appOpenManager?.updatePremiumStatus(true)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                null -> {
                }
            }
        })
    }

}
