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
package com.simplebudget.view.budgets.addBudget

import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityAddEditBudgetBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.namesAsCommaSeparatedString
import com.simplebudget.helper.extensions.toAccount
import com.simplebudget.helper.extensions.toAccounts
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.model.account.Account
import com.simplebudget.model.budget.Budget
import com.simplebudget.model.budget.RecurringBudgetType
import com.simplebudget.model.budget.arrayOfRecurringBudgetType
import com.simplebudget.model.category.Category
import com.simplebudget.prefs.*
import com.simplebudget.view.DatePickerDialogFragment
import com.simplebudget.view.accounts.AccountsSpinnerAdapter
import com.simplebudget.view.accounts.AccountsViewModel
import com.simplebudget.view.category.choose.ChooseCategoryActivity
import com.simplebudget.view.category.choose.ChooseCategoryViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import kotlin.math.abs
import androidx.core.graphics.createBitmap
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.iab.isUserPremium

/**
 * Activity to add budget
 *
 * @author Waheed Nazir
 */
class AddBudgetActivity : BaseActivity<ActivityAddEditBudgetBinding>() {

    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private val chooseCategoryViewModel: ChooseCategoryViewModel by viewModel()
    private val accountsViewModel: AccountsViewModel by viewModel()
    private val addBudgetViewModel: AddBudgetViewModel by viewModel()
    private lateinit var receiver: BroadcastReceiver
    private var adView: AdView? = null
    private var existingExpenseCategory: String = ""
    private var selectedCategories: ArrayList<Category> = ArrayList<Category>()
    private lateinit var miscellaneousCategory: Category
    private var activeAccount: Account? = null // Currently active account
    private var selectedAccount: Account? = null // Selected on list for display
    private lateinit var accountsSpinnerAdapter: AccountsSpinnerAdapter
    private var accounts: List<Account>? = null
    private var editingBudget: Budget? = null
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now()

