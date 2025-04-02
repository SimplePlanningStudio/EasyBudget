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
package com.simplebudget.view.search.base

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.core.app.ActivityCompat
import com.simplebudget.R
import com.simplebudget.databinding.ActivitySearchExpensesBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.editAccountNotifyBroadcast
import com.simplebudget.model.account.appendAccount
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import com.simplebudget.prefs.activeAccountLabel
import com.simplebudget.prefs.setActiveAccount
import com.simplebudget.view.accounts.AccountsBottomSheetDialogFragment
import com.simplebudget.view.search.SearchFragment
import org.koin.android.ext.android.inject


class SearchBaseActivity : BaseActivity<ActivitySearchExpensesBinding>() {

    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private lateinit var searchFragment: SearchFragment

    override fun createBinding(): ActivitySearchExpensesBinding {
        return ActivitySearchExpensesBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_SEARCH_SCREEN)

        searchFragment = SearchFragment.newInstance()

        //Add search fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayoutSearchExpenses, searchFragment)
        transaction.commit()

        //Selected account
        binding.layoutSelectAccount.tvSelectedAccount.text =
            String.format("%s", appPreferences.activeAccountLabel().appendAccount())
        binding.tvSearchingAccount.text = getString(
            R.string.you_are_searching_in,
            appPreferences.activeAccountLabel().appendAccount()
        )
        binding.layoutSelectAccount.llSelectAccount.setOnClickListener {
            val accountsBottomSheetDialogFragment =
                AccountsBottomSheetDialogFragment(onAccountSelected = { selectedAccount ->
                    binding.layoutSelectAccount.tvSelectedAccount.text =
                        selectedAccount.name.appendAccount()
                    binding.tvSearchingAccount.text =
                        getString(
                            R.string.you_are_searching_in,
                            selectedAccount.name.appendAccount()
                        )
                    binding.monthlyReportProgressBar.visibility = View.VISIBLE
                    // 2 Seconds delay and re-load will do the trick :)
                    object : CountDownTimer(2000, 2000) {

                        override fun onTick(millisUntilFinished: Long) {
                        }

                        override fun onFinish() {
                            binding.monthlyReportProgressBar.visibility = View.GONE
                            ActivityCompat.startActivity(
                                this@SearchBaseActivity,
                                Intent(this@SearchBaseActivity, SearchBaseActivity::class.java),
                                null
                            )
                            finish()
                        }
                    }.start()
                    //Log event
                    analyticsManager.logEvent(Events.KEY_ACCOUNT_SWITCHED)
                }, onAccountUpdated = { updatedAccount ->
                    //Account id is same as active account id and now account name is edited we need to update label.
                    if (appPreferences.activeAccount() == updatedAccount.id) {
                        binding.layoutSelectAccount.tvSelectedAccount.text =
                            updatedAccount.name.appendAccount()
                        appPreferences.setActiveAccount(updatedAccount.id, updatedAccount.name)
                        binding.tvSearchingAccount.text =
                            getString(
                                R.string.you_are_searching_in,
                                updatedAccount.name.appendAccount()
                            )
                        editAccountNotifyBroadcast()
                    }
                    //Log event
                    analyticsManager.logEvent(Events.KEY_ACCOUNT_UPDATED)
                })
            accountsBottomSheetDialogFragment.show(
                supportFragmentManager, accountsBottomSheetDialogFragment.tag
            )
        }
    }
}
