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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentBudgetDetailsBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.budget.Budget
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.report.DataModels
import com.simplebudget.view.report.adapter.MainAdapter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate

class BudgetDetailsFragment : BaseFragment<FragmentBudgetDetailsBinding>() {

    private val appPreferences: AppPreferences by inject()
    private val viewModel: BudgetDetailsViewModel by viewModel()
    private var budget: Budget? = null
    private var adView: AdView? = null
// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentBudgetDetailsBinding =
        FragmentBudgetDetailsBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        budget = arguments?.getParcelable(ARG_BUDGET_DETAILS) as Budget?
        if (budget == null) requireActivity().finish()

        binding?.title?.text = budget?.goal
        viewModel.allExpenses.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentContent?.visibility = View.VISIBLE
            if (result.isEmpty()) {
                binding?.recyclerViewSearch?.visibility = View.GONE
                binding?.monthlyReportFragmentEmptyState?.visibility = View.VISIBLE
            } else {
                binding?.recyclerViewSearch?.visibility = View.VISIBLE
                binding?.monthlyReportFragmentEmptyState?.visibility = View.GONE
            }
        }

        /**
         * Observe this for only revenue, expenses (as this livedata holds search results for reports to print, export)
         * Now: We are displaying expandable search results like monthly reports as these are easy to visualise.
         */
        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                DataModels.MonthlyReportData.Empty -> {
                    binding?.balanceContainer?.visibility = View.GONE
                    binding?.recyclerViewSearch?.visibility = View.GONE
                }

                is DataModels.MonthlyReportData.Data -> {
                    if (result.allExpensesOfThisMonth.isNotEmpty()) {
                        binding?.balanceContainer?.visibility = View.VISIBLE
                        binding?.recyclerViewSearch?.visibility = View.VISIBLE


                        binding?.tvTotalCredit?.text =
                            CurrencyHelper.getFormattedCurrencyString(
                                appPreferences, result.revenuesAmount
                            )
                        binding?.tvTotalDebit?.text =
                            CurrencyHelper.getFormattedCurrencyString(
                                appPreferences, result.expensesAmount
                            )

                        configureRecyclerView(
                            binding?.recyclerViewSearch!!, MainAdapter(
                                result.allExpensesParentList, appPreferences, onBannerClick = { _ ->
                                })
                        )
                    }
                }
            }
        }

        /**
         * Loading
         */
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding?.searchExpensesProgressBar?.visibility =
                if (loading) View.VISIBLE else View.GONE
        }
        /**
         * Banner ads
         */
        binding?.adViewContainer?.let {
            loadBanner(
                appPreferences.isUserPremium(),
                binding?.adViewContainer!!,
                onBannerAdRequested = { bannerAdView ->
                    this.adView = bannerAdView
                }
            )
        }
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView,
        adapter: MainAdapter,
    ) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    /**
     * Called when opening the activity
     */
    override fun onResume() {
        resumeBanner(adView)
        // Load expenses here
        budget?.let {
            viewModel.loadExpenses(it.id!!, it.startDate, it.endDate)
        }
        super.onResume()
    }

    /**
     *
     */
    override fun onDestroyView() {
        destroyBanner(adView)
        adView = null
        super.onDestroyView()
    }

    /**
     * SearchFragment
     */
    companion object {
        fun newInstance(budget: Budget?): BudgetDetailsFragment = BudgetDetailsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_BUDGET_DETAILS, budget)
            }
        }
    }
}