    override fun createBinding(): ActivityAddEditBudgetBinding =
        ActivityAddEditBudgetBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_ADD_BUDGET_SCREEN)

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
                    INTENT_IAB_STATUS_CHANGED -> chooseCategoryViewModel.onIabStatusChanged()
                }
            }
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, filter)

        chooseCategoryViewModel.premiumStatusLiveData.observe(this) { isPremium ->
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
        // In case of editing budget we'll receive it's associated category,account
        addBudgetViewModel.existingBudgetEventStream.observe(this) { existingBudgetData ->
            if (existingBudgetData != null) {
                setUpTextFields(
                    description = existingBudgetData.editingBudget?.goal,
                    amount = existingBudgetData.editingBudget?.budgetAmount ?: 0.0,
                    categories = existingBudgetData.categories,
                    type = existingBudgetData.type,
                    account = existingBudgetData.account
                )
            } else {
                setUpTextFields(
                    description = null,
                    amount = null,
                    categories = emptyList(),
                    type = null,
                    account = activeAccount
                )
            }
        }

        //Load editing budget
        if (savedInstanceState == null) {
            editingBudget = intent.getParcelableExtra(REQUEST_CODE_BUDGET) as Budget?
            addBudgetViewModel.initExistingBudgetToEdit(editingBudget)
        }


        chooseCategoryViewModel.miscellaneousCategory.observe(this) { category ->
            miscellaneousCategory = category
        }

        addBudgetViewModel.doneAddingWithBudget.observe(this) { budget ->
            budget?.let {
                finish()
            }
        }
        lifecycleScope.launch {
            accountsViewModel.activeAccountFlow.collect { activeAccountTypeEntity ->
                activeAccountTypeEntity?.let {
                    activeAccount = activeAccountTypeEntity.toAccount()
                    selectedAccount = activeAccountTypeEntity.toAccount()
                    binding.selectAccount.setText(selectedAccount?.name ?: "")
                }
            }
        }

        lifecycleScope.launch {
            accountsViewModel.allAccountsFlow.collect { accountEntities ->
                accounts = accountEntities.toAccounts()
                accountsSpinnerAdapter = AccountsSpinnerAdapter(
                    this@AddBudgetActivity, R.layout.spinner_item, accounts!!, true
                )
            }
        }

        setUpButtons()
        addBudgetViewModel.expenseFirstInstanceDateLiveData.observe(this) { startDate ->
            startDate?.let {
                this.startDate = startDate
                setUpStartDateButton(startDate)
            }
        }

        addBudgetViewModel.expenseLastInstanceDateLiveData.observe(this) { endDate ->
            endDate?.let {
                this.endDate = endDate
                setUpEndDateButton(endDate)
            }
        }
        addBudgetViewModel.onUpdateFirstInstance(editingBudget?.startDate)
        addBudgetViewModel.onUpdateLastInstance(editingBudget?.endDate)
        setResult(Activity.RESULT_CANCELED)
        //Handle categories
        binding.selectCategory.setOnClickListener {
            handleCategoryLaunch()
        }

        var progressDialog: ProgressDialog? = null
        addBudgetViewModel.savingBudgetEventStream.observe(this) {
            // Show a ProgressDialog
            val dialog = ProgressDialog(this)
            dialog.isIndeterminate = true
            dialog.setTitle(R.string.recurring_expense_add_loading_title)
            dialog.setMessage(getString(R.string.saving_budget))
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.show()
            progressDialog = dialog
        }

        addBudgetViewModel.finishLiveData.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null
            setResult(Activity.RESULT_OK)
            finish()
        }

        addBudgetViewModel.errorEventStream.observe(this) {
            progressDialog?.dismiss()
            progressDialog = null
            AlertDialog.Builder(this).setTitle(R.string.oops)
                .setMessage(getString(R.string.error_occurred_try_again))
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
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
        if (selectedCategories.isEmpty()) {
            binding.selectCategory.error = resources.getString(R.string.select_category)
            ok = false
        }

        return ok
    }

    /**
     *
     */
    private fun setUpStartDateButton(startDate: LocalDate) {
        binding.firstInstance.setText(startDate.getFormattedDate(this))
        binding.firstInstance.setOnClickListener {
            val fragment = DatePickerDialogFragment(
                originalDate = startDate
            ) { _, year, monthOfYear, dayOfMonth ->
                addBudgetViewModel.onUpdateFirstInstance(
                    LocalDate.of(
                        year, monthOfYear + 1, dayOfMonth
                    )
                )
            }
            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    /**
     *
     */
    private fun setUpEndDateButton(endDate: LocalDate) {
        binding.lastInstance.setText(endDate.getFormattedDate(this))
        binding.lastInstance.setOnClickListener {
            val fragment = DatePickerDialogFragment(
                originalDate = endDate,
                minDateOverride = startDate.plusDays(1)
            ) { _, year, monthOfYear, dayOfMonth ->
                addBudgetViewModel.onUpdateLastInstance(
                    LocalDate.of(
                        year, monthOfYear + 1, dayOfMonth
                    )
                )
            }
            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    /**
     * Set-up revenue and payment buttons
     */
    private fun setUpButtons() {
        binding.saveBudget.setOnClickListener {
            if (validateInputs()) {
                addBudgetViewModel.saveBudget(
                    goal = binding.descriptionEdittext.text.toString(),
                    accountId = selectedAccount?.id ?: appPreferences.activeAccount(),
                    budgetAmount = getCurrentAmount(),
                    type = RecurringBudgetType.valueOf(
                        binding.interval.text.toString()
                    ),
                    selectedCategories
                )
                analyticsManager.logEvent(
                    Events.KEY_BUDGET_ADDED, mapOf(
                        Events.KEY_ADD_BUDGET_CATEGORIES to selectedCategories.namesAsCommaSeparatedString(),
                        Events.KEY_ADD_BUDGET_INTERVAL to binding.interval.text.toString(),
                    )
                )
            }
        }
        // interval
        binding.interval.setOnClickListener {
            if (editingBudget == null) {
                // Not editing a budget
                intervalDialog()
            } else if (editingBudget?.associatedRecurringBudget != null && editingBudget?.associatedRecurringBudget?.type != RecurringBudgetType.ONE_TIME) {
                // Show interval dialog only if it's a recurring budget
                intervalDialog()
            }

        }
        // Select account
        binding.selectAccount.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(getString(R.string.select_account))
                .setAdapter(accountsSpinnerAdapter) { dialog, which ->
                    selectedAccount = accountsSpinnerAdapter.getItem(which)
                    binding.selectAccount.setText(selectedAccount?.name ?: "")
                    dialog.dismiss()
                }.show()
        }
    }

    /**
     * Interval dialog
     */
    private fun intervalDialog() {
        //For budget editing don't allow change interval
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.select_interval))
            .setItems(arrayOfRecurringBudgetType) { dialog, which ->
                val selected = arrayOfRecurringBudgetType[which]
                binding.interval.setText(selected)
                binding.endingDateInputLayout.visibility =
                    if (which == 0) View.VISIBLE else View.GONE
                dialog.dismiss()
            }.show()
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(
        description: String?,
        amount: Double?,
        categories: List<Category>,
        type: RecurringBudgetType?,
        account: Account?,
    ) {
        binding.amountInputlayout.hint = resources.getString(
            R.string.budget_amount_hint, appPreferences.getUserCurrency().symbol
        )

        if (type == null) {
            binding.interval.setText(RecurringBudgetType.MONTHLY.name) // Monthly
        } else {
            binding.interval.setText(type.name)
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
        selectedCategories.addAll(categories)
        addChipsToCategories()
        binding.selectAccount.setText(account?.name ?: "")

        try {
            binding.endingDateInputLayout.visibility =
                if (type != null && type == RecurringBudgetType.ONE_TIME) View.VISIBLE else View.GONE

            // Check if editing a one-time budget
            if (editingBudget != null && editingBudget?.associatedRecurringBudget == null) {
                binding.intervalTextInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
                binding.intervalTextInputLayout.setEndIconDrawable(R.drawable.ic_contact_us_black)
                binding.intervalTextInputLayout.setEndIconOnClickListener {
                    DialogUtil.createDialog(
                        this,
                        title = getString(R.string.edit_one_time_budget_interval),
                        message = getString(R.string.cant_edit_one_time_budget_interval),
                        positiveBtn = getString(R.string.ok),
                        negativeBtn = "",
                        isCancelable = true,
                        positiveClickListener = {},
                        negativeClickListener = {},
                    )?.show()
                }
            } else {
                binding.intervalTextInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
            }
        } catch (_: Exception) {
        }
    }

    /**
     *
     */
    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(binding.amountEdittext.text.toString())
    }

    /**
     *
     */
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
    private var chooseCategoriesActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val categories =
                result.data?.getParcelableArrayListExtra<Category>(ChooseCategoryActivity.REQUEST_CODE_SELECTED_CATEGORY) as ArrayList<Category>
            selectedCategories.clear()
            selectedCategories.addAll(categories)
            addChipsToCategories()
            binding.scrollViewAddBudget.post {
                binding.scrollViewAddBudget.smoothScrollTo(
                    0, binding.saveBudget.bottom + 50 //50px extra buffer for the scroll.
                )
            }
        }


    /**
     * Launch Choose Categories Activity
     */
    private fun handleCategoryLaunch() {
        chooseCategoriesActivityLauncher.launch(
            Intent(this, ChooseCategoryActivity::class.java).putExtra(
                ChooseCategoryActivity.REQUEST_CODE_MULTI_SELECT, true
            )
        )
    }


    companion object {
        const val REQUEST_CODE_BUDGET = "RQ_EDIT_BUDGET"
    }


    /**
     * Add categories chips to the select category
     */
    private fun addChipsToCategories() {
        val spannableStringBuilder = SpannableStringBuilder()
        val margin = resources.displayMetrics.density * 2 // Convert 2dp to pixels

        selectedCategories.forEach { category ->
            // Create a Chip programmatically
            val chip = Chip(this).apply {
                text = category.name
                textSize = 10f
                isCloseIconVisible = false
                setChipBackgroundColorResource(R.color.white)
                setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
            }
            // Create a bitmap from the Chip view
            chip.measure(
                View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED
            )
            chip.layout(0, 0, chip.measuredWidth, chip.measuredHeight)

            val bitmap = createBitmap(chip.measuredWidth, chip.measuredHeight)
            val canvas = Canvas(bitmap)
            chip.draw(canvas)

            val chipSpan = ImageSpan(this, bitmap)

            // Append a blank space for margin before the chip
            val blankDrawable = createBitmap(margin.toInt(), chip.measuredHeight)
            spannableStringBuilder.append(" ")

            // Add the blank space as a margin
            val marginSpan = ImageSpan(this, blankDrawable)
            spannableStringBuilder.setSpan(
                marginSpan,
                spannableStringBuilder.length - 1,
                spannableStringBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Append chip span to the SpannableStringBuilder
            val start = spannableStringBuilder.length
            spannableStringBuilder.append(" ${category.name} ")
            val end = spannableStringBuilder.length

            spannableStringBuilder.setSpan(
                chipSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Set the updated text with chips back to the EditText
        binding.selectCategory.text = spannableStringBuilder

        binding.scrollViewAddBudget.post {
            binding.scrollViewAddBudget.smoothScrollTo(
                0, binding.saveBudget.bottom + 50 //50px extra buffer for the scroll.
            )
        }

    }


}
