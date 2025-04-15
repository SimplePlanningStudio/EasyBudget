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
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidListener
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityMainBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.banner.RetrofitClient
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.iab.PREMIUM_PARAMETER_KEY
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.account.appendAccount
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpenseDeleteType
import com.simplebudget.prefs.*
import com.simplebudget.push.MyFirebaseMessagingService
import com.simplebudget.view.RatingPopup
import com.simplebudget.view.accounts.AccountDetailsActivity
import com.simplebudget.view.accounts.AccountsBottomSheetDialogFragment
import com.simplebudget.view.breakdown.base.BreakDownBaseActivity
import com.simplebudget.view.budgets.base.BudgetBaseActivity
import com.simplebudget.view.category.manage.ManageCategoriesActivity
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
import com.simplebudget.view.settings.aboutus.AboutUsActivity
import com.simplebudget.view.settings.backup.BackupSettingsActivity
import com.simplebudget.view.settings.help.HelpActivity
import com.simplebudget.view.settings.webview.WebViewActivity
import com.simplebudget.view.welcome.WelcomeActivity
import com.simplebudget.view.welcome.getOnboardingStep
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.simplebudget.helper.ads.AdSdkManager
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main activity containing Calendar and List of expenses
 *
 * @author Benoit LETONDOR
 */
@SuppressLint("NotifyDataSetChanged")
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var receiver: BroadcastReceiver

    private lateinit var calendarFragment: CalendarFragment
    private lateinit var expensesViewAdapter: ExpensesRecyclerViewAdapter

    private var lastStopDate: Date? = null

    private val viewModel: MainViewModel by viewModel()
    private val appPreferences: AppPreferences by inject()
    private val toastManager: ToastManager by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private var isUserPremium = false
    private var expenseOfSelectedDay: Double = 0.0

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var adView: AdView? = null
    private var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager? = null
    private var appUpdateManager: AppUpdateManager? = null
