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
package com.simplebudget.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.db.impl.toCategoriesNamesList
import com.simplebudget.helper.SingleLiveEvent
import com.simplebudget.model.Category
import com.simplebudget.model.ExpenseCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

/**
 * ExpenseEditViewModel to handle categories.
 */
class CategoriesViewModel(private val db: DB) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var categories: ArrayList<String> = ArrayList()
    val categoriesLiveData = SingleLiveEvent<ArrayList<String>>()

    /**
     *
     */
    private fun loadCategories(currentCategories: ArrayList<String>) {
        viewModelScope.launch {
            categories.clear()
            withContext(Dispatchers.Default) {
                db.getCategories().toCategoriesNamesList().let {
                    categories.addAll(it)
                }
                //If changes in categories added / deleted during selection just reverse so that user can see newly added records
                if (currentCategories.isNotEmpty() && categories.size > currentCategories.size) categories.reverse()
                categoriesLiveData.postValue(categories)
                //Save categories into DB
                if (categories.isEmpty()) {
                    refreshCategories()
                }
            }
        }
    }

    /**
     * Reload categories if you have added or remove some categories call this
     */
    fun reloadCategories(currentCategories: ArrayList<String>) {
        loadCategories(currentCategories)
    }

    /**
     *
     */
    fun saveCategory(categoryType: String?) {
        doSaveCategory(categoryType)
    }

    /**
     * Save category it might be new and not exist already.
     */
    private fun doSaveCategory(categoryType: String?) {
        categoryType?.let {
            if (it.isEmpty() || it.isBlank()) return
            if (!categories.contains((categoryType.uppercase()))) {
                viewModelScope.launch {
                    withContext(Dispatchers.Default) {
                        db.persistCategories(
                            Category(categoryType.uppercase())
                        )
                    }
                }
            }
        }
    }

    /**
     * Delete category from DB
     */
    fun deleteCategory(categoryType: String?) {
        categoryType?.let {
            if (it.isEmpty() || it.isBlank()) return
            viewModelScope.launch {
                db.deleteCategory(categoryType.uppercase())
            }
        }
    }

    /**
     * Add categories and keep user's categories as well.
     */
    private fun refreshCategories() {
        viewModelScope.launch {
            ExpenseCategories.getCategoriesList().forEach { name ->
                db.persistCategories(Category(name))
            }
        }
    }

    override fun onCleared() {
        db.close()
        super.onCleared()
    }


    init {
        loadCategories(ArrayList())
    }
}