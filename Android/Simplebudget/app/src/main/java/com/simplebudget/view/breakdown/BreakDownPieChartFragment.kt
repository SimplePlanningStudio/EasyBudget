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
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentPieChartBreakDownBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.helper.extensions.beVisible
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.expenseedit.ExpenseEditActivity
import com.simplebudget.view.main.MainActivity
import com.simplebudget.view.recurringexpenseadd.RecurringExpenseEditActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.util.*


class BreakDownPieChartFragment : BaseFragment<FragmentPieChartBreakDownBinding>() {
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
    ): FragmentPieChartBreakDownBinding =
        FragmentPieChartBreakDownBinding.inflate(inflater, container, false)


    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_PIE_CHART_SCREEN)

        date = requireArguments().getSerializable(ARG_DATE) as LocalDate
        type = getString(R.string.all_label)

        viewModel.monthlyReportDataLiveDataForAllTypesOfExpenses.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentProgressBar?.visibility = View.GONE

            when (result) {
                BreakDownViewModel.MonthlyBreakDownData.Empty -> {
                    lisOfExpenses.clear()
                    binding?.breakDownEmptyState?.visibility = View.VISIBLE
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
                    binding?.chart?.beGone()
                }

                is BreakDownViewModel.MonthlyBreakDownData.Data -> {
                    lisOfExpenses.clear()
                    binding?.chart?.beVisible()
                    binding?.breakDownEmptyState?.visibility = View.GONE
                    lisOfExpenses.addAll(result.allExpensesOfThisMonth)
                    initChart()
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
                    binding?.balancesContainer?.visibility =
                        if (lisOfExpenses.isEmpty()) View.GONE else View.VISIBLE

                    //Expense count
                    analyticsManager.logEvent(
                        Events.KEY_PIE_BREAKDOWN,
                        mapOf(Events.KEY_PIE_BREAKDOWN_EXPENSES_COUNT to lisOfExpenses.size)
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
     *
     */
    private fun initChart() {
        binding?.chart?.setUsePercentValues(true)
        binding?.chart?.description?.isEnabled = false
        val sortedListOfExpenses =
            lisOfExpenses.sortedBy { expense -> expense.amountSpend }.takeLast(5)
        binding?.chart?.centerText =
            generateCenterSpannableText(sortedListOfExpenses.size, lisOfExpenses.size)
        binding?.chart?.setHoleColor(Color.WHITE)
        setData(sortedListOfExpenses)
        binding?.chart?.setEntryLabelColor(Color.BLACK)
        binding?.chart?.setEntryLabelTextSize(12f)
        binding?.chart?.legend?.isEnabled = true
        val legend: Legend? = binding?.chart?.legend
        legend?.form = Legend.LegendForm.CIRCLE
        legend?.formSize = 16f
        legend?.textSize = 12f
        legend?.xEntrySpace = 16f
        legend?.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        binding?.breakDownEmptyState?.visibility =
            if (lisOfExpenses.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Set data
     */
    private fun setData(sortedList: List<BreakDownViewModel.CategoryWiseExpense>) {
        val values = ArrayList<PieEntry>()
        sortedList.forEach {
            values.add(PieEntry(it.amountSpend.toFloat(), it.category, it.amountSpend))
        }
        val dataSet = PieDataSet(values, "")
        dataSet.sliceSpace = 6f
        dataSet.selectionShift = 8f

        // add a lot of colors
        val colors = ArrayList<Int>()
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.LIBERTY_COLORS) colors.add(c)
        for (c in ColorTemplate.PASTEL_COLORS) colors.add(c)
        colors.add(ColorTemplate.getHoloBlue())
        dataSet.colors = colors

        //dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        //dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(12f)
        data.setValueTextColor(Color.BLACK)
        binding?.chart?.data = data
        binding?.chart?.isDrawHoleEnabled = true
        binding?.chart?.setDrawRoundedSlices(true)
        binding?.chart?.animateXY(1000, 1000)
        binding?.chart?.invalidate()
    }

    /**
     *
     */
    private fun generateCenterSpannableText(
        topExpensesSize: Int, actualSize: Int,
    ): SpannableString {
        if (topExpensesSize == 0) return SpannableString("")
        val expenseLabel = if (type == getString(R.string.all_label)) "Expenses" else type
        val info = when (actualSize) {
            in 1..5 -> "\nPercentage of $expenseLabel."
            else -> "\nPercentage of top\n$topExpensesSize $expenseLabel."
        }
        val s = SpannableString("${date.getMonthTitle(requireContext())}$info")
        s.setSpan(RelativeSizeSpan(1.7f), 0, s.length, 0)
        s.setSpan(ForegroundColorSpan(Color.BLACK), 0, s.length, 0)
        s.setSpan(StyleSpan(Typeface.ITALIC), 0, s.length, 0)
        s.setSpan(RelativeSizeSpan(.7f), s.length - info.length, s.length, 0)
        s.setSpan(StyleSpan(Typeface.ITALIC), s.length - info.length, s.length, 0)
        s.setSpan(ForegroundColorSpan(Color.GRAY), s.length - info.length, s.length, 0)
        return s
    }

    /**
     *
     */
    companion object {
        fun newInstance(date: LocalDate): BreakDownPieChartFragment =
            BreakDownPieChartFragment().apply {
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

