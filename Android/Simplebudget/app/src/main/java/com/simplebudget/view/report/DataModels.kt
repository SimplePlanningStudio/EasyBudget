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

import com.simplebudget.helper.banner.AppBanner
import com.simplebudget.model.expense.Expense

class DataModels {
    open class SuperParent()
    data class Parent(var category: String, var totalCredit: Double, var totalDebit: Double) :
        SuperParent()

    data class Child(var expense: Expense) : SuperParent()

    sealed class CustomTriple {
        class Data(
            var category: String,
            var totalCredit: Double,
            var totalDebit: Double,
            var amountSpend: Double,
            var expenses: ArrayList<Expense>,
        ) : SuperParent()
    }


    data class BannerItem(val banner: AppBanner?) : SuperParent()

    sealed class MonthlyReportData {
        object Empty : MonthlyReportData()
        class Data(
            val expenses: List<Expense>,
            val revenues: List<Expense>,
            val allExpensesOfThisMonth: List<SuperParent>,
            val allExpensesParentList: List<CustomTriple.Data>,
            val expensesAmount: Double,
            val revenuesAmount: Double,
        ) : MonthlyReportData()
    }
}