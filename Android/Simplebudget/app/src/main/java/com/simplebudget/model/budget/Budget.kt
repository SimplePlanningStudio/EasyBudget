/*
 *   Copyright 2024 Waheed Nazir
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
package com.simplebudget.model.budget

import android.os.Parcel
import android.os.Parcelable
import com.simplebudget.model.recurringexpense.RecurringExpenseType
import java.time.LocalDate

data class Budget(
    val id: Long?,
    val goal: String,
    val accountId: Long,
    val categoryId: Long,
    val budgetAmount: Double,
    val remainingAmount: Double,
    val spentAmount: Double,
    val date: LocalDate,
    val associatedRecurringBudget: RecurringBudget?,
    val categoryName: String = ""
) : Parcelable {

    constructor(
        goal: String,
        accountId: Long,
        categoryId: Long,
        budgetAmount: Double,
        remainingAmount: Double,
        spentAmount: Double,
        date: LocalDate,
        associatedRecurringBudget: RecurringBudget?,
        categoryName: String = ""
    ) : this(
        null,
        goal,
        accountId,
        categoryId,
        budgetAmount,
        remainingAmount,
        spentAmount,
        date,
        associatedRecurringBudget,
        categoryName
    )

    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readParcelable(RecurringBudget::class.java.classLoader),
        parcel.readString() ?: "",
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(goal)
        parcel.writeLong(accountId)
        parcel.writeLong(categoryId)
        parcel.writeDouble(budgetAmount)
        parcel.writeDouble(remainingAmount)
        parcel.writeDouble(spentAmount)
        parcel.writeLong(date.toEpochDay())
        parcel.writeParcelable(associatedRecurringBudget, flags)
        parcel.writeString(categoryName)
    }

    companion object CREATOR : Parcelable.Creator<Budget> {
        override fun createFromParcel(parcel: Parcel): Budget {
            return Budget(parcel)
        }

        override fun newArray(size: Int): Array<Budget?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "Budget(" +
                "id=$id, " +
                "goal='$goal', " +
                "accountId=$accountId, " +
                "categoryId=$categoryId, " +
                "budgetAmount=$budgetAmount, " +
                "remainingAmount=$remainingAmount, " +
                "spentAmount=$spentAmount, " +
                "date=$date, " +
                "associatedRecurringBudget=${associatedRecurringBudget.toString()}, " +
                "categoryName='$categoryName')"
    }
}