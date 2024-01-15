/*
 *   Copyright 2024 Benoit LETONDOR
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
package com.simplebudget.view.settings

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.roomorama.caldroid.CaldroidFragment
import com.simplebudget.R
import com.simplebudget.helper.*
import com.simplebudget.iab.Iab
import com.simplebudget.prefs.*
import com.simplebudget.view.RatingPopup
import com.simplebudget.view.breakdown.base.BreakDownBaseActivity
import com.simplebudget.view.premium.PremiumActivity
import com.simplebudget.view.premium.PremiumSuccessActivity
import com.simplebudget.view.report.base.MonthlyReportBaseActivity
import com.simplebudget.view.reset.ResetAppDataActivity
import com.simplebudget.view.selectcurrency.SelectCurrencyFragment
import com.simplebudget.view.settings.SettingsActivity.Companion.SHOW_BACKUP_INTENT_KEY
import com.simplebudget.view.settings.aboutus.AboutUsActivity
import com.simplebudget.view.settings.backup.BackupSettingsActivity
import com.simplebudget.view.settings.help.HelpActivity
import org.koin.android.ext.android.inject

/**
 * Fragment to display preferences
 *
 * @author Benoit LETONDOR
 */
class PreferencesFragment : PreferenceFragmentCompat() {

    /**
     * The dialog to select a new currency (will be null if not shown)
     */
    private var selectCurrencyDialog: SelectCurrencyFragment? = null

    /**
     * Broadcast receiver (used for currency selection)
     */
    private lateinit var receiver: BroadcastReceiver

    /**
     * Launcher for notification permission request
     */
    private lateinit var notificationRequestPermissionLauncher: ActivityResultLauncher<String>

    /**
     *
     */
    private val iab: Iab by inject()

