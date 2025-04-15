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
package com.simplebudget.view.settings.releaseHistory

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.databinding.ActivityReleaseHistoryTimelineBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.Logger
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.beVisible
import com.simplebudget.helper.stickytimelineview.callback.SectionCallback
import com.simplebudget.helper.stickytimelineview.model.SectionInfo
import com.simplebudget.iab.isUserPremium
import com.simplebudget.prefs.AppPreferences
import org.koin.android.ext.android.inject

class ReleaseHistoryTimelineActivity : BaseActivity<ActivityReleaseHistoryTimelineBinding>() {


    private val analyticsManager: AnalyticsManager by inject()
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null

    /**
     *
     */
    override fun createBinding(): ActivityReleaseHistoryTimelineBinding =
        ActivityReleaseHistoryTimelineBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_APP_RELEASE_SCREEN)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initVerticalRecyclerView()

        /**
         * Banner ads
         */
        loadBanner(
            appPreferences.isUserPremium(),
            binding.adViewContainer,
            onBannerAdRequested = { bannerAdView ->
                this.adView = bannerAdView
            })
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
     *
     */
    private fun initVerticalRecyclerView() {
        val singerList = getSingerList()
        binding.verticalRecyclerView.adapter = ReleaseHistoryAdapter(
            layoutInflater, singerList, R.layout.recycler_release_history_row
        )
        //Currently only LinearLayoutManager is supported.
        binding.verticalRecyclerView.layoutManager = LinearLayoutManager(
            this, RecyclerView.VERTICAL, false
        )
        binding.verticalRecyclerView.addItemDecoration(getSectionCallback(singerList))
    }

    //Get data method
    private fun getSingerList(): List<ReleaseHistory> = ReleaseHistoryRepo().historyList


    //Get SectionCallback method
    private fun getSectionCallback(singerList: List<ReleaseHistory>): SectionCallback {
        return object : SectionCallback {
            //In your data, implement a method to determine if this is a section.
            override fun isSection(position: Int): Boolean =
                singerList[position].versionCode != singerList[position - 1].versionCode

            //Implement a method that returns a SectionHeader.
            override fun getSectionHeader(position: Int): SectionInfo {
                val singer = singerList[position]
                return SectionInfo(singer.versionCode, singer.versionLabel)
            }

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
