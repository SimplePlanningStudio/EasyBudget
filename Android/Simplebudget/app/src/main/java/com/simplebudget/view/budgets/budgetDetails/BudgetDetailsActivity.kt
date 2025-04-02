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
package com.simplebudget.view.budgets.budgetDetails

import android.os.Bundle
import android.view.MenuItem
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityBudgetDetailsBinding
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.model.budget.Budget
import com.simplebudget.view.budgets.addBudget.AddBudgetActivity.Companion.REQUEST_CODE_BUDGET
import org.koin.android.ext.android.inject


class BudgetDetailsActivity : BaseActivity<ActivityBudgetDetailsBinding>() {

    private lateinit var budgetDetailsFragment: BudgetDetailsFragment
    private var budget: Budget? = null
    private val analyticsManager: AnalyticsManager by inject()

    override fun createBinding(): ActivityBudgetDetailsBinding {
        return ActivityBudgetDetailsBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_BUDGET_DETAILS_SCREEN)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Get the budget
        budget = intent.getParcelableExtra(REQUEST_CODE_BUDGET) as Budget?

        if (budget != null && budget?.id != null) {
            budgetDetailsFragment = BudgetDetailsFragment.newInstance(budget)

            //Add budget details fragment
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.frameLayoutSearchExpenses, budgetDetailsFragment)
            transaction.commit()
        } else {
            finish()
        }
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

            else -> super.onOptionsItemSelected(item)
        }
    }
}
