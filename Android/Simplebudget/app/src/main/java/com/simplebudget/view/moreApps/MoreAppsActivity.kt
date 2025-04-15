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
package com.simplebudget.view.moreApps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import com.simplebudget.R
import com.simplebudget.databinding.ActivityMoreAppsBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import org.koin.android.ext.android.inject
import androidx.core.net.toUri
import com.google.android.gms.ads.AdView
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.iab.isUserPremium
import com.simplebudget.prefs.AppPreferences

class MoreAppsActivity : BaseActivity<ActivityMoreAppsBinding>(),
    MoreAppsFragment.OnListFragmentInteractionListener {

    private val analyticsManager: AnalyticsManager by inject()
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null


    override fun createBinding(): ActivityMoreAppsBinding {
        return ActivityMoreAppsBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_MORE_APPS_SCREEN)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.fragment_container, MoreAppsFragment.newInstance(1))
        fragmentTransaction.addToBackStack("MoreAppsFragment")
        fragmentTransaction.commit()
        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
        /**
         * Banner ads
         */
        loadBanner(
            appPreferences.isUserPremium(),
            binding.adViewContainer,
            onBannerAdRequested = { bannerAdView ->
                this.adView = bannerAdView
            }
        )
    }

    /**
     * Handle back pressed
     */
    private fun handleBackPressed() {
        finish()
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            handleBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     *
     */
    override fun onListFragmentInteraction(item: AppModel.AppItem) {
        try {
            item.title?.let {
                analyticsManager.logEvent(
                    Events.KEY_MORE_APPS, mapOf(
                        Events.KEY_MORE_APPS_APP_CLICKED to it
                    )
                )
            }
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    (MoreApps.PLAY_STORE_BASE_LINK + item.storeLink).toUri()
                )
            )
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    (MoreApps.PLAY_STORE_BASE_LINK + item.storeLink).toUri()
                )
            )
        }
    }

    /**
     * Leaving the activity
     */
    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    /**
     * Opening the activity
     */
    override fun onResume() {
        resumeBanner(adView)
        super.onResume()
    }

    /**
     * Destroying the activity
     */
    override fun onDestroy() {
        destroyBanner(adView)
        adView = null
        super.onDestroy()
    }
}