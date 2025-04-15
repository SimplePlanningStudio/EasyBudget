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
package com.simplebudget.view.reset

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityResetAppDataBinding
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.iab.isUserPremium
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.main.MainActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Delete all app data and restart the app
 *
 * @author Waheed Nazir
 */
class ResetAppDataActivity : BaseActivity<ActivityResetAppDataBinding>() {

    private val viewModel: ResetAppDataViewModel by viewModel()
    private val analyticsManager: AnalyticsManager by inject()
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null

    override fun createBinding(): ActivityResetAppDataBinding =
        ActivityResetAppDataBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_RESET_APP_SCREEN)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.btnProceedWithReset.setOnClickListener {
            resetConfirmation()
        }

        viewModel.progress.observe(this) {
            it?.let {
                binding.progress.visibility = if (it) View.VISIBLE else View.GONE
                binding.btnProceedWithReset.isClickable = it
            }
        }

        viewModel.clearDataEventStream.observe(this) {
            analyticsManager.logEvent(Events.KEY_SETTINGS_RESET_APP_DONE)
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        //Show Banner Ad
        loadBanner(
            appPreferences.isUserPremium(),
            binding.adViewContainer,
            onBannerAdRequested = { bannerAdView ->
                this.adView = bannerAdView
            }
        )
    }


    /**
     * Reset confirmation
     */
    private fun resetConfirmation() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder
            .setTitle("Final Reset Confirmation")
            .setMessage("Are you sure you want to reset?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.setPositiveButton("Reset") { dialog, _ ->
                viewModel.clearAppData()
                dialog.cancel()
            }
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(resources.getColor(R.color.budget_red))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(resources.getColor(R.color.budget_green))
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
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
