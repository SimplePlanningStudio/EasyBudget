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
package com.simplebudget.view.settings.aboutus

import android.os.Bundle
import android.view.MenuItem
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.databinding.ActivityAboutUsBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.iab.isUserPremium
import com.simplebudget.prefs.AppPreferences
import org.koin.android.ext.android.inject
import kotlin.getValue

/**
 * Activity that displays settings using the [HelpFragment]
 *
 * @author Waheed Nazir
 */
class AboutUsActivity : BaseActivity<ActivityAboutUsBinding>() {

    private lateinit var aboutUsFragment: AboutUsFragment
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null

    override fun createBinding(): ActivityAboutUsBinding =
        ActivityAboutUsBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        aboutUsFragment =
            supportFragmentManager.findFragmentById(R.id.aboutUsFragment) as AboutUsFragment


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