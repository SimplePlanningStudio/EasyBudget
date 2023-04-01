/*
 *   Copyright 2023 Benoit LETONDOR
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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.databinding.ActivityRecurringExpenseAddBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.model.RecurringExpenseType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.hasUserSawSwitchExpenseHint
import com.simplebudget.prefs.resetUserSawSwitchExpenseHint
import com.simplebudget.prefs.setUserSawSwitchExpenseHint
import com.simplebudget.view.DatePickerDialogFragment
import com.simplebudget.view.category.CategoriesSearchActivity
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
    private val viewModel: RecurringExpenseEditViewModel by viewModel()
    private lateinit var receiver: BroadcastReceiver
    private var adView: AdView? = null
    private var existingExpenseCategory: String = ""
    private var isEdit: Boolean = false
    private var isRevenue: Boolean = false

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
                if (initDate <= 0) LocalDate.now() else
                    LocalDate.ofEpochDay(initDate),
                intent.getParcelableExtra("expense")
            )
        }

        viewModel.premiumStatusLiveData.observe(this) { isPremium ->
            if (isPremium) {
                val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
                adContainerView.visibility = View.INVISIBLE
            } else {
                loadAndDisplayBannerAds()
            }
        }

        setUpButtons()
        setResult(Activity.RESULT_CANCELED)

        if (willAnimateActivityEnter()) {
            animateActivityEnter(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.saveExpenseFab.animateFABAppearance()
                }
            })
        } else {
            binding.saveExpenseFab.animateFABAppearance()
        }

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
                    categoryType = existingValues.categoryType
                )
            } else {
                setUpTextFields(
                    description = null,
                    amount = null,
                    type = null,
                    categoryType = null
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

            AlertDialog.Builder(this)
                .setTitle(R.string.oops)
                .setMessage(getString(R.string.error_occurred_try_again))
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        viewModel.expenseAddBeforeInitDateEventStream.observe(this) {
            AlertDialog.Builder(this)
                .setTitle(R.string.expense_add_before_init_date_dialog_title)
                .setMessage(R.string.expense_add_before_init_date_dialog_description)
                .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateConfirmed(
                        getCurrentAmount(),
                        binding.descriptionEdittext.text.toString(),
                        getRecurringTypeFromSpinnerSelection(binding.expenseTypeSpinner.selectedItemPosition),
                        binding.tvCategoryName.text?.toString()
                            ?: ExpenseCategoryType.MISCELLANEOUS.name
                    )
                }
                .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                    viewModel.onAddExpenseBeforeInitDateCancelled()
                }
                .show()
        }

        //Handle category
        binding.tvCategoryName.setOnClickListener {
            handleCategoryLaunch()
        }

        //Show hint switch income / expense
        showCaseChangeExpenseIncomeSwitch()
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
        if (category.trim().isEmpty())
            binding.tvCategoryName.text = ExpenseCategoryType.MISCELLANEOUS.name.uppercase()

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
                viewModel.onSave(
                    getCurrentAmount(),
                    binding.descriptionEdittext.text.toString(),
                    getRecurringTypeFromSpinnerSelection(binding.expenseTypeSpinner.selectedItemPosition),
                    binding.tvCategoryName.text?.toString()?.uppercase()
                        ?: ExpenseCategoryType.MISCELLANEOUS.name
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
        categoryType: String?
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
            setSpinnerSelectionFromRecurringType(type)
        } else {
            setSpinnerSelectionFromRecurringType(RecurringExpenseType.MONTHLY)
        }

        if (description != null) {
            binding.descriptionEdittext.setText(description)
            binding.descriptionEdittext.setSelection(
                binding.descriptionEdittext.text?.length
                    ?: 0
            ) // Put focus at the end of the text
        }

        binding.amountEdittext.preventUnsupportedInputForDecimals()

        if (amount != null) {
            binding.amountEdittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
        }

        existingExpenseCategory = categoryType ?: ""
        binding.tvCategoryName.text = existingExpenseCategory
    }

    /**
     * Get the recurring expense type associated with the spinner selection
     *
     * @param spinnerSelectedItem index of the spinner selection
     * @return the corresponding expense type
     */
    private fun getRecurringTypeFromSpinnerSelection(spinnerSelectedItem: Int): RecurringExpenseType {
        return when (spinnerSelectedItem) {
            0 -> RecurringExpenseType.DAILY
            1 -> RecurringExpenseType.WEEKLY
            2 -> RecurringExpenseType.BI_WEEKLY
            3 -> RecurringExpenseType.TER_WEEKLY
            4 -> RecurringExpenseType.FOUR_WEEKLY
            5 -> RecurringExpenseType.MONTHLY
            6 -> RecurringExpenseType.BI_MONTHLY
            7 -> RecurringExpenseType.TER_MONTHLY
            8 -> RecurringExpenseType.SIX_MONTHLY
            9 -> RecurringExpenseType.YEARLY
            else -> RecurringExpenseType.NOTHING
        }
    }

    private fun setSpinnerSelectionFromRecurringType(type: RecurringExpenseType) {
        val selectionIndex = when (type) {
            RecurringExpenseType.DAILY -> 0
            RecurringExpenseType.WEEKLY -> 1
            RecurringExpenseType.BI_WEEKLY -> 2
            RecurringExpenseType.TER_WEEKLY -> 3
            RecurringExpenseType.FOUR_WEEKLY -> 4
            RecurringExpenseType.MONTHLY -> 5
            RecurringExpenseType.BI_MONTHLY -> 6
            RecurringExpenseType.TER_MONTHLY -> 7
            RecurringExpenseType.SIX_MONTHLY -> 8
            RecurringExpenseType.YEARLY -> 9
            else -> 0
        }
        binding.expenseTypeSpinner.setSelection(selectionIndex, false)
    }

    /**
     * Set up the date button
     */
    private fun setUpDateButton(date: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern(
            resources.getString(R.string.add_expense_date_format),
            Locale.getDefault()
        )
        binding.dateButton.text = formatter.format(date)

        binding.dateButton.setOnClickListener {
            val fragment = DatePickerDialogFragment(date) { _, year, monthOfYear, dayOfMonth ->
                viewModel.onDateChanged(LocalDate.of(year, monthOfYear + 1, dayOfMonth))
            }

            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(binding.amountEdittext.text.toString())
    }

    /**
     *
     */
    private fun loadAndDisplayBannerAds() {
        try {
            val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
            adContainerView.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(this, windowManager.defaultDisplay)!!
            adView = AdView(this)
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            adContainerView.addView(adView)
            val actualAdRequest = AdRequest.Builder()
                .build()
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
     *
     */
    override fun onDestroy() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
        super.onDestroy()
    }


    /**
     * Start activity for result
     */
    private var securityActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val category =
                result.data?.getStringExtra(CategoriesSearchActivity.REQUEST_CODE_SELECTED_CATEGORY)
                    ?: ""
            binding.tvCategoryName.text = category
        }

    /**
     * Launch Security Activity
     */
    private fun handleCategoryLaunch() {
        securityActivityLauncher.launch(
            Intent(this, CategoriesSearchActivity::class.java).putExtra(
                CategoriesSearchActivity.REQUEST_CODE_CURRENT_EDIT_CATEGORY,
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
                }
            )
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