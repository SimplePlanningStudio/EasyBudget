package com.simplebudget.view.report

import com.simplebudget.model.Expense

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
            var expenses: ArrayList<Expense>
        ) : SuperParent()
    }


    sealed class MonthlyReportData {
        object Empty : MonthlyReportData()
        class Data(
            val expenses: List<Expense>,
            val revenues: List<Expense>,
            val allExpensesOfThisMonth: List<SuperParent>,
            val allExpensesParentList: List<CustomTriple.Data>,
            val expensesAmount: Double,
            val revenuesAmount: Double
        ) : MonthlyReportData()
    }
}