package com.simplebudget.db.impl.budgets

import androidx.room.*
import com.simplebudget.db.impl.expenses.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Query("SELECT * FROM budget")
    suspend fun getAllBudgets(): List<BudgetEntity>


    @Query("SELECT * FROM budget WHERE startDate >= :monthStartDate AND startDate <= :monthEndDate AND accountId = :accountId")
    suspend fun getAllBudgets(
        monthStartDate: LocalDate,
        monthEndDate: LocalDate,
        accountId: Long,
    ): List<BudgetEntity>

    @Query("DELETE FROM monthlybudget WHERE _budget_id = :budgetId")
    suspend fun deleteRecurringBudgetUsingId(budgetId: Long)

    @Query("DELETE FROM budget WHERE _budget_id = :budgetId")
    suspend fun deleteOneTimeBudget(budgetId: Long)

    //It will delete all future budgets of this recurring budget from budget table
    @Query("DELETE FROM budget WHERE monthly_id = :monthlyId AND startDate >= :dateFrom")
    suspend fun deleteAllBudgetForRecurringBudgetFromDate(monthlyId: Long, dateFrom: LocalDate)

    @Query("UPDATE budget SET spentAmount = :spentAmount, remainingAmount= :remainingAmount WHERE _budget_id = :budgetId")
    suspend fun updateBudget(
        budgetId: Long,
        spentAmount: Long,
        remainingAmount: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBudget(recurringBudget: RecurringBudgetEntity): Long

    @Query("SELECT * FROM monthlybudget WHERE _budget_id = :budgetId")
    suspend fun getRecurringBudget(budgetId: Long): RecurringBudgetEntity?

    @Query("SELECT * FROM monthlybudget WHERE accountId = :accountId")
    suspend fun getRecurringBudgetByAccount(
        accountId: Long,
    ): RecurringBudgetEntity?

    @Query("DELETE FROM monthlybudget WHERE _budget_id = :budgetId")
    suspend fun deleteRecurringBudget(budgetId: Long)

    @Query("DELETE FROM budget_category_cross_ref WHERE _budget_id = :budgetId")
    suspend fun deleteBudgetCategoryCrossRefUsingBudgetId(budgetId: Long)

    @Query("DELETE FROM budget_category_cross_ref WHERE _category_id = :categoryId")
    suspend fun deleteBudgetCategoryCrossRefUsingCategoryId(categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategoryCrossRef(crossRef: BudgetCategoryCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategoryCrossRefs(crossRefs: List<BudgetCategoryCrossRef>)

    @Transaction
    suspend fun insertBudgetWithCategories(budget: BudgetEntity, categoryIds: List<Long>) {
        val budgetId = insertBudget(budget) // Insert budget and get its ID
        val crossRefs = categoryIds.map { categoryId ->
            BudgetCategoryCrossRef(budgetId, categoryId)
        }
        insertBudgetCategoryCrossRefs(crossRefs) // Insert the cross-references
    }

    @Transaction
    @Query("SELECT * FROM budget WHERE _budget_id = :budgetId")
    suspend fun getBudgetWithCategories(budgetId: Long): BudgetWithCategories


    @Query("SELECT * FROM budget_category_cross_ref WHERE _category_id = :categoryId")
    suspend fun getBudgetsForCategory(categoryId: Long): List<BudgetCategoryCrossRef>

    @Transaction
    @Query(
        """
        SELECT * FROM budget 
        WHERE _budget_id IN (
            SELECT _budget_id FROM budget_category_cross_ref WHERE _category_id = :categoryId
        ) AND accountId = :accountId
    """
    )
    suspend fun getBudgetsWithCategoriesByCategoryAndAccount(
        categoryId: Long,
        accountId: Long,
    ): List<BudgetWithCategories>


    /**
     * startDate <= :monthEndDate → Ensures that the budget started before or within the month.
     * endDate >= :monthStartDate → Ensures that the budget hasn't ended before the month starts.
     * A budget spans multiple months, it will be included in all those months.
     */
    @Transaction
    @Query(
        """
    SELECT * FROM budget
    WHERE accountId = :accountId
    AND startDate <= :monthEndDate
    AND endDate >= :monthStartDate
    """
    )
    suspend fun getBudgetsWithCategoriesByAccount(
        monthStartDate: LocalDate,
        monthEndDate: LocalDate,
        accountId: Long,
    ): List<BudgetWithCategories>

    @Query(
        """
    UPDATE budget
SET spentAmount = COALESCE((
    SELECT COALESCE(SUM(amount), 0)
    FROM expense
    WHERE accountId = :accountId
    AND categoryId IN (
        SELECT _category_id
        FROM budget_category_cross_ref
        WHERE _budget_id = budget._budget_id
    )
    AND date >= CASE 
                   WHEN startDate > :startDate THEN startDate 
                   ELSE :startDate 
               END
    AND date <= CASE 
                   WHEN endDate < :endDate THEN endDate 
                   ELSE :endDate 
               END
), 0) -- Fallback to 0 if the query returns NULL
WHERE startDate <= :endDate AND endDate >= :startDate
    """
    )
    suspend fun updateBudgetsSpentAmount(startDate: LocalDate, endDate: LocalDate, accountId: Long)


    @Query("SELECT MIN(startDate) FROM budget")
    suspend fun getOldestBudgetStartDate(): LocalDate?

    @Query(
        """
    SELECT * 
    FROM expense 
    WHERE categoryId IN (
        SELECT _category_id 
        FROM budget_category_cross_ref 
        WHERE _budget_id = :budgetId
    )
    AND date BETWEEN MAX(
        (SELECT startDate FROM budget WHERE _budget_id = :budgetId), :startDate
    ) AND MIN(
        (SELECT endDate FROM budget WHERE _budget_id = :budgetId), :endDate
    )
    """
    )
    suspend fun getExpensesForBudget(
        budgetId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ExpenseEntity>

}