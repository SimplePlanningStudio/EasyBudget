/*
 *   Copyright 2025 Waheed Nazir
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
package com.simplebudget.helper.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.simplebudget.R
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.InternetUtils
import com.simplebudget.helper.Logger
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.helper.extensions.beInvisible
import com.simplebudget.helper.extensions.beVisible

/**
 * Banner ad helper to load and destroy banner ad for Fragments and Activities
 * @author Waheed Nazir
 */
object AdViewHelper {

    /**
     * Load banner ad.
     * @param isPremium premium status
     * @param context context of fragment or activity
     * @param container ad view container
     *
     * For Fragment use:
     * - Use [loadBanner] to load this banner ad for Fragment
     *
     * For Activity use:
     * - Use [android.app.Activity.loadBanner] to load this ad for Activity
     */
    fun loadBannerAd(
        isPremium: Boolean,
        context: Context,
        container: ViewGroup,
        onBannerAdRequested: (AdView?) -> Unit,
    ) {
        try {
            var adView: AdView? = null
            if (isPremium.not() && InternetUtils.isInternetAvailable(context)) {
                AdSdkManager.initialize(context) {
                    //Load banner ad
                    container.beVisible()
                    val adSize = AdSizeUtils.getAdSize(
                        context, (context as Activity).windowManager.defaultDisplay
                    )
                    adView = AdView(context).apply {
                        adUnitId = context.getString(R.string.banner_ad_unit_id)
                        setAdSize(adSize)
                        adListener = object : AdListener() {
                            override fun onAdClosed() {
                                loadBannerAd(isPremium, context, container, onBannerAdRequested)
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Logger.warning("BannerAd", "Ad failed: ${error.message}")
                            }
                        }
                    }
                    container.addView(adView)
                    adView.loadAd(AdRequest.Builder().build())
                }
            } else {
                container.beInvisible()
            }
        } catch (e: Exception) {
            Logger.error(context.getString(R.string.error_while_displaying_banner_ad), e)
        }
    }
}
