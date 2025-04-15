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
package com.simplebudget.view.settings.aboutus

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.helper.InternetUtils
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.intentOpenWebsite
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.view.moreApps.MoreAppsActivity
import com.simplebudget.view.settings.openSource.OpenSourceDisclaimerActivity
import com.simplebudget.view.settings.releaseHistory.ReleaseHistoryTimelineActivity
import com.simplebudget.view.settings.webview.WebViewActivity
import org.koin.android.ext.android.inject
import kotlin.getValue

/**
 * Fragment to display About Us preferences
 *
 * @author Waheed Nazir
 */
class AboutUsFragment : PreferenceFragmentCompat() {


    private val toastManager: ToastManager by inject()

    private val analyticsManager: AnalyticsManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about_us_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * App version
         */
        val prefAppVersion: Preference =
            findPreference(resources.getString(R.string.setting_app_version_key))!!
        prefAppVersion.title = getString(R.string.setting_app_version)
        prefAppVersion.summary = String.format(
            "%s",
            BuildConfig.VERSION_NAME
        )

        /*
        * Open privacy policy
        */
        findPreference<Preference>(resources.getString(R.string.setting_privacy_policy_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                if (InternetUtils.isInternetAvailable(requireActivity())) {
                    analyticsManager.logEvent(Events.KEY_PRIVACY_POLICY)
                    WebViewActivity.start(
                        requireActivity(),
                        getString(R.string.privacy_policy_url),
                        getString(R.string.simple_budget_privacy_policy),
                        false
                    )
                } else {
                    toastManager.showShort(getString(R.string.no_internet_connection))
                }
                false
            }

        /*
        * App version click
        */
        findPreference<Preference>(resources.getString(R.string.setting_app_version_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val sendIntent =
                    Intent(requireActivity(), ReleaseHistoryTimelineActivity::class.java)
                startActivity(sendIntent)
                false
            }
        /*
         * Open copyright disclaimer
         */
        findPreference<Preference>(resources.getString(R.string.setting_app_open_source_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val sendIntent = Intent(requireActivity(), OpenSourceDisclaimerActivity::class.java)
                startActivity(sendIntent)
                false
            }


        /*
         * More apps
         */
        findPreference<Preference>(resources.getString(R.string.setting_more_apps_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(activity, MoreAppsActivity::class.java))
                false
            }
    }
}
