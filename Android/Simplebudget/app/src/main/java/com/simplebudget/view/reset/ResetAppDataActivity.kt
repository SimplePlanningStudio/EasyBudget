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
package com.simplebudget.view.reset

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityResetAppDataBinding
import com.simplebudget.view.main.MainActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Delete all app data and restart the app
 *
 * @author Waheed Nazir
 */
class ResetAppDataActivity : BaseActivity<ActivityResetAppDataBinding>() {

    private val viewModel: ResetAppDataViewModel by viewModel()


    override fun createBinding(): ActivityResetAppDataBinding =
        ActivityResetAppDataBinding.inflate(layoutInflater)

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.btnProceedWithReset.setOnClickListener {
            resetConfirmation()
        }

        viewModel.clearDataEventStream.observe(this) {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }


    /**
     * Reset confirmation
     */
    private fun resetConfirmation() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder
            .setTitle("Final Reset Confirmation")
            .setMessage("Are you sure you want to reset?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.setPositiveButton("Reset") { dialog, _ ->
                viewModel.clearAppData()
                dialog.cancel()
            }
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(resources.getColor(R.color.budget_red))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(resources.getColor(R.color.budget_green))
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
