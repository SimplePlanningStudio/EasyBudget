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
package com.simplebudget.view.breakdown

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentBarchartBreakDownBinding
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.InternetUtils
import com.simplebudget.helper.Logger
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.breakdown.base.BreakDownBaseActivity
import com.simplebudget.view.expenseedit.ExpenseEditActivity
import com.simplebudget.view.main.MainActivity
import com.simplebudget.view.recurringexpenseadd.RecurringExpenseEditActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.util.ArrayList

const val ARG_DATE = "arg_date"

class BreakDownBarChartFragment : BaseFragment<FragmentBarchartBreakDownBinding>() {
    /**
     * The first date of the month at 00:00:00
     */
    private lateinit var date: LocalDate
    private var type: String = ""
    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val viewModel: BreakDownViewModel by viewModel()
    private var adView: AdView? = null
    private val lisOfExpenses: ArrayList<BreakDownViewModel.CategoryWiseExpense> = ArrayList()

// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): FragmentBarchartBreakDownBinding =
        FragmentBarchartBreakDownBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_LINEAR_BREAKDOWN_SCREEN)

        date = requireArguments().getSerializable(ARG_DATE) as LocalDate
        type = getString(R.string.expenses)
        binding?.filter?.text = type
        analyticsManager.logEvent(
            Events.KEY_LINEAR_BREAKDOWN_FILTER,
            mapOf(Events.KEY_VALUE to TYPE.EXPENSE.name)
        )
        viewModel.monthlyReportDataLiveDataForAllTypesOfExpenses.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentProgressBar?.visibility = View.GONE

            when (result) {
                BreakDownViewModel.MonthlyBreakDownData.Empty -> {
                    lisOfExpenses.clear()
                    binding?.breakDownEmptyState?.visibility = View.VISIBLE
                    binding?.llRecyclerViewContents?.visibility = View.GONE

                    binding?.monthlyReportFragmentRevenuesTotalTv?.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    binding?.monthlyReportFragmentExpensesTotalTv?.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    binding?.monthlyReportFragmentBalanceTv?.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    binding?.monthlyReportFragmentBalanceTv?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.budget_green
                        )
                    )
                    binding?.balancesContainer?.visibility = View.GONE
                }

                is BreakDownViewModel.MonthlyBreakDownData.Data -> {
                    lisOfExpenses.clear()
                    lisOfExpenses.addAll(result.allExpensesOfThisMonth)
                    binding?.balancesContainer?.visibility = View.VISIBLE
                    binding?.llRecyclerViewContents?.visibility = View.VISIBLE
                    binding?.breakDownEmptyState?.visibility =
                        if (lisOfExpenses.size == 0) View.VISIBLE else View.GONE
                    if (result.allExpensesOfThisMonth.isNotEmpty()) {
                        configureRecyclerView(
                            binding?.monthlyReportFragmentRecyclerView!!,
                            BreakDownRecyclerViewAdapter(
                                result.allExpensesOfThisMonth,
                                result.totalExpenses,
                                appPreferences
                            )
                        )
                    }
                    binding?.monthlyReportFragmentRevenuesTotalTv?.text =
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences, result.revenuesAmount
                        )
                    binding?.monthlyReportFragmentExpensesTotalTv?.text =
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences, result.expensesAmount
                        )

                    val balance = result.revenuesAmount - result.expensesAmount
                    binding?.monthlyReportFragmentBalanceTv?.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
                    binding?.monthlyReportFragmentBalanceTv?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (balance >= 0) R.color.budget_green else R.color.budget_red
                        )
                    )
                }
            }
        }
        viewModel.loadDataForMonth(date, BreakdownType.getSelectedType(requireContext(), type))
        /**
         * Banner ads
         */
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding?.adViewContainer?.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }

        /**
         * Handle clicks for Add expenses
         */
        handleAddExpenses()

        /**
         * Filters
         */
        binding?.filter?.setOnClickListener {
            BreakdownType.showTypeDialog(
                requireActivity(),
                type,
                onLanguageSelected = { selectedType ->
                    binding?.filter?.text = selectedType // Update label
                    type = selectedType // Update the type to save it to locally
                    val typeEnum = BreakdownType.getSelectedType(requireContext(), type)
                    binding?.balancesContainer?.visibility =
                        if (typeEnum == TYPE.ALL.name && lisOfExpenses.isNotEmpty()) View.VISIBLE else View.GONE

                    viewModel.loadDataForMonth(
                        date, typeEnum
                    )
                    analyticsManager.logEvent(
                        Events.KEY_LINEAR_BREAKDOWN_FILTER,
                        mapOf(Events.KEY_VALUE to typeEnum)
                    )
                })
        }
    }


    /**
     *
     */
    private fun handleAddExpenses() {
        binding?.fabNewExpense?.setOnClickListener {
            val startIntent = Intent(requireActivity(), ExpenseEditActivity::class.java)
            startIntent.putExtra("date", LocalDate.now().toEpochDay())
            ActivityCompat.startActivityForResult(
                requireActivity(), startIntent, MainActivity.ADD_EXPENSE_ACTIVITY_CODE, null
            )
        }

        binding?.fabNewRecurringExpense?.setOnClickListener {
            val startIntent = Intent(requireActivity(), RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", LocalDate.now().toEpochDay())
            ActivityCompat.startActivityForResult(
                requireActivity(), startIntent, MainActivity.ADD_EXPENSE_ACTIVITY_CODE, null
            )
        }
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView, adapter: BreakDownRecyclerViewAdapter,
    ) {
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    /**
     *
     */
    companion object {
        fun newInstance(date: LocalDate): BreakDownBarChartFragment =
            BreakDownBarChartFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DATE, date)
                }
            }
    }

    /**
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
            if (InternetUtils.isInternetAvailable(requireActivity()).not()) return
            binding?.adViewContainer?.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                requireContext(), requireActivity().windowManager.defaultDisplay
            )
            adView = AdView(requireContext())
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            binding?.adViewContainer?.addView(adView)
            val actualAdRequest = AdRequest.Builder().build()
            adView?.setAdSize(adSize)
            adView?.loadAd(actualAdRequest)
            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdOpened() {}
                override fun onAdClosed() {
                    loadAndDisplayBannerAds()
                }
            }
        } catch (e: Exception) {
            Logger.error(getString(R.string.error_while_displaying_banner_ad), e)
        }
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    /**
     * Called when opening the activity
     */
    override fun onResume() {
        adView?.resume()
        super.onResume()
    }

    // Called when the fragment is no longer in use. This is called after onStop() and before onDetach().
    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}