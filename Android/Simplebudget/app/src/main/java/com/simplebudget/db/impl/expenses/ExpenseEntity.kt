/*
 *   Copyright 2025 Benoit LETONDOR
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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpense
import java.time.LocalDate

@Entity(
    tableName = "expense",
    indices = [Index(value = ["date"], name = "D_i")]
)
class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_expense_id")
    val id: Long?,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "amount")
    val amount: Long,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "monthly_id")
    val associatedRecurringExpenseId: Long?,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "accountId")
    val accountId: Long,
    @ColumnInfo(name = "categoryId")
    val categoryId: Long,
) {

    fun toExpense(associatedRecurringExpense: RecurringExpense?) = Expense(
        id,
        title,
        amount / 100.0,
        date,
        associatedRecurringExpense,
        category,
        accountId,
        categoryId
    )
}