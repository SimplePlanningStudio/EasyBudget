package com.simplebudget.view.accounts.adapter

import com.simplebudget.model.expense.Expense

class AccountDataModels {
    open class SuperParentAccount()

    data class ParentAccount(
        val accountId: Long, var account: String, var totalCredit: Double, var totalDebit: Double
    ) : SuperParentAccount()

    data class ChildAccount(var expense: Expense) : SuperParentAccount()

    sealed class CustomTripleAccount {
        class Data(
            val accountId: Long,
            var account: String,
            var totalCredit: Double,
            var totalDebit: Double,
            var expenses: ArrayList<Expense>
        ) : SuperParentAccount()
    }


    sealed class MonthlyAccountData {
        class Data(
            val expenses: List<Expense>,
            val revenues: List<Expense>,
            val allExpensesOfThisMonth: List<SuperParentAccount>,
            val allExpensesParentList: List<CustomTripleAccount.Data>,
            val expensesAmount: Double,
            val revenuesAmount: Double
        ) : MonthlyAccountData()
    }
}