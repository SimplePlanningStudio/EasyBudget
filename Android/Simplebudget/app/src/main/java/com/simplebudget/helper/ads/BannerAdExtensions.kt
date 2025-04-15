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
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdView

/**
 * Load banner extension for fragment
 *  - Calling [AdViewHelper.loadBannerAd] from [Fragment]
 *  @param isPremium premium status of user
 *  @param container ad view container for banner
 */
fun Fragment.loadBanner(
    isPremium: Boolean,
    container: ViewGroup,
    onBannerAdRequested: (AdView?) -> Unit,
) {
    context?.let { AdViewHelper.loadBannerAd(isPremium, it, container, onBannerAdRequested) }
}

/**
 * Load banner extension for Activity
 * - Calling [AdViewHelper.loadBannerAd] from [Activity]
 *  @param isPremium premium status of user
 *  @param container ad view container for banner
 */
fun Activity.loadBanner(
    isPremium: Boolean,
    container: ViewGroup,
    onBannerAdRequested: (AdView?) -> Unit,
) {
    AdViewHelper.loadBannerAd(isPremium, this, container, onBannerAdRequested)
}

/**
 * Destroy banner extension for Fragment
 */
fun Fragment.destroyBanner(adView: AdView?) {
    adView?.destroy()
}

/**
 * Destroy banner extension for Activity
 */
fun Activity.destroyBanner(adView: AdView?) {
    adView?.destroy()
}

/**
 * Resume banner extension for Fragment
 */
fun Fragment.resumeBanner(adView: AdView?) {
    adView?.resume()
}

/**
 * Resume banner extension for Activity
 */
fun Activity.resumeBanner(adView: AdView?) {
    adView?.resume()
}

/**
 * Pause banner extension for Fragment
 */
fun Fragment.pauseBanner(adView: AdView?) {
    adView?.pause()
}

/**
 * Pause banner extension for Activity
 */
fun Activity.pauseBanner(adView: AdView?) {
    adView?.pause()
}