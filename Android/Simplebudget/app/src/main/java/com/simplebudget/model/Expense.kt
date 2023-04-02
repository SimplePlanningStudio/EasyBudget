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
package com.simplebudget.model

import android.os.Parcel
import android.os.Parcelable
import java.time.LocalDate
import java.util.*

data class Expense(
    val id: Long?,
    val title: String,
    val amount: Double,
    val date: LocalDate,
    val associatedRecurringExpense: RecurringExpense?,
    val category: String
) : Parcelable {

    constructor(
        title: String,
        amount: Double,
        date: LocalDate,
        category: String
    ) : this(null, title, amount, date, null, category)

    constructor(
        id: Long,
        title: String,
        amount: Double,
        date: LocalDate,
        category: String
    ) : this(id, title, amount, date, null, category)

    constructor(
        title: String,
        amount: Double,
        date: LocalDate,
        associatedRecurringExpense: RecurringExpense,
        category: String
    ) : this(null, title, amount, date, associatedRecurringExpense, category)

    private constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readDouble(),
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readParcelable(RecurringExpense::class.java.classLoader),
        parcel.readString() ?: ExpenseCategoryType.MISCELLANEOUS.name
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