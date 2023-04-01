/*
 *   Copyright 2023 Waheed Nazir
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
import com.simplebudget.databinding.ActivitySuccessPremiumBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.view.splash.SplashActivity

/**
 *
 */
class PremiumSuccessActivity : BaseActivity<ActivitySuccessPremiumBinding>() {

    var isBackEnabled: Boolean = false

    /**
     *
     */
    companion object {
        const val REQUEST_CODE_IS_BACK_ENABLED = "BackEnabled"
    }

    override fun createBinding(): ActivitySuccessPremiumBinding {
        return ActivitySuccessPremiumBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isBackEnabled = intent.getBooleanExtra(REQUEST_CODE_IS_BACK_ENABLED, false)
        if (isBackEnabled) {
            binding.btnLetsProceed.text = String.format("%s", getString(R.string.back))
        } else {
            binding.btnLetsProceed.text =
                String.format("%s", getString(R.string.proceed_with_adding_expenses))
        }

        /**
         *
         */
        binding.btnLetsProceed.setOnClickListener {
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
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isBackEnabled) {
            finish()
        } else {
            finishAffinity()
            startActivity(Intent(this, SplashActivity::class.java))
        }
    }
}
