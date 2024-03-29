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
package com.simplebudget.view.premium

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingClient
import com.simplebudget.iab.PremiumPurchaseFlowResult
import com.simplebudget.R
import com.simplebudget.databinding.ActivityPremiumBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.RedeemPromo
import com.simplebudget.iab.InAppProductsAdapter
import com.simplebudget.iab.PremiumFlowStatus
import com.simplebudget.iab.SKU_SUBSCRIPTION
import com.simplebudget.view.settings.webview.WebViewActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity that contains the premium screen. This activity should return with a
 * [Activity.RESULT_OK] if user has successfully purchased premium.
 *
 */
class PremiumActivity : BaseActivity<ActivityPremiumBinding>() {

    private val viewModel: PremiumViewModel by viewModel()

    override fun createBinding(): ActivityPremiumBinding {
        return ActivityPremiumBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var loadingProgressDialog: ProgressDialog? = null
        viewModel.premiumFlowErrorEventStream.observe(this) { status ->
            when (status) {
                PremiumPurchaseFlowResult.Cancelled -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                }

                is PremiumPurchaseFlowResult.Error -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null

                    AlertDialog.Builder(this).setTitle(R.string.oops)
                        .setMessage(getString(R.string.iab_purchase_error_message, status.reason))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                else -> {}
            }
        }

        viewModel.premiumFlowStatusLiveData.observe(this) { status ->
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
                        true,
                        false
                    )
                }

                PremiumFlowStatus.DONE -> {
                    loadingProgressDialog?.dismiss()
                    loadingProgressDialog = null
                    //Update Premium Status So That Ad can't be displayed
                    startActivity(
                        Intent(this, PremiumSuccessActivity::class.java).putExtra(
                            PremiumSuccessActivity.REQUEST_CODE_IS_BACK_ENABLED, false
                        )
                    )
                    finish()
                }

                null -> {
                    PremiumSuccessActivity
                }
            }
        }
        viewModel.liveDataProductDetails.observe(this) { productList ->
            productList?.let { products ->
                binding.recyclerViewInAppProducts.layoutManager = LinearLayoutManager(this)
                val adapter = InAppProductsAdapter(products) { selectedProduct ->
                    viewModel.setSelectedProduct(selectedProduct)
                }
                binding.recyclerViewInAppProducts.adapter = adapter
                val dividerItemDecoration =
                    DividerItemDecoration(
                        this,
                        LinearLayout.VERTICAL
                    )
                binding.recyclerViewInAppProducts.addItemDecoration(dividerItemDecoration)
            }
        }

        viewModel.liveDataSelectedProduct.observe(this) { selectedProduct ->
            selectedProduct?.let {
                if (selectedProduct.productType == BillingClient.ProductType.INAPP) {
                    binding.buttonBuy.text = getString(R.string.buy_permanently)
                    binding.screenTitle.text = getString(R.string.onetime_payment)
                } else {
                    binding.buttonBuy.text = getString(R.string.subscribe_monthly)
                    binding.screenTitle.text = getString(R.string.subscription_payment)
                }
            }
        }

        binding.promoCode.setOnClickListener { RedeemPromo.openPromoCodeDialog(this) }
        binding.buttonBuy.setOnClickListener {
            if (viewModel.getSelectedProduct()?.productType == BillingClient.ProductType.INAPP) {
                viewModel.onBuyInAppPremiumClicked(this)
            } else {
                viewModel.onBuySubscriptionPremiumClicked(
                    this,
                    viewModel.getSelectedProduct()?.productId ?: SKU_SUBSCRIPTION
                )
            }
        }
        binding.whatPeopleSay.setOnClickListener {
            WebViewActivity.start(this, getString(R.string.simple_budget_reviews_url))
        }
    }
}
