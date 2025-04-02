/*
 *   Copyright 2025 Benoit LETONDOR
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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import com.simplebudget.R
import com.simplebudget.databinding.ActivitySettingsBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.SHOW_PIN
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.prefs.*
import com.simplebudget.view.reset.ResetAppDataActivity
import com.simplebudget.view.security.SecurityActivity
import org.koin.android.ext.android.inject

/**
 * Activity that displays settings using the [PreferencesFragment]
 *
 * @author Benoit LETONDOR
 */
class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading = false
    private val appPreferences: AppPreferences by inject()
    private lateinit var preferencesFragment: PreferencesFragment
    private val analyticsManager: AnalyticsManager by inject()


    override fun createBinding(): ActivitySettingsBinding =
        ActivitySettingsBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_SETTINGS_SCREEN)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferencesFragment =
            supportFragmentManager.findFragmentById(R.id.preferencesFragment) as PreferencesFragment
    }

    /**
     *
     */
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

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
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


    /**
     * Launch Security Activity
     */
    fun handleAppPasswordProtection() {
        val intent = Intent(this, SecurityActivity::class.java)
            .putExtra("HASH", appPreferences.appPasswordHash())
            .putExtra("TAB_INDEX", appPreferences.appProtectionType())
            .putExtra(SecurityActivity.REQUEST_CODE_SECURITY_TYPE, SecurityActivity.SET_PIN)
        securityActivityLauncher.launch(intent)
    }

    /**
     * Start activity for result
     */
    var securityActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {

                preferencesFragment.updateAppPasswordProtectionLabel()

            } else if (result.resultCode == Activity.RESULT_OK) {

                val hasPasswordProtection = appPreferences.isAppPasswordProtectionOn()
                appPreferences.setAppPasswordProtectionOn(!hasPasswordProtection)
                appPreferences.setAppPasswordHash(
                    (if (hasPasswordProtection) "" else result?.data?.getStringExtra(
                        "HASH"
                    ) ?: "")
                )
                appPreferences.setHiddenProtectionType(SHOW_PIN)

                if (!hasPasswordProtection) {
                    preferencesFragment.displayPasswordProtectionDisclaimer()
                }

                preferencesFragment.updateAppPasswordProtectionLabel()
            }
        }

}
