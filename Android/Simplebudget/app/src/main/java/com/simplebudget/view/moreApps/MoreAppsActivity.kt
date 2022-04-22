/*
 *   Copyright 2022 Waheed Nazir
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
package com.simplebudget.view.moreApps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.simplebudget.R
import com.simplebudget.databinding.ActivityMoreAppsBinding
import com.simplebudget.helper.BaseActivity

class MoreAppsActivity : BaseActivity<ActivityMoreAppsBinding>(),
    MoreAppsFragment.OnListFragmentInteractionListener {


    override fun createBinding(): ActivityMoreAppsBinding {
        return ActivityMoreAppsBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.fragment_container, MoreAppsFragment.newInstance(1))
        fragmentTransaction.addToBackStack("MoreAppsFragment")
        fragmentTransaction.commit()
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

    /**
     *
     */
    override fun onListFragmentInteraction(item: AppModel.AppItem) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(MoreApps.PLAY_STORE_BASE_LINK + item.storeLink)
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(MoreApps.PLAY_STORE_BASE_LINK + item.storeLink)
                )
            )
        }
    }

    override fun onBackPressed() {
        finish()
    }
}