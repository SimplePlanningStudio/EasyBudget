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
package com.simplebudget.model.expense

import android.os.Parcel
import android.os.Parcelable
import com.simplebudget.model.account.AccountType
import com.simplebudget.model.account.Accounts
import com.simplebudget.model.recurringexpense.RecurringExpense
import com.simplebudget.model.category.ExpenseCategoryType
import com.simplebudget.prefs.activeAccount
import java.time.LocalDate

data class Expense(
    val id: Long?,
    val title: String,
    val amount: Double,
    val date: LocalDate,
    val associatedRecurringExpense: RecurringExpense?,
    val category: String,
    val accountId: Long
) : Parcelable {

    constructor(
        title: String,
        amount: Double,
        date: LocalDate,
        category: String,
        accountId: Long
    ) : this(null, title, amount, date, null, category, accountId)

    constructor(
        id: Long,
        title: String,
        amount: Double,
        date: LocalDate,
        category: String,
        accountId: Long
    ) : this(id, title, amount, date, null, category, accountId)

    constructor(
        title: String,
        amount: Double,
        date: LocalDate,
        associatedRecurringExpense: RecurringExpense,
        category: String,
        accountId: Long
    ) : this(
        null,
        title,
        amount,
        date,
        associatedRecurringExpense,
        category,
        accountId
    )

    private constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readDouble(),
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readParcelable(RecurringExpense::class.java.classLoader),
        parcel.readString() ?: ExpenseCategoryType.MISCELLANEOUS.name,
        parcel.readLong()
    )

    init {
        if (title.isEmpty()) {
            throw IllegalArgumentException("title is empty")
        }
    }

    fun isRevenue() = amount < 0

    fun isFutureExpense() = date.isAfter(LocalDate.now())
    fun isPastExpense() = date.isBefore(LocalDate.now())
    fun isRecurring() = associatedRecurringExpense != null
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(title)
        parcel.writeDouble(amount)
        parcel.writeLong(date.toEpochDay())
        parcel.writeParcelable(associatedRecurringExpense, flags)
        parcel.writeString(category)
        parcel.writeLong(accountId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Expense> {
        override fun createFromParcel(parcel: Parcel): Expense {
            return Expense(parcel)
        }

        override fun newArray(size: Int): Array<Expense?> {
            return arrayOfNulls(size)
        }
    }

}