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
package com.simplebudget.view.category.choose

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.databinding.ActivitySearchCategoryBinding
import com.simplebudget.helper.AdSizeUtils
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.helper.extensions.toCategories
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.model.category.Category
import com.simplebudget.model.category.ExpenseCategories
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.prefs.*
import com.simplebudget.view.category.manage.ManageCategoriesActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.collections.ArrayList


class ChooseCategoryActivity : BaseActivity<ActivitySearchCategoryBinding>() {

    private lateinit var miscellaneousCategory: Category
    private var selectedCategory: Category? = null
    private var currentCategoryName = ""
    private val viewModelCategory: ChooseCategoryViewModel by viewModel()
    private var categories: ArrayList<Category> = ArrayList()
    private val appPreferences: AppPreferences by inject()
    private var adView: AdView? = null
    private lateinit var receiver: BroadcastReceiver

    companion object {
        const val REQUEST_CODE_CURRENT_EDIT_CATEGORY = "CURRENT_EDIT_CATEGORY"
        const val REQUEST_CODE_SELECTED_CATEGORY = "SELECTED_CATEGORY"
    }

    private lateinit var searchAdapter: ChooseCategoryAdapter

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

        currentCategoryName = intent?.getStringExtra(REQUEST_CODE_CURRENT_EDIT_CATEGORY) ?: ""

        // Register receiver
        val filter = IntentFilter()
        filter.addAction(INTENT_IAB_STATUS_CHANGED)
        filter.addAction(Intent.ACTION_VIEW)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    INTENT_IAB_STATUS_CHANGED -> viewModelCategory.onIabStatusChanged()
                }
            }
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, filter)

        viewModelCategory.premiumStatusLiveData.observe(this) { isPremium ->
            if (isPremium) {
                val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
                adContainerView.visibility = View.INVISIBLE
            } else {
                loadAndDisplayBannerAds()
            }
        }


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
            categories.add(0, Category(id = null, name = newCategory))
            binding.searchEditText.setText("")
            binding.linearLayoutEmptyState.visibility = View.GONE
            binding.recyclerViewCategories.visibility = View.VISIBLE
            toggleImageView("")
            searchAdapter.notifyDataSetChanged()
            viewModelCategory.saveCategory(Category(id = null, name = newCategory))
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
                showCaseView(targetView = it,
                    title = getString(R.string.edit_categories),
                    message = getString(R.string.edit_categories_show_view_message),
                    handleGuideListener = {
                        appPreferences.setUserCompleteManageCategoriesFromSelectCategoryShowCaseView()
                    })
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
            viewModelCategory.refreshCategories()
        }

    /**
     *
     */
    private fun launchManageCategories() {
        val startIntent = Intent(this, ManageCategoriesActivity::class.java)
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
            R.id.action_edit_categories -> {
                launchManageCategories()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Start activity for result
     */
    private var voiceSearchIntentLauncher =
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
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            }
            voiceSearchIntentLauncher.launch(intent)
        }
    }

    /**
     *
     */
    private fun filterWithQuery(query: String) {
        if (query.trim().isNotEmpty()) {
            val filteredList: List<Category> = onFilterChanged(query)
            attachAdapter(filteredList)
            toggleRecyclerView(filteredList)
        } else if (query.trim().isEmpty()) {
            attachAdapter(categories)
        }
    }

    /**
     *
     */
    private fun onFilterChanged(filterQuery: String): List<Category> {
        val filteredList = ArrayList<Category>()
        for (category in categories) {
            if (category.name.uppercase().contains(filterQuery.uppercase())) {
                filteredList.add(category)
            }
        }
        return filteredList
    }

    /**
     *
     */
    private fun attachAdapter(list: List<Category>) {
        searchAdapter = ChooseCategoryAdapter(list) { selectedCategory ->
            this.selectedCategory = selectedCategory
            doneWithSelection()
        }
        binding.recyclerViewCategories.adapter = searchAdapter
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewCategories.context, LinearLayout.VERTICAL
        )
        binding.recyclerViewCategories.addItemDecoration(dividerItemDecoration)
    }

    /**
     * Load categories from DB
     */
    private fun loadCategories() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModelCategory.categoriesFlow.collect { categoriesEntities ->
                categories.clear()
                if (categoriesEntities.isNotEmpty()) {
                    categories.addAll(categoriesEntities.toCategories().asReversed())
                    attachAdapter(categories)
                    binding.progressBar.visibility = View.INVISIBLE
                }
            }
        }
        viewModelCategory.miscellaneousCategory.observe(this) { category ->
            miscellaneousCategory = category
        }
    }

    /**
     *
     */
    private fun doneWithSelection() {
        setResult(
            Activity.RESULT_OK, Intent().putExtra(
                REQUEST_CODE_SELECTED_CATEGORY, selectedCategory ?: miscellaneousCategory
            )
        )
        finish()
    }

    /**
     *
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        doneWithSelection()
    }

    /**
     *
     */
    private fun toggleRecyclerView(categoriesList: List<Category>) {
        if (categoriesList.isEmpty()) {
            binding.recyclerViewCategories.visibility = View.INVISIBLE
            binding.linearLayoutEmptyState.visibility = View.VISIBLE
            binding.tvNotFound.text = String.format(
                getString(R.string.no_category_found), binding.searchEditText.text.toString()
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
    private fun loadAndDisplayBannerAds() {
        try {
            val adContainerView = findViewById<FrameLayout>(R.id.ad_view_container)
            adContainerView.visibility = View.VISIBLE
            val adSize: AdSize = AdSizeUtils.getAdSize(this, windowManager.defaultDisplay)
            adView = AdView(this)
            adView?.adUnitId = getString(R.string.banner_ad_unit_id)
            adContainerView.addView(adView)
            val actualAdRequest = AdRequest.Builder().build()
            adView?.setAdSize(adSize)
            adView?.loadAd(actualAdRequest)
            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdOpened() {}
                override fun onAdClosed() {
                    loadAndDisplayBannerAds()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    /**
     *
     */
    override fun onDestroy() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
        super.onDestroy()
    }
}