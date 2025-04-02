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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.base.BaseFragment
import com.simplebudget.databinding.FragmentBudgetBinding
import com.simplebudget.helper.ARG_DATE
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.DialogUtil
import com.simplebudget.helper.FREE_BUDGETS_LIMIT
import com.simplebudget.helper.Logger
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
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
    private var adView: AdView? = null
    private var budgetsAdapter: BudgetsAdapter? = null
    private var budgets: ArrayList<Budget> = ArrayList<Budget>()
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
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding?.adViewContainer?.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
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
     * Show dialog with options Edit, Delete
     */
    private fun handleItemClick(budget: Budget, position: Int) {
        val options = arrayOf("Transactions", "Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext()).setTitle(
            String.format(
                "%s", "Manage budget"
            )
        ).setItems(options) { dialog, which ->
            when (options[which]) {
                "Transactions" -> {
                    startActivity(
                        Intent(
                            requireActivity(), BudgetDetailsActivity::class.java
                        ).putExtra(REQUEST_CODE_BUDGET, budget)
                    )
                }

                "Edit" -> {
                    editBudget(budget)
                }

                "Delete" -> {
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
                        negativeClickListener = {}).show()
                }

                else -> {}

            }
            dialog.dismiss()
        }.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }.setCancelable(false).show()
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
                negativeClickListener = {}).show()
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
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
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
        date?.let {
            viewModel.loadBudgets(it)
        }
        super.onResume()
    }

    // Called when the fragment is no longer in use. This is called after onStop() and before onDetach().
    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}