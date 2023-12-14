/*
 *   Copyright 2023 Waheed Nazir
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
import com.simplebudget.helper.updateAccountNotifyBroadcast
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccountLabel
import com.simplebudget.view.accounts.AccountsBottomSheetDialogFragment
import com.simplebudget.view.report.base.MonthlyReportBaseActivity
import com.simplebudget.view.search.SearchFragment
import org.koin.android.ext.android.inject


class SearchBaseActivity : BaseActivity<ActivitySearchExpensesBinding>() {

    private val appPreferences: AppPreferences by inject()
    private lateinit var searchFragment: SearchFragment

    override fun createBinding(): ActivitySearchExpensesBinding {
        return ActivitySearchExpensesBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchFragment = SearchFragment.newInstance()

        //Add search fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayoutSearchExpenses, searchFragment)
        transaction.commit()

        //Selected account
        binding.layoutSelectAccount.tvSelectedAccount.text =
            String.format("%s", appPreferences.activeAccountLabel())
        binding.layoutSelectAccount.llSelectAccount.setOnClickListener {
            val accountsBottomSheetDialogFragment = AccountsBottomSheetDialogFragment {
                binding.layoutSelectAccount.tvSelectedAccount.text = it.name
                updateAccountNotifyBroadcast()

                binding.monthlyReportProgressBar.visibility = View.VISIBLE
                // 2 Seconds delay and re-load will do the trick :)
                object : CountDownTimer(2000, 2000) {

                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        binding.monthlyReportProgressBar.visibility = View.GONE
                        finish()
                        ActivityCompat.startActivity(
                            this@SearchBaseActivity,
                            Intent(this@SearchBaseActivity, SearchBaseActivity::class.java),
                            null
                        )
                    }
                }.start()

            }
            accountsBottomSheetDialogFragment.show(
                supportFragmentManager, accountsBottomSheetDialogFragment.tag
            )
        }
    }
}
