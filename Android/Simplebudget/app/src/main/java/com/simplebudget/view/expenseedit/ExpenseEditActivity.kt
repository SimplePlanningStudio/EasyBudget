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
package com.simplebudget.view.expenseedit

import android.app.Activity
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityExpenseEditBinding
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
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.prefs.*
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
 * Activity to add a new expense
 *
 * @author Benoit LETONDOR
 */
class ExpenseEditActivity : BaseActivity<ActivityExpenseEditBinding>() {

    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val viewModel: ExpenseEditViewModel by viewModel()
    private val accountsViewModel: AccountsViewModel by viewModel()
    private lateinit var receiver: BroadcastReceiver
    private var adView: AdView? = null
    private var existingExpenseCategory: String = ""
    private var existingExpenseCategoryId: Long = 53 // Miscellaneous category
    private var isEdit: Boolean = false
    private var isRevenue: Boolean = false
    private var activeAccount: Account? = null // Currently active account
    private var selectedAccount: Account? = null // Selected on list for display
    private var accountsSpinnerAdapter: AccountsSpinnerAdapter? = null
    private var accounts: List<Account>? = emptyList()


    override fun createBinding(): ActivityExpenseEditBinding =
        ActivityExpenseEditBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_ADD_EXPENSE_SCREEN)

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

        viewModel.premiumStatusLiveData.observe(this) { isPremium ->
            if (isPremium) {
                binding.adViewContainer.beGone()
            } else {
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
                    this@ExpenseEditActivity, R.layout.spinner_item, accounts ?: emptyList()
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

        viewModel.existingExpenseEventStream.observe(this) { existingValues ->
            if (existingValues != null) {
                setUpTextFields(
                    existingValues.title,
                    existingValues.amount,
                    existingValues.categoryType,
                    existingValues.accountId,
                    existingValues.categoryId
                )
            } else {
                setUpTextFields(
                    description = null,
                    amount = null,
                    categoryType = null,
                    accountId = 0L,
                    categoryId = 0L
                )
            }
        }

        if (savedInstanceState == null) {
            viewModel.initWithDateAndExpense(
                LocalDate.ofEpochDay(intent.getLongExtra("date", LocalDate.now().toEpochDay())),
                intent.getParcelableExtra("expense")
            )
        }

        setUpButtons()

        setResult(Activity.RESULT_CANCELED)

        binding.dateButton.removeButtonBorder()

        viewModel.editTypeLiveData.observe(this) { (isRevenue, isEdit) ->
            this.isEdit = isEdit
            this.isRevenue = isRevenue
            setExpenseTypeTextViewLayout(isRevenue, isEdit)
        }

        viewModel.expenseDateLiveData.observe(this) { date ->
            setUpDateButton(date)
        }

        viewModel.finishEventStream.observe(this) {
            setResult(Activity.RESULT_OK)
            finish()
        }

        viewModel.expenseAddBeforeInitDateEventStream.observe(this) {
            AlertDialog.Builder(this).setTitle(R.string.expense_add_before_init_date_dialog_title)
                .setMessage(R.string.expense_add_before_init_date_dialog_description)
                .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateConfirmed(
                        getCurrentAmount(),
                        binding.descriptionEdittext.text.toString(),
                        binding.tvCategoryName.text?.toString() ?: "",
                        selectedAccount?.id ?: appPreferences.activeAccount(),
                        existingExpenseCategoryId
                    )
                }
                .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateCancelled()
                }.show()
        }

        //Handle categories
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
                val value = java.lang.Double.valueOf(amount)
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
        if (category.trim().isEmpty()) {
            binding.tvCategoryName.text = ExpenseCategoryType.MISCELLANEOUS.name.uppercase()
        }

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
                    ?: ExpenseCategoryType.MISCELLANEOUS.name.uppercase()
                viewModel.onSave(
                    getCurrentAmount(),
                    binding.descriptionEdittext.text.toString(),
                    selectedCategory,
                    selectedAccount?.id ?: appPreferences.activeAccount(),
                    existingExpenseCategoryId

                )
                //As account switched need to update selected account
                if (selectedAccount?.id != activeAccount?.id) {
                    accountsViewModel.updateActiveAccount(selectedAccount!!)
                    this@ExpenseEditActivity.updateAccountNotifyBroadcast()
                }
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_ADD_EXPENSE,
                    mapOf(
                        Events.KEY_ADD_EXPENSE_SELECTED_CATEGORY to selectedCategory
                    )
                )
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEdit: Boolean) {
        if (isRevenue) {
            binding.expenseTypeTv.setText(R.string.income)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            binding.expenseTypeSwitch.isChecked = true

            setTitle(if (isEdit) R.string.title_activity_edit_income else R.string.title_activity_add_income)
        } else {
            binding.expenseTypeTv.setText(R.string.payment)
            binding.expenseTypeTv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            binding.expenseTypeSwitch.isChecked = false

            setTitle(if (isEdit) R.string.title_activity_edit_expense else R.string.title_activity_add_expense)
        }
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(
        description: String?,
        amount: Double?,
        categoryType: String?,
        accountId: Long,
        categoryId: Long?,
    ) {
        binding.amountInputlayout.hint =
            resources.getString(R.string.amount, appPreferences.getUserCurrency().symbol)

        if (description != null) {
            binding.descriptionEdittext.setText(description)
            if (binding.descriptionEdittext.text.isNullOrEmpty().not())
                binding.descriptionEdittext.setSelection(
                    binding.descriptionEdittext.text?.length ?: 0
                ) // Put focus at the end of the text
        }

        binding.amountEdittext.preventUnsupportedInputForDecimals()

        if (amount != null) {
            binding.amountEdittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
        }
        //Hold this to display over category spinner
        existingExpenseCategory = categoryType ?: existingExpenseCategory
        existingExpenseCategoryId =
            categoryId ?: existingExpenseCategoryId // Miscellaneous category
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

    /**
     *
     */
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
            binding.tvCategoryName.text = category?.name ?: existingExpenseCategory

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
