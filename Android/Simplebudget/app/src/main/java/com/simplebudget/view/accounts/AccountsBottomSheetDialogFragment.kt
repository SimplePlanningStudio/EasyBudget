package com.simplebudget.view.accounts

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplebudget.R
import com.simplebudget.base.BaseDialogFragment
import com.simplebudget.databinding.AccountsBottomSheetDialogFragmentBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.ads.AdSdkManager
import com.simplebudget.helper.ads.NativeTemplateStyle
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.helper.extensions.beVisible
import com.simplebudget.helper.extensions.isDefault
import com.simplebudget.helper.extensions.toAccount
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.Account
import com.simplebudget.model.account.appendAccount
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.setActiveAccount
import com.simplebudget.view.accounts.adapter.AccountsAdapter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.graphics.drawable.toDrawable


class AccountsBottomSheetDialogFragment(
    private val onAccountSelected: (Account) -> Unit,
    private val onAccountUpdated: (Account) -> Unit,
) : BaseDialogFragment<AccountsBottomSheetDialogFragmentBinding>() {

    private val accountsViewModel: AccountsViewModel by viewModel()

    private val toastManager: ToastManager by inject()

    private val appPreferences: AppPreferences by inject()

    private lateinit var activeAccount: Account // Currently active account

    private var selectedAccount: Account? = null // Selected on list for display

    private lateinit var accountsAdapter: AccountsAdapter

    private var accounts: List<Account> = emptyList()

    private var nativeAd: NativeAd? = null

    companion object {
        const val TAG = "AccountsBottomSheetDialogFragment"
    }

    override fun onCreateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): AccountsBottomSheetDialogFragmentBinding =
        AccountsBottomSheetDialogFragmentBinding.inflate(inflater, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountsViewModel.progress.observe(this) { show ->
            show?.let { binding?.progress?.visibility = if (show) View.VISIBLE else View.GONE }
        }

        accountsViewModel.deletedSuccessfully.observe(this) { success ->
            success?.let {
                if (this@AccountsBottomSheetDialogFragment.isDetached.not()) {
                    requireActivity().updateAccountNotifyBroadcast()
                }
            }
        }

        lifecycleScope.launch {
            accountsViewModel.activeAccountFlow.collect { activeAccountTypeEntity ->
                activeAccountTypeEntity?.let {
                    appPreferences.setActiveAccount(
                        accountId = activeAccountTypeEntity.id,
                        accountName = activeAccountTypeEntity.name
                    )
                    activeAccount = activeAccountTypeEntity.toAccount()
                    selectedAccount = activeAccountTypeEntity.toAccount()
                }
            }
        }

        lifecycleScope.launch {
            accountsViewModel.allAccountsFlow.collect { accountEntities ->
                if (accountEntities.isNotEmpty()) {
                    accounts = accountEntities.toAccounts()
                    if (accounts.isNotEmpty()) {
                        val currentActiveAccount: Account? =
                            accounts.singleOrNull { act -> (act.isActive == 1) }
                        binding?.addAccount?.visibility =
                            if ((ACCOUNTS_LIMIT - accounts.size) > 0) View.VISIBLE else View.GONE
                        setUpAdapter(currentActiveAccount ?: accounts.first())
                    }
                }
            }
        }

        binding?.ivClose?.setOnClickListener { dismiss() }

        binding?.addAccount?.setOnClickListener {
            handleAddAndEditAccount()
        }
        if (appPreferences.isUserPremium()
                .not() && InternetUtils.isInternetAvailable(requireContext())
        ) {
            binding?.nativeAdTemplate?.beVisible()
            AdSdkManager.initialize(requireContext()) {
                //Load native ads
                loadNativeAd()
            }
        } else {
            binding?.nativeAdTemplate?.beGone()
        }
    }

    /**
     *  Update current active account and dismiss list of accounts bottom screen
     */
    private fun updateAndDismiss() {
        selectedAccount?.let {
            appPreferences.setActiveAccount(
                accountId = selectedAccount!!.id, accountName = selectedAccount!!.name
            )
            onAccountSelected(selectedAccount!!)
            accountsViewModel.updateActiveAccount(selectedAccount!!)
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
        val options = if (clickedAccount.isDefault()) {
            // DEFAULT_ACCOUNT can't be edited / deleted so update and return back!
            arrayOf(
                "Select Account",
                "Edit Account",
            )
        } else {
            arrayOf(
                "Select Account",
                "Edit Account",
                "Delete Account",
            )
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle(
            String.format(
                "%s %s %s", "Manage", clickedAccount.name, "Account"
            )
        ).setItems(options) { dialog, which ->
            when (options[which]) {
                "Select Account" -> updateAndDismiss()
                "Edit Account" -> handleAddAndEditAccount(existingAccount = clickedAccount)
                "Delete Account" -> {
                    if (clickedAccount.isDefault() && this@AccountsBottomSheetDialogFragment.isDetached.not()) {
                        toastManager.showShort(
                            getString(R.string.default_account_delete_disclaimer)
                        )
                    } else {
                        removeConfirmation(clickedAccount, position)
                    }
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
        try {
            if (this@AccountsBottomSheetDialogFragment.isDetached && accounts.isEmpty()) return
            AddEditAccountDialog.open(
                requireActivity(),
                account = existingAccount, // Always null in case of adding new account
                addUpdateAccount = { newAccountTriple ->
                    val accountAlreadyExists = accounts.filter { act ->
                        (act.name.uppercase().contains(newAccountTriple.first))
                    }
                    if (accountAlreadyExists.isEmpty()) {
                        // Add you account to DB
                        if (newAccountTriple.second) {
                            // Editing case
                            val editedAccount = Account(
                                id = newAccountTriple.third?.id, name = newAccountTriple.first
                            )
                            onAccountUpdated.invoke(editedAccount)
                            accountsViewModel.updateActiveAccount(editedAccount)
                        } else {
                            // Adding new case
                            accountsViewModel.addAccount(Account(name = newAccountTriple.first))
                        }
                    } else {
                        toastManager.showShort(getString(R.string.account_already_exists))
                    }
                },
                remainingAccounts = (ACCOUNTS_LIMIT - accounts.size),
                isPremiumUser = appPreferences.isUserPremium(),
                dismissAccountBottomSheet = {
                    dismiss()
                },
                toastManager = toastManager
            )
        } catch (e: Exception) {
            Logger.error("AddEditAccountDialog: Error handling add edit account", e)
        }
    }

    /**
     * Delete account confirmation
     */
    private fun removeConfirmation(clickedAccount: Account, position: Int) {
        val builder = android.app.AlertDialog.Builder(requireActivity())
        builder.setCancelable(false)
        builder.setTitle(getString(R.string.title_delete_account_and_its_transactions)).setMessage(
            getString(
                R.string.description_delete_account_and_its_transactions,
                clickedAccount.name.appendAccount()
            )
        ).setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }.setPositiveButton(getString(R.string.yes_delete_it)) { dialog, _ ->
            if (this@AccountsBottomSheetDialogFragment.isDetached.not()) {
                toastManager.showLong("${getString(R.string.deleted)} ${clickedAccount.name.appendAccount()}!")
            }
            if (accounts.isEmpty()) {
                dialog.cancel()
                return@setPositiveButton
            }
            accountsAdapter.delete(position)
            accountsViewModel.deleteAccount(clickedAccount, accounts.first())
            accountsAdapter.notifyItemChanged(position)
            dialog.cancel()
            object : CountDownTimer(500, 500) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    if (this@AccountsBottomSheetDialogFragment.isDetached.not()) {
                        requireActivity().updateAccountNotifyBroadcast()
                    }
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


    /**
     * Load native ads
     */
    private fun loadNativeAd() {
        try {
            val builder = AdLoader.Builder(requireActivity(), getString(R.string.native_ad_unit_id))
            builder.forNativeAd { nativeAd ->
                populateNativeAdView(nativeAd)
            }
            val adLoader = builder.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Logger.warning("NativeAd", "Failed to load: ${loadAdError.message}")
                    nativeAd = null
                }
            }).build()
            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Logger.error("NativeAd", "Failed to load: ${e.localizedMessage}", e)
        }
    }

    /**
     *
     */
    private fun populateNativeAdView(nativeAd: NativeAd) {
        try {
            val styles = NativeTemplateStyle.Builder()
                .withMainBackgroundColor(requireActivity().getColor(R.color.white).toDrawable())
                .build()
            binding?.nativeAdTemplate?.setStyles(styles)
            binding?.nativeAdTemplate?.setNativeAd(nativeAd)
            this.nativeAd = nativeAd
        } catch (e: Exception) {
            Logger.error("NativeTemplateStyle", "Failed to populate: ${e.localizedMessage}", e)
        }
    }

    /**
     * Destroy native ads
     */
    override fun onDestroyView() {
        this.nativeAd?.destroy()
        this.nativeAd = null
        super.onDestroyView()
    }
}