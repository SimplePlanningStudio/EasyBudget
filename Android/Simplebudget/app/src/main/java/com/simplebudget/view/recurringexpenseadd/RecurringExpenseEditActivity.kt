/*
 *   Copyright 2025 Benoit LETONDOR
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
package com.simplebudget.view.recurringexpenseadd

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityRecurringExpenseAddBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.helper.extensions.toAccount
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.model.recurringexpense.ExpenseType
import com.simplebudget.model.recurringexpense.RecurringExpenseType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import com.simplebudget.prefs.hasUserSawSwitchExpenseHint
import com.simplebudget.prefs.setUserSawSwitchExpenseHint
import com.simplebudget.view.DatePickerDialogFragment
import com.simplebudget.view.accounts.AccountsSpinnerAdapter
import com.simplebudget.view.accounts.AccountsViewModel
import com.simplebudget.view.category.choose.ChooseCategoryActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

/**
 *
 */
class RecurringExpenseEditActivity : BaseActivity<ActivityRecurringExpenseAddBinding>() {

    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val viewModel: RecurringExpenseEditViewModel by viewModel()
    private val accountsViewModel: AccountsViewModel by viewModel()
    private lateinit var receiver: BroadcastReceiver
    private var adView: AdView? = null
    private var existingExpenseCategory: String = ""
    private var existingExpenseCategoryId: Long = 53 //Miscellaneous
    private var isEdit: Boolean = false
    private var isRevenue: Boolean = false

    private var activeAccount: Account? = null // Currently active account
    private var selectedAccount: Account? = null // Selected on list for display
    private var accountsSpinnerAdapter: AccountsSpinnerAdapter? = null
    private var accounts: List<Account>? = emptyList()

