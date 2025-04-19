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
package com.simplebudget.base

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewbinding.ViewBinding
import com.simplebudget.BuildConfig
import com.simplebudget.R
import com.simplebudget.helper.APP_BANNER_DISPLAY_THRESHOLD
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.Logger
import com.simplebudget.helper.Rate
import com.simplebudget.helper.getFormattedDate
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.allowFontsScaling
import com.simplebudget.prefs.getAppInstallationDate
import com.simplebudget.prefs.getShowBannerCount
import com.simplebudget.prefs.getShowBannerDate
import com.simplebudget.prefs.saveAppInstallationDate
import com.simplebudget.prefs.saveShowBannerCount
import com.simplebudget.prefs.saveShowBannerDate
import com.simplebudget.push.MyFirebaseMessagingService.Companion.ACTION_TRIGGER_DOWNLOAD
import com.simplebudget.push.MyFirebaseMessagingService.Companion.NOTIFICATION_ID_NEW_FEATURES
import com.simplebudget.view.main.MainActivity
import org.koin.android.ext.android.inject

abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: V
    private lateinit var listener: MyBroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val appPreferences: AppPreferences by inject()

    abstract fun createBinding(): V

    /**
     * On create
     */
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding = createBinding()
        this.binding = binding
        setContentView(binding.root)

        // Download apps dialog
        listener = MyBroadcastReceiver()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(listener, IntentFilter(ACTION_TRIGGER_DOWNLOAD))

        // Save app installation day or day 1 after that day we'll show app promotion banner
        if (appPreferences.getAppInstallationDate().isEmpty()) {
            appPreferences.saveAppInstallationDate(DateHelper.today.getFormattedDate(this))
        }
    }

    override fun attachBaseContext(newBase: Context) {
        try {
            if (appPreferences.allowFontsScaling()) {
                super.attachBaseContext(newBase)
            } else {
                //Lock the fonts scaling
                val configuration = Configuration(newBase.resources.configuration)
                configuration.fontScale =
                    1.0f  // 👈 Lock the font size regardless of device settings
                val context = newBase.createConfigurationContext(configuration)
                super.attachBaseContext(context)
            }
        } catch (_: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    /**
     *
     */
    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_TRIGGER_DOWNLOAD -> {
                    val packageToDownload = intent.getStringExtra("package") ?: ""
                    val title = intent.getStringExtra("title") ?: ""
                    val body = intent.getStringExtra("body") ?: ""
                    if (title.isNotEmpty() && body.isNotEmpty() && packageToDownload.isNotEmpty()) {
                        showDownloadDialog(title, body, packageToDownload)
                    }
                }
            }
        }
    }

    /**
     *
     */
    private fun showDownloadDialog(title: String, body: String, packageToDownload: String) {
        try {
            val alert = AlertDialog.Builder(this)
            alert.setTitle(title)
            alert.setCancelable(false)
            alert.setIcon(R.drawable.ic_download_app)
            alert.setMessage(body)
            alert.setPositiveButton(
                "Yes"
            ) { _, _ -> Rate.openPlayStore(packageToDownload, this) }
            alert.setNegativeButton(
                "Cancel"
            ) { dialog, _ ->
                dialog.cancel()
                //Cancel notification.
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                    NOTIFICATION_ID_NEW_FEATURES
                )
            }
            alert.show()
        } catch (e: Exception) {
            Logger.error(
                BaseActivity::class.java.simpleName,
                getString(R.string.error_showing_download_app_dialog),
                e
            )
        }
    }


    /**
     * It'll check the threshold to show banner for now 3 times per day only.
     */
    protected fun shouldShowBanner(): Boolean {
        if (BuildConfig.DEBUG) {
            return true
        } else {
            val lastShownDate = appPreferences.getShowBannerDate()
            val currentDate = DateHelper.today.getFormattedDate(this)
            val showCount = appPreferences.getShowBannerCount()
            val installDate = appPreferences.getAppInstallationDate()

            // Check if it's the first day
            if (installDate == currentDate) {
                return false
            }
            return if (lastShownDate == currentDate) {
                showCount < APP_BANNER_DISPLAY_THRESHOLD
            } else {
                true
            }
        }
    }


    /**
     * Whenever the banner displayed we need to update the display count
     */
    protected fun updateBannerCount() {
        val currentDate = DateHelper.today.getFormattedDate(this)
        val lastShownDate = appPreferences.getShowBannerDate()
        val currentCount = appPreferences.getShowBannerCount()

        if (lastShownDate == currentDate) {
            appPreferences.saveShowBannerCount((currentCount + 1))
        } else {
            appPreferences.saveShowBannerDate(currentDate)
            appPreferences.saveShowBannerCount(1)

        }
    }

    /**
     *
     */
    override fun onDestroy() {
        localBroadcastManager.unregisterReceiver(listener)
        super.onDestroy()
    }
}