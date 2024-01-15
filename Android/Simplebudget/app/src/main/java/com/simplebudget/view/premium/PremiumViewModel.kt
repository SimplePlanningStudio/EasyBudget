/*
 *   Copyright 2024 Benoit LETONDOR
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.simplebudget.iab.Iab
import com.simplebudget.iab.PremiumPurchaseFlowResult
import com.simplebudget.helper.SingleLiveEvent
import com.simplebudget.iab.PremiumFlowStatus
import kotlinx.coroutines.launch

class PremiumViewModel(private val iab: Iab) : ViewModel() {
    val premiumFlowStatusLiveData = MutableLiveData(PremiumFlowStatus.NOT_STARTED)
    val liveDataProductDetails = MutableLiveData(emptyList<ProductDetails>())
    val premiumFlowErrorEventStream = SingleLiveEvent<PremiumPurchaseFlowResult>()
    val liveDataSelectedProduct = SingleLiveEvent<ProductDetails?>()

    fun getSelectedProduct(): ProductDetails? {
        return liveDataSelectedProduct.value
    }

    fun setSelectedProduct(selectedProduct: ProductDetails?) {
        liveDataSelectedProduct.value = selectedProduct
    }

    fun onBuyInAppPremiumClicked(activity: Activity) {
        premiumFlowStatusLiveData.value = PremiumFlowStatus.LOADING

        viewModelScope.launch {
            when (val result = iab.launchPremiumPurchaseFlow(activity)) {
                PremiumPurchaseFlowResult.Cancelled -> {
                    premiumFlowErrorEventStream.value = result
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.NOT_STARTED
                }
                PremiumPurchaseFlowResult.Success -> {
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.DONE
                }
                is PremiumPurchaseFlowResult.Error -> {
                    premiumFlowErrorEventStream.value = result
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.NOT_STARTED
                }
            }
        }
    }

    fun onBuySubscriptionPremiumClicked(activity: Activity, productId: String) {
        premiumFlowStatusLiveData.value = PremiumFlowStatus.LOADING

        viewModelScope.launch {
            when (val result = iab.launchPremiumPurchaseSubscriptionFlow(activity, productId)) {
                PremiumPurchaseFlowResult.Cancelled -> {
                    premiumFlowErrorEventStream.value = result
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.NOT_STARTED
                }
                PremiumPurchaseFlowResult.Success -> {
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.DONE
                }
                is PremiumPurchaseFlowResult.Error -> {
                    premiumFlowErrorEventStream.value = result
                    premiumFlowStatusLiveData.value = PremiumFlowStatus.NOT_STARTED
                }
            }
        }
    }

    private fun onQueryProductDetails() {
        viewModelScope.launch {
            iab.queryProductDetails().collect { productList ->
                // handle the unified list of all products, subscription and in-app
                if (productList.isNotEmpty()) {
                    liveDataProductDetails.value = productList
                    setSelectedProduct(productList.first())
                }
            }
        }
    }


    init {
        onQueryProductDetails()
    }

}