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
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.helper.Logger
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.concurrent.Executor

@Suppress("DeferredResultUnused")
class CachedDBImpl(
    private val wrappedDB: DB,
    private val cacheStorage: CacheDBStorage,
    private val executor: Executor
) : DB {
    override suspend fun clearAllTables() {
        wrappedDB.clearAllTables()
        wipeCache()
    }

    override fun ensureDBCreated() {
        wrappedDB.ensureDBCreated()
    }

    override suspend fun triggerForceWriteToDisk() {
        wrappedDB.triggerForceWriteToDisk()
    }

    override fun close() {
        wrappedDB.close()
    }

    override suspend fun persistCategory(category: Category): Category =
        wrappedDB.persistCategory(category)

    override suspend fun persistCategories(categories: List<Category>) {
        wrappedDB.persistCategories(categories)
    }

    override fun getCategories(): Flow<List<CategoryEntity>> = wrappedDB.getCategories()

    override suspend fun getCategory(categoryId: Long): CategoryEntity =
        wrappedDB.getCategory(categoryId)

    override suspend fun getMiscellaneousCategory(): CategoryEntity =
        wrappedDB.getMiscellaneousCategory()

    override suspend fun deleteCategory(category: Category) {
        wrappedDB.deleteCategory(category)
    }

    override suspend fun deleteCategory(categoryName: String?) {
        wrappedDB.deleteCategory(categoryName)
    }

    override suspend fun isCategoriesTableEmpty(): Boolean = wrappedDB.isCategoriesTableEmpty()

    override fun getAccountTypes(): Flow<List<AccountTypeEntity>> = wrappedDB.getAccountTypes()
    override suspend fun getAllAccounts(): List<AccountTypeEntity> = wrappedDB.getAllAccounts()

    override fun getActiveAccount(): Flow<AccountTypeEntity> = wrappedDB.getActiveAccount()
    override suspend fun getAccount(accountId: Long): AccountTypeEntity =
        wrappedDB.getAccount(accountId)

    override suspend fun persistAccountTypes(accounts: List<Account>) {
        wrappedDB.persistAccountTypes(accounts)
    }

    override suspend fun persistAccountType(account: Account) {
        wrappedDB.persistAccountType(account)
    }

    override suspend fun accountAlreadyExists(name: String): Int {
        return wrappedDB.accountAlreadyExists(name)
    }

    override suspend fun deleteAccountType(account: Account) {
        wrappedDB.deleteAccountType(account)
    }

    override suspend fun isAccountsTypeTableEmpty(): Boolean = wrappedDB.isAccountsTypeTableEmpty()

    /**
     * Disable multiple account settings and reset to default acount
     */
    override suspend fun resetActiveAccount() {
        wrappedDB.resetActiveAccount()
    }

    /**
     * Set passing account to active
     */
    override suspend fun setActiveAccount(accountName: String) {
        wrappedDB.setActiveAccount(accountName)
    }

    /**
     * Set passing account to active
     */
    override suspend fun setActiveAccount(accountId: Long) {
        wipeCache()
        wrappedDB.setActiveAccount(accountId)
    }

    /**
     * Delete all one time expenses / recurring expenses for given account id
     * This function mostly be called for account deletion case.
     */
    override suspend fun deleteAllExpensesOfAnAccount(accountId: Long) {
        wrappedDB.deleteAllExpensesOfAnAccount(accountId)
    }

    /**
     * Expenses
     */
    override suspend fun persistExpense(expense: Expense): Expense {
        val newExpense = wrappedDB.persistExpense(expense)

        wipeCache()

        return newExpense
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate, accountId: Long): Boolean {
        val expensesForDay = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate]
        }

        return if (expensesForDay == null) {
            executor.execute(
                CacheExpensesForMonthRunnable(
                    dayDate, this, cacheStorage, accountId
                )
            )
            wrappedDB.hasExpenseForDay(dayDate, accountId)
        } else {
            expensesForDay.isNotEmpty()
        }
    }

    override suspend fun getExpensesForDay(
        dayDate: LocalDate, accountId: Long
    ): List<Expense> {
        val cached = synchronized(cacheStorage.expenses) {
            cacheStorage.expenses[dayDate]
        }
        if (cached != null) {
            return cached
        } else {
            executor.execute(
                CacheExpensesForMonthRunnable(
                    dayDate.startOfMonth(), this, cacheStorage, accountId
                )
            )
        }

        return wrappedDB.getExpensesForDay(dayDate, accountId)
    }

    private suspend fun getExpensesForDayWithoutCache(dayDate: LocalDate, accountId: Long) =
        wrappedDB.getExpensesForDay(dayDate, accountId)

    override suspend fun getExpensesForMonth(
        monthStartDate: LocalDate
    ): List<Expense> = wrappedDB.getExpensesForMonth(monthStartDate)

    override suspend fun getExpensesForMonthWithoutCheckingAccount(): List<Expense> =
        wrappedDB.getExpensesForMonthWithoutCheckingAccount()

    override suspend fun searchExpenses(search_query: String): List<Expense> =
        wrappedDB.searchExpenses(search_query)

    override suspend fun getAllExpenses(
        startDate: LocalDate, endDate: LocalDate
    ): List<Expense> = wrappedDB.getAllExpenses(startDate, endDate)

    override suspend fun getAllExpenses(): List<Expense> = wrappedDB.getAllExpenses()

    override suspend fun getBalanceForDay(dayDate: LocalDate, accountId: Long): Double {
        val cached = synchronized(cacheStorage.balances) {
            cacheStorage.balances[dayDate]
        }
        if (cached != null) {
            return cached
        } else {
            executor.execute(
                CacheBalanceForMonthRunnable(
                    dayDate.startOfMonth(), this, cacheStorage, accountId
                )
            )
        }
        return wrappedDB.getBalanceForDay(dayDate, accountId)
    }

    private suspend fun getBalanceForDayWithoutCache(
        dayDate: LocalDate, accountId: Long
    ): Double = wrappedDB.getBalanceForDay(dayDate, accountId)

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

    override suspend fun getAllExpenseForRecurringExpense(
        recurringExpense: RecurringExpense
    ): List<Expense> = wrappedDB.getAllExpenseForRecurringExpense(recurringExpense)

    override suspend fun deleteAllExpenseForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense, fromDate: LocalDate
    ) {
        wrappedDB.deleteAllExpenseForRecurringExpenseFromDate(recurringExpense, fromDate)

        wipeCache()
    }

    override suspend fun getAllExpensesForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense, fromDate: LocalDate
    ): List<Expense> = wrappedDB.getAllExpensesForRecurringExpenseFromDate(
        recurringExpense, fromDate
    )

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ) {
        wrappedDB.deleteAllExpenseForRecurringExpenseBeforeDate(
            recurringExpense, beforeDate
        )

        wipeCache()
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ): List<Expense> = wrappedDB.getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense, beforeDate
    )

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ): Boolean = wrappedDB.hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense, beforeDate
    )

    override suspend fun findRecurringExpenseForId(
        recurringExpenseId: Long
    ): RecurringExpense? = wrappedDB.findRecurringExpenseForId(recurringExpenseId)

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
        private val cacheStorage: CacheDBStorage,
        private val accountId: Long
    ) : Runnable {

        override fun run() {
            synchronized(cacheStorage.expenses) {
                if (cacheStorage.expenses.containsKey(startOfMonthDate) && cacheStorage.accountId == accountId) {
                    return
                }
            }
            // Save the month we wanna load cache for
            var currentDate = startOfMonthDate
            val month = currentDate.month

            Logger.debug("DBCache: Caching expenses for month: $month")

            // Iterate over day of month (while are still on that month)
            while (currentDate.month == month) {
                val expensesForDay = runBlocking {
                    db.getExpensesForDayWithoutCache(
                        currentDate, accountId
                    )
                }

                synchronized(cacheStorage.expenses) {
                    cacheStorage.expenses.put(currentDate, expensesForDay)
                }

                currentDate = currentDate.plusDays(1)
            }

            cacheStorage.accountId = accountId
            Logger.debug("DBCache: Expenses cached for month: $month")
        }
    }

    private class CacheBalanceForMonthRunnable(
        private val startOfMonthDate: LocalDate,
        private val db: CachedDBImpl,
        private val cacheStorage: CacheDBStorage,
        private val accountId: Long
    ) : Runnable {

        override fun run() {
            synchronized(cacheStorage.balances) {
                if (cacheStorage.balances.containsKey(startOfMonthDate) && cacheStorage.accountId == accountId) {
                    return
                }
            }

            // Save the month we wanna load cache for
            var currentDate = startOfMonthDate
            val month = currentDate.month

            Logger.debug("DBCache: Caching balance for month: $month")

            // Iterate over day of month (while are still on that month)
            while (currentDate.month == month) {
                val balanceForDay =
                    runBlocking { db.getBalanceForDayWithoutCache(currentDate, accountId) }

                synchronized(cacheStorage.balances) {
                    cacheStorage.balances.put(currentDate, balanceForDay)
                }

                currentDate = currentDate.plusDays(1)
            }
            cacheStorage.accountId = accountId
            Logger.debug("DBCache: Balance cached for month: $month")
        }
    }
}

interface CacheDBStorage {
    val expenses: MutableMap<LocalDate, List<Expense>>
    val balances: MutableMap<LocalDate, Double>
    var accountId: Long
}


private fun LocalDate.startOfMonth() = LocalDate.of(year, month, 1)