package com.simplebudget.db.impl.budgets

import androidx.room.*
import com.simplebudget.model.budget.Budget
import com.simplebudget.model.budget.RecurringBudget
import com.simplebudget.model.category.Category
import java.time.LocalDate

@Entity(
    tableName = "budget",
    indices = [
        Index(value = ["startDate"], name = "S_D"),
        Index(value = ["endDate"], name = "E_D"),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_budget_id")
    val id: Long?,
    @ColumnInfo(name = "goal")
    val goal: String,
    @ColumnInfo(name = "accountId")
    val accountId: Long,
    @ColumnInfo(name = "budgetAmount")
    val budgetAmount: Long,
    @ColumnInfo(name = "remainingAmount")
    val remainingAmount: Long,
    @ColumnInfo(name = "spentAmount")
    val spentAmount: Long,
    @ColumnInfo(name = "startDate")
    val startDate: LocalDate,
    @ColumnInfo(name = "endDate")
    val endDate: LocalDate,
    @ColumnInfo(name = "monthly_id")
    val associatedRecurringBudgetId: Long?,
) {
    fun toBudget(associatedRecurringBudget: RecurringBudget?, categories: List<Category>) = Budget(
        id,
        goal,
        accountId,
        budgetAmount / 100.0,
        remainingAmount / 100.0,
        spentAmount / 100.0,
        startDate,
        endDate,
        associatedRecurringBudget,
        categories
    )
}