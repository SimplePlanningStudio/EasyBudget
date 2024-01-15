/*
 *   Copyright 2024 Waheed Nazir
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
import android.os.CountDownTimer
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
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.editAccountNotifyBroadcast
import com.simplebudget.helper.getMonthTitleWithPastAndFuture
import com.simplebudget.helper.removeButtonBorder
import com.simplebudget.helper.updateAccountNotifyBroadcast
import com.simplebudget.model.account.appendAccount
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import com.simplebudget.prefs.activeAccountLabel
import com.simplebudget.prefs.setActiveAccount
import com.simplebudget.view.accounts.AccountsBottomSheetDialogFragment
import com.simplebudget.view.breakdown.base.BreakDownBaseActivity
import com.simplebudget.view.report.MonthlyReportFragment
import com.simplebudget.view.report.MonthlyReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate


/**
 * Activity that displays monthly report
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportBaseActivity : BaseActivity<ActivityMonthlyReportBinding>(),
    ViewPager.OnPageChangeListener {

    private val viewModel: MonthlyReportBaseViewModel by viewModel()

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
            viewModel.loadData()
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

        viewModel.selectedPositionLiveData.observe(this) { (position, date, isLastMonth) ->
            binding.monthlyReportViewPager.setCurrentItem(position, true)
            binding.monthlyReportMonthTitleTv.text = date.getMonthTitleWithPastAndFuture(this)

            binding.monthlyReportNextMonthButton.visibility =
                if (isLastMonth) View.GONE else View.VISIBLE
            binding.monthlyReportPreviousMonthButton.visibility =
                if (position == 0) View.GONE else View.VISIBLE
        }

        //Selected account
        binding.layoutSelectAccount.tvSelectedAccount.text =
            appPreferences.activeAccountLabel().appendAccount()
        binding.layoutSelectAccount.llSelectAccount.setOnClickListener {
            val accountsBottomSheetDialogFragment =
                AccountsBottomSheetDialogFragment(onAccountSelected = { selectedAccount ->
                    binding.layoutSelectAccount.tvSelectedAccount.text =
                        selectedAccount.name.appendAccount()
                    updateAccountNotifyBroadcast()
                    binding.monthlyReportProgressBar.visibility = View.VISIBLE
                    // 2 Seconds delay and re-load will do the trick :)
                    object : CountDownTimer(2000, 2000) {

                        override fun onTick(millisUntilFinished: Long) {
                        }

                        override fun onFinish() {
                            binding.monthlyReportProgressBar.visibility = View.GONE
                            finish()
                            val startIntent = Intent(
                                this@MonthlyReportBaseActivity,
                                MonthlyReportBaseActivity::class.java
                            )
                            ActivityCompat.startActivity(
                                this@MonthlyReportBaseActivity,
                                startIntent,
                                null
                            )
                        }
                    }.start()
                }, onAccountUpdated = { updatedAccount ->
                    //Account id is same as active account id and now account name is edited we need to update label.
                    if (appPreferences.activeAccount() == updatedAccount.id) {
                        binding.layoutSelectAccount.tvSelectedAccount.text =
                            updatedAccount.name.appendAccount()
                        appPreferences.setActiveAccount(updatedAccount.id, updatedAccount.name)
                        binding.layoutSelectAccount.tvSelectedAccount.text =
                            updatedAccount.name.appendAccount()
                        editAccountNotifyBroadcast()
                    }
                })
            accountsBottomSheetDialogFragment.show(
                supportFragmentManager, accountsBottomSheetDialogFragment.tag
            )
        }
    }

// ------------------------------------------>
    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

// ------------------------------------------>

    /**
     * Configure the [.pager] adapter and listener.
     */
    private fun configureViewPager(dates: List<LocalDate>) {
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
