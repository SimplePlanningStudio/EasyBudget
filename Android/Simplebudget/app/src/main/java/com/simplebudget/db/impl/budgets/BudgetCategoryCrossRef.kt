package com.simplebudget.db.impl.budgets

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.simplebudget.db.impl.categories.CategoryEntity

/**
 * BudgetCategoryCrossRef (Junction Table): This table creates the many-to-many relationship
 * between BudgetEntity and CategoryEntity tables.
 */
@Entity(
    tableName = "budget_category_cross_ref",
    primaryKeys = ["_budget_id", "_category_id"],
    indices = [Index(value = ["_category_id"], name = "C_I")],
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["_budget_id"],
            childColumns = ["_budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetCategoryCrossRef(
    @ColumnInfo(name = "_budget_id")
    val budgetId: Long,
    @ColumnInfo(name = "_category_id")
    val categoryId: Long,
)
