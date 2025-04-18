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
package com.simplebudget.view.report

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.model.expense.Expense
import com.simplebudget.db.DB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.*

class MonthlyReportViewModel(private val db: DB) : ViewModel() {
    val monthlyReportDataLiveData = MutableLiveData<DataModels.MonthlyReportData?>()
    val expenses = mutableListOf<Expense>()
    val revenues = mutableListOf<Expense>()
    private val allExpensesOfThisMonth = mutableListOf<DataModels.SuperParent>()
    private val allExpensesParentList = mutableListOf<DataModels.CustomTriple.Data>()
    var revenuesAmount = 0.0
    var expensesAmount = 0.0
    var balance = 0.0
    private val hashMap = hashMapOf<String, DataModels.CustomTriple.Data>()


    fun loadDataForMonth(month: LocalDate) {
        viewModelScope.launch {
            val expensesForMonth = withContext(Dispatchers.IO) {
                db.getExpensesForMonth(month)
            }

            if (expensesForMonth.isEmpty()) {
                monthlyReportDataLiveData.value = DataModels.MonthlyReportData.Empty
                return@launch
            }

            withContext(Dispatchers.IO) {
                expenses.clear()
                revenues.clear()
                allExpensesParentList.clear()
                allExpensesOfThisMonth.clear()
                revenuesAmount = 0.0
                expensesAmount = 0.0
                hashMap.clear()

                for (expense in expensesForMonth) {
                    // Adding category into map with empty list
                    if (!hashMap.containsKey(expense.category)) hashMap[expense.category] =
                        DataModels.CustomTriple.Data(expense.category, 0.0, 0.0, 0.0, ArrayList())
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
                            hashMap[key]?.category!!, tCredit, tDebit
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
    }

    override fun onCleared() {
        hashMap.clear()
        super.onCleared()
    }
}