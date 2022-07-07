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
package com.simplebudget.view.breakdown.base

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.simplebudget.databinding.ActivityBreakdownExpensesBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.prefs.*
import com.simplebudget.view.breakdown.BreakDownFragment
import com.simplebudget.view.main.MainActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class BreakDownBaseActivity : BaseActivity<ActivityBreakdownExpensesBinding>(),
    ViewPager.OnPageChangeListener {

    private val viewModel: BreakDownBaseViewModel by viewModel()
    private var ignoreNextPageSelectedEvent: Boolean = false
    private var isAddedExpense: Boolean = false

    /**
     *
     */
    override fun createBinding(): ActivityBreakdownExpensesBinding {
        return ActivityBreakdownExpensesBinding.inflate(layoutInflater)
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

        viewModel.datesLiveData.observe(this) { dates ->
            configureViewPager(dates)

            binding.monthlyReportProgressBar.visibility = View.GONE
            binding.monthlyReportContent.visibility = View.VISIBLE
        }

        viewModel.selectedPositionLiveData.observe(
            this
        ) { (position, _, _) ->
            if (!ignoreNextPageSelectedEvent) {
                binding.monthlyReportViewPager.setCurrentItem(position, true)
            }
            ignoreNextPageSelectedEvent = false
            // Last and first available month
            val isFirstMonth = position == 0
        }
    }

    // ------------------------------------------>
    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
// ------------------------------------------>
    /**
     *
     */
    override fun onBackPressed() {
        if (isAddedExpense) {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        } else {
            finish()
        }
    }

    /**
     * Configure the [.pager] adapter and listener.
     */
    private fun configureViewPager(dates: List<Date>) {
        binding.monthlyReportViewPager.offscreenPageLimit = 0
        binding.monthlyReportViewPager.adapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return BreakDownFragment.newInstance(dates[position])
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


    /**
     *
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MainActivity.ADD_EXPENSE_ACTIVITY_CODE || requestCode == MainActivity.MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.loadData(intent.getBooleanExtra(FROM_NOTIFICATION_EXTRA, false))
                isAddedExpense = true
            }
        }
    }

    companion object {
        /**
         * Extra to add the the launch intent to specify that user comes from the notification (used to
         * show not the current month but the last one)
         */
        const val FROM_NOTIFICATION_EXTRA = "fromNotif"
    }
}
