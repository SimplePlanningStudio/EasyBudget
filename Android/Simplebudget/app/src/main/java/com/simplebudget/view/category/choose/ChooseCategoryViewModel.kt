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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.helper.extensions.toCategory
import com.simplebudget.iab.Iab
import com.simplebudget.model.category.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ExpenseEditViewModel to handle categories.
 */
class ChooseCategoryViewModel(
    private val db: DB, private val iab: Iab
) : ViewModel() {

    val premiumStatusLiveData = MutableLiveData<Boolean>()

    /**
     *
     */
    private val _categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categoriesFlow: StateFlow<List<CategoryEntity>> = _categoriesFlow

    /**
     *
     */
    private val miscellaneousCategoryOfBudgetLiveData = MutableLiveData<Category>()
    val miscellaneousCategory: LiveData<Category> = miscellaneousCategoryOfBudgetLiveData

    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    /**
     *
     */
    private fun loadCategories() {
        viewModelScope.launch {
            db.getCategories().collect { categoryEntityList ->
                _categoriesFlow.value = categoryEntityList
            }
        }
    }

    /**
     *
     */
    fun refreshCategories() {
        viewModelScope.launch {
            db.getCategories().collect { categoryEntityList ->
                _categoriesFlow.value = categoryEntityList
            }
        }
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
     * Get miscellaneous category
     */
    private fun getMiscellaneousCategory() {
        viewModelScope.launch {
            val miscellaneousCategory = db.getMiscellaneousCategory()
            miscellaneousCategoryOfBudgetLiveData.postValue(miscellaneousCategory.toCategory())
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

    override fun onCleared() {
        db.close()
        super.onCleared()
    }


    init {
        loadCategories()
        getMiscellaneousCategory()
        premiumStatusLiveData.value = iab.isUserPremium()
    }
}