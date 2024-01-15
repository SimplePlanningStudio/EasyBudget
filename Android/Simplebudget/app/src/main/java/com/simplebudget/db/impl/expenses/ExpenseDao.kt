/*
 *   Copyright 2024 Benoit LETONDOR
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
package com.simplebudget.db.impl.expenses

import androidx.room.*
import com.simplebudget.db.impl.recurringexpenses.RecurringExpenseEntity
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery
import java.time.LocalDate

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistExpense(expenseEntity: ExpenseEntity): Long

    @Query("SELECT COUNT(*) FROM expense WHERE date = :dayDate AND accountId = :accountId LIMIT 1")
    suspend fun hasExpenseForDay(dayDate: LocalDate, accountId: Long): Int

    @Query("SELECT * FROM expense WHERE date = :dayDate AND accountId = :accountId")
    suspend fun getExpensesForDay(dayDate: LocalDate, accountId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE date >= :monthStartDate AND date <= :monthEndDate AND accountId = :accountId")
    suspend fun getExpensesForMonth(
        monthStartDate: LocalDate,
        monthEndDate: LocalDate,
        accountId: Long
    ): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE date >= :monthStartDate AND date <= :monthEndDate")
    suspend fun getExpensesForMonthWithoutCheckingAccount(
        monthStartDate: LocalDate,
        monthEndDate: LocalDate,
    ): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expense WHERE date <= :dayDate AND accountId = :accountId")
    suspend fun getBalanceForDay(dayDate: LocalDate, accountId: Long): Long?

    @Query("SELECT SUM(amount) FROM expense WHERE date >= :monthStartDate AND date <= :dayDate AND accountId = :accountId AND category = :category")
    suspend fun getBalanceForACategory(monthStartDate: LocalDate, dayDate: LocalDate, accountId: Long, category: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistRecurringExpense(recurringExpenseEntity: RecurringExpenseEntity): Long

    @Delete
    suspend fun deleteRecurringExpense(recurringExpenseEntity: RecurringExpenseEntity)

    @Delete
    suspend fun deleteExpense(expenseEntity: ExpenseEntity)

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId")
    suspend fun deleteAllExpenseForRecurringExpense(recurringExpenseId: Long)

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND accountId = :accountId")
    suspend fun getAllExpenseForRecurringExpense(
        recurringExpenseId: Long,
        accountId: Long
    ): List<ExpenseEntity>

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date > :fromDate")
    suspend fun deleteAllExpenseForRecurringExpenseFromDate(
        recurringExpenseId: Long,
        fromDate: LocalDate
    )

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND date > :fromDate AND accountId = :accountId")
    suspend fun getAllExpensesForRecurringExpenseFromDate(
        recurringExpenseId: Long,
        fromDate: LocalDate,
        accountId: Long
    ): List<ExpenseEntity>

    @Query("DELETE FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate AND accountId = :accountId")
    suspend fun deleteAllExpenseForRecurringExpenseBeforeDate(
        recurringExpenseId: Long,
        beforeDate: LocalDate,
        accountId: Long
    )

    @Query("SELECT * FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate AND accountId = :accountId")
    suspend fun getAllExpensesForRecurringExpenseBeforeDate(
        recurringExpenseId: Long,
        beforeDate: LocalDate,
        accountId: Long
    ): List<ExpenseEntity>

    @Query("SELECT count(*) FROM expense WHERE monthly_id = :recurringExpenseId AND date < :beforeDate AND accountId = :accountId LIMIT 1")
    suspend fun hasExpensesForRecurringExpenseBeforeDate(
        recurringExpenseId: Long,
        beforeDate: LocalDate,
        accountId: Long
    ): Int

    @Query("SELECT * FROM monthlyexpense WHERE _expense_id = :recurringExpenseId AND accountId = :accountId LIMIT 1")
    suspend fun findRecurringExpenseForId(
        recurringExpenseId: Long,
        accountId: Long
    ): RecurringExpenseEntity?

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int

    @Query("SELECT * FROM expense ORDER BY date AND accountId = :accountId LIMIT 1")
    suspend fun getOldestExpense(accountId: Long): ExpenseEntity?

    // SELECT * FROM user WHERE name LIKE :searchQuery
    @Query("SELECT * FROM expense WHERE date >= :startDate AND date <= :endDate AND accountId = :accountId AND (UPPER(title) LIKE UPPER(:search_query) OR UPPER(category) LIKE UPPER(:search_query) OR amount = :amount OR amount = :minusAmount)")
    suspend fun searchExpenses(search_query: String,startDate: LocalDate, endDate: LocalDate, accountId: Long, amount:Long, minusAmount:Long): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE date >= :startDate AND date <= :endDate AND accountId = :accountId")
    suspend fun getAllExpenses(
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long
    ): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE accountId = :accountId")
    suspend fun getAllExpenses(accountId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expense WHERE date > :todayDate AND accountId = :accountId")
    suspend fun getAllFutureExpenses(
        todayDate: LocalDate,
        accountId: Long
    ): List<ExpenseEntity>


    @Query("DELETE FROM expense WHERE accountId = :accountId")
    suspend fun deleteAllExpenses(accountId: Long)

    @Query("DELETE FROM monthlyexpense WHERE accountId = :accountId")
    suspend fun deleteAllRecurringExpenses(accountId: Long)

    // Update query example
    /*@Query("UPDATE expense SET accountId = :accountId")
    suspend fun updateAccountType(accountId: Long)*/
}