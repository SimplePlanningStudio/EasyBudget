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
package com.simplebudget.base

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewbinding.ViewBinding
import com.simplebudget.R
import com.simplebudget.helper.Rate
import com.simplebudget.push.MyFirebaseMessagingService.Companion.ACTION_TRIGGER_DOWNLOAD
import com.simplebudget.push.MyFirebaseMessagingService.Companion.NOTIFICATION_ID_NEW_FEATURES

abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: V
    private lateinit var listener: MyBroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager

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
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
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