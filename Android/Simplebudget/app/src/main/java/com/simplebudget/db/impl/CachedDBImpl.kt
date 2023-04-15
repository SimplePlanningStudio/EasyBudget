/*
 *   Copyright 2023 Benoit LETONDOR , Waheed Nazir
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
package com.simplebudget.db.impl

import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpense
import com.simplebudget.db.DB
import com.simplebudget.helper.Logger
import com.simplebudget.model.category.Category
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.concurrent.Executor

@Suppress("DeferredResultUnused")
class CachedDBImpl(
    private val wrappedDB: DB,
    private val cacheStorage: CacheDBStorage,
    private val executor: Executor
) : DB {

    override fun ensureDBCreated() {
        wrappedDB.ensureDBCreated()
    }

    override suspend fun triggerForceWriteToDisk() {
        wrappedDB.triggerForceWriteToDisk()
    }

    override fun close() {
        wrappedDB.close()
    }

    override suspend fun persistCategories(category: Category): Category {
        return wrappedDB.persistCategories(category)
    }

    override suspend fun getCategories(): List<Category> {
        return wrappedDB.getCategories()
    }

    override suspend fun deleteCategory(category: Category) {
        wrappedDB.deleteCategory(category)
    }

    override suspend fun deleteCategory(categoryName: String?) {
        wrappedDB.deleteCategory(categoryName)
    }

    /**
     * Expenses
     */
    override suspend fun persistExpense(expense: Expense): Expense {
        val newExpense = wrappedDB.persistExpense(expense)

        wipeCache()

        return newExpense
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate): Boolean {
        val expensesForDay = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate]
        }

        return if (expensesForDay == null) {
            executor.execute(CacheExpensesForMonthRunnable(dayDate, this, cacheStorage))
            wrappedDB.hasExpenseForDay(dayDate)
        } else {
            expensesForDay.isNotEmpty()
        }
    }

    override suspend fun getExpensesForDay(dayDate: LocalDate): List<Expense> {
        val cached = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate]
        }
        if (cached != null) {
            return cached
        } else {
            executor.execute(
                CacheExpensesForMonthRunnable(
                    dayDate.startOfMonth(),
                    this,
                    cacheStorage
                )
            )
        }

        return wrappedDB.getExpensesForDay(dayDate)
    }

    private suspend fun getExpensesForDayWithoutCache(dayDate: LocalDate) =
        wrappedDB.getExpensesForDay(dayDate)

    override suspend fun getExpensesForMonth(monthStartDate: LocalDate): List<Expense> =
        wrappedDB.getExpensesForMonth(monthStartDate)

    override suspend fun searchExpenses(search_query: String): List<Expense> =
        wrappedDB.searchExpenses(search_query)

    override suspend fun getAllExpenses(startDate: LocalDate, endDate: LocalDate): List<Expense> =
        wrappedDB.getAllExpenses(startDate, endDate)

    override suspend fun getAllExpenses(): List<Expense> = wrappedDB.getAllExpenses()

    override suspend fun getBalanceForDay(dayDate: LocalDate): Double {
        val cached = synchronized(cacheStorage.balances) {
            cacheStorage.balances[dayDate]
        }
        if (cached != null) {
            return cached
        } else {
            executor.execute(
                CacheBalanceForMonthRunnable(
                    dayDate.startOfMonth(),
                    this,
                    cacheStorage
                )
            )
        }
        return wrappedDB.getBalanceForDay(dayDate)
    }

    private suspend fun getBalanceForDayWithoutCache(dayDate: LocalDate): Double =
        wrappedDB.getBalanceForDay(dayDate)

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense =
        wrappedDB.persistRecurringExpense(recurringExpense)

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        wrappedDB.deleteRecurringExpense(recurringExpense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        wrappedDB.deleteExpense(expense)

        wipeCache()
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        wrappedDB.deleteAllExpenseForRecurringExpense(recurringExpense)

        wipeCache()
    }

    override suspend fun getAllExpenseForRecurringExpense(recurringExpense: RecurringExpense): List<Expense> =
        wrappedDB.getAllExpenseForRecurringExpense(recurringExpense)

    override suspend fun deleteAllExpenseForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense,
        fromDate: LocalDate
    ) {
        wrappedDB.deleteAllExpenseForRecurringExpenseFromDate(recurringExpense, fromDate)

        wipeCache()
    }

    override suspend fun getAllExpensesForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense,
        fromDate: LocalDate
    ): List<Expense> =
        wrappedDB.getAllExpensesForRecurringExpenseFromDate(recurringExpense, fromDate)

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ) {
        wrappedDB.deleteAllExpenseForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

        wipeCache()
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): List<Expense> =
        wrappedDB.getAllExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense,
        beforeDate: LocalDate
    ): Boolean = wrappedDB.hasExpensesForRecurringExpenseBeforeDate(recurringExpense, beforeDate)

    override suspend fun findRecurringExpenseForId(recurringExpenseId: Long): RecurringExpense? =
        wrappedDB.findRecurringExpenseForId(recurringExpenseId)

    override suspend fun getOldestExpense(): Expense? = wrappedDB.getOldestExpense()

    /**
     * Instantly wipe all cached data
     */
    private fun wipeCache() {
        Logger.debug("DBCache: Wipe all")

        synchronized(cacheStorage.balances) {
            cacheStorage.balances.clear()
        }

        synchronized(cacheStorage.expenses) {
            cacheStorage.expenses.clear()
        }
    }

    private class CacheExpensesForMonthRunnable(
        private val startOfMonthDate: LocalDate,
        private val db: CachedDBImpl,
        private val cacheStorage: CacheDBStorage
    ) : Runnable {

        override fun run() {
            synchronized(cacheStorage.expenses) {
                if (cacheStorage.expenses.containsKey(startOfMonthDate)) {
                    return
                }
            }

            // Save the month we wanna load cache for
            var currentDate = startOfMonthDate
            val month = currentDate.month

            Logger.debug("DBCache: Caching expenses for month: $month")

            // Iterate over day of month (while are still on that month)
            while (currentDate.month == month) {
                val expensesForDay = runBlocking { db.getExpensesForDayWithoutCache(currentDate) }

                synchronized(cacheStorage.expenses) {
                    cacheStorage.expenses.put(currentDate, expensesForDay)
                }

                currentDate = currentDate.plusDays(1)
            }

            Logger.debug("DBCache: Expenses cached for month: $month")
        }
    }

    private class CacheBalanceForMonthRunnable(
        private val startOfMonthDate: LocalDate,
        private val db: CachedDBImpl,
        private val cacheStorage: CacheDBStorage
    ) : Runnable {

        override fun run() {
            synchronized(cacheStorage.balances) {
                if (cacheStorage.balances.containsKey(startOfMonthDate)) {
                    return
                }
            }

            // Save the month we wanna load cache for
            var currentDate = startOfMonthDate
            val month = currentDate.month

            Logger.debug("DBCache: Caching balance for month: $month")

            // Iterate over day of month (while are still on that month)
            while (currentDate.month == month) {
                val balanceForDay = runBlocking { db.getBalanceForDayWithoutCache(currentDate) }

                synchronized(cacheStorage.balances) {
                    cacheStorage.balances.put(currentDate, balanceForDay)
                }

                currentDate = currentDate.plusDays(1)
            }

            Logger.debug("DBCache: Balance cached for month: $month")
        }
    }
}

interface CacheDBStorage {
    val expenses: MutableMap<LocalDate, List<Expense>>
    val balances: MutableMap<LocalDate, Double>
}


private fun LocalDate.startOfMonth() = LocalDate.of(year, month, 1)