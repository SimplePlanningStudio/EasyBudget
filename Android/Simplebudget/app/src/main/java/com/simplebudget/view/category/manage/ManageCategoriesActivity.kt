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
package com.simplebudget.view.category.manage

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplebudget.R
import com.simplebudget.base.BaseActivity
import com.simplebudget.databinding.ActivityManageCategoriesBinding
import com.simplebudget.helper.*
import com.simplebudget.helper.extensions.toCategories
import com.simplebudget.iab.INTENT_IAB_STATUS_CHANGED
import com.simplebudget.model.category.Category
import com.simplebudget.model.category.ExpenseCategories
import com.simplebudget.prefs.*
import com.simplebudget.view.premium.PremiumActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class ManageCategoriesActivity : BaseActivity<ActivityManageCategoriesBinding>(),
    ManageCategoriesAdapter.ManageCategoriesListener {

    private val viewModelCategory: ManageCategoriesViewModel by viewModel()
    private var categories: ArrayList<Category> = ArrayList()
    private lateinit var manageCategoriesAdapter: ManageCategoriesAdapter
    private var adView: AdView? = null
    private lateinit var receiver: BroadcastReceiver
    private val appPreferences: AppPreferences by inject()

    /**
     *
     */
    override fun createBinding(): ActivityManageCategoriesBinding {
        return ActivityManageCategoriesBinding.inflate(layoutInflater)
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
            manageCategoriesAdapter.notifyDataSetChanged()
            toggleImageView("")
        }

        binding.btnAdd.setOnClickListener {
            val newCategory = binding.searchEditText.text.toString().uppercase()
            categories.add(0, Category(id = null, name = newCategory))
            binding.searchEditText.setText("")
            binding.linearLayoutEmptyState.visibility = View.GONE
            binding.recyclerViewCategories.visibility = View.VISIBLE
            toggleImageView("")
            manageCategoriesAdapter.notifyDataSetChanged()
            viewModelCategory.saveCategory(Category(id = null, name = newCategory))
        }
        handleVoiceSearch()
        loadCategories()
    }


    // ------------------------------------------>
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_manage_categories, menu)
        return true
    }

    /**
     * Creating a custom menu option.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_done_editing)
        val rootView = menuItem?.actionView as LinearLayout?
        val tvDoneEditing = rootView?.findViewById<TextView>(R.id.tvDoneEditing)
        tvDoneEditing?.let {
            it.setOnClickListener {
                doneWithManaging()
            }
        }
        return true
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                doneWithManaging()
                true
            }
            R.id.action_done_editing -> {
                doneWithManaging()
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
            val filteredList: ArrayList<Category> = onFilterChanged(query)
            attachAdapter(filteredList)
            toggleRecyclerView(filteredList)
        } else if (query.trim().isEmpty()) {
            attachAdapter(categories)
        }
    }

    /**
     *
     */
    private fun onFilterChanged(filterQuery: String): ArrayList<Category> {
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
    private fun attachAdapter(list: ArrayList<Category>) {
        manageCategoriesAdapter =
            ManageCategoriesAdapter(list, this, appPreferences.getUserCurrency().symbol)
        binding.recyclerViewCategories.adapter = manageCategoriesAdapter
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerViewCategories.context, LinearLayout.VERTICAL
        )
        binding.recyclerViewCategories.addItemDecoration(dividerItemDecoration)
    }

    /**
     * Load categories from DB
     */
    private fun loadCategories() {
        //Load categories
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
     * Show dialog with options Edit, Delete
     */
    private fun handleItemClick(selectedCategory: Category, position: Int) {
        if (selectedCategory.name == getString(R.string.miscellaneous)) {
            DialogUtil.createDialog(this,
                title = getString(R.string.oops),
                message = getString(R.string.cant_edit_miscellaneous),
                positiveBtn = getString(R.string.ok),
                negativeBtn = "",
                isCancelable = true,
                positiveClickListener = {},
                negativeClickListener = {}).show()
            return
        }
        val options = arrayOf(
            "Edit",
            "Delete",
        )
        MaterialAlertDialogBuilder(this).setTitle(
            String.format(
                "%s %s",
                "Manage",
                selectedCategory.name
            )
        ).setItems(options) { dialog, which ->
            when (options[which]) {
                "Edit" -> editCategory(selectedCategory, position)
                "Delete" -> removeConfirmation(selectedCategory, position)
                else -> {}

            }
            dialog.dismiss()
        }.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }.setCancelable(false).show()
    }

    /**
     * Edit Category
     */
    private fun editCategory(
        selectedCategory: Category, position: Int
    ) {
        EditCategoryDialog.open(this, selectedCategory, updateCategory = { newCategory ->
            categories.add(
                position, Category(
                    selectedCategory.id, name = newCategory
                )
            )
            viewModelCategory.saveCategory(
                Category(
                    selectedCategory.id, name = newCategory
                )
            )
            manageCategoriesAdapter.notifyItemChanged(position)
            toast(getString(R.string.category_updated_successfully))
        })
    }

    /**
     * Delete confirmation
     */
    private fun removeConfirmation(selectedCategory: Category, position: Int) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete ${selectedCategory.name}?")
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }.setPositiveButton("Yes") { dialog, _ ->
                manageCategoriesAdapter.delete(position)
                viewModelCategory.deleteCategory(selectedCategory)
                manageCategoriesAdapter.notifyItemChanged(position)
                toast("${getString(R.string.deleted)} category ${selectedCategory.name}")
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
    override fun onCategorySelected(selectedCategory: Category, position: Int) {
        handleItemClick(selectedCategory, position)
    }

    /**
     * Done with managing categories.
     */
    private fun doneWithManaging() {
        setResult(Activity.RESULT_OK)
        finish()
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