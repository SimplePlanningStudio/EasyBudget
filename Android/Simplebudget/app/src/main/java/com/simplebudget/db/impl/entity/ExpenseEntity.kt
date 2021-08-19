/*
 *   Copyright 2021 Benoit LETONDOR
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
package com.simplebudget.db.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplebudget.model.Expense
import com.simplebudget.model.RecurringExpense
import java.util.*

@Entity(tableName = "expense",
        indices = [Index(value = ["date"], name = "D_i")])
class ExpenseEntity(@PrimaryKey(autoGenerate = true)
                    @ColumnInfo(name = "_expense_id")
                    val id: Long?,
                    @ColumnInfo(name = "title")
                    val title: String,
                    @ColumnInfo(name = "amount")
                    val amount: Long,
                    @ColumnInfo(name = "date")
                    val date: Date,
                    @ColumnInfo(name = "monthly_id")
                    val associatedRecurringExpenseId: Long?) {

    fun toExpense(associatedRecurringExpense: RecurringExpense?) = Expense(
        id,
        title,
        amount / 100.0,
        date,
        associatedRecurringExpense
    )
}