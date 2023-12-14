/*
 *   Copyright 2023 Benoit LETONDOR
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
package com.simplebudget.iab

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow

interface Iab {
    fun isIabReady(): Boolean
    fun isUserPremium(): Boolean
    fun updateIAPStatusIfNeeded()
    suspend fun launchPremiumPurchaseFlow(activity: Activity): PremiumPurchaseFlowResult
    suspend fun launchPremiumPurchaseSubscriptionFlow(
        activity: Activity,
        productId: String
    ): PremiumPurchaseFlowResult

    suspend fun queryProductDetails(): Flow<List<ProductDetails>>
}

sealed class PremiumPurchaseFlowResult {
    object Cancelled : PremiumPurchaseFlowResult()
    object Success : PremiumPurchaseFlowResult()
    class Error(val reason: String) : PremiumPurchaseFlowResult()
}

enum class PremiumFlowStatus {
    NOT_STARTED,
    LOADING,
    DONE
}

/**
 * Intent action broadcast when the status of iab changed
 */
const val INTENT_IAB_STATUS_CHANGED = "iabStatusChanged"