package com.simplebudget.db.impl.budgets

import androidx.room.*
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.model.budget.RecurringBudget
import com.simplebudget.model.budget.RecurringBudgetType
import com.simplebudget.model.recurringexpense.RecurringExpenseType
import java.time.LocalDate

@Entity(tableName = "monthlybudget")
data class RecurringBudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "_budget_id")
    val id: Long?,
    @ColumnInfo(name = "goal")
    val goal: String,
    @ColumnInfo(name = "accountId")
    val accountId: Long,
    @ColumnInfo(name = "budgetAmount")
    val originalAmount: Long,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "recurringDate")
    val recurringDate: LocalDate,
    @ColumnInfo(name = "modified")
    val modified: Boolean
) {
    fun toRecurringBudget() = RecurringBudget(
        id,
        goal,
        accountId,
        originalAmount / 100.0,
        RecurringBudgetType.valueOf(type),
        recurringDate,
        modified,
    )
}