package com.simplebudget.db.impl.budgets

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.model.budget.RecurringBudget
import com.simplebudget.model.category.Category

data class BudgetWithCategories(
    @Embedded val budget: BudgetEntity,
    @Relation(
        parentColumn = "_budget_id",
        entityColumn = "_category_id",
        associateBy = Junction(BudgetCategoryCrossRef::class)
    )
    val categories: List<CategoryEntity>
)
