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
package com.simplebudget.view.accounts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityAccountDetailsBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.isVisible
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.Account
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getBanner
import com.simplebudget.view.accounts.adapter.AccountDataModels
import com.simplebudget.view.accounts.adapter.AccountDetailsAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.net.toUri
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner

/**
 *
 */
class AccountDetailsActivity : BaseActivity<ActivityAccountDetailsBinding>() {

    /**
     * The first date of the month at 00:00:00
     */
    private val toastManager: ToastManager by inject()
    private val appPreferences: AppPreferences by inject()
    private val accountsViewModel: AccountsViewModel by viewModel()
    private val analyticsManager: AnalyticsManager by inject()
    private var adView: AdView? = null
    private lateinit var accountDetailsAdapter: AccountDetailsAdapter
    private var accounts: List<Account> = emptyList()
    private var tvMenuAddAccount: TextView? = null


    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AccountDetailsActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun createBinding(): ActivityAccountDetailsBinding {
        return ActivityAccountDetailsBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Screen event
        analyticsManager.logEvent(Events.KEY_ACCOUNT_DETAILS_SCREEN)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.progressBarAccountDetails.visibility = View.VISIBLE

        lifecycleScope.launch {
            accountsViewModel.allAccountsFlow.collect { accountEntities ->
                accounts = accountEntities.toAccounts()
                // val currentActiveAccount = accounts.singleOrNull { act -> (act.isActive == 1) }
            }
        }

        accountsViewModel.monthlyReportDataLiveData.observe(this) { result ->
            binding.progressBarAccountDetails.visibility = View.GONE

            when (result) {
                is AccountDataModels.MonthlyAccountData.Data -> {
                    //Log account count
                    analyticsManager.logEvent(
                        Events.KEY_ACCOUNT_DETAILS, mapOf(
                            Events.KEY_ACCOUNTS_COUNTS to result.allExpensesParentList.size
                        )
                    )
                    accountDetailsAdapter = AccountDetailsAdapter(
                        ArrayList(result.allExpensesParentList), appPreferences
                    ) { selectedAccountDetails ->
                    }
                    configureRecyclerView(
                        binding.monthlyReportFragmentRecyclerView, accountDetailsAdapter
                    )
                }
            }
        }
        accountsViewModel.loadAccountDetailsWithBalance()

        /**
         * Banner ads
         */
        loadBanner(
            appPreferences.isUserPremium(),
            binding.adViewContainer,
            onBannerAdRequested = { bannerAdView ->
                this.adView = bannerAdView
            }
        )
        showAppBanner()
    }

    /**
     * Show app promotion banner
     */
    private fun showAppBanner() {
        // Show app banner promotion
        if (appPreferences.isUserPremium().not() && shouldShowBanner()) {
            val banner = appPreferences.getBanner()
            binding.bannerLayout.bannerCard.visibility = banner?.let { View.VISIBLE } ?: View.GONE
            banner?.let {
                if (AppInstallHelper.isInstalled(banner.packageName ?: "", this).not()) {
                    binding.bannerLayout.bannerTitle.text = banner.title
                    binding.bannerLayout.bannerDescription.text = banner.description
                    Glide.with(this).load(banner.imageUrl).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.bannerLayout.bannerImage)
                    binding.bannerLayout.bannerCard.setOnClickListener {
                        if (banner.appName != null && banner.packageName != null) {
                            analyticsManager.logEvent(
                                Events.KEY_PROMOTION_BANNER_CLICKED, mapOf<String, String>(
                                    Events.KEY_PROMOTION_BANNER_NAME to banner.appName,
                                    Events.KEY_PROMOTION_BANNER_PACKAGE to banner.packageName,
                                )
                            )
                        }
                        banner.redirectUrl?.let {
                            val intent = Intent(Intent.ACTION_VIEW, banner.redirectUrl.toUri())
                            startActivity(intent)
                        }
                    }
                    updateBannerCount()
                    if (binding.bannerLayout.bannerCard.isVisible()) {
                        if (banner.appName != null && banner.packageName != null) {
                            analyticsManager.logEvent(
                                Events.KEY_PROMOTION_BANNER_SHOWN, mapOf<String, String>(
                                    Events.KEY_PROMOTION_BANNER_NAME to banner.appName,
                                    Events.KEY_PROMOTION_BANNER_PACKAGE to banner.packageName,
                                )
                            )
                        }
                    }
                }
            }
        } else {
            binding.bannerLayout.bannerCard.visibility = View.GONE
        }
    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView, adapter: AccountDetailsAdapter,
    ) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        synAddAccountMenuVisibility()
    }

    /**
     * Whenever we are adding new account we have to call thi function so that we can sync the add account visibility.
     */
    private fun synAddAccountMenuVisibility() {
        tvMenuAddAccount?.visibility =
            if ((ACCOUNTS_LIMIT - accountDetailsAdapter.itemCount) <= 0) View.GONE else View.VISIBLE
    }

    // ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_accounts, menu)
        return true
    }

    /**
     * Creating a custom menu option.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_add_account)
        val rootView = menuItem?.actionView as LinearLayout?
        tvMenuAddAccount = rootView?.findViewById<TextView>(R.id.tvMenuAddAccount)
        tvMenuAddAccount?.let {
            it.setOnClickListener {
                //Log add account
                analyticsManager.logEvent(
                    Events.KEY_ADD_ACCOUNT_CLICKED, mapOf(
                        Events.KEY_ACCOUNTS_COUNTS to accountDetailsAdapter.itemCount
                    )
                )
                handleAddAndEditAccount()
            }
        }
        return true
    }

    /**
     * existingAccount: Null in case of new account
     */
    private fun handleAddAndEditAccount(existingAccount: Account? = null) {
        AddEditAccountDialog.open(
            this@AccountDetailsActivity,
            account = existingAccount, // Always null in case of adding new account
            addUpdateAccount = { newAccountTriple ->
                val accountAlreadyExists = accounts.filter { act ->
                    (act.name.uppercase().contains(newAccountTriple.first))
                }
                if (accountAlreadyExists.isEmpty()) {
                    // Add you account to DB
                    if (newAccountTriple.second) {
                        // Editing case
                        accountsViewModel.updateActiveAccount(
                            Account(
                                id = newAccountTriple.third?.id, name = newAccountTriple.first
                            )
                        )
                    } else {
                        // Adding new case
                        //Log account add
                        analyticsManager.logEvent(Events.KEY_ACCOUNT_ADDED)
                        accountsViewModel.addAccount(Account(name = newAccountTriple.first))
                        // Update the account list by adding new item no need to reload it from
                        // DB there won't be any new expenses for newly created accounts.
                        //Account doesn't exists already so proceed with adding new!
                        accountDetailsAdapter.addNewAccount(
                            AccountDataModels.CustomTripleAccount.Data(
                                0L, newAccountTriple.first, 0.0, 0.0, ArrayList()
                            )
                        )
                        binding.monthlyReportFragmentRecyclerView.scrollTo(0, 0)
                        synAddAccountMenuVisibility()
                    }
                } else {
                    toastManager.showShort(getString(R.string.account_already_exists))
                }
            },
            remainingAccounts = (ACCOUNTS_LIMIT - accountDetailsAdapter.itemCount),
            isPremiumUser = appPreferences.isUserPremium(),
            dismissAccountBottomSheet = {
                // No Action required here
            },
            toastManager = toastManager
        )
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

    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    override fun onResume() {
        resumeBanner(adView)
        super.onResume()
    }

    override fun onDestroy() {
        destroyBanner(adView)
        adView = null
        super.onDestroy()
    }
}
