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
package com.simplebudget.view.budgets.budgetDetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.model.expense.Expense
import com.simplebudget.view.report.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.collections.ArrayList

/**
 * Constants Values
 */

class BudgetDetailsViewModel(
    private val db: DB,
) : ViewModel() {

    private val allExpensesLiveData = MutableLiveData<List<Expense>>()
    val allExpenses: LiveData<List<Expense>> = allExpensesLiveData

    private val loadingMutableLiveData = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = loadingMutableLiveData


    // Searched results in case of search use case
    val monthlyReportDataLiveData = MutableLiveData<DataModels.MonthlyReportData>()

    val expenses = mutableListOf<Expense>()
    val revenues = mutableListOf<Expense>()
    private val allExpensesOfThisMonth = mutableListOf<DataModels.SuperParent>()
    private val allExpensesParentList = mutableListOf<DataModels.CustomTriple.Data>()
    var revenuesAmount = 0.0
    var expensesAmount = 0.0
    var balance = 0.0
    private val hashMap = hashMapOf<String, DataModels.CustomTriple.Data>()


    /**
     * Load expenses for this month
     */
    fun loadExpenses(budgetId: Long, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                db.getExpensesForBudget(budgetId, startDate, endDate)
            }
            allExpensesLiveData.postValue(results)
            loadingMutableLiveData.postValue(false)
            loadDataForReports(results)
        }
    }

    /**
     * This method will load data into lists.
     * So we can use this for printing / downloading reports.
     */
    private fun loadDataForReports(expensesResults: List<Expense>) {
        if (expensesResults.isEmpty()) {
            monthlyReportDataLiveData.value = DataModels.MonthlyReportData.Empty
            return
        }
        viewModelScope.launch {
            expenses.clear()
            revenues.clear()
            allExpensesParentList.clear()
            allExpensesOfThisMonth.clear()
            revenuesAmount = 0.0
            expensesAmount = 0.0
            hashMap.clear()

            withContext(Dispatchers.IO) {
                for (expense in expensesResults) {
                    // Adding category into map with empty list
                    if (!hashMap.containsKey(expense.category)) hashMap[expense.category] =
                        DataModels.CustomTriple.Data(
                            expense.category,
                            0.0,
                            0.0,
                            0.0,
                            ArrayList<Expense>()
                        )
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

                hashMap.keys.forEach { key ->
                    val tCredit = hashMap[key]?.totalCredit ?: 0.0
                    val tDebit = hashMap[key]?.totalDebit ?: 0.0
                    allExpensesOfThisMonth.add(
                        DataModels.Parent(
                            hashMap[key]?.category!!,
                            tCredit,
                            tDebit
                        )
                    )
                    val amountSpend =
                        if (tCredit > tDebit) (tCredit - tDebit) else (tDebit - tCredit)
                    allExpensesParentList.add(
                        DataModels.CustomTriple.Data(
                            hashMap[key]?.category!!,
                            tCredit,
                            tDebit,
                            amountSpend,
                            hashMap[key]?.expenses ?: ArrayList()
                        )
                    )
                    hashMap[key]?.expenses?.forEach { expense ->
                        allExpensesOfThisMonth.add(DataModels.Child(expense))
                    }
                }
                balance = revenuesAmount - expensesAmount
            }

            allExpensesParentList.sortByDescending { it.amountSpend }

            monthlyReportDataLiveData.postValue(
                DataModels.MonthlyReportData.Data(
                    expenses,
                    revenues,
                    allExpensesOfThisMonth,
                    allExpensesParentList,
                    expensesAmount,
                    revenuesAmount
                )
            )
        }
    }

    override fun onCleared() {
        hashMap.clear()
        super.onCleared()
    }
}