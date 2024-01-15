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
package com.simplebudget.iab

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.*
import com.simplebudget.helper.Logger
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.main.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * SKU premium, lifetime, subscription
 */
const val SKU_PREMIUM_LEGACY = "premium"

/**
 * Monthly payments
 */
const val SKU_SUBSCRIPTION = "simple_budget_membership"

/**
 * Cache storage of the IAB status
 */
const val PREMIUM_PARAMETER_KEY = "premium"


class IabImpl(
    context: Context,
    private val appPreferences: AppPreferences,
) : Iab, PurchasesUpdatedListener, BillingClientStateListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingPurchaseEventMutableFlow = MutableSharedFlow<PremiumPurchaseFlowResult>()

    private var queryPurchasesJob: Job? = null

    private val appContext = context.applicationContext
    private val billingClient =
        BillingClient.newBuilder(appContext).setListener(this).enablePendingPurchases().build()

    /**
     * iab check status
     */
    private var iabStatus: PremiumCheckStatus = PremiumCheckStatus.INITIALIZING

    init {
        startBillingClient()
    }

    private fun startBillingClient() {
        try {
            setIabStatusAndNotify(PremiumCheckStatus.INITIALIZING)
            billingClient.startConnection(this)
        } catch (e: Exception) {
            Logger.error("Error while checking iab status", e)
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
        }
    }

    /**
     * Set the new iab status and notify the app by sending an [.INTENT_IAB_STATUS_CHANGED] intent
     *
     * @param status the new status
     */
    private fun setIabStatusAndNotify(status: PremiumCheckStatus) {
        iabStatus = status

        // Save status only on success
        if (status == PremiumCheckStatus.LEGACY_PREMIUM || status == PremiumCheckStatus.SUBSCRIBED || status == PremiumCheckStatus.NOT_PREMIUM) {
            appPreferences.setUserPremium((iabStatus == PremiumCheckStatus.LEGACY_PREMIUM) || (iabStatus == PremiumCheckStatus.SUBSCRIBED))
        }

        val intent = Intent(INTENT_IAB_STATUS_CHANGED)
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent)
    }

    override fun isIabReady(): Boolean {
        return isUserPremium() || iabStatus == PremiumCheckStatus.NOT_PREMIUM
    }

    /**
     * Is the user a premium user
     *
     * @return true if the user if premium, false otherwise
     */
    override fun isUserPremium(): Boolean {
        return appPreferences.isUserPremium() || (iabStatus == PremiumCheckStatus.LEGACY_PREMIUM || iabStatus == PremiumCheckStatus.SUBSCRIBED)
    }

    /**
     * Update the current IAP status if already checked
     */
    override fun updateIAPStatusIfNeeded() {
        Logger.debug("updateIAPStatusIfNeeded: $iabStatus")
        if (iabStatus == PremiumCheckStatus.NOT_PREMIUM) {
            setIabStatusAndNotify(PremiumCheckStatus.CHECKING)
            queryPurchases()
        } else if (iabStatus == PremiumCheckStatus.ERROR) {
            startBillingClient()
        }
    }

    override suspend fun launchPremiumPurchaseFlow(activity: Activity): PremiumPurchaseFlowResult {
        if (iabStatus != PremiumCheckStatus.NOT_PREMIUM) {
            return when (iabStatus) {
                PremiumCheckStatus.ERROR -> PremiumPurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
                PremiumCheckStatus.LEGACY_PREMIUM, PremiumCheckStatus.SUBSCRIBED -> PremiumPurchaseFlowResult.Error(
                    "You already bought Premium with that Google account. Restart the app if you don't have access to premium features."
                )
                else -> PremiumPurchaseFlowResult.Error("Runtime error: $iabStatus")
            }
        }

        val skuList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(SKU_PREMIUM_LEGACY)
                .setProductType(BillingClient.ProductType.INAPP).build()
        )

        val (billingResult, skuDetailsList) = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(skuList).build()
        )

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                setIabStatusAndNotify(PremiumCheckStatus.LEGACY_PREMIUM)
                return PremiumPurchaseFlowResult.Success
            }
            return PremiumPurchaseFlowResult.Error("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
        }

        if (skuDetailsList == null || skuDetailsList.isEmpty()) {
            return PremiumPurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: skuDetailsList is empty). Please restart the app and try again")
        }

        val product = skuDetailsList.first()

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()
        )

        val billingFlowParams =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList)
                .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)

        return pendingPurchaseEventMutableFlow.first()
    }

    override suspend fun launchPremiumPurchaseSubscriptionFlow(
        activity: Activity,
        productId: String
    ): PremiumPurchaseFlowResult {
        if (iabStatus != PremiumCheckStatus.NOT_PREMIUM) {
            return when (iabStatus) {
                PremiumCheckStatus.ERROR -> PremiumPurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
                PremiumCheckStatus.LEGACY_PREMIUM, PremiumCheckStatus.SUBSCRIBED -> PremiumPurchaseFlowResult.Error(
                    "You already bought Premium with that Google account. Restart the app if you don't have access to premium features."
                )
                else -> PremiumPurchaseFlowResult.Error("Runtime error: $iabStatus")
            }
        }

        val skuList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS).build()
        )

        val (billingResult, skuDetailsList) = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(skuList).build()
        )

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                setIabStatusAndNotify(PremiumCheckStatus.SUBSCRIBED)
                return PremiumPurchaseFlowResult.Success
            }
            return PremiumPurchaseFlowResult.Error("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
        }

        if (skuDetailsList == null || skuDetailsList.isEmpty()) {
            return PremiumPurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: skuDetailsList is empty). Please restart the app and try again")
        }

        val product = skuDetailsList.first()
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return PremiumPurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: null offerToken). Please restart the app and try again")

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
                .setOfferToken(offerToken).build()
        )

        val billingFlowParams =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList)
                .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)

        return pendingPurchaseEventMutableFlow.first()
    }

    private fun queryPurchases() {
        queryPurchasesJob?.cancel()
        queryPurchasesJob = scope.launch {
            val subscribedToPremiumResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )

            // Is it a failure?
            if (subscribedToPremiumResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error("Error while querying iab inventory: " + subscribedToPremiumResult.billingResult.responseCode)
                setIabStatusAndNotify(PremiumCheckStatus.ERROR)
                return@launch
            }

            val subscribed =
                subscribedToPremiumResult.purchasesList.any { it.products.contains(SKU_SUBSCRIPTION) }

            Logger.debug("iab query inventory was successful: $subscribed")

            if (subscribed) {
                setIabStatusAndNotify(PremiumCheckStatus.SUBSCRIBED)
                return@launch
            }

            val legacyPremiumResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            )

            // Is it a failure?
            if (legacyPremiumResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error("Error while querying iab inventory: " + legacyPremiumResult.billingResult.responseCode)
                setIabStatusAndNotify(PremiumCheckStatus.ERROR)
                return@launch
            }

            val legacyPremium =
                legacyPremiumResult.purchasesList.any { it.products.contains(SKU_PREMIUM_LEGACY) }

            Logger.debug("legacy iab query inventory was successful: $legacyPremium")

            if (legacyPremium) {
                setIabStatusAndNotify(PremiumCheckStatus.LEGACY_PREMIUM)
            } else {
                setIabStatusAndNotify(PremiumCheckStatus.NOT_PREMIUM)
            }
        }
    }

    override suspend fun queryProductDetails(): Flow<List<ProductDetails>> {
        val inAppFlow = getDetailsFlow(listOf(SKU_PREMIUM_LEGACY), BillingClient.ProductType.INAPP)
        val subsFlow = getDetailsFlow(listOf(SKU_SUBSCRIPTION), BillingClient.ProductType.SUBS)

        val allDetailsFlow = inAppFlow.zip(subsFlow) { inAppResult, subsResult ->
            return@zip inAppResult + subsResult
        }
        return allDetailsFlow
    }

    private fun getDetailsFlow(productIds: List<String>, type: String): Flow<List<ProductDetails>> {
        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(type)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        return flow {
            emit(billingClient.queryProductDetails(params))
        }.map { result ->
            result.productDetailsList ?: emptyList()
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Logger.debug("iab setup finished.")

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            setIabStatusAndNotify(PremiumCheckStatus.ERROR)
            Logger.error("Error while setting-up iab: " + billingResult.responseCode)
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.CHECKING)
        queryPurchases()
    }

    override fun onBillingServiceDisconnected() {
        Logger.debug("onBillingServiceDisconnected")

        scope.launch {
            pendingPurchaseEventMutableFlow.emit(PremiumPurchaseFlowResult.Error("Lost connection with Google Play"))
        }

        setIabStatusAndNotify(PremiumCheckStatus.ERROR)
    }


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Logger.debug("Purchase finished: " + billingResult.responseCode)

        scope.launch {
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.error("Error while purchasing premium: " + billingResult.responseCode)
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> pendingPurchaseEventMutableFlow.emit(
                        PremiumPurchaseFlowResult.Cancelled
                    )
                    else -> pendingPurchaseEventMutableFlow.emit(PremiumPurchaseFlowResult.Error("An error occurred (status code: " + billingResult.responseCode + ")"))
                }

                return@launch
            }


            if (purchases.isNullOrEmpty()) {
                pendingPurchaseEventMutableFlow.emit(PremiumPurchaseFlowResult.Error("No purchased item found"))
                return@launch
            }

            Logger.debug("Purchase successful.")

            for (purchase in purchases) {
                if (purchase.products.contains(SKU_PREMIUM_LEGACY) || purchase.products.contains(
                        SKU_SUBSCRIPTION
                    )
                ) {
                    val ackResult = billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()
                    )

                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        pendingPurchaseEventMutableFlow.emit(PremiumPurchaseFlowResult.Error("Error when acknowledging purchase with Google (${ackResult.responseCode}, ${ackResult.debugMessage}). Please try again"))
                        return@launch
                    }

                    setIabStatusAndNotify(PremiumCheckStatus.SUBSCRIBED)
                    pendingPurchaseEventMutableFlow.emit(PremiumPurchaseFlowResult.Success)

                    return@launch
                }
            }

            pendingPurchaseEventMutableFlow.emit(PremiumPurchaseFlowResult.Error("No purchased item found"))
        }
    }
}

private fun AppPreferences.setUserPremium(premium: Boolean) {
    putBoolean(PREMIUM_PARAMETER_KEY, premium)
}

fun AppPreferences.isUserPremium(): Boolean {
    return getBoolean(PREMIUM_PARAMETER_KEY, false)
}

private enum class PremiumCheckStatus {
    INITIALIZING,

    CHECKING,

    ERROR,

    NOT_PREMIUM,

    LEGACY_PREMIUM,

    SUBSCRIBED,
}