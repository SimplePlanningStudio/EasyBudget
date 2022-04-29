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
package com.simplebudget.view.report.base

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
import com.simplebudget.R
import com.simplebudget.databinding.ActivityMonthlyReportBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.helper.getMonthTitle
import com.simplebudget.helper.removeButtonBorder
import com.simplebudget.helper.showcaseviewlib.GuideView
import com.simplebudget.helper.showcaseviewlib.config.DismissType
import com.simplebudget.helper.showcaseviewlib.config.Gravity
import com.simplebudget.helper.showcaseviewlib.config.PointerType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.hasUserCompleteFutureExpensesShowCaseView
import com.simplebudget.prefs.setUserCompleteFutureExpensesShowCaseView
import com.simplebudget.view.futurepayments.FutureBaseActivity
import com.simplebudget.view.report.MonthlyReportFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportBaseActivity : BaseActivity<ActivityMonthlyReportBinding>(),
    ViewPager.OnPageChangeListener {

    private val viewModel: MonthlyReportBaseViewModel by viewModel()

    private var ignoreNextPageSelectedEvent: Boolean = false

    private val appPreferences: AppPreferences by inject()


    override fun createBinding(): ActivityMonthlyReportBinding {
        return ActivityMonthlyReportBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            viewModel.loadData(intent.getBooleanExtra(FROM_NOTIFICATION_EXTRA, false))
        }

        binding.monthlyReportPreviousMonthButton.text = "<"
        binding.monthlyReportNextMonthButton.text = ">"

        binding.monthlyReportPreviousMonthButton.setOnClickListener {
            viewModel.onPreviousMonthButtonClicked()
        }

        binding.monthlyReportNextMonthButton.setOnClickListener {
            viewModel.onNextMonthButtonClicked()
        }

        binding.monthlyReportPreviousMonthButton.removeButtonBorder()
        binding.monthlyReportNextMonthButton.removeButtonBorder()

        viewModel.datesLiveData.observe(this) { dates ->
            configureViewPager(dates)

            binding.monthlyReportProgressBar.visibility = View.GONE
            binding.monthlyReportContent.visibility = View.VISIBLE
        }

        viewModel.selectedPositionLiveData.observe(this) { (position, date, isLatestMonth) ->
            if (!ignoreNextPageSelectedEvent) {
                binding.monthlyReportViewPager.setCurrentItem(position, true)
            }

            ignoreNextPageSelectedEvent = false

            binding.monthlyReportMonthTitleTv.text = date.getMonthTitle(this)

            // Last and first available month
            val isFirstMonth = position == 0

            binding.monthlyReportNextMonthButton.isEnabled = !isLatestMonth
            binding.monthlyReportPreviousMonthButton.isEnabled = !isFirstMonth
        }
    }

// ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_reports, menu)
        return true
    }

    /**
     * Creating a custom menu option.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_future_expenses)
        val rootView = menuItem?.actionView as LinearLayout?
        val customFutureExpenseMenu = rootView?.findViewById<TextView>(R.id.tvMenuFutureExpense)
        customFutureExpenseMenu?.let {
            if (appPreferences.hasUserCompleteFutureExpensesShowCaseView().not()) {
                showCaseView(
                    targetView = it,
                    title = getString(R.string.future_expenses),
                    message = getString(R.string.future_expenses_show_view_message),
                    handleGuideListener = {
                        appPreferences.setUserCompleteFutureExpensesShowCaseView()
                    }
                )
            }
            it.setOnClickListener {
                ActivityCompat.startActivity(
                    this, Intent(this, FutureBaseActivity::class.java),
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
                return MonthlyReportFragment.newInstance(dates[position])
            }

            override fun getCount(): Int {
                return dates.size
            }
        }
        binding.monthlyReportViewPager.addOnPageChangeListener(this)
    }

// ------------------------------------------>

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        ignoreNextPageSelectedEvent = true

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
}
