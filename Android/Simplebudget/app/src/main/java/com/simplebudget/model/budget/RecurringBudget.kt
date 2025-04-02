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
import java.time.LocalDate

data class RecurringBudget(
    val id: Long?,
    val goal: String,
    val accountId: Long,
    val budgetAmount: Double,
    val type: RecurringBudgetType,
    val recurringDate: LocalDate,
    val modified: Boolean,
) : Parcelable {

    constructor(
        accountId: Long,
        goal: String,
        budgetAmount: Double,
        type: RecurringBudgetType,
        recurringDate: LocalDate,
        modified: Boolean,
    ) : this(
        null,
        goal,
        accountId,
        budgetAmount,
        type,
        recurringDate,
        modified
    )

    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readDouble(),
        RecurringBudgetType.values()[parcel.readInt()],
        LocalDate.ofEpochDay(parcel.readLong()),
        parcel.readByte() != 0.toByte()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, p1: Int) {
        parcel.writeValue(id)
        parcel.writeString(goal)
        parcel.writeLong(accountId)
        parcel.writeDouble(budgetAmount)
        parcel.writeInt(type.ordinal)
        parcel.writeLong(recurringDate.toEpochDay())
        parcel.writeByte(if (modified) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<RecurringBudget> {
        override fun createFromParcel(parcel: Parcel): RecurringBudget {
            return RecurringBudget(parcel)
        }

        override fun newArray(size: Int): Array<RecurringBudget?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "RecurringBudget(" +
                "id=$id, " +
                "goal='$goal', " +
                "accountId=$accountId, " +
                "budgetAmount=$budgetAmount, " +
                "recurringDate=$recurringDate, " +
                "Type='${type.name}', " +
                "modified='$modified')"
    }
}