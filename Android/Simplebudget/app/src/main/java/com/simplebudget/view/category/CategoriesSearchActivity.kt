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
package com.simplebudget.view.category

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import com.simplebudget.R
import com.simplebudget.databinding.ActivitySearchCategoryBinding
import com.simplebudget.helper.BaseActivity
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.model.ExpenseCategories
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.prefs.*
import com.simplebudget.view.CategoriesViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.collections.ArrayList


class CategoriesSearchActivity : BaseActivity<ActivitySearchCategoryBinding>(),
    CategorySearchAdapter.CategoryAdapterListener {

    private var selectedCategory = ""
    private var currentCategory = ""
    private val viewModelCategory: CategoriesViewModel by viewModel()
    private var categories: ArrayList<String> = ArrayList()
    private val appPreferences: AppPreferences by inject()

    companion object {
        const val REQUEST_CODE_CURRENT_EDIT_CATEGORY = "CURRENT_EDIT_CATEGORY"
        const val REQUEST_CODE_SELECTED_CATEGORY = "SELECTED_CATEGORY"
    }

    private lateinit var searchAdapter: CategorySearchAdapter

    /**
     *
     */
    override fun createBinding(): ActivitySearchCategoryBinding {
        return ActivitySearchCategoryBinding.inflate(layoutInflater)
    }

    /**
     *
     */
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        currentCategory =
            intent?.getStringExtra(REQUEST_CODE_CURRENT_EDIT_CATEGORY) ?: ""

        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().uppercase()
            filterWithQuery(query)
            toggleImageView(query)
        }

        //Handle empty state actions
        binding.btnSkip.setOnClickListener {
            binding.searchEditText.setText("")
            binding.linearLayoutEmptyState.visibility = View.GONE
            binding.recyclerViewCategories.visibility = View.VISIBLE
            attachAdapter(categories)
            searchAdapter.notifyDataSetChanged()
            toggleImageView("")
        }
        binding.btnAdd.setOnClickListener {
            val newCategory = binding.searchEditText.text.toString().uppercase()
            categories.add(0, newCategory)
            binding.searchEditText.setText("")
            binding.linearLayoutEmptyState.visibility = View.GONE
            binding.recyclerViewCategories.visibility = View.VISIBLE
            toggleImageView("")
            searchAdapter.notifyDataSetChanged()
            viewModelCategory.saveCategory(newCategory)
        }

        handleVoiceSearch()
        loadCategories()
    }

    // ------------------------------------------>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_select_categories, menu)
        return true
    }

    /**
     * Creating a custom menu option.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_edit_categories)
        val rootView = menuItem?.actionView as LinearLayout?
        val customFutureExpenseMenu = rootView?.findViewById<TextView>(R.id.tvEditCategories)
        customFutureExpenseMenu?.let {
            if (appPreferences.hasUserCompleteManageCategoriesFromSelectCategoryShowCaseView()
                    .not()
            ) {
                showCaseView(
                    targetView = it,
                    title = getString(R.string.edit_categories),
                    message = getString(R.string.edit_categories_show_view_message),
                    handleGuideListener = {
                        appPreferences.setUserCompleteManageCategoriesFromSelectCategoryShowCaseView()
                    }
                )
            }
            it.setOnClickListener {
                launchManageCategories()
            }
        }
        return true
    }


    /**
     * Start activity for result
     */
    private var manageCategoriesActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //Re-load categories from DB
            viewModelCategory.reloadCategories(categories)
        }

    /**
     *
     */
    private fun launchManageCategories() {
        val startIntent = Intent(this, CategoriesActivity::class.java)
        startIntent.putExtra(
            CategoriesActivity.REQUEST_CODE_SELECT_CATEGORY,
            CategoriesActivity.MANAGE_CATEGORIES
        )
        manageCategoriesActivityLauncher.launch(startIntent)
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_future_expenses -> {
                launchManageCategories()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Start activity for result
     */
    private var securityActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    .let { results ->
                        results?.get(0)
                    }
            binding.searchEditText.setText(spokenText ?: "")
        }

    /**
     * Handle Voice Search Activity
     */
    private fun handleVoiceSearch() {
        binding.voiceSearchQuery.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            }
            securityActivityLauncher.launch(intent)
        }
    }

    /**
     *
     */
    private fun filterWithQuery(query: String) {
        if (query.trim().isNotEmpty()) {
            val filteredList: List<String> = onFilterChanged(query)
            attachAdapter(filteredList)
            toggleRecyclerView(filteredList)
        } else if (query.trim().isEmpty()) {
            attachAdapter(categories)
        }
    }

    /**
     *
     */
    private fun onFilterChanged(filterQuery: String): List<String> {
        val filteredList = ArrayList<String>()
        for (category in categories) {
            if (category.uppercase().contains(filterQuery.uppercase())) {
                filteredList.add(category)
            }
        }
        return filteredList
    }

    /**
     *
     */
    private fun attachAdapter(list: List<String>) {
        searchAdapter = CategorySearchAdapter(list, this)
        binding.recyclerViewCategories.adapter = searchAdapter
    }

    /**
     * Load categories from DB
     */
    private fun loadCategories() {
        binding.progressBar.visibility = View.VISIBLE
        //Load categories
        viewModelCategory.categoriesLiveData.observe(this) { dbCat ->
            categories.clear()
            if (dbCat.isEmpty()) {
                ExpenseCategories.getCategoriesList().forEach { item ->
                    if (!categories.contains(item.uppercase()))
                        categories.add(item.uppercase())
                }
            } else {
                categories.addAll(dbCat)
            }

            attachAdapter(categories)
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     *
     */
    private fun doneWithSelection() {
        if (selectedCategory.trim().isEmpty()) {
            selectedCategory = if (selectedCategory.trim().isEmpty()) {
                if (currentCategory.trim().isEmpty())
                    ExpenseCategoryType.MISCELLANEOUS.name else currentCategory
            } else {
                ExpenseCategoryType.MISCELLANEOUS.name
            }
        }
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(REQUEST_CODE_SELECTED_CATEGORY, selectedCategory)
        )
        finish()
    }

    /**
     *
     */
    override fun onBackPressed() {
        doneWithSelection()
    }

    /**
     *
     */
    private fun toggleRecyclerView(categoriesList: List<String>) {
        if (categoriesList.isEmpty()) {
            binding.recyclerViewCategories.visibility = View.INVISIBLE
            binding.linearLayoutEmptyState.visibility = View.VISIBLE
            binding.tvNotFound.text = String.format(
                getString(R.string.no_category_found),
                binding.searchEditText.text.toString()
            )
        } else {
            binding.recyclerViewCategories.visibility = View.VISIBLE
            binding.linearLayoutEmptyState.visibility = View.GONE
        }
    }

    /**
     *
     */
    private fun toggleImageView(query: String) {
        if (query.isNotEmpty()) {
            binding.voiceSearchQuery.visibility = View.INVISIBLE
        } else if (query.isEmpty()) {
            binding.voiceSearchQuery.visibility = View.VISIBLE
        }
    }

    /**
     *
     */
    override fun onCategorySelected(selectedCategory: String) {
        this.selectedCategory = selectedCategory
        doneWithSelection()
    }
}