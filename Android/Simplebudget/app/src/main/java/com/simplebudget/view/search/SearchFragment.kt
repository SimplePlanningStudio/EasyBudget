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

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.simplebudget.R
import com.simplebudget.databinding.FragmentSearchBinding
import com.simplebudget.helper.*
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitTimestamp
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


private const val ARG_DATE = "arg_date"


class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    /**
     * The first date of the month at 00:00:00
     */
    private val appPreferences: AppPreferences by inject()
    private val viewModel: SearchViewModel by viewModel()
    private var adView: AdView? = null

// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSearchBinding =
        FragmentSearchBinding.inflate(inflater, container, false)

    /**
     * Enable menu options handling
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar.
        inflater.inflate(R.menu.menu_search, menu)
        val item = menu.findItem(R.id.action_search);
        val searchView = item?.actionView as SearchView
        // Customize search view text and hint colors
        val searchEditId = androidx.appcompat.R.id.search_src_text
        val et: EditText = searchView.findViewById<View>(searchEditId) as EditText
        et.setTextColor(Color.WHITE)
        et.setHintTextColor(Color.WHITE)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                return true
            }
        })

        //Expand Collapse listener
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                //Action Collapse
                return true
            }

            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                //Action Expand
                return true
            }
        })
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

    }

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentProgressBar?.visibility = View.GONE
            binding?.monthlyReportFragmentContent?.visibility = View.VISIBLE

            when (result) {
                SearchViewModel.MonthlyReportData.Empty -> {
                    binding?.monthlyReportFragmentRecyclerView?.visibility = View.GONE
                    binding?.monthlyReportFragmentEmptyState?.visibility = View.VISIBLE
                }
                is SearchViewModel.MonthlyReportData.Data -> {
                    configureRecyclerView(
                        binding?.monthlyReportFragmentRecyclerView!!,
                        SearchRecyclerViewAdapter(
                            result.expenses,
                            result.revenues,
                            result.allExpensesOfThisMonth,
                            appPreferences
                        )
                    )
                }
            }
        }
        val cal = Calendar.getInstance()
        cal.timeInMillis = appPreferences.getInitTimestamp()
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val today = Date()
        val startDate = cal.time
        viewModel.loadDataForMonth(startDate, today)
        /**
         * Banner ads
         */
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding?.adViewContainer?.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView,
        adapter: SearchRecyclerViewAdapter
    ) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(date: Date): SearchFragment = SearchFragment().apply {
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
