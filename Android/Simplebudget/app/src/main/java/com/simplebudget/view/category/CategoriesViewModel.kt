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
package com.simplebudget.view.category

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.helper.SingleLiveEvent
import com.simplebudget.iab.Iab
import com.simplebudget.model.category.Category
import com.simplebudget.model.category.ExpenseCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

/**
 * ExpenseEditViewModel to handle categories.
 */
class CategoriesViewModel(
    private val db: DB, private val iab: Iab
) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var categories: ArrayList<Category> = ArrayList()
    val categoriesLiveData = SingleLiveEvent<ArrayList<Category>>()

    val premiumStatusLiveData = MutableLiveData<Boolean>()
    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }


    /**
     *
     */
    private fun loadCategories(currentCategories: ArrayList<Category>) {
        viewModelScope.launch {
            categories.clear()
            withContext(Dispatchers.Default) {
                val dbCategories = db.getCategories()
                categories.addAll(dbCategories)
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
    fun reloadCategories(currentCategories: ArrayList<Category>) {
        loadCategories(currentCategories)
    }

    /**
     * Save category it might be new and not exist already.
     */
    fun saveCategory(category: Category) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                db.persistCategories(
                    category
                )
            }
        }
    }

    /**
     * Delete category from DB
     */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (category.id != null) {
                db.deleteCategory(category)
            } else {
                db.deleteCategory(category.name)
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
        premiumStatusLiveData.value = iab.isUserPremium()
    }
}