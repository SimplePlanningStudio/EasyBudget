package com.simplebudget.view.settings.openSource

import android.os.Bundle
import android.view.MenuItem
import com.simplebudget.R
import com.simplebudget.databinding.ActivityOpenSourceDisclaimerBinding
import com.simplebudget.helper.BaseActivity

class OpenSourceDisclaimerActivity : BaseActivity<ActivityOpenSourceDisclaimerBinding>() {

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
