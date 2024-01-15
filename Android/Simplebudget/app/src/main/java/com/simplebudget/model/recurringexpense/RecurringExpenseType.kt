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
package com.simplebudget.model.recurringexpense

import android.widget.Spinner


enum class RecurringExpenseType {
    /**
     * An expense that occurs every day
     */
    NOTHING,

    /**
     * An expense that occurs every day
     */
    DAILY,

    /**
     * An expense that occurs every week
     */
    WEEKLY,

    /**
     * An expense that occurs every 2 weeks
     */
    BI_WEEKLY,

    /**
     * An expense that occurs every 3 weeks
     */
    TER_WEEKLY,

    /**
     * An expense that occurs every 4 weeks
     */
    FOUR_WEEKLY,

    /**
     * An expense that occurs every month
     */
    MONTHLY,

    /**
     * An expense that occurs every 2 months
     */
    BI_MONTHLY,

    /**
     * An expense that occurs every 3 months
     */
    TER_MONTHLY,

    /**
     * An expense that occurs every 6 months
     */
    SIX_MONTHLY,

    /**
     * An expense that occurs once a year
     */
    YEARLY
}


object ExpenseType {
    /**
     * Get the recurring expense type associated with the spinner selection
     *
     * @param spinnerSelectedItem index of the spinner selection
     * @return the corresponding expense type
     */
    fun getRecurringTypeFromSpinnerSelection(spinnerSelectedItem: Int): RecurringExpenseType {
        return when (spinnerSelectedItem) {
            0 -> RecurringExpenseType.DAILY
            1 -> RecurringExpenseType.WEEKLY
            2 -> RecurringExpenseType.BI_WEEKLY
            3 -> RecurringExpenseType.TER_WEEKLY
            4 -> RecurringExpenseType.FOUR_WEEKLY
            5 -> RecurringExpenseType.MONTHLY
            6 -> RecurringExpenseType.BI_MONTHLY
            7 -> RecurringExpenseType.TER_MONTHLY
            8 -> RecurringExpenseType.SIX_MONTHLY
            9 -> RecurringExpenseType.YEARLY
            else -> RecurringExpenseType.NOTHING
        }
    }

    fun setSpinnerSelectionFromRecurringType(
        type: RecurringExpenseType,
        spinner: Spinner
    ) {
        val selectionIndex = when (type) {
            RecurringExpenseType.DAILY -> 0
            RecurringExpenseType.WEEKLY -> 1
            RecurringExpenseType.BI_WEEKLY -> 2
            RecurringExpenseType.TER_WEEKLY -> 3
            RecurringExpenseType.FOUR_WEEKLY -> 4
            RecurringExpenseType.MONTHLY -> 5
            RecurringExpenseType.BI_MONTHLY -> 6
            RecurringExpenseType.TER_MONTHLY -> 7
            RecurringExpenseType.SIX_MONTHLY -> 8
            RecurringExpenseType.YEARLY -> 9
            else -> 0
        }
        spinner.setSelection(selectionIndex, false)
    }
}
