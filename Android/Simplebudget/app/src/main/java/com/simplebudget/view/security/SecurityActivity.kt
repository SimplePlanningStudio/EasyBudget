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
package com.simplebudget.view.security

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simplebudget.R
import com.simplebudget.databinding.DialogSecurityBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.interfaces.HashListener


class SecurityActivity : BaseActivity<DialogSecurityBinding>(), HashListener {

    private lateinit var requiredHash: String
    private var showTabIndex: Int = 0

    companion object {
        const val REQUEST_CODE_SECURITY_TYPE = "VERIFICATION_TYPE"
        const val VERIFICATION = "VERIFICATION"
        const val SET_PIN = "SET_PIN"
        private var SECURITY_TYPE = ""
    }

    /**
     *
     */
    override fun createBinding(): DialogSecurityBinding {
        return DialogSecurityBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        setOrientation()
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        this.window
            .setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        requiredHash = intent.getStringExtra("HASH") ?: ""
        showTabIndex = intent.getIntExtra("TAB_INDEX", 0)
        SECURITY_TYPE = intent.getStringExtra(REQUEST_CODE_SECURITY_TYPE) ?: ""

        binding.pinLockHolder.initTab(requiredHash, this)

        binding.ivClose.setOnClickListener { onCancelFail() }
    }

    /**
     *
     */
    private fun setOrientation() {
        requestedOrientation = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /**
     *
     */
    private fun onCancelFail() {
        when (SECURITY_TYPE) {
            VERIFICATION -> {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            SET_PIN -> {
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().putExtra("HASH", binding.pinLockHolder.hash)
                )
                finish()
            }
        }
    }

    /**
     *
     */
    override fun receivedHash(hash: String, type: Int) {
        when (SECURITY_TYPE) {
            VERIFICATION -> {
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }
            SET_PIN -> {
                setResult(Activity.RESULT_OK, Intent().putExtra("HASH", binding.pinLockHolder.hash))
                finish()
            }
        }
    }

    /**
     *
     */
    override fun error(errorMsg: String) {
        binding.error.text = errorMsg
        binding.error.visibility = View.VISIBLE
        shakeAnimation(binding.error)
        object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                binding.error.visibility = View.GONE
            }
        }.start()
    }

    /**
     *
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        onCancelFail()
    }

    /**
     *
     */
    private fun shakeAnimation(view: TextView) {
        view.animation = AnimationUtils.loadAnimation(this, R.anim.shake_animation)
    }
}