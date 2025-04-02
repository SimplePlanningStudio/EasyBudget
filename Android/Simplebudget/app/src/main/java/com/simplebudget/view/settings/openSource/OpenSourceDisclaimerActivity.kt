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
package com.simplebudget.view.settings.openSource

import android.os.Bundle
import android.view.MenuItem
import com.simplebudget.databinding.ActivityOpenSourceDisclaimerBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import org.koin.android.ext.android.inject

class OpenSourceDisclaimerActivity : BaseActivity<ActivityOpenSourceDisclaimerBinding>() {


    private val analyticsManager: AnalyticsManager by inject()

    /**
     *
     */
    override fun createBinding(): ActivityOpenSourceDisclaimerBinding =
        ActivityOpenSourceDisclaimerBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Screen name event
        analyticsManager.logEvent(Events.KEY_OPEN_SOURCE_SCREEN)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
