package com.simplebudget.view.accounts

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplebudget.R
import com.simplebudget.base.BaseDialogFragment
import com.simplebudget.databinding.AccountsBottomSheetDialogFragmentBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.extensions.toAccount
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.Account
import com.simplebudget.model.account.AccountType
import com.simplebudget.model.category.Category
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import com.simplebudget.prefs.activeAccountLabel
import com.simplebudget.view.accounts.adapter.AccountDataModels
import com.simplebudget.view.accounts.adapter.AccountDetailsAdapter
import com.simplebudget.view.accounts.adapter.AccountsAdapter
import com.simplebudget.view.premium.PremiumActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccountsBottomSheetDialogFragment(private val onAccountSelected: (Account) -> Unit) :
    BaseDialogFragment<AccountsBottomSheetDialogFragmentBinding>() {

    private val accountsViewModel: AccountsViewModel by viewModel()

    private val appPreferences: AppPreferences by inject()

    private lateinit var activeAccount: Account // Currently active account

    private var selectedAccount: Account? = null // Selected on list for display

    private lateinit var accountsAdapter: AccountsAdapter

    private lateinit var accounts: List<Account>

    override fun onCreateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): AccountsBottomSheetDialogFragmentBinding =
        AccountsBottomSheetDialogFragmentBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountsViewModel.progress.observe(this) { show ->
            show?.let { binding?.progress?.visibility = if (show) View.VISIBLE else View.GONE }
        }

        accountsViewModel.deletedSuccessfully.observe(this) { success ->
            success?.let { requireActivity().updateAccountNotifyBroadcast() }
        }

        lifecycleScope.launch {
            accountsViewModel.activeAccountFlow.collect { activeAccountTypeEntity ->
                activeAccountTypeEntity?.let {
                    activeAccount = activeAccountTypeEntity.toAccount()
                    selectedAccount = activeAccountTypeEntity.toAccount()
                }
            }
        }

        lifecycleScope.launch {
            accountsViewModel.allAccountsFlow.collect { accountEntities ->
                accounts = accountEntities.toAccounts()
                val currentActiveAccount = accounts.singleOrNull { act -> (act.isActive == 1) }
                binding?.addAccount?.visibility =
                    if ((ACCOUNTS_LIMIT - accounts.size) > 0) View.VISIBLE else View.GONE
                setUpAdapter(currentActiveAccount ?: accounts.first())
            }
        }

        binding?.ivClose?.setOnClickListener { dismiss() }

        binding?.addAccount?.setOnClickListener {
            handleAddAndEditAccount()
        }
    }

    /**
     *  Update current active account and dismiss list of accounts bottom screen
     */
    private fun updateAndDismiss() {
        selectedAccount?.let {
            if (selectedAccount != activeAccount) {
                onAccountSelected(selectedAccount!!)
                accountsViewModel.updateActiveAccount(selectedAccount!!)
            }
        }
        dismiss()
    }

    private fun setUpAdapter(currentActiveAccount: Account) {
        accountsAdapter = AccountsAdapter(
            ArrayList(accounts), requireContext(), currentActiveAccount
        ) { clickedAccount ->
            selectedAccount = clickedAccount.second
            handleAccountItemClick(clickedAccount.second, clickedAccount.first)
        }
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerViewAccounts?.layoutManager = mLayoutManager
        binding?.recyclerViewAccounts?.itemAnimator =
            androidx.recyclerview.widget.DefaultItemAnimator()
        binding?.recyclerViewAccounts?.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(), LinearLayoutManager.VERTICAL
            )
        )
        binding?.recyclerViewAccounts?.adapter = accountsAdapter
    }

    /**
     * Handle account click
     */
    private fun handleAccountItemClick(clickedAccount: Account, position: Int) {
        if (clickedAccount.name.uppercase() == AccountType.SAVINGS.name) {
            // Savings account can't be edited / deleted so update and return back!
            updateAndDismiss()
            return
        }
        val options = if (clickedAccount.id != activeAccount.id) {
            arrayOf(
                "Select Account",
                "Edit Account",
                "Delete Account",
            )
        } else {
            arrayOf(
                "Edit Account",
                "Delete Account",
            )
        }
        MaterialAlertDialogBuilder(requireActivity()).setTitle(
            String.format(
                "%s %s %s", "Manage", clickedAccount.name, "Account"
            )
        ).setItems(options) { dialog, which ->
            when (options[which]) {
                "Select Account" -> updateAndDismiss()
                "Edit Account" -> handleAddAndEditAccount(existingAccount = clickedAccount)
                "Delete Account" -> {
                    if (clickedAccount.name?.uppercase() == AccountType.SAVINGS.name) requireActivity().toast(
                        getString(R.string.saving_account_delete_disclaimer)
                    ) else removeConfirmation(clickedAccount, position)
                }
                else -> {}

            }
            dialog.dismiss()
        }.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }.setCancelable(false).show()
    }

    /**
     * existingAccount: Null in case of new account
     */
    private fun handleAddAndEditAccount(existingAccount: Account? = null) {
        AddEditAccountDialog.open(
            requireActivity(),
            account = existingAccount, // Always null in case of adding new account
            updateAccount = { newAccountTriple ->
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
                }
            },
            remainingAccounts = (ACCOUNTS_LIMIT - accounts.size),
            isPremiumUser = appPreferences.isUserPremium()
        )
    }

    /**
     * Delete account confirmation
     */
    private fun removeConfirmation(clickedAccount: Account, position: Int) {
        val accountLabeling =
            if (clickedAccount.name.contains("ACCOUNT"))
                clickedAccount.name else
                String.format("%s %s", clickedAccount.name, "ACCOUNT")

        val builder = android.app.AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setTitle(getString(R.string.title_delete_account_and_its_transactions))
            .setMessage(getString(R.string.description_delete_account_and_its_transactions,accountLabeling))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }.setPositiveButton(getString(R.string.yes_delete_it)) { dialog, _ ->
                requireActivity().toast("${getString(R.string.deleted)} $accountLabeling!")
                accountsAdapter.delete(position)
                accountsViewModel.deleteAccount(clickedAccount, accounts.first())
                accountsAdapter.notifyItemChanged(position)
                dialog.cancel()
                object : CountDownTimer(2000, 2000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        requireActivity().updateAccountNotifyBroadcast()
                    }
                }.start()
            }
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(resources.getColor(R.color.budget_red))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(resources.getColor(R.color.budget_green))
    }
}