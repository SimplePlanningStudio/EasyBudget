/*
 *   Copyright 2021 Waheed Nazir
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
package com.simplebudget.view.premium

import android.content.Intent
import android.os.Bundle
import com.simplebudget.R
import com.simplebudget.helper.BaseActivity
import com.simplebudget.view.splash.SplashActivity
import kotlinx.android.synthetic.main.activity_success_premium.*

/**
 *
 */
class PremiumSuccessActivity : BaseActivity() {

    var isBackEnabled: Boolean = false

    /**
     *
     */
    companion object {
        const val REQUEST_CODE_IS_BACK_ENABLED = "BackEnabled"
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success_premium)

        isBackEnabled = intent.getBooleanExtra(REQUEST_CODE_IS_BACK_ENABLED, false)
        if (isBackEnabled) {
            btnLetsProceed.text = String.format("%s", getString(R.string.back))
        } else {
            btnLetsProceed.text =
                String.format("%s", getString(R.string.proceed_with_adding_expenses))
        }

        /**
         *
         */
        btnLetsProceed.setOnClickListener {
            if (isBackEnabled) {
                finish()
            } else {
                finishAffinity()
                startActivity(Intent(this, SplashActivity::class.java))
            }
        }
    }

    /**
     *
     */
    override fun onBackPressed() {
        if (isBackEnabled) {
            super.onBackPressed()
        } else {
            finishAffinity()
            startActivity(Intent(this, SplashActivity::class.java))
        }
    }
}
