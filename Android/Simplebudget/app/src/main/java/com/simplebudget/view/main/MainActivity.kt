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
package com.simplebudget.view.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.messaging.FirebaseMessaging
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidListener
import com.simplebudget.R
import com.simplebudget.SimpleBudget
import com.simplebudget.databinding.ActivityMainBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.model.Expense
import com.simplebudget.model.RecurringExpenseDeleteType
import com.simplebudget.prefs.*
import com.simplebudget.push.MyFirebaseMessagingService
import com.simplebudget.view.breakdown.base.BreakDownBaseActivity
import com.simplebudget.view.expenseedit.ExpenseEditActivity
import com.simplebudget.view.main.calendar.CalendarFragment
import com.simplebudget.view.premium.PremiumActivity
import com.simplebudget.view.recurringexpenseadd.RecurringExpenseEditActivity
import com.simplebudget.view.report.base.MonthlyReportBaseActivity
import com.simplebudget.view.search.base.SearchBaseActivity
import com.simplebudget.view.security.SecurityActivity
import com.simplebudget.view.selectcurrency.SelectCurrencyFragment
import com.simplebudget.view.settings.SettingsActivity
import com.simplebudget.view.settings.SettingsActivity.Companion.SHOW_BACKUP_INTENT_KEY
import com.simplebudget.view.welcome.WelcomeActivity
import org.koin.android.ext.android.inject
import java.util.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/**
 * Main activity containing Calendar and List of expenses
 *
 * @author Benoit LETONDOR
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var receiver: BroadcastReceiver

    private lateinit var calendarFragment: CalendarFragment
    private lateinit var expensesViewAdapter: ExpensesRecyclerViewAdapter

    private var lastStopDate: Date? = null

    private val viewModel: MainViewModel by viewModel()
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null
    private var isUserPremium = false
    private var expenseOfSelectedDay: Double = 0.0

// ------------------------------------------>

    /**
     * Launch Security Activity
     */
    private fun handleAppPasswordProtection() {
        if (appPreferences.isAppPasswordProtectionOn()) {
            binding.framelayoutOpaque.visibility = View.VISIBLE
            val intent = Intent(this, SecurityActivity::class.java)
                .putExtra("HASH", appPreferences.appPasswordHash())
                .putExtra("TAB_INDEX", appPreferences.appProtectionType())
                .putExtra(
                    SecurityActivity.REQUEST_CODE_SECURITY_TYPE,
                    SecurityActivity.VERIFICATION
                )
            securityActivityLauncher.launch(intent)
        }
    }

    /**
     * Start activity for result
     */
    var securityActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                finish()
            } else {
                binding.framelayoutOpaque.visibility = View.GONE
            }
        }

    /**
     * Create binding
     */
    override fun createBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        this.window
            .setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

        setSupportActionBar(binding.toolbar)
        handleAppPasswordProtection()
        initCalendarFragment(savedInstanceState)
        initRecyclerView()
        calendarRevealAnimation()

        /*
           Init firebase in app messaging click
        */
        //////////////////////////////////////////////////////////////////////
        FirebaseInAppMessaging.getInstance().addClickListener { inAppMessage, action ->
            try {
                val mapData: Map<*, *>? = inAppMessage.data
                action.button?.text?.toString()?.let { act ->
                    if (act == "Let's Try") {
                        mapData?.containsKey("route")?.let {
                            if (it) {
                                if (mapData["route"].toString() == "display_premium_screen") {
                                    startActivity(Intent(this, PremiumActivity::class.java))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //////////////////////////////////////////////////////////////////////

        // Register receiver
        val filter = IntentFilter()
        filter.addAction(INTENT_EXPENSE_DELETED)
        filter.addAction(INTENT_RECURRING_EXPENSE_DELETED)
        filter.addAction(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        filter.addAction(INTENT_SHOW_WELCOME_SCREEN)
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(Intent.ACTION_VIEW)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    INTENT_EXPENSE_ADDED -> {
                        viewModel.onExpenseAdded()
                    }
                    INTENT_EXPENSE_DELETED -> {
                        val expense = intent.getParcelableExtra<Expense>("expense")!!

                        viewModel.onDeleteExpenseClicked(expense)
                    }
                    INTENT_RECURRING_EXPENSE_DELETED -> {
                        val expense = intent.getParcelableExtra<Expense>("expense")!!
                        val deleteType = RecurringExpenseDeleteType.fromValue(
                            intent.getIntExtra(
                                "deleteType",
                                RecurringExpenseDeleteType.ALL.value
                            )
                        )!!

                        viewModel.onDeleteRecurringExpenseClicked(expense, deleteType)
                    }
                    SelectCurrencyFragment.CURRENCY_SELECTED_INTENT -> viewModel.onCurrencySelected()
                    INTENT_SHOW_WELCOME_SCREEN -> {
                        val startIntent = Intent(this@MainActivity, WelcomeActivity::class.java)
                        ActivityCompat.startActivityForResult(
                            this@MainActivity,
                            startIntent,
                            WELCOME_SCREEN_ACTIVITY_CODE,
                            null
                        )
                    }
                    INTENT_IAB_STATUS_CHANGED -> viewModel.onIabStatusChanged()
                }
            }
        }

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, filter)

        if (intent != null) {
            openSettingsIfNeeded(intent)
            openMonthlyReportIfNeeded(intent)
            openAddExpenseIfNeeded(intent)
            openAddRecurringExpenseIfNeeded(intent)
            openSettingsForBackupIfNeeded(intent)
        }

        viewModel.expenseDeletionSuccessEventStream.observe(
            this
        ) { (deletedExpense, newBalance) ->

            expensesViewAdapter.removeExpense(deletedExpense)
            updateBalanceDisplayForDay(expensesViewAdapter.getDate(), newBalance)
            calendarFragment.refreshView()

            val snackbar = Snackbar.make(
                binding.coordinatorLayout,
                if (deletedExpense.isRevenue()) R.string.income_delete_snackbar_text else R.string.expense_delete_snackbar_text,
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) {
                viewModel.onExpenseDeletionCancelled(deletedExpense)
            }
            snackbar.setActionTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewModel.expenseDeletionErrorEventStream.observe(this, Observer {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.oops)
                .setMessage(R.string.error_occurred_try_again)
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        })

        viewModel.expenseRecoverySuccessEventStream.observe(this) {
            // Nothing to do
        }

        viewModel.expenseRecoveryErrorEventStream.observe(this) { expense ->
            Logger.error("Error restoring deleted expense: $expense")
        }

        var expenseDeletionDialog: ProgressDialog? = null
        viewModel.recurringExpenseDeletionProgressEventStream.observe(this) { status ->
            when (status) {
                is MainViewModel.RecurringExpenseDeleteProgressState.Starting -> {
                    val dialog = ProgressDialog(this@MainActivity)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_delete_loading_title)
                    dialog.setMessage(resources.getString(R.string.recurring_expense_delete_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseDeletionDialog = dialog
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.ErrorCantDeleteBeforeFirstOccurrence -> {
                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.oops)
                        .setMessage(R.string.recurring_expense_delete_first_error_message)
                        .setNegativeButton(R.string.ok, null)
                        .show()
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.ErrorRecurringExpenseDeleteNotAssociated -> {
                    showGenericRecurringDeleteErrorDialog()

                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.ErrorIO -> {
                    showGenericRecurringDeleteErrorDialog()

                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
                is MainViewModel.RecurringExpenseDeleteProgressState.Deleted -> {
                    val snackbar = Snackbar.make(
                        binding.coordinatorLayout,
                        R.string.recurring_expense_delete_success_message,
                        Snackbar.LENGTH_LONG
                    )

                    snackbar.setAction(R.string.undo) {
                        viewModel.onRestoreRecurringExpenseClicked(
                            status.recurringExpense,
                            status.restoreRecurring,
                            status.expensesToRestore
                        )
                    }

                    snackbar.setActionTextColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.snackbar_action_undo
                        )
                    )
                    snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
                    snackbar.show()

                    expenseDeletionDialog?.dismiss()
                    expenseDeletionDialog = null
                }
            }
        }

        var expenseRestoreDialog: Dialog? = null
        viewModel.recurringExpenseRestoreProgressEventStream.observe(this) { status ->
            when (status) {
                is MainViewModel.RecurringExpenseRestoreProgressState.Starting -> {
                    val dialog = ProgressDialog(this@MainActivity)
                    dialog.isIndeterminate = true
                    dialog.setTitle(R.string.recurring_expense_restoring_loading_title)
                    dialog.setMessage(resources.getString(R.string.recurring_expense_restoring_loading_message))
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.setCancelable(false)
                    dialog.show()

                    expenseRestoreDialog = dialog
                }
                is MainViewModel.RecurringExpenseRestoreProgressState.ErrorIO -> {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.oops)
                        .setMessage(resources.getString(R.string.error_occurred_try_again))
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()

                    expenseRestoreDialog?.dismiss()
                    expenseRestoreDialog = null
                }
                is MainViewModel.RecurringExpenseRestoreProgressState.Restored -> {
                    Snackbar.make(
                        binding.coordinatorLayout,
                        R.string.recurring_expense_restored_success_message,
                        Snackbar.LENGTH_LONG
                    ).show()

                    expenseRestoreDialog?.dismiss()
                    expenseRestoreDialog = null
                }
            }
        }

        viewModel.startCurrentBalanceEditorEventStream.observe(this) { currentBalance ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_adjust_balance, null)
            val amountEditText = dialogView.findViewById<EditText>(R.id.balance_amount)
            amountEditText.setText(
                if (currentBalance == 0.0) "0" else CurrencyHelper.getFormattedAmountValue(
                    currentBalance
                )
            )
            amountEditText.preventUnsupportedInputForDecimals()
            amountEditText.setSelection(amountEditText.text.length) // Put focus at the end of the text

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.adjust_balance_title)
            builder.setMessage(R.string.adjust_balance_message)
            builder.setView(dialogView)
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                try {
                    val stringValue = amountEditText.text.toString()
                    if (stringValue.isNotBlank()) {
                        val newBalance = java.lang.Double.valueOf(stringValue)
                        viewModel.onNewBalanceSelected(
                            newBalance,
                            getString(R.string.adjust_balance_expense_title)
                        )
                    }
                } catch (e: Exception) {
                    Logger.error("Error parsing new balance", e)
                }

                dialog.dismiss()
            }

            val dialog = builder.show()

            // Directly show keyboard when the dialog pops
            amountEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                // Check if the device doesn't have a physical keyboard
                if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        }

        viewModel.currentBalanceEditingErrorEventStream.observe(this, Observer { exception ->
            Logger.error("Error while adjusting balance", exception)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.oops)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        })

        viewModel.currentBalanceEditedEventStream.observe(
            this,
            Observer { (expense, diff, newBalance) ->
                //Show snackbar
                val snackbar = Snackbar.make(
                    binding.coordinatorLayout,
                    resources.getString(
                        R.string.adjust_balance_snackbar_text,
                        CurrencyHelper.getFormattedCurrencyString(appPreferences, newBalance)
                    ),
                    Snackbar.LENGTH_LONG
                )
                snackbar.setAction(R.string.undo) {
                    viewModel.onCurrentBalanceEditedCancelled(expense, diff)
                }
                snackbar.setActionTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.snackbar_action_undo
                    )
                )

                snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
                snackbar.show()
            })

        viewModel.currentBalanceRestoringEventStream.observe(this, Observer {
            // Nothing to do
        })

        viewModel.currentBalanceRestoringErrorEventStream.observe(this, Observer { exception ->
            Logger.error("An error occurred during balance", exception)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.oops)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                .show()
        })

        viewModel.premiumStatusLiveData.observe(this) { isPremium ->
            isUserPremium = isPremium
            invalidateOptionsMenu()

            if (isPremium) {
                val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
                adContainerView.visibility = View.GONE
                val layoutParams: RelativeLayout.LayoutParams =
                    binding.llAddAmountContainer.layoutParams as RelativeLayout.LayoutParams
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                binding.llAddAmountContainer.layoutParams = layoutParams
            } else {
                loadAndDisplayBannerAds()
            }
        }

        viewModel.selectedDateChangeLiveData.observe(this) { (date, balance, expenses) ->
            refreshAllForDate(date, balance, expenses)

            var expensesAmount = 0.0
            Runnable {
                for (expense in expenses) {
                    if (!expense.isRevenue()) {
                        expensesAmount += expense.amount
                    }
                }
            }.run()

            val format = DateTimeFormatter.ofPattern(
                resources.getString(R.string.account_balance_date_format),
                Locale.getDefault()
            )

            var formatted =
                resources.getString(R.string.account_expense_format, format.format(date))

            if (formatted.endsWith(".:")) {
                formatted = formatted.substring(
                    0,
                    formatted.length - 2
                ) + "" // Remove . at the end of the month (ex: nov.: -> nov:)
            } else if (formatted.endsWith(". :")) {
                formatted = formatted.substring(
                    0,
                    formatted.length - 3
                ) + "" // Remove . at the end of the month (ex: nov. : -> nov :)
            }
            binding.expenseLine.text = formatted
            binding.switchHideBalance.isChecked = appPreferences.getDisplayBalance()

            expenseOfSelectedDay = expensesAmount

            if (appPreferences.getDisplayBalance()) {
                CurrencyHelper.getFormattedCurrencyString(appPreferences, expensesAmount)
                binding.expenseLineAmount.text =
                    CurrencyHelper.getFormattedCurrencyString(appPreferences, expenseOfSelectedDay)
            } else {
                binding.expenseLineAmount.text = BALANCE_PLACE_HOLDER
            }
        }

        // Routing towards weekly report if weekly report notification received
        if (intent?.hasExtra(MyFirebaseMessagingService.WEEKLY_REMINDER_KEY) == true) {
            val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
            ActivityCompat.startActivity(this@MainActivity, startIntent, null)
        }

        // Check and launch download campaign.
        checkIfItsDownloadCampaign()

        //checkToken()
    }

    /**
     * Check and launch download campaign.
     * If download campaign active, notification click should have package to redirect on play store.
     */
    private fun checkIfItsDownloadCampaign() {
        try {
            if (intent != null) {
                if (intent.hasExtra("package")) {
                    Rate.openPlayStore(intent.getStringExtra("package") ?: "", this)
                    //Cancel notification.
                    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                        MyFirebaseMessagingService.NOTIFICATION_ID_NEW_FEATURES
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Only enable for testing.
     */
    private fun checkToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d("FCM TOKEN", token ?: "")
        })
    }


    /**
     *
     */
    private fun calendarRevealAnimation() {
        try {
            val format = DateTimeFormatter.ofPattern(
                resources.getString(R.string.date_format_calender_reveal),
                Locale.getDefault()
            )
            val formattedDate = format.format(expensesViewAdapter.getDate())
            binding.todaysDate.text = String.format("%s", formattedDate)

            binding.revealCalendar.setOnClickListener {
                revealHideCalendar()
            }
            binding.llBalances.setOnClickListener { binding.revealCalendar.callOnClick() }
        } catch (e: Exception) {
        }
    }

    /**
     * Reveal hide calendar
     */
    private fun revealHideCalendar() {
        if (binding.calendarView.visibility == View.VISIBLE) {
            binding.arrowDown.setImageResource(R.drawable.ic_arrow_down)
            binding.calendarView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.debounce))
            binding.calendarView.visibility = View.GONE
        } else {
            binding.calendarView.visibility = View.VISIBLE
            binding.calendarView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce))
            binding.arrowDown.setImageResource(R.drawable.ic_arrow_up)
        }
    }

    /**
     *
     */
    override fun onStart() {
        super.onStart()

        // If the last stop happened yesterday (or another day), set and refresh to the current date
        if (lastStopDate != null) {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_YEAR)

            cal.time = lastStopDate!!
            val lastStopDay = cal.get(Calendar.DAY_OF_YEAR)

            if (currentDay != lastStopDay) {
                viewModel.onDayChanged()
            }

            lastStopDate = null
        }
    }

    /**
     *
     */
    override fun onStop() {
        lastStopDate = Date()
        super.onStop()
    }

    /**
     *
     */
    override fun onDestroy() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
        super.onDestroy()
    }

    /**
     *
     */
    override fun onSaveInstanceState(outState: Bundle) {
        calendarFragment.saveStatesToKey(outState, CALENDAR_SAVED_STATE)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_EXPENSE_ACTIVITY_CODE || requestCode == MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onExpenseAdded()
            }
        } else if (requestCode == WELCOME_SCREEN_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                viewModel.onWelcomeScreenFinished()
            } else if (resultCode == RESULT_CANCELED) {
                finish() // Finish activity if welcome screen is finish via back button
            }
        } else if (requestCode == SETTINGS_SCREEN_ACTIVITY_CODE) {
            calendarFragment.setFirstDayOfWeek(appPreferences.getCaldroidFirstDayOfWeek())
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("finish()"))
    override fun onBackPressed() {
        finish()
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        openSettingsIfNeeded(intent)
        openMonthlyReportIfNeeded(intent)
        openAddExpenseIfNeeded(intent)
        openAddRecurringExpenseIfNeeded(intent)
        openSettingsForBackupIfNeeded(intent)
    }

// ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        // Remove monthly report for non premium users
        if (!appPreferences.hasUserSawMonthlyReportHint()) {
            binding.introOverlay.visibility = View.VISIBLE
            binding.monthlyReportHint.visibility = View.VISIBLE
            binding.monthlyReportHintButton.setOnClickListener {
                appPreferences.setUserSawMonthlyReportHint()
                binding.monthlyReportHint.visibility = View.GONE
                binding.futureExpenseHint.visibility = View.VISIBLE
            }
        }

        if (!appPreferences.hasUserSawFutureExpensesHint()) {
            binding.futureExpenseHintButton.setOnClickListener {
                appPreferences.setUserSawFutureExpensesHint()
                binding.futureExpenseHint.visibility = View.GONE
                binding.searchHint.visibility = View.VISIBLE
            }
        }

        if (!appPreferences.hasUserSawSearchHint()) {
            binding.searchHintButton.setOnClickListener {
                appPreferences.setUserSawSearchHint()
                binding.searchHint.visibility = View.GONE
                binding.settingsHint.visibility = View.VISIBLE
            }
        }

        if (!appPreferences.hasUserSawSettingsHint()) {
            binding.settingsHintButton.setOnClickListener {
                appPreferences.setUserSawSettingsHint()
                binding.settingsHint.visibility = View.GONE
                binding.introOverlay.visibility = View.GONE
                openHideBalanceShowCase()
            }
        }

        return true
    }

    /**
     * Show case Hint for balance
     */
    private fun openHideBalanceShowCase() {
        if (appPreferences.hasUserSawHideBalanceHint().not()) {
            showCaseView(
                targetView = binding.contSwitchBalance,
                title = getString(R.string.hide_balance_hint_title),
                message = getString(R.string.hide_balance_hint_message),
                handleGuideListener = {
                    binding.introOverlay.visibility = View.GONE
                    showCaseAddSingleExpense()
                }
            )
        }
    }

    /**
     * Show case Hint for Add single Income / Expense
     */
    private fun showCaseAddSingleExpense() {
        if (appPreferences.hasUserSawAddSingleExpenseHint().not()) {
            showCaseView(
                targetView = binding.tvDummyViewForSingleHint,
                title = getString(R.string.add_single_expense_hint_title),
                message = getString(R.string.add_single_expense_hint_message),
                handleGuideListener = {
                    binding.introOverlay.visibility = View.GONE
                    showCaseAddRecurringExpense()
                }
            )
        }
    }

    /**
     * Show case Hint for Add recurring Income / Expense
     */
    private fun showCaseAddRecurringExpense() {
        if (appPreferences.hasUserSawAddRecurringExpenseHint().not()) {
            showCaseView(
                targetView = binding.tvDummyViewForRecurringHint,
                title = getString(R.string.add_recurring_expense_hint_title),
                message = getString(R.string.add_recurring_expense_hint_message),
                handleGuideListener = {
                    binding.introOverlay.visibility = View.GONE
                    binding.llDummyViewForHint.visibility = View.GONE
                }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val startIntent = Intent(this, SettingsActivity::class.java)
                ActivityCompat.startActivityForResult(
                    this@MainActivity,
                    startIntent,
                    SETTINGS_SCREEN_ACTIVITY_CODE,
                    null
                )

                return true
            }
            R.id.action_balance -> {
                viewModel.onAdjustCurrentBalanceClicked()
                return true
            }
            R.id.action_breakdown -> {
                val startIntent = Intent(this, BreakDownBaseActivity::class.java)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)
                return true
            }
            R.id.action_monthly_report -> {
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)
                return true
            }
            R.id.action_share -> {
                try {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        resources.getString(R.string.app_invite_message) + "\n" + "https://play.google.com/store/apps/details?id=gplx.simple.budgetapp"
                    )
                    sendIntent.type = "text/plain"
                    startActivity(sendIntent)
                } catch (e: Exception) {
                    Logger.error("An error occurred during sharing app activity start", e)
                }
                return true
            }
            R.id.action_search_expenses -> {
                ActivityCompat.startActivity(
                    this@MainActivity,
                    Intent(this, SearchBaseActivity::class.java),
                    null
                )
                return true
            }
            /*R.id.action_language -> {
                Languages.showLanguagesDialog(
                    this,
                    appPreferences.getCurrentLanguage(),
                    onLanguageSelected = { languageCode ->
                        appPreferences.setCurrentLanguage(languageCode)
                    })
                return true
            }*/
            else -> return super.onOptionsItemSelected(item)
        }
    }