    /**
     *
     */
    override fun createBinding(): ActivityRecurringExpenseAddBinding {
        return ActivityRecurringExpenseAddBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_ADD_RECURRING_SCREEN)
        analyticsManager.logEvent(Events.KEY_ADD_RECURRING_EXPENSE)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Register receiver
        val filter = IntentFilter()
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(Intent.ACTION_VIEW)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    INTENT_IAB_STATUS_CHANGED -> viewModel.onIabStatusChanged()
                }
            }
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, filter)


        if (savedInstanceState == null) {
            val initDate = intent.getLongExtra("dateStart", 0)
            viewModel.initWithDateAndExpense(
                if (initDate <= 0) LocalDate.now() else LocalDate.ofEpochDay(initDate),
                intent.getParcelableExtra("expense")
            )
        }

        viewModel.premiumStatusLiveData.observe(this) { isPremium ->
            if (isPremium) {
                binding.adViewContainer.beGone()
            } else {
                loadBanner(
                    appPreferences.isUserPremium(),
                    binding.adViewContainer,
                    onBannerAdRequested = { bannerAdView ->
                        this.adView = bannerAdView
                    }
                )
            }
        }

        // Getting account of budget
        accountsViewModel.getAccountFromId.observe(this) { account ->
            selectedAccount = account
            binding.spinnerAccountTitle.setSelection(accounts?.indexOf(selectedAccount) ?: 0, false)
        }
        // Load accounts
        lifecycleScope.launch {
            accountsViewModel.allAccountsFlow.collect { accountEntities ->
                accounts = accountEntities.toAccounts()
                accountsSpinnerAdapter = AccountsSpinnerAdapter(
                    this@RecurringExpenseEditActivity,
                    R.layout.spinner_item,
                    accounts ?: emptyList()
                )
                binding.spinnerAccountTitle.adapter = accountsSpinnerAdapter
                binding.spinnerAccountTitle.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parentView: AdapterView<*>?,
                            selectedItemView: View?,
                            position: Int,
                            id: Long,
                        ) {
                            selectedAccount = accountsSpinnerAdapter?.getItem(position)
                        }

                        override fun onNothingSelected(parentView: AdapterView<*>?) {
                            // Handle the case where nothing is selected (optional)
                        }
                    }
            }
        }

        setUpButtons()
        setResult(Activity.RESULT_CANCELED)
        binding.dateButton.removeButtonBorder() // Remove border

        viewModel.editTypeLiveData.observe(this) { (isRevenue, isEditing) ->
            this.isEdit = isEditing
            this.isRevenue = isRevenue
            setExpenseTypeTextViewLayout(isRevenue, isEditing)
        }

        viewModel.existingExpenseEventStream.observe(this) { existingValues ->
            if (existingValues != null) {
                setUpTextFields(
                    existingValues.title,
                    existingValues.amount,
                    type = existingValues.type,
                    categoryType = existingValues.categoryType,
                    accountId = existingValues.accountId,
                    categoryId = existingValues.categoryId
                )
            } else {
                setUpTextFields(
                    description = null,
                    amount = null,
                    type = null,
                    categoryType = null,
                    accountId = 0L,
                    categoryId = existingExpenseCategoryId
                )
            }
        }

        viewModel.expenseDateLiveData.observe(this) { date ->
            setUpDateButton(date)
        }

        var progressDialog: ProgressDialog? = null
        viewModel.savingIsRevenueEventStream.observe(this) { isRevenue ->
            // Show a ProgressDialog
            val dialog = ProgressDialog(this)
            dialog.isIndeterminate = true
            dialog.setTitle(R.string.recurring_expense_add_loading_title)
            dialog.setMessage(getString(if (isRevenue) R.string.recurring_income_add_loading_message else R.string.recurring_expense_add_loading_message))
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()

            progressDialog = dialog
        }

        viewModel.finishLiveData.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null
            setResult(Activity.RESULT_OK)
            finish()
        }

        viewModel.errorEventStream.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null

            AlertDialog.Builder(this).setTitle(R.string.oops)
                .setMessage(getString(R.string.error_occurred_try_again))
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
        }

        viewModel.expenseAddBeforeInitDateEventStream.observe(this) {
            AlertDialog.Builder(this).setTitle(R.string.expense_add_before_init_date_dialog_title)
                .setMessage(R.string.expense_add_before_init_date_dialog_description)
                .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateConfirmed(
                        getCurrentAmount(),
                        binding.descriptionEdittext.text.toString(),
                        ExpenseType.getRecurringTypeFromSpinnerSelection(binding.expenseTypeSpinner.selectedItemPosition),
                        binding.tvCategoryName.text?.toString()
                            ?: ExpenseCategoryType.MISCELLANEOUS.name,
                        selectedAccount?.id ?: appPreferences.activeAccount(),
                        existingExpenseCategoryId
                    )
                }
                .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateCancelled()
                }.show()
        }

        //Handle category
        binding.tvCategoryName.setOnClickListener {
            handleCategoryLaunch()
        }

        //Show hint switch income / expense
        showCaseChangeExpenseIncomeSwitch()

        // Load accounts data
        lifecycleScope.launch {
            accountsViewModel.activeAccountFlow.collect { activeAccountTypeEntity ->
                activeAccountTypeEntity?.let {
                    activeAccount = activeAccountTypeEntity.toAccount()
                    selectedAccount = activeAccountTypeEntity.toAccount()
                }
            }
        }
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Validate user inputs
     *
     * @return true if user inputs are ok, false otherwise
     */
    private fun validateInputs(): Boolean {
        var ok = true

        val description = binding.descriptionEdittext.text.toString()
        if (description.trim { it <= ' ' }.isEmpty()) {
            binding.descriptionEdittext.error = resources.getString(R.string.no_description_error)
            ok = false
        }

        val amount = binding.amountEdittext.text.toString()
        if (amount.trim { it <= ' ' }.isEmpty()) {
            binding.amountEdittext.error = resources.getString(R.string.no_amount_error)
            ok = false
        } else {
            try {
                val value = java.lang.Double.parseDouble(amount)
                if (value <= 0) {
                    binding.amountEdittext.error =
                        resources.getString(R.string.negative_amount_error)
                    ok = false
                }
            } catch (e: Exception) {
                binding.amountEdittext.error = resources.getString(R.string.invalid_amount)
                ok = false
            }

        }

        val category = binding.tvCategoryName.text.toString()
        if (category.trim().isEmpty()) binding.tvCategoryName.text =
            ExpenseCategoryType.MISCELLANEOUS.name.uppercase()

        return ok
    }

    /**
     * Set-up revenue and payment buttons
     */
    private fun setUpButtons() {
        binding.expenseTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onExpenseRevenueValueChanged(isChecked)
        }

        binding.expenseTypeTv.setOnClickListener {
            viewModel.onExpenseRevenueValueChanged(!binding.expenseTypeSwitch.isChecked)
        }

        binding.saveExpenseFab.setOnClickListener {
            if (validateInputs()) {
                val selectedCategory = binding.tvCategoryName.text?.toString()?.uppercase()
                    ?: ExpenseCategoryType.MISCELLANEOUS.name
                val type: RecurringExpenseType =
                    ExpenseType.getRecurringTypeFromSpinnerSelection(binding.expenseTypeSpinner.selectedItemPosition)
                viewModel.onSave(
                    getCurrentAmount(),
                    binding.descriptionEdittext.text.toString(),
                    type,
                    selectedCategory,
                    selectedAccount?.id ?: appPreferences.activeAccount(),
                    existingExpenseCategoryId
                )
                //As account switched need to update selected account
                if (selectedAccount?.id != activeAccount?.id) {
                    accountsViewModel.updateActiveAccount(selectedAccount!!)
                    this@RecurringExpenseEditActivity.updateAccountNotifyBroadcast()
                }
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_ADD_RECURRING_EXPENSE,
                    mapOf(
                        Events.KEY_ADD_EXPENSE_SELECTED_INTERVAL to type.name,
                        Events.KEY_ADD_EXPENSE_SELECTED_CATEGORY to selectedCategory
                    )
                )
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEditing: Boolean) {
        if (isRevenue) {
            binding.expenseTypeTv.setText(R.string.income)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            binding.expenseTypeSwitch.isChecked = true

            if (isEditing) {
                setTitle(R.string.title_activity_recurring_income_edit)
            } else {
                setTitle(R.string.title_activity_recurring_income_add)
            }
        } else {
            binding.expenseTypeTv.setText(R.string.payment)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            binding.expenseTypeSwitch.isChecked = false

            if (isEditing) {
                setTitle(R.string.title_activity_recurring_expense_edit)
            } else {
                setTitle(R.string.title_activity_recurring_expense_add)
            }
        }
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(
        description: String?,
        amount: Double?,
        type: RecurringExpenseType?,
        categoryType: String?,
        accountId: Long,
        categoryId: Long?,
    ) {
        binding.amountInputlayout.hint =
            resources.getString(R.string.amount, appPreferences.getUserCurrency().symbol)

        val recurringTypesString = arrayOf(
            getString(R.string.recurring_interval_daily),
            getString(R.string.recurring_interval_weekly),
            getString(R.string.recurring_interval_bi_weekly),
            getString(R.string.recurring_interval_ter_weekly),
            getString(R.string.recurring_interval_four_weekly),
            getString(R.string.recurring_interval_monthly),
            getString(R.string.recurring_interval_bi_monthly),
            getString(R.string.recurring_interval_ter_monthly),
            getString(R.string.recurring_interval_six_monthly),
            getString(R.string.recurring_interval_yearly)
        )

        val adapter = ArrayAdapter(this, R.layout.spinner_item, recurringTypesString)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.expenseTypeSpinner.adapter = adapter

        if (type != null) {
            ExpenseType.setSpinnerSelectionFromRecurringType(type, binding.expenseTypeSpinner)
        } else {
            ExpenseType.setSpinnerSelectionFromRecurringType(
                RecurringExpenseType.MONTHLY, binding.expenseTypeSpinner
            )
        }

        if (description != null) {
            binding.descriptionEdittext.setText(description)
            binding.descriptionEdittext.setSelection(
                binding.descriptionEdittext.text?.length ?: 0
            ) // Put focus at the end of the text
        }

        binding.amountEdittext.preventUnsupportedInputForDecimals()

        if (amount != null) {
            binding.amountEdittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
        }

        existingExpenseCategory = categoryType ?: existingExpenseCategory
        existingExpenseCategoryId = categoryId ?: existingExpenseCategoryId
        binding.tvCategoryName.text = existingExpenseCategory

        accountsViewModel.getAccountFromId(if (accountId != 0L) accountId else appPreferences.activeAccount())
    }

    /**
     * Set up the date button
     */
    private fun setUpDateButton(date: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern(
            resources.getString(R.string.add_expense_date_format), Locale.getDefault()
        )
        binding.dateButton.text = formatter.format(date)

        binding.dateButton.setOnClickListener {
            val fragment = DatePickerDialogFragment.newInstance(originalDate = date)
            fragment.listener =
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    viewModel.onDateChanged(LocalDate.of(year, monthOfYear + 1, dayOfMonth))
                }
            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(binding.amountEdittext.text.toString())
    }


    override fun onResume() {
        resumeBanner(adView)
        super.onResume()
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    /**
     *
     */
    override fun onDestroy() {
        destroyBanner(adView)
        adView = null
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
        super.onDestroy()
    }


    /**
     * Start activity for result
     */
    private var securityActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val category =
                result.data?.getParcelableExtra(ChooseCategoryActivity.REQUEST_CODE_SELECTED_CATEGORY) as Category?
            existingExpenseCategoryId = category?.id ?: existingExpenseCategoryId
            binding.tvCategoryName.text = category?.name ?: ""
        }

    /**
     * Launch Security Activity
     */
    private fun handleCategoryLaunch() {
        securityActivityLauncher.launch(
            Intent(this, ChooseCategoryActivity::class.java).putExtra(
                ChooseCategoryActivity.REQUEST_CODE_CURRENT_EDIT_CATEGORY,
                (binding.tvCategoryName.text?.toString() ?: existingExpenseCategory)
            )
        )
    }

    /**
     * Show case Hint to switch income / expense
     */
    private fun showCaseChangeExpenseIncomeSwitch() {
        if (appPreferences.hasUserSawSwitchExpenseHint().not()) {
            switchDemo()
            showCaseView(
                targetView = binding.expenseTypeSwitch,
                title = getString(R.string.switch_expense_income_hint_title),
                message = getString(R.string.switch_expense_income_hint_message),
                handleGuideListener = {
                    appPreferences.setUserSawSwitchExpenseHint()
                })
        }
    }


    /**
     * Switch expense demo for Hint
     */
    private fun switchDemo() {
        if (!isEdit) {
            object : CountDownTimer(4000, 2000) {
                override fun onTick(millisUntilFinished: Long) {
                    setExpenseTypeTextViewLayout(!isRevenue, isEdit)
                }

                override fun onFinish() {
                    setExpenseTypeTextViewLayout(isRevenue, isEdit)
                }
            }.start()
        }
    }
}