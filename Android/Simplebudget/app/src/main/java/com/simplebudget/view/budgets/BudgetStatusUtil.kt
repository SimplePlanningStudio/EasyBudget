package com.simplebudget.view.budgets

object BudgetStatus {
    fun get(budgetAmount: Double, spentAmount: Double): String {
        return try {
            when {
                spentAmount == 0.0 -> "No Started ⏳"
                spentAmount == budgetAmount * 0.5 -> "Reached 50% of Budget \uD83D\uDCCA"
                spentAmount < budgetAmount * 0.75 -> "On Track ✅"
                spentAmount in (budgetAmount * 0.75)..<budgetAmount -> "Near Limit ⚠\uFE0F"
                spentAmount == budgetAmount -> "Limit Reached \uD83D\uDEAB"
                spentAmount > budgetAmount -> "Over Spent \uD83D\uDEA8"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
