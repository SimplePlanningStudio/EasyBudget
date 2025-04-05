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
package com.simplebudget.view.settings.help

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.simplebudget.R
import com.simplebudget.helper.*
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.*
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.view.settings.faq.FAQActivity
import com.simplebudget.view.settings.webview.WebViewActivity
import org.koin.android.ext.android.inject


/**
 * Fragment to display Help preferences
 *
 * @author Waheed Nazir
 */
class HelpFragment : PreferenceFragmentCompat() {

    private val toastManager: ToastManager by inject()

    private val analyticsManager: AnalyticsManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.help_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_CONTACT_US_SCREEN)
        /*
         * Telegram channel button
         */
        findPreference<Preference>(resources.getString(R.string.setting_how_to_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                if (InternetUtils.isInternetAvailable(requireActivity())) {
                    analyticsManager.logEvent(Events.KEY_HOW_TO)
                    WebViewActivity.start(
                        requireActivity(),
                        getString(R.string.simple_budget_how_to_url),
                        getString(R.string.setting_how_to_title),
                        false
                    )
                } else {
                    toastManager.showShort(getString(R.string.no_internet_connection))
                }
                false
            }

        /*
         * Telegram channel button
         */
        findPreference<Preference>(resources.getString(R.string.setting_telegram_channel_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                analyticsManager.logEvent(Events.KEY_TELEGRAM)
                Intent().openTelegramChannel(requireActivity(), toastManager)
                false
            }

        /**
         * Whatsapp channel page
         * https://whatsapp.com/channel/0029VaIvpNp8F2pFBZOKLX28
         */
        findPreference<Preference>(resources.getString(R.string.setting_whatsapp_channel_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                analyticsManager.logEvent(Events.KEY_WHATSAPP)
                Intent().openWhatsAppChannel(requireActivity(), toastManager)
                false
            }

        /**
         * Youtube channel page
         * https://www.youtube.com/@simplebudget4045/featured
         */
        findPreference<Preference>(resources.getString(R.string.setting_youtube_channel_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                analyticsManager.logEvent(Events.KEY_YOUTUBE)
                Intent().openYoutubeChannel(requireActivity(), toastManager)
                false
            }
        /*
         * FAQ app
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_faq_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                analyticsManager.logEvent(Events.KEY_FAQ)
                startActivity(Intent(requireActivity(), FAQActivity::class.java))
                false
            }

        /*
         * Feedback of app
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_feedback_app_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                analyticsManager.logEvent(Events.KEY_FEEDBACK_GMAIL)
                Feedback.askForFeedback(requireActivity())
                false
            }

    }
}
