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
package com.simplebudget.view.search.base

import android.os.Bundle
import com.simplebudget.R
import com.simplebudget.databinding.ActivitySearchExpensesBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.view.search.SearchFragment


class SearchBaseActivity : BaseActivity<ActivitySearchExpensesBinding>() {

    override fun createBinding(): ActivitySearchExpensesBinding {
        return ActivitySearchExpensesBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Add search fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayoutSearchExpenses, SearchFragment.newInstance())
        transaction.commit()
    }
}
