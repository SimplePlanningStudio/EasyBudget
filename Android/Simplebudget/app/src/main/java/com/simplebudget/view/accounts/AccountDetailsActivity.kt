/*
 *   Copyright 2023 Waheed Nazir
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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityAccountDetailsBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.Account
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.accounts.adapter.AccountDataModels
import com.simplebudget.view.accounts.adapter.AccountDetailsAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate

/**
 *
 */
class AccountDetailsActivity : BaseActivity<ActivityAccountDetailsBinding>() {


    /**
     * The first date of the month at 00:00:00
     */
    private var date: LocalDate = LocalDate.now()

    private val appPreferences: AppPreferences by inject()
    private val accountsViewModel: AccountsViewModel by viewModel()
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
                    accountDetailsAdapter = AccountDetailsAdapter(
                        ArrayList(result.allExpensesParentList),
                        appPreferences
                    ) { selectedAccountDetails ->
                    }
                    configureRecyclerView(
                        binding.monthlyReportFragmentRecyclerView, accountDetailsAdapter
                    )
                }
            }
        }
        accountsViewModel.loadAccountDetailsWithBalance(date)

        /**
         * Banner ads
         */
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false)) {
            binding.adViewContainer.visibility = View.GONE
        } else {
            loadAndDisplayBannerAds()
        }

    }

    /**
     * Configure recycler view LayoutManager & adapter
     */
    private fun configureRecyclerView(
        recyclerView: RecyclerView, adapter: AccountDetailsAdapter
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
                    toast(getString(R.string.account_already_exists))
                }
            },
            remainingAccounts = (ACCOUNTS_LIMIT - accountDetailsAdapter.itemCount),
            isPremiumUser = appPreferences.isUserPremium()
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

// ------------------------------------------>
    /**
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
            binding.adViewContainer.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(
                this, windowManager.defaultDisplay
            )
            adView = AdView(this)
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            binding.adViewContainer.addView(adView)

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