// ------------------------------------------>

    /**
     * Launch Security Activity
     */
    private fun handleAppPasswordProtection() {
        if (appPreferences.isAppPasswordProtectionOn()) {
            binding.framelayoutOpaque.visibility = View.VISIBLE
            val intent = Intent(this, SecurityActivity::class.java).putExtra(
                "HASH", appPreferences.appPasswordHash()
            ).putExtra("TAB_INDEX", appPreferences.appProtectionType()).putExtra(
                SecurityActivity.REQUEST_CODE_SECURITY_TYPE, SecurityActivity.VERIFICATION
            )
            securityActivityLauncher.launch(intent)
        }
    }

    /**
     * Start activity for result
     */
    private var securityActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                finish()
            } else {
                binding.framelayoutOpaque.visibility = View.GONE
            }
        }

    /**
     * Start activity for result for app update
     */
    private var appUpdateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // ✅ Update completed
            } else {
                // ❌ Update canceled or failed
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
        //Check if on boarding done
        if (appPreferences.getOnboardingStep() != WelcomeActivity.STEP_COMPLETED) {
            startActivity(Intent(this@MainActivity, WelcomeActivity::class.java))
            finish()
        }
        setSupportActionBar(binding.toolbar)
        try {
            binding.toolbar.apply {
                logo = ContextCompat.getDrawable(context, R.drawable.ic_logo_white)
                setOnLongClickListener {
                    analyticsManager.logEvent(Events.KEY_DASHBOARD_ABOUT_US_LOGO)
                    startActivity(Intent(this@MainActivity, AboutUsActivity::class.java))
                    true
                }
            }
        } catch (e: Exception) {
            Logger.error(
                MainActivity::class.java.simpleName,
                getString(R.string.error_displaying_app_logo_on_toolbar),
                e
            )
        }
        handleAppPasswordProtection()
        initCalendarFragment(savedInstanceState)
        initRecyclerView()
        calendarRevealAnimation()

        analyticsManager.logEvent(Events.KEY_DASHBOARD_SCREEN)/*
           Init firebase in app messaging click
        */
        //////////////////////////////////////////////////////////////////////
        /*FirebaseInAppMessaging.getInstance().addClickListener { inAppMessage, action ->
            try {
                if (viewModel.isPremium().not()) {
                    val mapData: Map<*, *>? = inAppMessage.data
                    action.button?.text?.toString()?.let { act ->
                        if (act == "Let's Try") {
                            mapData?.containsKey("route")?.let {
                                if (it) {
                                    if (mapData["route"].toString() == "display_premium_screen") {
                                        becomePremium()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
        //////////////////////////////////////////////////////////////////////

        // Register receiver
        val filter = IntentFilter()
        filter.addAction(INTENT_EXPENSE_DELETED)
        filter.addAction(INTENT_RECURRING_EXPENSE_DELETED)
        filter.addAction(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        filter.addAction(INTENT_SHOW_WELCOME_SCREEN)
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(Intent.ACTION_VIEW)
        filter.addAction(INTENT_ACCOUNT_TYPE_UPDATED)
        filter.addAction(INTENT_ACCOUNT_TYPE_EDITED)

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
                                "deleteType", RecurringExpenseDeleteType.ALL.value
                            )
                        )!!

                        viewModel.onDeleteRecurringExpenseClicked(expense, deleteType)
                    }

                    SelectCurrencyFragment.CURRENCY_SELECTED_INTENT -> {
                        viewModel.refreshTodaysExpenses()
                    }

                    INTENT_SHOW_WELCOME_SCREEN -> {
                        val startIntent = Intent(this@MainActivity, WelcomeActivity::class.java)
                        ActivityCompat.startActivityForResult(
                            this@MainActivity, startIntent, WELCOME_SCREEN_ACTIVITY_CODE, null
                        )
                    }

                    INTENT_IAB_STATUS_CHANGED -> {
                        viewModel.onIabStatusChanged()
                    }

                    INTENT_ACCOUNT_TYPE_UPDATED -> {
                        // Default Account Type Updated, Refresh Calendar and Expenses as well.
                        val accountLabel = appPreferences.activeAccountLabel().appendAccount()
                        binding.layoutSelectAccount.tvSelectedAccount.text = accountLabel
                        viewModel.refreshTodaysExpenses() // Refresh to re-setup
                        val message = String.format(
                            "%s %s", accountLabel, "is selected!"
                        )
                        object : CountDownTimer(500, 500) {
                            override fun onTick(millisUntilFinished: Long) {
                            }

                            override fun onFinish() {
                                toastManager.showLong(message)
                            }
                        }.start()
                    }

                    INTENT_ACCOUNT_TYPE_EDITED -> {
                        // Account name edited so just changing label
                        val accountLabel = appPreferences.activeAccountLabel().appendAccount()
                        binding.layoutSelectAccount.tvSelectedAccount.text = accountLabel
                    }
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
            openBudgetIntroIfNeeded(intent)
            openBudgetsIfNeeded(intent)
            openHowToIfNeeded(intent)
            openBuyPremiumIfNeeded(intent)
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
                    this@MainActivity, R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewModel.expenseDeletionErrorEventStream.observe(this, Observer {
            AlertDialog.Builder(this@MainActivity).setTitle(R.string.oops)
                .setMessage(R.string.error_occurred_try_again)
                .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
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

                    AlertDialog.Builder(this@MainActivity).setTitle(R.string.oops)
                        .setMessage(R.string.recurring_expense_delete_first_error_message)
                        .setNegativeButton(R.string.ok, null).show()
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
                            this@MainActivity, R.color.snackbar_action_undo
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
                    AlertDialog.Builder(this@MainActivity).setTitle(R.string.oops)
                        .setMessage(resources.getString(R.string.error_occurred_try_again))
                        .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }.show()

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

            val description = String.format(
                "%s %s",
                getString(R.string.adjust_balance_message),
                "'${appPreferences.activeAccountLabel().appendAccount()}'"
            )
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.adjust_balance_title)
            builder.setMessage(description)
            builder.setView(dialogView)
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                try {
                    val stringValue = amountEditText.text.toString()
                    if (stringValue.isNotBlank()) {
                        val newBalance = java.lang.Double.valueOf(stringValue)
                        viewModel.onNewBalanceSelected(
                            newBalance, getString(R.string.adjust_balance_expense_title)
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

        viewModel.currentBalanceEditingErrorEventStream.observe(this) { exception ->
            Logger.error("Error while adjusting balance", exception)
            AlertDialog.Builder(this@MainActivity).setTitle(R.string.oops)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }.show()
        }

        viewModel.currentBalanceEditedEventStream.observe(
            this
        ) { (expense, diff, newBalance) ->
            //Show snackbar
            val snackbar = Snackbar.make(
                binding.coordinatorLayout, resources.getString(
                    R.string.adjust_balance_snackbar_text,
                    CurrencyHelper.getFormattedCurrencyString(appPreferences, newBalance)
                ), Snackbar.LENGTH_LONG
            )
            snackbar.setAction(R.string.undo) {
                viewModel.onCurrentBalanceEditedCancelled(expense, diff)
            }
            snackbar.setActionTextColor(
                ContextCompat.getColor(
                    this@MainActivity, R.color.snackbar_action_undo
                )
            )

            snackbar.duration = BaseTransientBottomBar.LENGTH_LONG
            snackbar.show()
        }

        viewModel.currentBalanceRestoringEventStream.observe(this) {
            // Nothing to do
        }

        viewModel.currentBalanceRestoringErrorEventStream.observe(this) { exception ->
            Logger.error("An error occurred during balance", exception)

            AlertDialog.Builder(this@MainActivity).setTitle(R.string.oops)
                .setMessage(R.string.adjust_balance_error_message)
                .setNegativeButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }.show()
        }

        viewModel.premiumStatusLiveData.observe(this) { isPremium ->
            isUserPremium = isPremium
            binding.ivPremiumIcon.setBackgroundResource(if (isPremium) R.drawable.ic_premium_icon_gold else R.drawable.ic_premium_icon_grey)
            invalidateOptionsMenu()
            if (isPremium) {
                val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
                adContainerView.visibility = View.GONE
                val layoutParams: RelativeLayout.LayoutParams =
                    binding.llAddAmountContainer.layoutParams as RelativeLayout.LayoutParams
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                binding.llAddAmountContainer.layoutParams = layoutParams
            } else {
                initConsentAndAdsSDK()
            }
            //Set ser property
            analyticsManager.setUserProperty(
                Events.KEY_ACCOUNT_TYPE,
                if (isPremium) Events.KEY_PREMIUM else Events.KEY_NOT_PREMIUM
            )
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })

        observeAndRefreshExpenses()

        // Handle drawer layout
        handleDrawerLayout()

        //Handle app banner promotion
        loadAppBannerContent()

        //Check for app update
        checkForAppUpdate()
    }

    /**
     * Handle back pressed
     */
    private fun handleBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            finish()
        }
    }

    /**
     * Load app promotion banner and save it for later usage.
     */
    private fun loadAppBannerContent() {
        try {
            if (appPreferences.isUserPremium().not()) {
                if (InternetUtils.isInternetAvailable(this) && shouldShowBanner()) {
                    // Inside your Activity or Fragment
                    lifecycleScope.launch {
                        try {
                            val bannerResponse =
                                withContext(Dispatchers.IO) { RetrofitClient.instance.getBanner() }
                            val apps = bannerResponse.apps
                            val bannerApp = apps.firstOrNull {
                                it.showBanner && AppInstallHelper.isInstalled(
                                    it.packageName ?: "", this@MainActivity
                                ).not()
                            }
                            appPreferences.saveBanner(bannerApp)

                        } catch (e: Exception) {
                            // Handle error, save null in case of failure
                            appPreferences.saveBanner(null)
                        }
                    }
                }
            } else {
                // Premium: No need banner as user is premium
                appPreferences.saveBanner(null)
            }
        } catch (e: Exception) {
            Logger.error(
                "PromotionBanner", "Error loading promotion banner: ${e.localizedMessage}", e
            )
        } finally {
            appPreferences.saveBanner(null)
        }
    }

    /**
     *
     */
    private fun handleDrawerLayout() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        // Display the hamburger icon
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        // Synchronize the state of the DrawerToggle with the state of the DrawerLayout
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_action_accounts -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_ACCOUNTS)
                    AccountDetailsActivity.start(this)
                }

                R.id.nav_action_balance -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_ADJUST_BALANCE)
                    viewModel.onAdjustCurrentBalanceClicked()
                }

                R.id.nav_action_categories -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_CATEGORIES)
                    val startIntent = Intent(this, ManageCategoriesActivity::class.java)
                    ActivityCompat.startActivity(this, startIntent, null)
                }

                R.id.nav_action_setup_budgets -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_BUDGETS)
                    openBudgetScreen()
                }

                R.id.nav_goto_settings -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_SETTINGS)
                    goToSettings()
                }

                R.id.nav_action_breakdown -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_LINEAR_BREAKDOWN)
                    openBreakDown(pieChart = false)
                }

                R.id.nav_action_pie_chart_breakdown -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_PI_CHART)
                    openBreakDown(pieChart = true)
                }

                R.id.nav_action_share -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_SHARE_APP)
                    shareApp()
                }

                R.id.nav_action_rate -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_RATE_APP)
                    RatingPopup(this, appPreferences, analyticsManager).show(true)
                }

                R.id.nav_what_people_say -> {
                    analyticsManager.logEvent(Events.KEY_SIDE_MENU_WHAT_PEOPLE_SAY)
                    WebViewActivity.start(this, getString(R.string.simple_budget_reviews_url))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        try {
            binding.navView.getHeaderView(0)?.let {
                val headerImage: ImageView =
                    binding.navView.getHeaderView(0).findViewById(R.id.header_image)
                val headerText: TextView =
                    binding.navView.getHeaderView(0).findViewById(R.id.header_text)

                headerImage.setOnClickListener {
                    analyticsManager.logEvent(Events.KEY_DASHBOARD_ABOUT_US_DRAWER)
                    startActivity(Intent(this@MainActivity, AboutUsActivity::class.java))
                }
                headerText.setOnClickListener {
                    analyticsManager.logEvent(Events.KEY_DASHBOARD_ABOUT_US_DRAWER)
                    startActivity(Intent(this@MainActivity, AboutUsActivity::class.java))
                }
            }
        } catch (e: Exception) {
            Logger.error(
                MainActivity::class.java.simpleName,
                getString(R.string.error_getting_side_menu_header),
                e
            )
        }
    }

    /**
     * Open budget screen
     */
    private fun openBudgetScreen(fromNotification: Boolean = false) {
        val startIntent = Intent(this, BudgetBaseActivity::class.java)
        startIntent.putExtra(BudgetBaseActivity.FROM_NOTIFICATION_EXTRA, fromNotification)
        ActivityCompat.startActivity(this, startIntent, null)
    }

    private fun observeAndRefreshExpenses() {
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
                resources.getString(R.string.account_balance_date_format), Locale.getDefault()
            )

            var formatted =
                resources.getString(R.string.account_expense_format, format.format(date))

            if (formatted.endsWith(".:")) {
                formatted = formatted.substring(
                    0, formatted.length - 2
                ) + "" // Remove . at the end of the month (ex: nov.: -> nov:)
            } else if (formatted.endsWith(". :")) {
                formatted = formatted.substring(
                    0, formatted.length - 3
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
        } else if (intent?.hasExtra(MyFirebaseMessagingService.MULTIPLE_ACCOUNT_KEY) == true) {
            appPreferences.setUserSawMultiAccountsHint(false)
            binding.multiAccountsHint.visibility = View.VISIBLE
            binding.layoutSelectAccount.llSelectAccount.callOnClick()
        }

        // Check and launch download campaign.
        checkIfItsDownloadCampaign()

        binding.ivPremiumIcon.setBackgroundResource(if (isUserPremium) R.drawable.ic_premium_icon_gold else R.drawable.ic_premium_icon_grey)
        binding.llBalances.setOnClickListener { revealHideCalendar() }
        binding.ivPremiumIcon.setOnClickListener {
            if (isUserPremium) toastManager.showShort(getString(R.string.thank_you_you_are_premium_user))
            else becomePremium()
            analyticsManager.logEvent(Events.KEY_DASHBOARD_PREMIUM_BUTTON)
        }

        // Check firebase token
        checkToken()
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
            Logger.error(
                MainActivity::class.java.simpleName,
                getString(R.string.error_checking_download_campaign_and_dismissing_notification),
                e
            )
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
            Logger.debug("FCM TOKEN", token ?: "")
            appPreferences.saveFCMToken(token)
        })
    }

    /**
     *
     */
    private fun calendarRevealAnimation() {
        try {
            // TODAY's DATE
            /*val format = DateTimeFormatter.ofPattern(
                resources.getString(R.string.date_format_calender_reveal), Locale.getDefault()
            )
            val formattedDate = format.format(expensesViewAdapter.getDate())
            String.format("%s", formattedDate)*/
            binding.llBalances.setOnClickListener { revealHideCalendar() }

            binding.ivCalendarCollapse.setOnClickListener {
                analyticsManager.logEvent(Events.KEY_DASHBOARD_CALENDAR_BUTTON)
                revealHideCalendar()
            }

            binding.layoutSelectAccount.tvSelectedAccount.text =
                String.format("%s", appPreferences.activeAccountLabel().appendAccount())
            binding.layoutSelectAccount.llSelectAccount.setOnClickListener {
                val existingFragment =
                    supportFragmentManager.findFragmentByTag(AccountsBottomSheetDialogFragment.TAG) as? AccountsBottomSheetDialogFragment
                if (existingFragment == null || existingFragment.isAdded.not()) {
                    val accountsBottomSheetDialogFragment =
                        AccountsBottomSheetDialogFragment(onAccountSelected = { selectedAccount ->
                            binding.layoutSelectAccount.tvSelectedAccount.text =
                                selectedAccount.name.appendAccount()
                            updateAccountNotifyBroadcast()
                            //Log event
                            analyticsManager.logEvent(Events.KEY_ACCOUNT_SWITCHED)
                        }, onAccountUpdated = { updatedAccount ->
                            if (appPreferences.activeAccount() == updatedAccount.id) {
                                binding.layoutSelectAccount.tvSelectedAccount.text =
                                    updatedAccount.name.appendAccount()
                                appPreferences.setActiveAccount(
                                    updatedAccount.id, updatedAccount.name
                                )
                                editAccountNotifyBroadcast()
                            }
                            //Log event
                            analyticsManager.logEvent(Events.KEY_ACCOUNT_UPDATED)
                        })
                    accountsBottomSheetDialogFragment.show(
                        supportFragmentManager, AccountsBottomSheetDialogFragment.TAG
                    )
                }
            }
        } catch (e: Exception) {
            Log.i("", "${e.message}")
        }
    }

    /**
     * Reveal hide calendar
     */
    private fun revealHideCalendar() {
        if (binding.calendarView.isVisible) {
            binding.calendarView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.debounce))
            binding.calendarView.visibility = View.GONE
            analyticsManager.logEvent(Events.KEY_DASHBOARD_CALENDAR_CLOSED)
        } else {
            binding.calendarView.visibility = View.VISIBLE
            binding.calendarView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce))
            analyticsManager.logEvent(Events.KEY_DASHBOARD_CALENDAR_OPEN)
        }
    }

    /**
     *
     */
    override fun onStart() {
        super.onStart()

        // If the last stop happened yesterday (or another day), set and refresh to the current date
        lastStopDate?.let {
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
        destroyBanner(adView)
        adView = null
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openSettingsIfNeeded(intent)
        openMonthlyReportIfNeeded(intent)
        openAddExpenseIfNeeded(intent)
        openAddRecurringExpenseIfNeeded(intent)
        openSettingsForBackupIfNeeded(intent)
        openBudgetIntroIfNeeded(intent)
        openBudgetsIfNeeded(intent)
        openHowToIfNeeded(intent)
        openBuyPremiumIfNeeded(intent)
    }

// ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        // Search Hint
        if (!appPreferences.hasUserSawSearchHint()) {
            binding.searchHint.visibility = View.VISIBLE
            binding.searchHintButton.setOnClickListener {
                appPreferences.setUserSawSearchHint()
                binding.searchHint.visibility = View.GONE
                binding.monthlyReportHint.visibility = View.VISIBLE
            }
        }

        // Monthly Report Hint
        if (!appPreferences.hasUserSawMonthlyReportHint()) {
            binding.monthlyReportHintButton.setOnClickListener {
                appPreferences.setUserSawMonthlyReportHint()
                binding.monthlyReportHint.visibility = View.GONE
                binding.backupHint.visibility = View.VISIBLE
            }
        }
        // Backup Hint
        if (!appPreferences.hasUserSawBackupHint()) {
            binding.backupHintButton.setOnClickListener {
                appPreferences.setUserSawBackupHint()
                binding.backupHint.visibility = View.GONE
                binding.helpHint.visibility = View.VISIBLE
            }
        }

        //Help Hint
        if (!appPreferences.hasUserSawHelpHint()) {
            binding.helpHintButton.setOnClickListener {
                appPreferences.setUserSawHelpHint()
                binding.helpHint.visibility = View.GONE
                binding.settingsHint.visibility = View.VISIBLE
            }
        }

        // Settings Hint
        if (!appPreferences.hasUserSawSettingsHint()) {
            binding.settingsHintButton.setOnClickListener {
                appPreferences.setUserSawSettingsHint()
                binding.settingsHint.visibility = View.GONE
                binding.multiAccountsHint.visibility = View.VISIBLE
                showPrivacyMenuForAdmob(menu)// Show privacy menu for new user, 1st install
                openMultipleAccountsShowCase()
            }
        } else {
            // Show privacy menu if user has already seen settings hint and newly exposed to privacy settings.
            showPrivacyMenuForAdmob(menu)
        }

        // Multiple accounts hint in sequence
        if (appPreferences.hasUserSawMultiAccountsHint().not()) {
            binding.multiAccountsHintButton.setOnClickListener {
                appPreferences.setUserSawMultiAccountsHint()
                binding.multiAccountsHint.visibility = View.GONE
                binding.calendarHint.visibility = View.VISIBLE
                openCalendarHintShowCase()
            }
        }
        // Calendar icon hint in sequence
        if (appPreferences.hasUserSawCalendarIconHint().not()) {
            binding.calendarHintButton.setOnClickListener {
                appPreferences.setUserSawCalendarIconHint()
                binding.calendarHint.visibility = View.GONE
                openHideBalanceShowCase()
            }
        }
        return true
    }

    /**
     * Needs to show this once all action bar hints are completed.
     * Settings hint is the last hint of action bar.
     */
    private fun showPrivacyMenuForAdmob(menu: Menu) {
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false).not()) {
            val moreMenu = menu.findItem(R.id.action_more)
            val show = googleMobileAdsConsentManager?.isPrivacyOptionsRequired ?: false
            moreMenu?.isVisible = show
        }
    }

    /**
     * Show case Hint for multiple accounts
     */
    private fun openMultipleAccountsShowCase() {
        if (appPreferences.hasUserSawMultiAccountsHint().not()) {
            binding.multiAccountsHint.visibility = View.VISIBLE
            binding.multiAccountsHintButton.setOnClickListener {
                binding.multiAccountsHint.visibility = View.GONE
                appPreferences.setUserSawMultiAccountsHint()
                openCalendarHintShowCase()
            }
        }
    }

    /**
     * Show case Hint for calendar hint
     */
    private fun openCalendarHintShowCase() {
        if (appPreferences.hasUserSawCalendarIconHint().not()) {
            binding.calendarHint.visibility = View.VISIBLE
            binding.calendarHintButton.setOnClickListener {
                binding.calendarHint.visibility = View.GONE
                appPreferences.setUserSawCalendarIconHint()
                openHideBalanceShowCase()
            }
        }
    }

    /**
     * Show case Hint for balance
     */
    private fun openHideBalanceShowCase() {
        binding.calendarHint.visibility = View.GONE
        if (appPreferences.hasUserSawHideBalanceHint().not()) {
            showCaseView(
                targetView = binding.contSwitchBalance,
                title = getString(R.string.hide_balance_hint_title),
                message = getString(R.string.hide_balance_hint_message),
                handleGuideListener = {
                    showCaseAddSingleExpense()
                })
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
                    showCaseAddRecurringExpense()
                })
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
                    sideMenuHint()

                })
        }
    }

    /**
     * Side menu hint with counter
     */
    private fun sideMenuHint() {
        binding.llDummyViewForHint.visibility = View.GONE
        binding.drawerLayout.open()
        binding.counter.visibility = View.VISIBLE
        // 3 Seconds delay we'll close the drawer!
        toastManager.showShort(getString(R.string.side_navigation_for_more_options))
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val countDown = (millisUntilFinished / 1000)
                if (countDown != 0L) binding.counter.text = String.format(
                    Locale.getDefault(), "%d", countDown
                )
            }

            override fun onFinish() {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                binding.counter.visibility = View.GONE
                val show = googleMobileAdsConsentManager?.isPrivacyOptionsRequired ?: false
                if (show) {
                    // Privacy settings hint
                    showPrivacyHint()
                }
            }
        }.start()
    }

    private fun showPrivacyHint() {
        if (appPreferences.hasUserSawPrivacyHint().not()) {
            binding.privacyHint.visibility = View.VISIBLE
            binding.privacyHintButton.setOnClickListener {
                appPreferences.setUserSawPrivacyHint()
                binding.privacyHint.visibility = View.GONE
            }
        }
    }

    /**
     * Open settings screen
     */
    private fun goToSettings() {
        val startIntent = Intent(this, SettingsActivity::class.java)
        ActivityCompat.startActivityForResult(
            this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null
        )
    }

    /**
     * Share App
     */
    private fun shareApp() = try {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            resources.getString(R.string.app_invite_message) + "\n" + "https://play.google.com/store/apps/details?id=gplx.simple.budgetapp"
        )
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    } catch (e: Exception) {
        Logger.error(getString(R.string.an_error_occurred_during_sharing_app_activity_start), e)
    }

    /**
     * Open Break Down
     */
    private fun openBreakDown(pieChart: Boolean) {
        val startIntent = Intent(this, BreakDownBaseActivity::class.java)
        startIntent.putExtra(BreakDownBaseActivity.REQUEST_CODE_FOR_PIE_CHART, pieChart)
        ActivityCompat.startActivity(this@MainActivity, startIntent, null)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                analyticsManager.logEvent(Events.KEY_DASHBOARD_SETTINGS_BUTTON)
                goToSettings()
                return true
            }

            R.id.action_monthly_report -> {
                analyticsManager.logEvent(Events.KEY_DASHBOARD_REPORT_BUTTON)
                val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                ActivityCompat.startActivity(this@MainActivity, startIntent, null)
                return true
            }

            R.id.action_search_expenses -> {
                analyticsManager.logEvent(Events.KEY_DASHBOARD_SEARCH_BUTTON)
                ActivityCompat.startActivity(
                    this@MainActivity, Intent(this, SearchBaseActivity::class.java), null
                )
                return true
            }

            R.id.action_help -> {
                analyticsManager.logEvent(Events.KEY_DASHBOARD_HELP_BUTTON)
                startActivity(Intent(this, HelpActivity::class.java))
                return true
            }

            R.id.action_backup -> {
                analyticsManager.logEvent(Events.KEY_DASHBOARD_BACKUP_BUTTON)
                startActivity(Intent(this, BackupSettingsActivity::class.java))
                return true
            }

            android.R.id.home -> {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }
                return true
            }

            R.id.action_more -> {
                val menuItemView = findViewById<View>(R.id.action_more)
                menuItemView?.let {
                    PopupMenu(this, menuItemView).apply {
                        menuInflater.inflate(R.menu.privacy_popup_menu, menu)
                        show()
                        setOnMenuItemClickListener { popupMenuItem ->
                            when (popupMenuItem.itemId) {
                                R.id.privacy_settings -> {
                                    // Handle changes to user consent.
                                    googleMobileAdsConsentManager?.showPrivacyOptionsForm(this@MainActivity) { formError ->
                                        if (formError != null) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                formError.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    true
                                }

                                else -> false
                            }
                        }
                        return super.onOptionsItemSelected(item)
                    }
                }
                return true
            }/*R.id.action_language -> {
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
            resources.getString(R.string.account_balance_date_format), Locale.getDefault()
        )
        var formatted = resources.getString(R.string.account_balance_format, formatter.format(day))
        if (formatted.endsWith(".:")) {
            formatted = formatted.substring(
                0, formatted.length - 2
            ) + "" // Remove . at the end of the month (ex: nov.: -> nov:)
        } else if (formatted.endsWith(". :")) {
            formatted = formatted.substring(
                0, formatted.length - 3
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
                analyticsManager.logEvent(
                    Events.KEY_DASHBOARD_BALANCE_PRIVACY,
                    mapOf(Events.KEY_VALUE to Events.KEY_DASHBOARD_BALANCE_PRIVACY_OFF)
                )
            } else {
                appPreferences.setDisplayBalance(false)
                binding.budgetLineAmount.text = BALANCE_PLACE_HOLDER
                binding.expenseLineAmount.text = BALANCE_PLACE_HOLDER
                analyticsManager.logEvent(
                    Events.KEY_DASHBOARD_BALANCE_PRIVACY,
                    mapOf(Events.KEY_VALUE to Events.KEY_DASHBOARD_BALANCE_PRIVACY_ON)
                )
            }
            expensesViewAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Open the settings activity if the given intent contains the [.INTENT_REDIRECT_TO_SETTINGS_EXTRA]
     * extra.
     */
    private fun openSettingsIfNeeded(intent: Intent?) {
        intent?.let {
            if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_EXTRA, false)) {
                val startIntent = Intent(this, SettingsActivity::class.java)
                ActivityCompat.startActivityForResult(
                    this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null
                )
            } else {
                val appLinkAction = intent.action
                val appLinkData: Uri? = intent.data
                if (Intent.ACTION_VIEW == appLinkAction) {
                    Log.e("appLinkData", "" + appLinkData)
                    appLinkData?.lastPathSegment?.also { path ->
                        if (path == "settings") {
                            val startIntent = Intent(this, SettingsActivity::class.java)
                            ActivityCompat.startActivityForResult(
                                this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Open the settings activity to display backup options if the given intent contains the
     * [.INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA] extra.
     */
    private fun openSettingsForBackupIfNeeded(intent: Intent?) {
        intent?.let {
            if (intent.getBooleanExtra(INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA, false)) {
                val startIntent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra(SHOW_BACKUP_INTENT_KEY, true)
                }
                ActivityCompat.startActivityForResult(
                    this@MainActivity, startIntent, SETTINGS_SCREEN_ACTIVITY_CODE, null
                )
            }
        }
    }

    /**
     * Open the monthly report activity if the given intent contains the monthly uri part.
     *
     * @param intent
     */
    private fun openMonthlyReportIfNeeded(intent: Intent?) {
        try {
            intent?.let {
                val data = intent.data
                if (data != null && "true" == data.getQueryParameter(KEY_MONTHLY)) {
                    val startIntent = Intent(this, MonthlyReportBaseActivity::class.java)
                    startIntent.putExtra(MonthlyReportBaseActivity.FROM_NOTIFICATION_EXTRA, true)
                    ActivityCompat.startActivity(this@MainActivity, startIntent, null)
                }
            }
        } catch (e: Exception) {
            Logger.error(getString(R.string.error_while_opening_report_activity_from_intent), e)
        }
    }

    /**
     * Open how to
     * @param intent
     */
    private fun openHowToIfNeeded(intent: Intent?) {
        try {
            intent?.let {
                val data = intent.data
                if (data != null && "true" == data.getQueryParameter(KEY_HOW_TO)) {
                    if (InternetUtils.isInternetAvailable(this)) {
                        analyticsManager.logEvent(Events.KEY_HOW_TO)
                        WebViewActivity.start(
                            this,
                            getString(R.string.simple_budget_how_to_url),
                            getString(R.string.setting_how_to_title),
                            false
                        )
                    } else {
                        toastManager.showShort(getString(R.string.no_internet_connection))
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Open premium screen
     * @param intent
     */
    private fun openBuyPremiumIfNeeded(intent: Intent?) {
        try {
            intent?.let {
                val data = intent.data
                if (data != null && "true" == data.getQueryParameter(KEY_PREMIUM)) {
                    if (appPreferences.isUserPremium().not()) {
                        becomePremium()
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Open budget activity if the given intent contains the budget uri part.
     *
     * @param intent
     */
    private fun openBudgetsIfNeeded(intent: Intent?) {
        try {
            intent?.let {
                val data = intent.data
                if (data != null && "true" == data.getQueryParameter(KEY_BUDGET)) {
                    openBudgetScreen(fromNotification = true)
                }
            }
        } catch (e: Exception) {
            Logger.error(getString(R.string.error_while_opening_budget_from_the_notification), e)
        }
    }

    /**
     * Open the budget intro dialog if the given intent contains the budget_intro uri part.
     * @param intent
     */
    private fun openBudgetIntroIfNeeded(intent: Intent?) {
        try {
            intent?.let {
                val data = intent.data
                if (data != null && "true" == data.getQueryParameter(KEY_BUDGET_INTRO)) {
                    showBudgetIntroDialog()
                }
            }
        } catch (e: Exception) {
            Logger.error(getString(R.string.error_while_opening_budgte_intro_from_notification), e)
        }
    }

    /**
     * Open the add expense screen if the given intent contains the [.INTENT_SHOW_ADD_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddExpenseIfNeeded(intent: Intent?) {
        intent?.let {
            if (intent.getBooleanExtra(INTENT_SHOW_ADD_EXPENSE, false)) {
                val startIntent = Intent(this, ExpenseEditActivity::class.java)
                startIntent.putExtra("date", LocalDate.now().toEpochDay())

                ActivityCompat.startActivityForResult(
                    this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null
                )
            }
        }
    }

    /**
     * Open the add recurring expense screen if the given intent contains the [.INTENT_SHOW_ADD_RECURRING_EXPENSE]
     * extra.
     *
     * @param intent
     */
    private fun openAddRecurringExpenseIfNeeded(intent: Intent?) {
        intent?.let {
            if (intent.getBooleanExtra(INTENT_SHOW_ADD_RECURRING_EXPENSE, false)) {
                val startIntent = Intent(this, RecurringExpenseEditActivity::class.java)
                startIntent.putExtra("dateStart", LocalDate.now().toEpochDay())

                ActivityCompat.startActivityForResult(
                    this, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null
                )
            }
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
                CaldroidFragment.START_DAY_OF_WEEK, appPreferences.getCaldroidFirstDayOfWeek()
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
                ActivityCompat.startActivityForResult(
                    this@MainActivity, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null
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
                        this@MainActivity, R.color.calendar_header_month_color
                    )
                )
                topLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.calendar_header_background
                    )
                )

                leftButton.text = "<"
                leftButton.textSize = 25f
                leftButton.gravity = Gravity.CENTER
                leftButton.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.calendar_month_button_color
                    )
                )
                leftButton.setBackgroundResource(R.drawable.calendar_month_switcher_button_drawable)

                rightButton.text = ">"
                rightButton.textSize = 25f
                rightButton.gravity = Gravity.CENTER
                rightButton.setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.calendar_month_button_color
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
                        this@MainActivity, R.color.calendar_background
                    )
                )
                (viewPager.parent as View?)?.setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.calendar_background
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
            ActivityCompat.startActivityForResult(
                this@MainActivity, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null
            )
        }

        binding.fabNewRecurringExpense.setOnClickListener {
            val startIntent = Intent(this@MainActivity, RecurringExpenseEditActivity::class.java)
            startIntent.putExtra("dateStart", calendarFragment.getSelectedDate().toEpochDay())
            ActivityCompat.startActivityForResult(
                this@MainActivity, startIntent, ADD_EXPENSE_ACTIVITY_CODE, null
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
                    if (binding.calendarView.isVisible) {
                        binding.calendarView.startAnimation(
                            AnimationUtils.loadAnimation(
                                this@MainActivity, R.anim.debounce
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
        AlertDialog.Builder(this@MainActivity).setTitle(R.string.oops)
            .setMessage(R.string.error_occurred_try_again)
            .setNegativeButton(R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun initConsentAndAdsSDK() {
        if (appPreferences.getBoolean(PREMIUM_PARAMETER_KEY, false).not()) {
            googleMobileAdsConsentManager =
                GoogleMobileAdsConsentManager.getInstance(applicationContext)
            googleMobileAdsConsentManager?.gatherConsent(this) { error ->
                if (error != null) {
                    // Consent not obtained in current session.
                    Log.d("MainActivity", "${error.errorCode}: ${error.message}")
                }
                if (googleMobileAdsConsentManager?.canRequestAds != false) {
                    initializeMobileAdsSdk()
                }

                if (googleMobileAdsConsentManager?.isPrivacyOptionsRequired == true) {
                    // Regenerate the options menu to include a privacy setting.
                    invalidateOptionsMenu()
                }
            }
            // This sample attempts to load ads using consent obtained in the previous session.
            if (googleMobileAdsConsentManager?.canRequestAds != false) {
                initializeMobileAdsSdk()
            }
            if (googleMobileAdsConsentManager?.canRequestAds != false) {
                loadBanner(
                    appPreferences.isUserPremium(),
                    binding.adViewContainer,
                    onBannerAdRequested = { bannerAdView ->
                        this.adView = bannerAdView
                    }
                )
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        // Initialize the Mobile Ads SDK.
        if (appPreferences.isUserPremium().not()) {
            if (isMobileAdsInitializeCalled.getAndSet(true)) {
                AdSdkManager.initialize(this) {
                    loadBanner(
                        appPreferences.isUserPremium(),
                        binding.adViewContainer,
                        onBannerAdRequested = { bannerAdView ->
                            this.adView = bannerAdView
                        })
                }
            } else {
                loadBanner(
                    appPreferences.isUserPremium(),
                    binding.adViewContainer,
                    onBannerAdRequested = { bannerAdView ->
                        this.adView = bannerAdView
                    })
            }
        }
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    /**
     * Called when leaving the activity
     */
    override fun onResume() {
        resumeBanner(adView)
        try {
            appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.an_update_has_just_been_downloaded),
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(getString(R.string.restart)) {
                        appUpdateManager?.completeUpdate()
                    }.show()
                }
            }
        } catch (e: Exception) {
            Logger.error("AppUpdate", "Error progress app update: ${e.localizedMessage}", e)
        }
        super.onResume()
    }

    private fun becomePremium() {
        startActivity(Intent(this, PremiumActivity::class.java))
    }

    /**
     * Budget intro dialog
     */
    private fun showBudgetIntroDialog() {
        try {
            if (appPreferences.isDoneDisplayingBudgetIntroDialog().not()) {
                DialogUtil.createDialog(
                    this,
                    title = getString(R.string.budget_intro_title),
                    message = String.format(
                        Locale.getDefault(),
                        "%s%s%s",
                        getString(R.string.budget_intro_message),
                        "\n\n\n",
                        getString(R.string.you_can_access_it_from_the_side_menu)
                    ),
                    positiveBtn = getString(R.string.try_now),
                    negativeBtn = getString(R.string.later),
                    isCancelable = false,
                    positiveClickListener = {
                        // Perform action for trying the feature
                        analyticsManager.logEvent(Events.KEY_BUDGETS_FROM_INTRO)
                        openBudgetScreen()
                    },
                    negativeClickListener = {})?.show()

                //Done displaying intro dialog
                appPreferences.setDoneDisplayingBudgetIntroDialog()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Check for app updates
     */
    private fun checkForAppUpdate() {
        try {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            val appUpdateInfoTask = appUpdateManager?.appUpdateInfo
            appUpdateInfoTask?.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(
                        AppUpdateType.FLEXIBLE
                    )
                ) {
                    val appUpdateOptions =
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()

                    appUpdateManager?.startUpdateFlowForResult(
                        appUpdateInfo, appUpdateLauncher, appUpdateOptions
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error("AppUpdate", "Error app update: ${e.localizedMessage}", e)
        }
    }

    companion object {
        const val ADD_EXPENSE_ACTIVITY_CODE = 101
        const val MANAGE_RECURRING_EXPENSE_ACTIVITY_CODE = 102
        const val WELCOME_SCREEN_ACTIVITY_CODE = 103
        const val SETTINGS_SCREEN_ACTIVITY_CODE = 104
        const val INTENT_ACCOUNT_TYPE_UPDATED = "intent.account.type.updated"
        const val INTENT_ACCOUNT_TYPE_EDITED = "intent.account.type.EDITED"
        const val INTENT_EXPENSE_DELETED = "intent.expense.deleted"
        const val INTENT_EXPENSE_ADDED = "intent.expense.added"
        const val INTENT_RECURRING_EXPENSE_DELETED = "intent.expense.monthly.deleted"
        const val INTENT_SHOW_WELCOME_SCREEN = "intent.welcomscreen.show"
        const val INTENT_SHOW_ADD_EXPENSE = "intent.addexpense.show"
        const val INTENT_SHOW_ADD_RECURRING_EXPENSE = "intent.addrecurringexpense.show"

        const val INTENT_REDIRECT_TO_SETTINGS_EXTRA = "intent.extra.redirecttosettings"
        const val INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA =
            "intent.extra.redirecttosettingsforbackup"
        private const val CALENDAR_SAVED_STATE = "calendar_saved_state"
    }
}