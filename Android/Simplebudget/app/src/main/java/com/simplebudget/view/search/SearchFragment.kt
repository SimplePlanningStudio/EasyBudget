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
package com.simplebudget.view.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.chip.Chip
import com.simplebudget.R
import com.simplebudget.databinding.FragmentSearchBinding
import com.simplebudget.helper.*
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.prefs.AppPreferences
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*


class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    /**
     * The first date of the month at 00:00:00
     */
    private val appPreferences: AppPreferences by inject()
    private val viewModel: SearchViewModel by viewModel()
    private var adView: AdView? = null
    private val dayFormatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSearchBinding =
        FragmentSearchBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.expenses.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentContent?.visibility = View.VISIBLE
            if (result.isEmpty()) {
                binding?.recyclerViewSearch?.visibility = View.GONE
                binding?.monthlyReportFragmentEmptyState?.visibility = View.VISIBLE
            } else {
                binding?.recyclerViewSearch?.visibility = View.VISIBLE
                binding?.monthlyReportFragmentEmptyState?.visibility = View.GONE

                binding?.recyclerViewSearch?.layoutManager =
                    LinearLayoutManager(activity)
                binding?.recyclerViewSearch?.adapter = SearchRecyclerViewAdapter(
                    result,
                    appPreferences
                )
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
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding?.adViewContainer?.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }

        /**
         * Handle search expenses
         */
        binding?.searchEditText?.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            binding?.ivClearTick?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
            if (query.isEmpty()) viewModel.loadThisMonthExpenses()
        }

        /**
         * Clear search
         */
        binding?.ivClearTick?.setOnClickListener {
            binding?.searchEditText?.text?.clear()
            resetToDefaultChipThisMonth()
        }


        /**
         * Handle action done for search expenses
         */
        binding?.searchEditText?.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding?.searchEditText?.text.toString().trim()
                viewModel.searchExpenses(query)
                Keyboard.hideSoftKeyboard(requireContext(), binding?.searchEditText!!)
                binding?.chipReset?.visibility = View.GONE
                binding?.chipGroup?.clearCheck()
                binding?.searchResultsFor?.visibility = View.VISIBLE
                binding?.searchResultsFor?.text =
                    String.format(getString(R.string.search_results_for_), query)
                return@OnEditorActionListener true
            }
            false
        })

        binding?.searchResultsFor?.visibility = View.VISIBLE
        binding?.searchResultsFor?.text =
            String.format(getString(R.string.search_results_for_), SearchUtil.THIS_MONTH)
        //Add top searches
        SearchUtil.getTopSearches().forEach {
            addChipsForTopSearches(it)
        }
        /**
         *
         */
        binding?.chipReset?.setOnClickListener {
            resetToDefaultChipThisMonth()
            viewModel.loadThisMonthExpenses()
        }
    }

    /**
     * Call this for clear / reset chip click
     */
    private fun resetToDefaultChipThisMonth() {
        binding?.chipReset?.visibility = View.GONE
        binding?.chipGroup?.clearCheck()
        binding?.searchResultsFor?.visibility = View.VISIBLE
        binding?.searchResultsFor?.text =
            String.format(getString(R.string.search_results_for_), SearchUtil.THIS_MONTH)
        binding?.chipGroup?.findViewWithTag<Chip>(SearchUtil.THIS_MONTH)?.let {
            it.isChecked = true
        }
        binding?.searchEditText?.text?.clear()
    }


    /**
     * Add hardcoded chips for Top searches
     */
    private fun addChipsForTopSearches(
        chipText: String,
        showClose: Boolean = false,
        isChecked: Boolean = false
    ) {
        val chip = Chip(requireContext())
        chip.text = chipText
        chip.tag = chipText
        chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_download_app)
        chip.isChipIconVisible = false
        chip.isCloseIconVisible = (showClose && chipText != ExpenseCategoryType.MISCELLANEOUS.name)
        // necessary to get single selection working
        chip.isClickable = true
        binding?.chipGroup?.isSingleSelection = !showClose
        chip.isCheckable = !showClose
        chip.isChecked = if (chipText == SearchUtil.THIS_MONTH) true else isChecked
        binding?.chipGroup?.addView(chip as View)

        chip.setOnClickListener {
            val clickChipText = (it as Chip).text ?: ""
            binding?.searchResultsFor?.visibility = View.VISIBLE
            binding?.chipReset?.visibility = View.VISIBLE
            binding?.searchResultsFor?.text =
                String.format(getString(R.string.search_results_for_), clickChipText)
            when (clickChipText) {
                SearchUtil.TODAY -> viewModel.loadTodayExpenses()
                SearchUtil.YESTERDAY -> viewModel.loadYesterdayExpenses()
                SearchUtil.TOMORROW -> viewModel.loadTomorrowExpenses()
                SearchUtil.THIS_WEEK -> viewModel.loadThisWeekExpenses()
                SearchUtil.LAST_WEEK -> viewModel.loadLastWeekExpenses()
                SearchUtil.THIS_MONTH -> viewModel.loadThisMonthExpenses()
                SearchUtil.PICK_A_DATE -> {
                    requireActivity().pickSingleDate(onDateSet = { date ->
                        viewModel.loadExpensesForADate(date)
                        binding?.searchResultsFor?.visibility = View.VISIBLE
                        binding?.searchResultsFor?.text =
                            String.format(
                                getString(R.string.search_results_for_),
                                dayFormatter.format(date)
                            )
                    })
                }
                SearchUtil.PICK_A_DATE_RANGE -> {
                    requireActivity().pickDateRange(onDateSet = { dates ->
                        val cal = Calendar.getInstance()
                        cal.time = dates.second
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        viewModel.loadExpensesForGivenDates(dates.first, cal.time)
                        binding?.searchResultsFor?.visibility = View.VISIBLE
                        binding?.searchResultsFor?.text =
                            String.format(
                                getString(R.string.search_results_for_range),
                                dayFormatter.format(dates.first),
                                dayFormatter.format(dates.second)
                            )
                    })
                }
            }
        }
    }

    /**
     * SearchFragment
     */
    companion object {
        fun newInstance(): SearchFragment = SearchFragment()
    }

    /**
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
            binding?.adViewContainer?.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                requireContext(),
                requireActivity().windowManager.defaultDisplay
            )!!
            adView = AdView(requireContext())
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            binding?.adViewContainer?.addView(adView)
            val actualAdRequest = AdRequest.Builder()
                .build()
            adView?.setAdSize(adSize)
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
