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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.model.Expense
import com.simplebudget.db.DB
import com.simplebudget.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * Constants Values
 */

class SearchViewModel(
    private val db: DB,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val monthlyReportDataLiveData = MutableLiveData<MonthlyReportData>()
    val expenses = mutableListOf<Expense>()
    val revenues = mutableListOf<Expense>()
    val allExpensesOfThisMonth = mutableListOf<SuperParent>()
    var revenuesAmount = 0.0
    var expensesAmount = 0.0
    var balance = 0.0
    val hashMap = hashMapOf<String, CustomTriple.Data>()

    open class SuperParent()
    data class Parent(var category: String, var totalCredit: Double, var totalDebit: Double) :
        SuperParent()

    data class Child(var expense: Expense) : SuperParent()

    sealed class CustomTriple {
        class Data(
            var category: String,
            var totalCredit: Double,
            var totalDebit: Double,
            var expenses: ArrayList<Expense>
        ) : CustomTriple()
    }

    sealed class MonthlyReportData {
        object Empty : MonthlyReportData()
        class Data(
            val expenses: List<Expense>,
            val revenues: List<Expense>,
            val allExpensesOfThisMonth: List<SuperParent>,
            val expensesAmount: Double,
            val revenuesAmount: Double
        ) : MonthlyReportData()
    }

    fun loadDataForMonth(dayDate: Date, endDate: Date) {
        viewModelScope.launch {
            val expensesForMonth = withContext(Dispatchers.Default) {
                db.getAllExpenses(dayDate, endDate)
            }

            if (expensesForMonth.isEmpty()) {
                monthlyReportDataLiveData.value = MonthlyReportData.Empty
                return@launch
            }

            expenses.clear()
            revenues.clear()
            revenuesAmount = 0.0
            expensesAmount = 0.0

            hashMap.clear()
            withContext(Dispatchers.Default) {
                for (expense in expensesForMonth) {
                    // Adding category into map with empty list
                    if (!hashMap.containsKey(expense.category))
                        hashMap[expense.category] =
                            CustomTriple.Data(expense.category, 0.0, 0.0, ArrayList<Expense>())
                    var tCredit: Double = hashMap[expense.category]?.totalCredit ?: 0.0
                    var tDebit: Double = hashMap[expense.category]?.totalDebit ?: 0.0

                    if (expense.isRevenue()) {
                        revenues.add(expense)
                        revenuesAmount -= expense.amount
                        tCredit -= expense.amount
                    } else {
                        expenses.add(expense)
                        expensesAmount += expense.amount
                        tDebit += expense.amount
                    }
                    hashMap[expense.category]?.totalCredit = tCredit
                    hashMap[expense.category]?.totalDebit = tDebit
                    hashMap[expense.category]?.expenses?.add(expense)
                }
            }

            hashMap.keys.forEach { key ->
                allExpensesOfThisMonth.add(
                    Parent(
                        hashMap[key]?.category!!,
                        hashMap[key]?.totalCredit ?: 0.0,
                        hashMap[key]?.totalDebit ?: 0.0
                    )
                )
                hashMap[key]?.expenses?.forEach { expense ->
                    allExpensesOfThisMonth.add(Child(expense))
                }
            }
            balance = revenuesAmount - expensesAmount

            monthlyReportDataLiveData.value =
                MonthlyReportData.Data(
                    expenses, revenues, allExpensesOfThisMonth, expensesAmount, revenuesAmount
                )
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