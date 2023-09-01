/*
 *   Copyright 2023 Benoit LETONDOR
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
package com.simplebudget.db

import com.simplebudget.model.category.Category
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpense
import java.io.Closeable
import java.time.LocalDate

interface DB : Closeable {

    suspend fun clearAllTables()

    fun ensureDBCreated()


    suspend fun triggerForceWriteToDisk()

    /**
     * Categories
     */
    suspend fun persistCategories(category: Category): Category

    suspend fun getCategories(): List<Category>

    suspend fun deleteCategory(category: Category)

    suspend fun deleteCategory(categoryName: String?)

    /**
     * Expenses
     */
    suspend fun persistExpense(expense: Expense): Expense

    suspend fun hasExpenseForDay(dayDate: LocalDate): Boolean

    suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense>

    suspend fun getExpensesForMonth(monthStartDate: LocalDate): List<Expense>

    suspend fun searchExpenses(search_query: String): List<Expense>

    suspend fun getAllExpenses(startDate: LocalDate, endDate: LocalDate): List<Expense>

    suspend fun getAllExpenses(): List<Expense>

    suspend fun getBalanceForDay(dayDate: LocalDate): Double

    suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense

    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)

    suspend fun deleteExpense(expense: Expense)

    suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense)

    suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense>

    suspend fun deleteAllExpenseForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense,
        fromDate: LocalDate
    )

    suspend fun getAllExpensesForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense,
        fromDate: LocalDate
    ): List<Expense>

    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    )

    suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): List<Expense>

    suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): Boolean

    suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense?

    suspend fun getOldestExpense(): Expense?
}