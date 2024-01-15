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
package com.simplebudget.view.settings.help

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.simplebudget.R
import com.simplebudget.helper.*
import com.simplebudget.helper.extensions.*
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.view.settings.faq.FAQActivity
import org.koin.android.ext.android.inject


/**
 * Fragment to display Help preferences
 *
 * @author Waheed Nazir
 */
class HelpFragment : PreferenceFragmentCompat() {

    private val toastManager: ToastManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.help_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Telegram channel button
         */
        findPreference<Preference>(resources.getString(R.string.setting_telegram_channel_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Intent().openTelegramChannel(requireActivity(), toastManager)
                false
            }

        /**
         * Whatsapp channel page
         * https://whatsapp.com/channel/0029VaIvpNp8F2pFBZOKLX28
         */
        findPreference<Preference>(resources.getString(R.string.setting_whatsapp_channel_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Intent().openWhatsAppChannel(requireActivity(), toastManager)
                false
            }

        /**
         * Youtube channel page
         * https://www.youtube.com/@simplebudget4045/featured
         */
        findPreference<Preference>(resources.getString(R.string.setting_youtube_channel_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Intent().openYoutubeChannel(requireActivity(), toastManager)
                false
            }
        /*
         * FAQ app
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_faq_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(requireActivity(), FAQActivity::class.java))
                false
            }

        /*
         * Feedback of app
         */
        findPreference<Preference>(resources.getString(R.string.setting_category_feedback_app_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                Feedback.askForFeedback(requireActivity())
                false
            }

    }
}
