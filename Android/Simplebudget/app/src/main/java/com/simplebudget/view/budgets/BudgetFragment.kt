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
package com.simplebudget.view.budgets


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdView
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentBudgetBinding
import com.simplebudget.helper.ARG_DATE
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.DialogUtil
import com.simplebudget.helper.FREE_BUDGETS_LIMIT
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.budget.Budget
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.budgets.addBudget.AddBudgetActivity
import com.simplebudget.view.budgets.addBudget.AddBudgetActivity.Companion.REQUEST_CODE_BUDGET
import com.simplebudget.view.budgets.base.BudgetBaseActivity
import com.simplebudget.view.budgets.budgetDetails.BudgetDetailsActivity
import com.simplebudget.view.premium.PremiumActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate

/**
 * Fragment that displays expenses for a given budget
 */
class BudgetFragment : BaseFragment<FragmentBudgetBinding>() {
    /**
     * The first date of the month at 00:00:00
     */
    private var date: LocalDate? = null
    private val appPreferences: AppPreferences by inject()
    private val viewModel: BudgetViewModel by viewModel()
    private val analyticsManager: AnalyticsManager by inject()
    private var budgetsAdapter: BudgetsAdapter? = null
    private var budgets: ArrayList<Budget> = ArrayList<Budget>()
    private var adView: AdView? = null
// ---------------------------------->

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentBudgetBinding = FragmentBudgetBinding.inflate(inflater, container, false)

    /**
     *
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getSerializable(ARG_DATE, LocalDate::class.java) as LocalDate
        } else {
            requireArguments().getSerializable(ARG_DATE) as LocalDate
        }

        viewModel.loadingLiveData.observe(viewLifecycleOwner) { load ->
            binding?.progress?.visibility = if (load) View.VISIBLE else View.INVISIBLE
        }

        viewModel.budgetsLiveData.observe(viewLifecycleOwner) { list ->
            // budgets count
            analyticsManager.logEvent(
                Events.KEY_BUDGETS, mapOf(Events.KEY_BUDGETS_COUNT to list.size)
            )
            budgets.clear()
            budgets.addAll(list)
            attachAdapter()
        }
        //Handle empty state actions
        binding?.btnSkip?.setOnClickListener {
            requireActivity().finish()
        }

        binding?.btnAdd?.setOnClickListener {
            addBudget()
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
     *
     */
    private fun attachAdapter() {
        if (budgets.isNotEmpty()) {
            emptyViewsGone()
            budgetsAdapter =
                BudgetsAdapter(requireActivity(), budgets, appPreferences) { budget, position ->
                    handleItemClick(budget, position)
                }
            binding?.recyclerView?.layoutManager = LinearLayoutManager(activity)
            binding?.recyclerView?.adapter = budgetsAdapter
            budgetsAdapter?.notifyDataSetChanged()
        } else {
            emptyViewsVisible()
        }
    }

    /**
     */
    private fun emptyViewsVisible() {
        binding?.recyclerView?.visibility = View.GONE
        binding?.emptyViews?.visibility = View.VISIBLE
    }

    /**
     */
    private fun emptyViewsGone() {
        binding?.recyclerView?.visibility = View.VISIBLE
        binding?.emptyViews?.visibility = View.GONE
    }


    /**
     * Show bottom sheet dialog with options Edit, Delete
     */
    private fun handleItemClick(budget: Budget, position: Int) {
        try {
            val existingFragment =
                parentFragmentManager.findFragmentByTag(ManageBudgetBottomSheet.TAG) as? ManageBudgetBottomSheet
            if (existingFragment == null || existingFragment.isAdded.not()) {

                ManageBudgetBottomSheet(budget) { action ->
                    when (action) {
                        ManageBudgetBottomSheet.Action.TRANSACTIONS -> {
                            startActivity(
                                Intent(requireActivity(), BudgetDetailsActivity::class.java)
                                    .putExtra(REQUEST_CODE_BUDGET, budget)
                            )
                        }

                        ManageBudgetBottomSheet.Action.EDIT -> {
                            editBudget(budget)
                        }

                        ManageBudgetBottomSheet.Action.DELETE -> {
                            DialogUtil.createDialog(
                                requireContext(),
                                title = getString(R.string.delete_budget),
                                message = getString(R.string.proceed_with_deletion),
                                positiveBtn = getString(R.string.ok),
                                negativeBtn = getString(R.string.cancel),
                                isCancelable = true,
                                positiveClickListener = {
                                    date?.let {
                                        budgetsAdapter?.delete(position)
                                        viewModel.deleteBudget(budget)
                                        viewModel.loadBudgets(it)
                                    }
                                },
                                negativeClickListener = {}
                            )?.show()
                        }
                    }
                }.show(parentFragmentManager, ManageBudgetBottomSheet.TAG)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Start activity for result for add budget
     */
    private var addBudgetActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //Re-load budgets from DB
            date?.let {
                //Refresh dates when budget is added for past dates
                if (it.isBefore(DateHelper.today) && activity is BudgetBaseActivity) {
                    (activity as BudgetBaseActivity).refreshDates()
                } else {
                    viewModel.loadBudgets(it)
                }
            }
        }

    /**
     * Add budget
     */
    fun addBudget() {
        if (appPreferences.isUserPremium()
                .not() && BuildConfig.DEBUG.not() && budgets.size >= FREE_BUDGETS_LIMIT
        ) {
            DialogUtil.createDialog(
                requireContext(),
                title = getString(R.string.become_premium),
                message = getString(R.string.to_setup_more_budgets_you_need_to_upgrade_to_premium),
                positiveBtn = getString(R.string.sure),
                negativeBtn = getString(R.string.cancel),
                isCancelable = true,
                positiveClickListener = {
                    startActivity(
                        Intent(
                            requireActivity(), PremiumActivity::class.java
                        )
                    )
                },
                negativeClickListener = {})?.show()
        } else {
            addBudgetActivityLauncher.launch(
                Intent(
                    requireActivity(),
                    AddBudgetActivity::class.java
                )
            )
            //Log add budget event
            analyticsManager.logEvent(Events.KEY_ADD_BUDGET)
        }
    }

    /**
     * Edit Budget
     */
    fun editBudget(budget: Budget) {
        val intent = Intent(requireActivity(), AddBudgetActivity::class.java)
        intent.putExtra(AddBudgetActivity.REQUEST_CODE_BUDGET, budget)
        addBudgetActivityLauncher.launch(intent)
        //Log edit budget event
        analyticsManager.logEvent(Events.KEY_EDIT_BUDGET)
    }

    companion object {
        fun newInstance(date: LocalDate): BudgetFragment = BudgetFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
        }
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
        date?.let {
            viewModel.loadBudgets(it)
        }
        super.onResume()
    }

    /**
     * Destroy banner
     */
    override fun onDestroyView() {
        destroyBanner(adView)
        adView = null
        super.onDestroyView()
    }
}