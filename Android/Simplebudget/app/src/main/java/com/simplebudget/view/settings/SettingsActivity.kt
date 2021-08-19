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
package com.simplebudget.view.settings

import android.os.Bundle
import android.view.MenuItem
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import com.simplebudget.R
import com.simplebudget.helper.BaseActivity
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Activity that displays settings using the [PreferencesFragment]
 *
 * @author Benoit LETONDOR
 */
class SettingsActivity : BaseActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    /**
     * showInterstitial
     */
     fun showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            // The interstitial ad wasn't ready yet
            loadInterstitial()
        }
    }

    /**
     * loadInterstitial
     */
     fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            getString(R.string.interstitial_ad_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                    mAdIsLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // Ad was loaded
                    mInterstitialAd = interstitialAd
                    mAdIsLoading = false
                    listenInterstitialAds()
                }
            })
    }

    /**
     *
     */
    private fun listenInterstitialAds() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
            }

            override fun onAdShowedFullScreenContent() {
                // Ad showed fullscreen content
                mInterstitialAd = null
            }
        }
    }


    companion object {
        /**
         * Key to specify that the backup options should be shown to the user
         */
        const val SHOW_BACKUP_INTENT_KEY = "showBackup"
    }

}
