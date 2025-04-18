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
package com.simplebudget.view.category.choose

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.AdView
import com.simplebudget.R
import com.simplebudget.databinding.ActivitySearchCategoryBinding
import com.simplebudget.base.BaseActivity
import com.simplebudget.helper.AppInstallHelper
import com.simplebudget.helper.Logger
import com.simplebudget.helper.SortOption
import com.simplebudget.helper.ads.destroyBanner
import com.simplebudget.helper.ads.loadBanner
import com.simplebudget.helper.ads.pauseBanner
import com.simplebudget.helper.ads.resumeBanner
import com.simplebudget.helper.analytics.AnalyticsManager
import com.simplebudget.helper.analytics.Events
import com.simplebudget.helper.extensions.beGone
import com.simplebudget.helper.extensions.showCaseView
import com.simplebudget.helper.extensions.toCategories
import com.simplebudget.helper.toast.ToastManager
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.iab.isUserPremium
import com.simplebudget.model.category.Category
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
    private val viewModelCategory: ChooseCategoryViewModel by viewModel()
    private var categories: ArrayList<Category> = ArrayList()
    private val selectedCategories = mutableSetOf<Category>()
    private val appPreferences: AppPreferences by inject()
    private val analyticsManager: AnalyticsManager by inject()
    private var adView: AdView? = null
    private lateinit var receiver: BroadcastReceiver
    private var selectedOption: SortOption = SortOption.Alphabetically
    private var isMultiSelect: Boolean = false
    private val toastManager: ToastManager by inject()


    companion object {
        const val REQUEST_CODE_CURRENT_EDIT_CATEGORY = "CURRENT_EDIT_CATEGORY"
        const val REQUEST_CODE_SELECTED_CATEGORY = "SELECTED_CATEGORY"
        const val REQUEST_CODE_MULTI_SELECT = "IS_MULTI_SELECT"
    }

    private lateinit var chooseCategoryAdapter: ChooseCategoryAdapter

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

        // Screen name event
        analyticsManager.logEvent(Events.KEY_CHOOSE_CATEGORY_SCREEN)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        selectedOption = appPreferences.getSortingType()

        isMultiSelect = intent.getBooleanExtra(REQUEST_CODE_MULTI_SELECT, false)

        binding.doneWithSelection.visibility = if (isMultiSelect) View.VISIBLE else View.GONE
        binding.searchCardView.visibility = if (isMultiSelect) View.GONE else View.VISIBLE
        title =
            if (isMultiSelect) getString(R.string.select_categories) else getString(R.string.select_category)

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
                binding.adViewContainer.beGone()
            } else {
                /**
                 * Banner ads
                 */
                loadBanner(
                    appPreferences.isUserPremium(),
                    binding.adViewContainer,
                    onBannerAdRequested = { bannerAdView ->
                        this.adView = bannerAdView
                    })
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
            chooseCategoryAdapter.notifyDataSetChanged()
            toggleImageView("")
        }
        binding.btnAdd.setOnClickListener {
            try {
                val newCategory = binding.searchEditText.text?.toString()
                    ?.takeIf { it.isNotBlank() && it.lowercase() != "null" }?.uppercase()
                    ?: ExpenseCategoryType.MISCELLANEOUS.name
                categories.add(0, Category(id = null, name = newCategory))
                binding.searchEditText.setText("")
                binding.linearLayoutEmptyState.visibility = View.GONE
                binding.recyclerViewCategories.visibility = View.VISIBLE
                toggleImageView("")
                chooseCategoryAdapter.notifyDataSetChanged()
                viewModelCategory.saveCategory(Category(id = null, name = newCategory))
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_CATEGORY_ADDED, mapOf(
                        Events.KEY_VALUE to ChooseCategoryActivity::class.java.simpleName
                    )
                )
            } catch (_: Exception) {
            }
        }

        handleVoiceSearch()
        loadCategories()

        // In case of multi selection we need to get trigger once done with selection
        binding.doneWithSelection.setOnClickListener {
            if (chooseCategoryAdapter.getSelectedCategories().isNotEmpty()) {
                selectedCategories.clear()
                selectedCategories.addAll(chooseCategoryAdapter.getSelectedCategories())
                doneWithSelection()
            } else {
                toastManager.showShort(getString(R.string.please_select_categories))
            }
        }

        //Show banner
        showAppBanner()

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
    }

    /**
     * Handle back pressed
     */
    private fun handleBackPressed() {
        doneWithSelection()
    }


    /**
     * Show app promotion banner
     */
    private fun showAppBanner() {
        // Show app banner promotion
        if (appPreferences.isUserPremium().not() && shouldShowBanner()) {
            val banner = appPreferences.getBanner()
            binding.bannerLayout.bannerCard.visibility = banner?.let { View.VISIBLE } ?: View.GONE
            banner?.let {
                if (AppInstallHelper.isInstalled(banner.packageName ?: "", this).not()) {
                    binding.bannerLayout.bannerTitle.text = banner.title
                    binding.bannerLayout.bannerDescription.text = banner.description
                    Glide.with(this).load(banner.imageUrl).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.bannerLayout.bannerImage)
                    binding.bannerLayout.bannerCard.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.redirectUrl))
                        startActivity(intent)
                    }
                    updateBannerCount()
                }
            }
        } else {
            binding.bannerLayout.bannerCard.visibility = View.GONE
        }
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
                handleBackPressed()
                true
            }

            R.id.action_edit_categories -> {
                launchManageCategories()
                true
            }

            R.id.action_sort_categories -> {
                // Sorting categories
                showPopupMenu()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.anchor)
        popupMenu.menuInflater.inflate(R.menu.categories_sort_options_popup_menu, popupMenu.menu)

        // Set listener for menu item clicks
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_alphabetically -> {
                    selectedOption = SortOption.Alphabetically
                    appPreferences.setSortingType(SortOption.Alphabetically.name)
                    attachAdapter(categories)
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_CATEGORY_SORTED, mapOf(
                            Events.KEY_VALUE to SortOption.Alphabetically.name,
                            Events.KEY_VALUE to ChooseCategoryActivity::class.java.simpleName
                        )
                    )
                    true
                }

                R.id.menu_by_latest -> {
                    selectedOption = SortOption.ByLatest
                    appPreferences.setSortingType(SortOption.ByLatest.name)
                    attachAdapter(categories)
                    //Log event
                    analyticsManager.logEvent(
                        Events.KEY_CATEGORY_SORTED, mapOf(
                            Events.KEY_VALUE to SortOption.ByLatest.name,
                            Events.KEY_VALUE to ChooseCategoryActivity::class.java.simpleName
                        )
                    )
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
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
        try {
            binding.voiceSearchQuery.setOnClickListener {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                }
                voiceSearchIntentLauncher.launch(intent)
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_CATEGORY_VOICE_SEARCHED,
                    mapOf(Events.KEY_VALUE to ChooseCategoryActivity::class.java.simpleName)
                )
            }
        } catch (e: Exception) {
            Logger.error(
                ChooseCategoryActivity::class.java.simpleName,
                "Error in searching Categories using voice ${e.localizedMessage}",
                e
            )
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
        val sortedCategories = list.sortedBy { it.name } // Sort Alphabetically
        chooseCategoryAdapter = ChooseCategoryAdapter(
            if (selectedOption == SortOption.Alphabetically) sortedCategories else list,
            onCategorySelected = { selectedCategory ->
                this.selectedCategory = selectedCategory
                doneWithSelection()
            },
            onCategoryChosen = { chosenCategory ->
                if (selectedCategories.contains(chosenCategory)) {
                    selectedCategories.remove(chosenCategory)
                } else {
                    selectedCategories.add(chosenCategory)
                }
            },
            isMultiSelect
        )
        binding.recyclerViewCategories.adapter = chooseCategoryAdapter
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
     * Handle category / categories selection and move the screen back!
     */
    private fun doneWithSelection() {
        val data = Intent().apply {
            if (isMultiSelect) {
                putParcelableArrayListExtra(
                    REQUEST_CODE_SELECTED_CATEGORY, ArrayList(selectedCategories.toList())
                )
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_CATEGORY_SELECTED, mapOf(
                        Events.KEY_VALUE to "multi_selection",
                    )
                )
            } else {
                putExtra(REQUEST_CODE_SELECTED_CATEGORY, selectedCategory ?: miscellaneousCategory)
                //Log event
                analyticsManager.logEvent(
                    Events.KEY_CATEGORY_SELECTED, mapOf(
                        Events.KEY_VALUE to "single_selection",
                    )
                )
            }
        }
        setResult(Activity.RESULT_OK, data)
        finish()
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

    override fun onResume() {
        resumeBanner(adView)
        super.onResume()
    }

    /**
     * Called when leaving the activity
     */
    override fun onPause() {
        pauseBanner(adView)
        super.onPause()
    }

    /**
     *
     */
    override fun onDestroy() {
        destroyBanner(adView)
        adView = null
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiver)
        super.onDestroy()
    }
}