    private val appPreferences: AppPreferences by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Notifications permission
        notificationRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) {
                    activity?.let {
                        MaterialAlertDialogBuilder(it).setTitle(R.string.setting_notification_permission_rejected_dialog_title)
                            .setMessage(R.string.setting_notification_permission_rejected_dialog_description)
                            .setPositiveButton(R.string.setting_notification_permission_rejected_dialog_accept_cta) { dialog, _ ->
                                dialog.dismiss()
                                showNotificationPermissionIfNeeded()
                            }
                            .setNegativeButton(R.string.setting_notification_permission_rejected_dialog_not_now_cta) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    }
                }
                findPreference<Preference>(resources.getString(R.string.setting_enable_app_notifications_key))?.isVisible =
                    true
                findPreference<Preference>(resources.getString(R.string.setting_enable_app_notifications_key))?.setSummary(
                    if (isNotificationsPermissionGranted()) {
                        R.string.backup_settings_backups_activated
                    } else {
                        R.string.backup_settings_backups_deactivated
                    }
                )
            }

        if (Build.VERSION.SDK_INT < 33) {
            findPreference<Preference>(resources.getString(R.string.setting_enable_app_notifications_key))?.isVisible =
                false
        } else {
            findPreference<Preference>(resources.getString(R.string.setting_enable_app_notifications_key))?.isVisible =
                isNotificationsPermissionGranted().not()
            findPreference<Preference>(resources.getString(R.string.setting_enable_app_notifications_key))?.setSummary(
                if (isNotificationsPermissionGranted()) {
                    R.string.backup_settings_backups_activated
                } else {
                    R.string.backup_settings_backups_deactivated
                }
            )
            // Handle launch of notification permission
            findPreference<Preference>(resources.getString(R.string.setting_enable_app_notifications_key))?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    activity?.let {
                        showNotificationPermissionIfNeeded()
                    }
                    false
                }
        }


        /*
         * Rating button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_rate_button_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity?.let { activity ->
                    RatingPopup(activity, appPreferences).show(true)
                }
                false
            }

        /*
         * Reset App Data
         */
        findPreference<Preference>(resources.getString(R.string.setting_reset_app_data_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity?.let { activity ->
                    startActivity(Intent(activity, ResetAppDataActivity::class.java))
                }
                false
            }
        /*
         * Change language button
         */
        /*findPreference<Preference>(resources.getString(R.string.setting_category_change_language_button_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity?.let { activity ->
                    Languages.showLanguagesDialog(
                        activity,
                        appPreferences.getCurrentLanguage(),
                        onLanguageSelected = { languageCode ->
                            appPreferences.setCurrentLanguage(languageCode)
                        })
                }
                false
            }*/

        /*
         * Start day of week
         */
        val firstDayOfWeekPref =
            findPreference<Preference>(getString(R.string.setting_category_start_day_of_week_key))
        firstDayOfWeekPref?.summary = resources.getString(
            R.string.setting_category_start_day_of_week_summary,
            getWeekDaysName(appPreferences.getCaldroidFirstDayOfWeek())
        )
        firstDayOfWeekPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val weeks = arrayOf(
                "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
            )
            MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.setting_category_start_day_of_week_title))
                .setItems(weeks) { dialog, which ->
                    appPreferences.setCaldroidFirstDayOfWeek(getWeekDayId(weeks[which]))
                    firstDayOfWeekPref?.summary = resources.getString(
                        R.string.setting_category_start_day_of_week_summary,
                        getWeekDaysName(appPreferences.getCaldroidFirstDayOfWeek())
                    )
                    dialog.dismiss()
                }.show()
            true
        }

        /*
         * Full app lock
         */
        val enableAppPasswordProtection =
            findPreference<Preference>(getString(R.string.setting_enable_app_protection_key))
        enableAppPasswordProtection?.summary =
            String.format("%s", if (appPreferences.isAppPasswordProtectionOn()) "ON" else "OFF")
        enableAppPasswordProtection?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                if (!iab.isUserPremium()) (activity as SettingsActivity).loadInterstitial()
                (activity as SettingsActivity).handleAppPasswordProtection()
                true
            }
        /*
         * Monthly Reports
         */
        val monthlyReports =
            findPreference<Preference>(getString(R.string.setting_monthly_reports_key))
        monthlyReports?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val startIntent = Intent(requireActivity(), MonthlyReportBaseActivity::class.java)
            startIntent.putExtra(MonthlyReportBaseActivity.FROM_NOTIFICATION_EXTRA, false)
            ActivityCompat.startActivity(requireActivity(), startIntent, null)
            true
        }

        /*
         * Monthly Breakdown
         */
        val monthlyBreakDown =
            findPreference<Preference>(getString(R.string.setting_monthly_breakdown_key))
        monthlyBreakDown?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val startIntent = Intent(requireActivity(), BreakDownBaseActivity::class.java)
            startIntent.putExtra(BreakDownBaseActivity.FROM_NOTIFICATION_EXTRA, false)
            ActivityCompat.startActivity(requireActivity(), startIntent, null)
            true
        }

        /*
         * Backup
         */
        findPreference<Preference>(getString(R.string.setting_category_backup))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(context, BackupSettingsActivity::class.java))
                false
            }
        updateBackupPreferences()

        /*
         * Share app
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_share_app_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
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

                false
            }

        /*
         * Help & support / Contact Us
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_contact_us))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(requireActivity(), HelpActivity::class.java))
                false
            }

        /*
         * Currency change button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_currency_change_button_key))?.let { currencyPreference ->
            currencyPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                selectCurrencyDialog = SelectCurrencyFragment()
                selectCurrencyDialog!!.show(
                    (activity as SettingsActivity).supportFragmentManager, "SelectCurrency"
                )

                false
            }
            setCurrencyPreferenceTitle(currencyPreference)
        }


        /*
         * Warning limit button
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_limit_set_button_key))?.let { limitWarningPreference ->
            limitWarningPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    val dialogView =
                        activity?.layoutInflater?.inflate(R.layout.dialog_set_warning_limit, null)
                    val limitEditText =
                        dialogView?.findViewById<View>(R.id.warning_limit) as EditText
                    limitEditText.setText(appPreferences.getLowMoneyWarningAmount().toString())
                    limitEditText.setSelection(limitEditText.text.length) // Put focus at the end of the text

                    context?.let { context ->
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(R.string.adjust_limit_warning_title)
                        builder.setMessage(R.string.adjust_limit_warning_message)
                        builder.setView(dialogView)
                        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        builder.setPositiveButton(R.string.ok) { _, _ ->
                            var limitString = limitEditText.text.toString()
                            if (limitString.trim { it <= ' ' }.isEmpty()) {
                                limitString =
                                    "0" // Set a 0 value if no value is provided (will lead to an error displayed to the user)
                            }

                            try {
                                val newLimit = Integer.valueOf(limitString)

                                // Invalid value, alert the user
                                if (newLimit <= 0) {
                                    throw IllegalArgumentException("limit should be > 0")
                                }

                                appPreferences.setLowMoneyWarningAmount(newLimit)
                                setLimitWarningPreferenceTitle(limitWarningPreference)
                            } catch (e: Exception) {
                                AlertDialog.Builder(context).setTitle(R.string.oops)
                                    .setMessage(resources.getString(R.string.adjust_limit_warning_error_message))
                                    .setPositiveButton(R.string.ok) { dialog1, _ -> dialog1.dismiss() }
                                    .show()
                            }
                        }

                        val dialog = builder.show()

                        // Directly show keyboard when the dialog pops
                        limitEditText.onFocusChangeListener =
                            View.OnFocusChangeListener { _, hasFocus ->
                                // Check if the device doesn't have a physical keyboard
                                if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
                                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                                }
                            }
                    }

                    false
                }

            setLimitWarningPreferenceTitle(limitWarningPreference)

            /*
             * Remove Ads button, Premium
            */
            refreshPremiumPreference()
        }

        /*
         * Notifications
         */
        val updateNotificationPref =
            findPreference<CheckBoxPreference>(resources.getString(R.string.setting_category_notifications_update_key))
        updateNotificationPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            appPreferences.setUserAllowUpdatePushes((it as CheckBoxPreference).isChecked)
            true
        }
        updateNotificationPref?.isChecked = appPreferences.isUserAllowingUpdatePushes()


        /*
         * Broadcast receiver
         */
        val filter = IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(appContext: Context, intent: Intent) {
                if (SelectCurrencyFragment.CURRENCY_SELECTED_INTENT == intent.action && selectCurrencyDialog != null) {
                    findPreference<Preference>(resources.getString(R.string.setting_category_currency_change_button_key))?.let { currencyPreference ->
                        setCurrencyPreferenceTitle(currencyPreference)
                    }
                    selectCurrencyDialog?.dismiss()
                    selectCurrencyDialog = null
                }
            }
        }

        context?.let { context ->
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        }

        /*
         * Check if we should show backup options
         */
        if (activity?.intent?.getBooleanExtra(SHOW_BACKUP_INTENT_KEY, false) == true) {
            activity?.intent?.putExtra(SHOW_BACKUP_INTENT_KEY, false)
            startActivity(Intent(context, BackupSettingsActivity::class.java))
        }


        // Redeem promo code pref
        findPreference<Preference>(resources.getString(R.string.setting_category_premium_redeem_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                RedeemPromo.openPromoCodeDialog(activity)
                false
            }

        // About Us
        findPreference<Preference>(resources.getString(R.string.setting_about_us_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(context, AboutUsActivity::class.java))
                false
            }
    }

    /**
     * Check if notifications permission is granted or not
     */
    private fun isNotificationsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT < 33) {
            false
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Ask for notifications permissions
     */
    private fun showNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (isNotificationsPermissionGranted().not()) notificationRequestPermissionLauncher.launch(
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    /**
     *
     */
    private fun updateBackupPreferences() {
        findPreference<Preference>(getString(R.string.setting_category_backup))?.setSummary(
            if (appPreferences.isBackupEnabled()) {
                R.string.backup_settings_backups_activated
            } else {
                R.string.backup_settings_backups_deactivated
            }
        )
    }

    /**
     *
     */
    override fun onResume() {
        super.onResume()
        updateBackupPreferences()
    }

    /**
     * Set the currency preference title according to selected currency
     *
     * @param currencyPreference
     */
    private fun setCurrencyPreferenceTitle(currencyPreference: Preference) {
        currencyPreference.title = resources.getString(
            R.string.setting_category_currency_change_button_title,
            appPreferences.getUserCurrency().symbol
        )
    }

    /**
     * Set the limit warning preference title according to the selected limit
     *
     * @param limitWarningPreferenceTitle
     */
    private fun setLimitWarningPreferenceTitle(limitWarningPreferenceTitle: Preference) {
        limitWarningPreferenceTitle.title = resources.getString(
            R.string.setting_category_limit_set_button_title,
            CurrencyHelper.getFormattedCurrencyString(
                appPreferences, appPreferences.getLowMoneyWarningAmount().toDouble()
            )
        )
    }

    override fun onDestroy() {
        context?.let { context ->
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
        super.onDestroy()
    }


    /**
     * Show the right premium preference depending on the user state
     */
    private fun refreshPremiumPreference() {
        val isPremium = iab.isUserPremium()
        val pref: Preference =
            findPreference(resources.getString(R.string.setting_category_premium_key))!!
        if (isPremium) {
            pref.title = getString(R.string.setting_category_premium_status_title)
            pref.summary = getString(R.string.setting_category_premium_status_message)
            findPreference<Preference>(resources.getString(R.string.setting_category_premium_redeem_key))?.isVisible =
                false
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(requireActivity(), PremiumSuccessActivity::class.java).putExtra(
                        PremiumSuccessActivity.REQUEST_CODE_IS_BACK_ENABLED, true
                    )
                )
                false
            }
        } else {
            findPreference<Preference>(resources.getString(R.string.setting_category_premium_redeem_key))?.isVisible =
                true
            pref.title = getString(R.string.setting_category_not_premium_status_title)
            pref.summary = getString(R.string.setting_category_not_premium_status_message)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showBecomePremiumDialog()
                false
            }
        }
    }

    private fun showBecomePremiumDialog() {
        // Launch in app here
        startActivity(Intent(context, PremiumActivity::class.java))
    }

    /**
     *
     */
    private fun getWeekDaysName(weekDay: Int): String {
        return when (weekDay) {
            CaldroidFragment.SUNDAY -> "SUNDAY"
            CaldroidFragment.MONDAY -> "MONDAY"
            CaldroidFragment.TUESDAY -> "TUESDAY"
            CaldroidFragment.WEDNESDAY -> "WEDNESDAY"
            CaldroidFragment.THURSDAY -> "THURSDAY"
            CaldroidFragment.FRIDAY -> "FRIDAY"
            CaldroidFragment.SATURDAY -> "SATURDAY"
            else -> "MONDAY"
        }
    }

    /**
     *
     */
    private fun getWeekDayId(weekDay: String): Int {
        return when (weekDay) {
            "SUNDAY" -> CaldroidFragment.SUNDAY
            "MONDAY" -> CaldroidFragment.MONDAY
            "TUESDAY" -> CaldroidFragment.TUESDAY
            "WEDNESDAY" -> CaldroidFragment.WEDNESDAY
            "THURSDAY" -> CaldroidFragment.THURSDAY
            "FRIDAY" -> CaldroidFragment.FRIDAY
            "SATURDAY" -> CaldroidFragment.SATURDAY
            else -> CaldroidFragment.MONDAY
        }
    }

    /**
     *
     */
    fun displayPasswordProtectionDisclaimer() {
        val builder = AlertDialog.Builder(requireContext()).setCancelable(false)
            .setOnDismissListener { if (!iab.isUserPremium()) (activity as SettingsActivity).showInterstitial() }
            .setTitle("App password successfully set!")
            .setMessage("Note: If you forgot your password just clear or re-install app.\n\nYou can't reset your password.That's why it is recommended to enable backup so that your data would be saved in case of reset.\n\nThank you")
            .setPositiveButton("Got it!") { _, _ ->
            }
        builder.create().show()
    }

    /**
     *
     */
    fun updateAppPasswordProtectionLabel() {
        val enableAppPasswordProtection =
            findPreference<Preference>(getString(R.string.setting_enable_app_protection_key))
        enableAppPasswordProtection?.summary =
            String.format("%s", if (appPreferences.isAppPasswordProtectionOn()) "ON" else "OFF")
    }
}
