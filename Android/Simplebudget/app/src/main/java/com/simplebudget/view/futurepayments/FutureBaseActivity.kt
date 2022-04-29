/*
 *   Copyright 2022 Waheed Nazir
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
package com.simplebudget.view.futurepayments

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.databinding.ActivityFutureExpensesBinding
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.helper.getMonthTitle
import com.simplebudget.helper.showcaseviewlib.GuideView
import com.simplebudget.helper.showcaseviewlib.config.DismissType
import com.simplebudget.helper.showcaseviewlib.config.Gravity
import com.simplebudget.helper.showcaseviewlib.config.PointerType
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.*
import com.simplebudget.view.breakdown.base.BreakDownBaseActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.collections.ArrayList


class FutureBaseActivity : BaseActivity<ActivityFutureExpensesBinding>(),
    ViewPager.OnPageChangeListener {

    private val viewModel: FutureBaseViewModel by viewModel()
    private var dates: ArrayList<Date> = ArrayList()
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null

    /**
     *
     */
    override fun createBinding(): ActivityFutureExpensesBinding {
        return ActivityFutureExpensesBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) viewModel.loadData()

        viewModel.datesLiveData.observe(this) { dates ->
            this.dates.clear()
            this.dates.addAll(dates)
            configureViewPager(this.dates)
            binding.monthlyReportProgressBar.visibility = View.GONE
            binding.monthlyReportContent.visibility = View.VISIBLE
        }

        viewModel.selectedPositionLiveData.observe(
            this
        ) { (position, date, showNextButton, showPreviousButton) ->
            binding.tvMonthTitle.text = date.getMonthTitle(this)
            binding.monthlyReportViewPager.setCurrentItem(position, true)
            binding.buttonPreviousMonth.visibility =
                if (showPreviousButton) View.VISIBLE else View.INVISIBLE
            binding.buttonNextMonth.visibility =
                if (showNextButton) View.VISIBLE else View.INVISIBLE
        }

        binding.buttonNextMonth.setOnClickListener {
            viewModel.onNextMonthButtonClicked()
        }
        binding.buttonPreviousMonth.setOnClickListener {
            viewModel.onPreviousMonthButtonClicked()
        }

        /**
         * Banner ads
         */
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding.adViewContainer.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }
    }

    // ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_breakdown, menu)
        return true
    }

    /**
     * Creating a custom menu option.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_breakdown)
        val rootView = menuItem?.actionView as LinearLayout?
        val customFutureExpenseMenu = rootView?.findViewById<TextView>(R.id.tvMenuBreakdown)
        customFutureExpenseMenu?.let {
            if (appPreferences.hasUserCompleteExpensesBreakDownShowCaseView().not()) {
                showCaseView(
                    targetView = it,
                    title = getString(R.string.title_expenses_breakdown),
                    message = getString(R.string.expenses_breakdown_show_view_message),
                    handleGuideListener = {
                        appPreferences.setUserCompleteExpensesBreakDownShowCaseView()
                    }
                )
            }
            it.setOnClickListener {
                ActivityCompat.startActivity(
                    this, Intent(this, BreakDownBaseActivity::class.java),
                    null
                )
            }
        }
        return true
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_future_expenses -> {
                ActivityCompat.startActivity(
                    this, Intent(this, FutureBaseActivity::class.java),
                    null
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

// ------------------------------------------>

    /**
     * Configure the [.pager] adapter and listener.
     */
    private fun configureViewPager(dates: List<Date>) {
        binding.monthlyReportViewPager.offscreenPageLimit = 0
        binding.monthlyReportViewPager.adapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return FutureExpenseFragment.newInstance(dates[position])
            }

            override fun getCount(): Int {
                return dates.size
            }
        }
        binding.monthlyReportViewPager.addOnPageChangeListener(this)
    }


    /**
     *
     */
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        viewModel.onPageSelected(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    companion object {
        /**
         * Extra to add the the launch intent to specify that user comes from the notification (used to
         * show not the current month but the last one)
         */
        const val FROM_NOTIFICATION_EXTRA = "fromNotif"
    }

    /**
     *  Resuming banner ad
     */
    override fun onResume() {
        adView?.resume()
        super.onResume()
    }

    /**
     * Pausing banner ad
     */
    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    /**
     * Loading banner ads
     */
    private fun loadAndDisplayBannerAds() {
        try {
            binding.adViewContainer.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                this,
                windowManager.defaultDisplay
            )!!
            adView = AdView(this)
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            binding.adViewContainer.addView(adView)
            val actualAdRequest = AdRequest.Builder()
                .build()
            adView?.adSize = adSize
            adView?.loadAd(actualAdRequest)
            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdOpened() {}
                override fun onAdClosed() {
                    loadAndDisplayBannerAds()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
