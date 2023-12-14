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

import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpense
import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import java.time.LocalDate

interface DB : Closeable {

    suspend fun clearAllTables()

    fun ensureDBCreated()


    suspend fun triggerForceWriteToDisk()

    /**
     * Save a category
     */
    suspend fun persistCategory(category: Category): Category

    /**
     * Save list of categories
     */
    suspend fun persistCategories(categories: List<Category>)

    fun getCategories(): Flow<List<CategoryEntity>>
    suspend fun getCategory(categoryId: Long): CategoryEntity
    suspend fun getMiscellaneousCategory(): CategoryEntity

    suspend fun deleteCategory(category: Category)

    suspend fun deleteCategory(categoryName: String?)

    suspend fun isCategoriesTableEmpty(): Boolean

    /**
     * Get all stored accounts types
     */
    fun getAccountTypes(): Flow<List<AccountTypeEntity>>
    suspend fun getAllAccounts(): List<AccountTypeEntity>

    /**
     * Get active account
     */
    fun getActiveAccount(): Flow<AccountTypeEntity>

    suspend fun getAccount(accountId: Long): AccountTypeEntity

    /**
     * Save list of available accounts
     */
    suspend fun persistAccountTypes(accounts: List<Account>)

    /**
     * Save account type
     */
    suspend fun persistAccountType(account: Account)

    /**
     * Delete account
     */
    suspend fun deleteAccountType(account: Account)

    suspend fun isAccountsTypeTableEmpty(): Boolean

    suspend fun resetActiveAccount()

    suspend fun setActiveAccount(accountName: String)

    suspend fun setActiveAccount(accountId: Long)

    /**
     * Delete all one time expenses / recurring expenses for given account id
     * This function mostly be called for account deletion case.
     */
    suspend fun deleteAllExpensesOfAnAccount(accountId: Long)

    /**
     * Expenses
     */
    suspend fun persistExpense(expense: Expense): Expense

    suspend fun hasExpenseForDay(dayDate: LocalDate, accountId: Long): Boolean

    suspend fun getExpensesForDay(dayDate: LocalDate, accountId: Long): List<Expense>

    suspend fun getExpensesForMonth(
        monthStartDate: LocalDate
    ): List<Expense>

    /**
     * This method will return expenses for current month from start date to until today
     */
    suspend fun getExpensesForMonthWithoutCheckingAccount(): List<Expense>

    suspend fun searchExpenses(search_query: String): List<Expense>

    suspend fun getAllExpenses(startDate: LocalDate, endDate: LocalDate): List<Expense>

    suspend fun getAllExpenses(): List<Expense>

    suspend fun getBalanceForDay(dayDate: LocalDate, accountId: Long): Double

    suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense

    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)

    suspend fun deleteExpense(expense: Expense)

    suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense)

    suspend fun getAllExpenseForRecurringExpense(
        recurringExpense: RecurringExpense
    ): List<Expense>

    suspend fun deleteAllExpenseForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense, fromDate: LocalDate
    )

    suspend fun getAllExpensesForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense, fromDate: LocalDate
    ): List<Expense>

    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    )

    suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ): List<Expense>

    suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ): Boolean

    suspend fun findRecurringExpenseForId(
        recurringExpenseId: Long
    ): RecurringExpense?

    suspend fun getOldestExpense(): Expense?
}