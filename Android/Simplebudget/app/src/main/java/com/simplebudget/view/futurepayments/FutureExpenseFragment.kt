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
package com.simplebudget.view.futurepayments

import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.simplebudget.R
import com.simplebudget.databinding.FragmentFutureExpenseBinding
import com.simplebudget.helper.*
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitTimestamp
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


private const val ARG_DATE = "arg_date"

class FutureExpenseFragment : BaseFragment<FragmentFutureExpenseBinding>() {
    /**
     * The first date of the month at 00:00:00
     */
    private lateinit var date: Date
    private val appPreferences: AppPreferences by inject()
    private val viewModel: FutureExpenseViewModel by viewModel()

// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentFutureExpenseBinding =
        FragmentFutureExpenseBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        date = requireArguments().getSerializable(ARG_DATE) as Date

        viewModel.monthlyReportDataLiveData.observe(viewLifecycleOwner) { result ->
            binding?.monthlyReportFragmentProgressBar?.visibility = View.GONE
            binding?.monthlyReportFragmentContent?.visibility = View.VISIBLE

            when (result) {
                FutureExpenseViewModel.MonthlyReportData.Empty -> {
                    binding?.monthlyReportFragmentRecyclerView?.visibility = View.GONE
                    binding?.monthlyReportFragmentEmptyState?.visibility = View.VISIBLE
                }
                is FutureExpenseViewModel.MonthlyReportData.Data -> {
                    configureRecyclerView(
                        binding?.monthlyReportFragmentRecyclerView!!,
                        FutureRecyclerViewAdapter(
                            result.expenses,
                            result.revenues,
                            result.allExpensesOfThisMonth,
                            appPreferences
                        )
                    )
                }
            }
        }
        viewModel.loadDataForMonth(date)
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView,
        adapter: FutureRecyclerViewAdapter
    ) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(date: Date): FutureExpenseFragment = FutureExpenseFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
        }
    }

}
