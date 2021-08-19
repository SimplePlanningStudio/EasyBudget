/*
 *   Copyright 2021 Benoit LETONDOR
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
package com.simplebudget.view.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

private const val ARG_DATE = "arg_date"

/**
 * Fragment that displays monthly report for a given month
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportFragment : Fragment() {
    /**
     * The first date of the month at 00:00:00
     */
    private lateinit var date: Date

    private val appPreferences: AppPreferences by inject()
    private val viewModel: MonthlyReportViewModel by viewModel()
    private lateinit var thisView: View
    private var adView: AdView? = null

// ---------------------------------->

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        date = requireArguments().getSerializable(ARG_DATE) as Date

        // Inflate the layout for this fragment
        thisView = inflater.inflate(R.layout.fragment_monthly_report, container, false)

        val progressBar =
            thisView.findViewById<ProgressBar>(R.id.monthly_report_fragment_progress_bar)
        val content = thisView.findViewById<View>(R.id.monthly_report_fragment_content)
        val recyclerView =
            thisView.findViewById<RecyclerView>(R.id.monthly_report_fragment_recycler_view)
        val emptyState = thisView.findViewById<View>(R.id.monthly_report_fragment_empty_state)
        val revenuesAmountTextView =
            thisView.findViewById<TextView>(R.id.monthly_report_fragment_revenues_total_tv)
        val expensesAmountTextView =
            thisView.findViewById<TextView>(R.id.monthly_report_fragment_expenses_total_tv)
        val balanceTextView =
            thisView.findViewById<TextView>(R.id.monthly_report_fragment_balance_tv)

        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner, { result ->
            progressBar.visibility = View.GONE
            content.visibility = View.VISIBLE

            when (result) {
                MonthlyReportViewModel.MonthlyReportData.Empty -> {
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE

                    revenuesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    expensesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    balanceTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, 0.0)
                    balanceTextView.setTextColor(
                        ContextCompat.getColor(
                            balanceTextView.context,
                            R.color.budget_green
                        )
                    )
                }
                is MonthlyReportViewModel.MonthlyReportData.Data -> {
                    configureRecyclerView(
                        recyclerView,
                        MonthlyReportRecyclerViewAdapter(
                            result.expenses,
                            result.revenues,
                            appPreferences
                        )
                    )

                    revenuesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, result.revenuesAmount)
                    expensesAmountTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, result.expensesAmount)

                    val balance = result.revenuesAmount - result.expensesAmount
                    balanceTextView.text =
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
                    balanceTextView.setTextColor(
                        ContextCompat.getColor(
                            balanceTextView.context,
                            if (balance >= 0) R.color.budget_green else R.color.budget_red
                        )
                    )
                }
            }
        })

        viewModel.loadDataForMonth(date)

        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            val adContainerView = thisView.findViewById<FrameLayout>(R.id.ad_view_container)
            adContainerView.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }

        return thisView
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView,
        adapter: MonthlyReportRecyclerViewAdapter
    ) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(date: Date): MonthlyReportFragment = MonthlyReportFragment().apply {
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
            val adContainerView = thisView.findViewById<FrameLayout>(R.id.ad_view_container)
            adContainerView.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                requireContext(),
                requireActivity().windowManager.defaultDisplay
            )!!
            adView = AdView(requireContext())
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            adContainerView.addView(adView)
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
}