// ------------------------------------------>

    /**
     * Update the balance for the given day
     */
    private fun updateBalanceDisplayForDay(day: LocalDate, balance: Double) {
        val formatter = DateTimeFormatter.ofPattern(
            resources.getString(R.string.account_balance_date_format),
            Locale.getDefault()
        )
        var formatted = resources.getString(R.string.account_balance_format, formatter.format(day))
        if (formatted.endsWith(".:")) {
            formatted = formatted.substring(
                0,
                formatted.length - 2
            ) + "" // Remove . at the end of the month (ex: nov.: -> nov:)
        } else if (formatted.endsWith(". :")) {
            formatted = formatted.substring(
                0,
                formatted.length - 3
            ) + "" // Remove . at the end of the month (ex: nov. : -> nov :)
        }
        binding.budgetLine.text = formatted
        binding.switchHideBalance.isChecked = appPreferences.getDisplayBalance()

        if (appPreferences.getDisplayBalance()) {
            binding.budgetLineAmount.text =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
            binding.expenseLineAmount.text =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, expenseOfSelectedDay)
        } else {
            binding.budgetLineAmount.text = BALANCE_PLACE_HOLDER
            binding.expenseLineAmount.text = BALANCE_PLACE_HOLDER
        }

        binding.switchHideBalance.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                appPreferences.setDisplayBalance(true)
                binding.budgetLineAmount.text =
                    CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
                binding.expenseLineAmount.text =
                    CurrencyHelper.getFormattedCurrencyString(appPreferences, expenseOfSelectedDay)
            } else {
                appPreferences.setDisplayBalance(false)
                binding.budgetLineAmount.text = BALANCE_PLACE_HOLDER
                binding.expenseLineAmount.text = BALANCE_PLACE_HOLDER
            }
            expensesViewAdapter?.notifyDataSetChanged()
        }
    }

    /**
     * Open the settings activity if the given intent contains the [.INTENT_REDIRECT_TO_SETTINGS_EXTRA]
     * extra.
     */
    private fun openSettingsIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_EXTRA, false)) {
            val startIntent = Intent(this, SettingsActivity::class.java)
            ActivityCompat.startActivityForResult(
                this@MainActivity,
                startIntent,
                SETTINGS_SCREEN_ACTIVITY_CODE,
                null
            )
        }
    }

    /**
     * Open the settings activity to display backup options if the given intent contains the
     * [.INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA] extra.
     */
    private fun openSettingsForBackupIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA, false)) {
            val startIntent = Intent(this, SettingsActivity::class.java).apply {
                putExtra(SHOW_BACKUP_INTENT_KEY, true)
            }
            ActivityCompat.startActivityForResult(
                this@MainActivity,
                startIntent,
                SETTINGS_SCREEN_ACTIVITY_CODE,
                null
            )
        }
    }

    /**
     * Open the monthly report activity if the given intent contains the monthly uri part.
     *
     * @param intent
     */
    private fun openMonthlyReportIfNeeded(intent: Intent) {
        try {
            val data = intent.data
            if (data != null && "true" == data.getQueryParameter("monthly")) {
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                startIntent.putExtra(MonthlyReportBaseActivity.FROM_NOTIFICATION_EXTRA, true)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)
            }
        } catch (e: Exception) {
            Logger.error("Error while opening report activity", e)
        }

    }

    /**
     * Open the add expense screen if the given intent contains the [.INTENT_SHOW_ADD_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddExpenseIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_EXPENSE, false)) {
            val startIntent = Intent(this, ExpenseEditActivity::class.java)
            startIntent.putExtra("date", LocalDate.now().toEpochDay())

            ActivityCompat.startActivityForResult(
                this,
                startIntent,
                ADD_EXPENSE_ACTIVITY_CODE,
                null
            )
        }
    }

    /**
     * Open the add recurring expense screen if the given intent contains the [.INTENT_SHOW_ADD_RECURRING_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddRecurringExpenseIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_SHOW_ADD_RECURRING_EXPENSE, false)) {
            val startIntent = Intent(this, RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", LocalDate.now().toEpochDay())

            ActivityCompat.startActivityForResult(
                this,
                startIntent,
                ADD_EXPENSE_ACTIVITY_CODE,
                null
            )
        }
    }

// ------------------------------------------>

    private fun initCalendarFragment(savedInstanceState: Bundle?) {
        calendarFragment = CalendarFragment()

        if (savedInstanceState != null && savedInstanceState.containsKey(CALENDAR_SAVED_STATE)) {
            calendarFragment.restoreStatesFromKey(savedInstanceState, CALENDAR_SAVED_STATE)
        } else {
            val args = Bundle()
            val cal = Calendar.getInstance()
            args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1)
            args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR))
            args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true)
            args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, false)
            args.putInt(
                CaldroidFragment.START_DAY_OF_WEEK,
                appPreferences.getCaldroidFirstDayOfWeek()
            )
            args.putBoolean(CaldroidFragment.ENABLE_CLICK_ON_DISABLED_DATES, false)
            args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.caldroid_style)

            calendarFragment.arguments = args
            calendarFragment.setMinDate(
                (appPreferences.getInitDate()
                    ?: LocalDate.now()).computeCalendarMinDateFromInitDate()
            )
        }

        val listener = object : CaldroidListener() {
            override fun onSelectDate(date: LocalDate, view: View) {
                viewModel.onSelectDate(date)
            }

            override fun onLongClickDate(date: LocalDate, view: View?) // Add expense on long press
            {
                val startIntent = Intent(this@MainActivity, ExpenseEditActivity::class.java)
                startIntent.putExtra("date", date.toEpochDay())

                // Get the absolute location on window for Y value
                val viewLocation = IntArray(2)
                view!!.getLocationInWindow(viewLocation)

                startIntent.putExtra(ANIMATE_TRANSITION_KEY, true)
                startIntent.putExtra(CENTER_X_KEY, view.x.toInt() + view.width / 2)
                startIntent.putExtra(CENTER_Y_KEY, viewLocation[1] + view.height / 2)

                ActivityCompat.startActivityForResult(
                    this@MainActivity,
                    startIntent,
                    ADD_EXPENSE_ACTIVITY_CODE,
                    null
                )
            }

            override fun onChangeMonth(month: Int, year: Int) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.YEAR, year)
            }

            override fun onCaldroidViewCreated() {
                val viewPager = calendarFragment.dateViewPager
                val leftButton = calendarFragment.leftArrowButton
                val rightButton = calendarFragment.rightArrowButton
                val textView = calendarFragment.monthTitleTextView
                val weekDayGreedView = calendarFragment.weekdayGridView
                val topLayout =
                    this@MainActivity.findViewById<LinearLayout>(com.caldroid.R.id.calendar_title_view)

                val params = textView.layoutParams as LinearLayout.LayoutParams
                params.gravity = Gravity.TOP
                params.setMargins(
                    0,
                    0,
                    0,
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_text_padding_bottom)
                )
                textView.layoutParams = params

                topLayout.setPadding(
                    0,
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_padding_top),
                    0,
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_padding_bottom)
                )

                val leftButtonParams = leftButton.layoutParams as LinearLayout.LayoutParams
                leftButtonParams.setMargins(
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_buttons_margin),
                    0,
                    0,
                    0
                )
                leftButton.layoutParams = leftButtonParams

                val rightButtonParams = rightButton.layoutParams as LinearLayout.LayoutParams
                rightButtonParams.setMargins(
                    0,
                    0,
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_month_buttons_margin),
                    0
                )
                rightButton.layoutParams = rightButtonParams

                textView.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.calendar_header_month_color
                    )
                )
                topLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.calendar_header_background
                    )
                )

                leftButton.text = "<"
                leftButton.textSize = 25f
                leftButton.gravity = Gravity.CENTER
                leftButton.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.calendar_month_button_color
                    )
                )
                leftButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable)

                rightButton.text = ">"
                rightButton.textSize = 25f
                rightButton.gravity = Gravity.CENTER
                rightButton.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.calendar_month_button_color
                    )
                )
                rightButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable)

                weekDayGreedView.setPadding(
                    0,
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_weekdays_padding_top),
                    0,
                    this@MainActivity.resources.getDimensionPixelSize(R.dimen.calendar_weekdays_padding_bottom)
                )

                viewPager.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.calendar_background
                    )
                )
                (viewPager.parent as View?)?.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.calendar_background
                    )
                )
            }
        }

        calendarFragment.caldroidListener = listener

        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.calendarView, calendarFragment)
        t.commit()
    }

    private fun initRecyclerView() {
        binding.fabNewExpense.setOnClickListener {
            val startIntent = Intent(this@MainActivity, ExpenseEditActivity::class.java)
            startIntent.putExtra("date", calendarFragment.getSelectedDate().toEpochDay())

            startIntent.putExtra(ANIMATE_TRANSITION_KEY, true)
            startIntent.putExtra(
                CENTER_X_KEY,
                binding.fabNewExpense.x.toInt() + (binding.fabNewExpense.width.toFloat() / 1.2f).toInt()
            )
            startIntent.putExtra(
                CENTER_Y_KEY,
                binding.fabNewExpense.y.toInt() + (binding.fabNewExpense.height.toFloat() / 1.2f).toInt()
            )

            ActivityCompat.startActivityForResult(
                this@MainActivity,
                startIntent,
                ADD_EXPENSE_ACTIVITY_CODE,
                null
            )
        }

        binding.fabNewRecurringExpense.setOnClickListener {
            val startIntent = Intent(this@MainActivity, RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", calendarFragment.getSelectedDate().toEpochDay())

            startIntent.putExtra(ANIMATE_TRANSITION_KEY, true)
            startIntent.putExtra(
                CENTER_X_KEY,
                binding.fabNewRecurringExpense.x.toInt() + (binding.fabNewRecurringExpense.width.toFloat() / 1.2f).toInt()
            )
            startIntent.putExtra(
                CENTER_Y_KEY,
                binding.fabNewRecurringExpense.y.toInt() + (binding.fabNewRecurringExpense.height.toFloat() / 1.2f).toInt()
            )

            ActivityCompat.startActivityForResult(
                this@MainActivity,
                startIntent,
                ADD_EXPENSE_ACTIVITY_CODE,
                null
            )
        }

        /*
         * Expense Recycler view
         */
        binding.expensesRecyclerView.layoutManager = LinearLayoutManager(this)

        expensesViewAdapter = ExpensesRecyclerViewAdapter(this, appPreferences, LocalDate.now())
        binding.expensesRecyclerView.adapter = expensesViewAdapter

        binding.expensesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    // Scrolling up
                    if (binding.calendarView.visibility == View.VISIBLE) {
                        binding.arrowDown.setImageResource(R.drawable.ic_arrow_down)
                        binding.calendarView.startAnimation(
                            AnimationUtils.loadAnimation(
                                this@MainActivity,
                                R.anim.debounce
                            )
                        )
                        binding.calendarView.visibility = View.GONE
                    }
                } else {
                    // Scrolling down
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    AbsListView.OnScrollListener.SCROLL_STATE_FLING -> {
                        // Do something
                    }
                    AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                        // Do something
                    }
                    else -> {
                        // Do something
                    }
                }
            }
        })
    }

    private fun refreshRecyclerViewForDate(date: LocalDate, expenses: List<Expense>) {
        expensesViewAdapter.setDate(date, expenses)
        if (expenses.isNotEmpty()) {
            binding.expensesRecyclerView.visibility = View.VISIBLE
            binding.emptyExpensesRecyclerViewPlaceholder.visibility = View.GONE
        } else {
            binding.expensesRecyclerView.visibility = View.GONE
            binding.emptyExpensesRecyclerViewPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun refreshAllForDate(date: LocalDate, balance: Double, expenses: List<Expense>) {
        refreshRecyclerViewForDate(date, expenses)
        updateBalanceDisplayForDay(date, balance)
        calendarFragment.setSelectedDate(date)
        calendarFragment.refreshView()
    }

    /**
     * Show a generic alert dialog telling the user an error occured while deleting recurring expense
     */
    private fun showGenericRecurringDeleteErrorDialog() {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.oops)
            .setMessage(R.string.error_occurred_try_again)
            .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
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

    companion object {
        const val ADD_EXPENSE_ACTIVITY_CODE = 101
        const val MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE = 102
        const val WELCOME_SCREEN_ACTIVITY_CODE = 103
        const val SETTINGS_SCREEN_ACTIVITY_CODE = 104
        const val INTENT_EXPENSE_DELETED = "intent.expense.deleted"
        const val INTENT_EXPENSE_ADDED = "intent.expense.added"
        const val INTENT_RECURRING_EXPENSE_DELETED = "intent.expense.monthly.deleted"
        const val INTENT_SHOW_WELCOME_SCREEN = "intent.welcomscreen.show"
        const val INTENT_SHOW_ADD_EXPENSE = "intent.addexpense.show"
        const val INTENT_SHOW_ADD_RECURRING_EXPENSE = "intent.addrecurringexpense.show"

        const val INTENT_REDIRECT_TO_SETTINGS_EXTRA = "intent.extra.redirecttosettings"
        const val INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA =
            "intent.extra.redirecttosettingsforbackup"

        const val ANIMATE_TRANSITION_KEY = "animate"
        const val CENTER_X_KEY = "centerX"
        const val CENTER_Y_KEY = "centerY"

        private const val CALENDAR_SAVED_STATE = "calendar_saved_state"
    }
}