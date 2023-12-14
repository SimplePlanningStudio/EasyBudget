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
package com.simplebudget.db.impl

import androidx.sqlite.db.SimpleSQLiteQuery
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpense
import com.simplebudget.db.DB
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.db.impl.expenses.ExpenseEntity
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.extensions.*
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DBImpl(private val roomDB: RoomDB, private val appPreferences: AppPreferences) : DB {

    override suspend fun clearAllTables() {
        roomDB.clearAllTables()
    }

    override fun ensureDBCreated() {
        roomDB.openHelper.writableDatabase.close()
    }

    /**
     * The query "PRAGMA wal_checkpoint(full)" is a SQLite pragma that is used to manually trigger
     * a write-ahead logging (WAL) checkpoint in a SQLite database. This pragma is typically used
     * to optimize the database's write-ahead log.
     */
    override suspend fun triggerForceWriteToDisk() {
        roomDB.expenseDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
    }

    /**
     * Category implementations
     */
    override suspend fun persistCategories(categories: List<Category>) {
        roomDB.categoryDao().persistCategories(categories.toCategoryEntities())
    }

    /**
     * Category implementations
     */
    override suspend fun persistCategory(category: Category): Category {
        val newId = roomDB.categoryDao().persistCategory(category.toCategoryEntity())
        return category.copy(id = newId)
    }

    override fun getCategories(): Flow<List<CategoryEntity>> = roomDB.categoryDao().getCategories()

    /**
     * Get category from category id
     */
    override suspend fun getCategory(categoryId: Long): CategoryEntity =
        roomDB.categoryDao().getCategory(categoryId)

    override suspend fun getMiscellaneousCategory(): CategoryEntity =
        roomDB.categoryDao().getMiscellaneousCategory()

    override suspend fun deleteCategory(categoryName: String?) {
        if (categoryName.isNullOrBlank()) return
        roomDB.categoryDao().deleteCategory(categoryName)
    }

    override suspend fun deleteCategory(category: Category) {
        if (category.id == null) {
            throw IllegalArgumentException("deleteCategory called with a category that has no id")
        }
        roomDB.categoryDao().deleteCategory(category.toCategoryEntity())
    }

    /**
     * Check rows count if equal to zero table is empty
     */
    override suspend fun isCategoriesTableEmpty(): Boolean {
        val rowCount = roomDB.categoryDao().getRowCount()
        return (rowCount == 0)
    }

    /**
     * Check rows count if equal to zero table is empty
     */
    override suspend fun isAccountsTypeTableEmpty(): Boolean {
        val rowCount = roomDB.accountTypeDao().getRowCount()
        return (rowCount == 0)
    }

    /**
     * Disable multiple account settings and reset to default account
     */
    override suspend fun resetActiveAccount() {
        roomDB.accountTypeDao().resetActiveAccount()
    }

    /**
     * Set passing account to active
     */
    override suspend fun setActiveAccount(accountName: String) {
        roomDB.accountTypeDao().setActiveAccount(accountName)
    }

    /**
     * Set passing account to active
     */
    override suspend fun setActiveAccount(accountId: Long) {
        roomDB.accountTypeDao().setActiveAccount(accountId)
    }

    /**
     * Delete all one time expenses / recurring expenses for given account id
     * This function mostly be called for account deletion case.
     */
    override suspend fun deleteAllExpensesOfAnAccount(accountId: Long) {
        roomDB.expenseDao().deleteAllExpenses(accountId)
        roomDB.expenseDao().deleteAllRecurringExpenses(accountId)
    }

    /**
     * Save accounts
     */
    override suspend fun persistAccountTypes(accounts: List<Account>) {
        roomDB.accountTypeDao().insertAllAccountTypes(accounts.toAccountTypeEntities())
    }

    /**
     * Save account
     */
    override suspend fun persistAccountType(account: Account) {
        roomDB.accountTypeDao().insertAccountType(account.toAccountEntity())
    }

    /**
     * Delete account
     */
    override suspend fun deleteAccountType(account: Account) {
        roomDB.accountTypeDao().deleteAccountType(account.toAccountEntity())
    }

    override fun getAccountTypes(): Flow<List<AccountTypeEntity>> =
        roomDB.accountTypeDao().getAllAccountTypes()

    override suspend fun getAllAccounts(): List<AccountTypeEntity> =
        roomDB.accountTypeDao().getAllAccounts()

    override fun getActiveAccount(): Flow<AccountTypeEntity> =
        roomDB.accountTypeDao().getActiveAccount()

    override suspend fun getAccount(accountId: Long): AccountTypeEntity {
        return roomDB.accountTypeDao().getAccount(accountId)
    }

    override fun close() {
        roomDB.close()
    }

    override suspend fun persistExpense(expense: Expense): Expense {
        val newId = roomDB.expenseDao().persistExpense(expense.toExpenseEntity())
        return expense.copy(id = newId)
    }

    override suspend fun hasExpenseForDay(dayDate: LocalDate, accountId: Long): Boolean {
        return roomDB.expenseDao().hasExpenseForDay(dayDate, accountId) > 0
    }

    override suspend fun getExpensesForDay(
        dayDate: LocalDate,
        accountId: Long
    ): List<Expense> {
        return roomDB.expenseDao().getExpensesForDay(dayDate, accountId)
            .toExpenses(this)
    }

    override suspend fun getExpensesForMonth(
        monthStartDate: LocalDate
    ): List<Expense> {
        val monthEndDate = monthStartDate.plusMonths(1).minusDays(1)

        return roomDB.expenseDao()
            .getExpensesForMonth(monthStartDate, monthEndDate, appPreferences.activeAccount())
            .toExpenses(this)
    }

    override suspend fun getExpensesForMonthWithoutCheckingAccount(): List<Expense> {
        return roomDB.expenseDao()
            .getExpensesForMonthWithoutCheckingAccount(DateHelper.startDayOfMonth, DateHelper.today)
            .toExpenses(this)
    }

    override suspend fun searchExpenses(search_query: String): List<Expense> {
        // Search results for last 3 months to Today
        return roomDB.expenseDao().searchExpenses(
            search_query = "%$search_query%",
            startDate = DateHelper.lastThreeMonth,
            endDate = DateHelper.today,
            accountId = appPreferences.activeAccount()
        ).toExpenses(this)
    }

    override suspend fun getAllExpenses(
        startDate: LocalDate, endDate: LocalDate
    ): List<Expense> {
        return roomDB.expenseDao()
            .getAllExpenses(startDate, endDate, appPreferences.activeAccount()).toExpenses(this)
    }

    override suspend fun getAllExpenses(): List<Expense> {
        return roomDB.expenseDao().getAllExpenses(appPreferences.activeAccount()).toExpenses(this)
    }

    override suspend fun getBalanceForDay(dayDate: LocalDate, accountId: Long): Double {
        return roomDB.expenseDao().getBalanceForDay(dayDate, accountId)
            .getRealValueFromDB()
    }

    override suspend fun persistRecurringExpense(recurringExpense: RecurringExpense): RecurringExpense {
        val newId =
            roomDB.expenseDao().persistRecurringExpense(recurringExpense.toRecurringExpenseEntity())
        return recurringExpense.copy(id = newId)
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        if (recurringExpense.id == null) {
            throw IllegalArgumentException("deleteRecurringExpense called with a recurring expense that has no id")
        }

        roomDB.expenseDao().deleteRecurringExpense(recurringExpense.toRecurringExpenseEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        if (expense.id == null) {
            throw IllegalArgumentException("deleteExpense called with an expense that has no id")
        }

        roomDB.expenseDao().deleteExpense(expense.toExpenseEntity())
    }

    override suspend fun deleteAllExpenseForRecurringExpense(recurringExpense: RecurringExpense) {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpense called with a recurring expense that has no id")

        roomDB.expenseDao().deleteAllExpenseForRecurringExpense(recurringExpenseId)
    }

    override suspend fun getAllExpenseForRecurringExpense(
        recurringExpense: RecurringExpense
    ): List<Expense> {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("getAllExpenseForRecurringExpense called with a recurring expense that has no id")

        return roomDB.expenseDao()
            .getAllExpenseForRecurringExpense(recurringExpenseId, appPreferences.activeAccount())
            .toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense, fromDate: LocalDate
    ) {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseFromDate called with a recurring expense that has no id")

        return roomDB.expenseDao()
            .deleteAllExpenseForRecurringExpenseFromDate(recurringExpenseId, fromDate)
    }

    override suspend fun getAllExpensesForRecurringExpenseFromDate(
        recurringExpense: RecurringExpense, fromDate: LocalDate
    ): List<Expense> {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseFromDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseFromDate(
            recurringExpenseId, fromDate, appPreferences.activeAccount()
        ).toExpenses(this)
    }

    override suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ) {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("deleteAllExpenseForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().deleteAllExpenseForRecurringExpenseBeforeDate(
            recurringExpenseId, beforeDate, appPreferences.activeAccount()
        )
    }

    override suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ): List<Expense> {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("getAllExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().getAllExpensesForRecurringExpenseBeforeDate(
            recurringExpenseId, beforeDate, appPreferences.activeAccount()
        ).toExpenses(this)
    }

    override suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpense: RecurringExpense, beforeDate: LocalDate
    ): Boolean {
        val recurringExpenseId = recurringExpense.id
            ?: throw IllegalArgumentException("hasExpensesForRecurringExpenseBeforeDate called with a recurring expense that has no id")

        return roomDB.expenseDao().hasExpensesForRecurringExpenseBeforeDate(
            recurringExpenseId, beforeDate, appPreferences.activeAccount()
        ) > 0
    }

    override suspend fun findRecurringExpenseForId(
        recurringExpenseId: Long
    ): RecurringExpense? {
        return roomDB.expenseDao()
            .findRecurringExpenseForId(recurringExpenseId, appPreferences.activeAccount())
            ?.toRecurringExpense()
    }

    override suspend fun getOldestExpense(): Expense? {
        return roomDB.expenseDao().getOldestExpense(appPreferences.activeAccount())?.toExpense(this)
    }
}

private suspend fun List<ExpenseEntity>.toExpenses(db: DB): List<Expense> {
    return map { it.toExpense(db) }
}

private suspend fun ExpenseEntity.toExpense(db: DB): Expense {
    val recurringExpense = this.associatedRecurringExpenseId?.let { id ->
        db.findRecurringExpenseForId(id)
    }
    return toExpense(recurringExpense)
}