/*
 *   Copyright 2022 Benoit LETONDOR
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
package com.simplebudget.view.expenseedit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.iab.Iab
import com.simplebudget.db.DB
import com.simplebudget.db.impl.toCategoriesNamesList
import com.simplebudget.helper.SingleLiveEvent
import com.simplebudget.model.Category
import com.simplebudget.model.Expense
import com.simplebudget.model.ExpenseCategories
import com.simplebudget.model.ExpenseCategoryType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class ExpenseEditViewModel(
    private val db: DB,
    private val appPreferences: AppPreferences,
    private val iab: Iab
) : ViewModel() {
    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    private var expense: Expense? = null

    val premiumStatusLiveData = MutableLiveData<Boolean>()
    val expenseDateLiveData = MutableLiveData<Date>()
    val editTypeLiveData = MutableLiveData<ExpenseEditType>()
    val existingExpenseEventStream = SingleLiveEvent<ExistingExpenseData?>()
    val expenseAddBeforeInitDateEventStream = SingleLiveEvent<Unit>()
    val finishEventStream = MutableLiveData<Unit?>()

    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    fun initWithDateAndExpense(date: Date, expense: Expense?) {
        this.expense = expense
        this.expenseDateLiveData.value = expense?.date ?: date
        this.editTypeLiveData.value =
            ExpenseEditType(expense?.isRevenue() ?: false, expense != null)

        existingExpenseEventStream.value =
            if (expense != null) ExistingExpenseData(
                expense.title,
                expense.amount,
                expense.category
            ) else null
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeLiveData.value = ExpenseEditType(isRevenue, expense != null)
    }

    fun onSave(value: Double, description: String, expenseCategoryType: String) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return

        if (date.before(Date(appPreferences.getInitTimestamp()))) {
            expenseAddBeforeInitDateEventStream.value = Unit
            return
        }
        doSaveExpense(value, description, isRevenue, date, expenseCategoryType)
    }

    fun onAddExpenseBeforeInitDateConfirmed(
        value: Double,
        description: String,
        expenseCategoryType: String
    ) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return

        doSaveExpense(value, description, isRevenue, date, expenseCategoryType)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(
        value: Double,
        description: String,
        isRevenue: Boolean,
        date: Date,
        expenseCategoryType: String
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val expense = expense?.copy(
                    title = description,
                    amount = if (isRevenue) -value else value,
                    date = date,
                    category = expenseCategoryType
                ) ?: Expense(
                    description,
                    if (isRevenue) -value else value,
                    date,
                    expenseCategoryType
                )
                db.persistExpense(expense)
            }
            finishEventStream.value = null
        }
    }

    fun onDateChanged(date: Date) {
        this.expenseDateLiveData.value = date
    }

    override fun onCleared() {
        db.close()

        super.onCleared()
    }


    init {
        premiumStatusLiveData.value = iab.isUserPremium()
    }
}


data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(val title: String, val amount: Double, val categoryType: String)