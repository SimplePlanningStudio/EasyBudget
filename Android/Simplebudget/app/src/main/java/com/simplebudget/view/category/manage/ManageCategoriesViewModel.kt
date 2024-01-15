/*
 *   Copyright 2024 Waheed Nazir
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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.iab.Iab
import com.simplebudget.model.category.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ExpenseEditViewModel to handle categories.
 */
class ManageCategoriesViewModel(
    private val db: DB, private val iab: Iab
) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    lateinit var categoriesFlow: Flow<List<CategoryEntity>>
    val premiumStatusLiveData = MutableLiveData<Boolean>()
    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    fun isUserPremium(): Boolean = premiumStatusLiveData.value ?: false

    /**
     *
     */
    private fun loadCategories() {
        categoriesFlow = db.getCategories()
    }

    /**
     * Save category it might be new and not exist already.
     */
    fun saveCategory(category: Category) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                db.persistCategory(
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


    init {
        loadCategories()
        premiumStatusLiveData.value = iab.isUserPremium()
    }
}