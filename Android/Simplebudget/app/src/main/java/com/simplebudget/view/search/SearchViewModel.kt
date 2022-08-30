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
package com.simplebudget.view.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.model.Expense
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * Constants Values
 */

class SearchViewModel(
    private val db: DB
) : ViewModel() {

    private val allExpensesLiveData = MutableLiveData<List<Expense>>()
    val expenses: LiveData<List<Expense>> = allExpensesLiveData

    private val loadingMutableLiveData = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = loadingMutableLiveData


    init {
        loadThisMonthExpenses()
    }

    /**
     * Search expenses
     */
    fun searchExpenses(search_query: String) {
        loadingMutableLiveData.value = true
        viewModelScope.launch {
            allExpensesLiveData.postValue(db.searchExpenses(search_query))
            loadingMutableLiveData.postValue(false)
        }
    }

    /**
     * Load Today's expenses
     */
    fun loadTodayExpenses() {
        loadExpensesForADate(Date())
    }

    /**
     * Load Yesterday's expenses
     */
    fun loadYesterdayExpenses() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = Date().time // Set today date
        cal.add(Calendar.DAY_OF_MONTH, -1) // Go to 1 day back to get Yesterday
        loadExpensesForADate(cal.time)
    }

    /**
     * Load expenses for this week
     */
    fun loadThisWeekExpenses() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, 1)
        val d1 = cal.time
        loadExpensesForGivenDates(d1, Date()) // Starting week to Today's date
    }

    /**
     * Load expenses for this month
     */
    fun loadThisMonthExpenses() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        loadingMutableLiveData.value = true
        val startDate = cal.time
        viewModelScope.launch {
            allExpensesLiveData.postValue(db.getExpensesForMonth(startDate))
            loadingMutableLiveData.postValue(false)
        }
    }

    /**
     * Load expenses for last week
     */
    fun loadLastWeekExpenses() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, 1)
        val d1 = cal.time
        cal.add(Calendar.WEEK_OF_MONTH, -1)
        loadExpensesForGivenDates(cal.time, d1)
    }

    /**
     * Load Tomorrow's expenses
     */
    fun loadTomorrowExpenses() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = Date().time // Set today date
        cal.add(Calendar.DAY_OF_MONTH, 1) // Go to 1 day ahead to get Tomorrow
        loadExpensesForADate(cal.time)
    }

    /**
     *
     */
    fun loadExpensesForADate(date: Date) {
        loadingMutableLiveData.value = true
        viewModelScope.launch {
            allExpensesLiveData.postValue(db.getExpensesForDay(date))
            loadingMutableLiveData.postValue(false)
        }
    }

    /**
     *
     */
    fun loadExpensesForGivenDates(startDate: Date, endDate: Date) {
        loadingMutableLiveData.value = true
        viewModelScope.launch {
            allExpensesLiveData.postValue(db.getAllExpenses(startDate, endDate))
            loadingMutableLiveData.postValue(false)
        }
    }

    /**
     *
     */
    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}