/*
 *   Copyright 2025 Waheed Nazir
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
import com.simplebudget.model.category.Category
import java.time.LocalDate

data class Budget(
    val id: Long?,
    val goal: String,
    val accountId: Long,
    val budgetAmount: Double,
    val remainingAmount: Double,
    val spentAmount: Double,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val associatedRecurringBudget: RecurringBudget?,
    val categories: List<Category>,
) : Parcelable {

    constructor(
        goal: String,
        accountId: Long,
        budgetAmount: Double,
        remainingAmount: Double,
        spentAmount: Double,
        startDate: LocalDate,
        endDate: LocalDate,
        associatedRecurringBudget: RecurringBudget?,
        categories: List<Category>,
    ) : this(
        null,
        goal,
        accountId,
        budgetAmount,
        remainingAmount,
        spentAmount,
        startDate,
        endDate,
        associatedRecurringBudget,
        categories
    )

    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        LocalDate.ofEpochDay(parcel.readLong()),
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readParcelable(RecurringBudget::class.java.classLoader),
        categories = parcel.createTypedArrayList(Category.CREATOR)!!
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(goal)
        parcel.writeLong(accountId)
        parcel.writeDouble(budgetAmount)
        parcel.writeDouble(remainingAmount)
        parcel.writeDouble(spentAmount)
        parcel.writeLong(startDate.toEpochDay())
        parcel.writeLong(endDate.toEpochDay())
        parcel.writeParcelable(associatedRecurringBudget, flags)
        parcel.writeTypedList(categories)

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
                "budgetAmount=$budgetAmount, " +
                "remainingAmount=$remainingAmount, " +
                "spentAmount=$spentAmount, " +
                "StartDate=$startDate, " +
                "EndDate=$endDate, " +
                "associatedRecurringBudget=${associatedRecurringBudget.toString()})"
    }
}