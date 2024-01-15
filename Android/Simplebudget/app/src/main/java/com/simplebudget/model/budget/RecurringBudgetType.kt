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
package com.simplebudget.model.budget

import android.widget.Spinner


enum class RecurringBudgetType {
    /**
     * An expense that occurs every month
     */
    MONTHLY,
}


object BudgetType {
    /**
     * Get the recurring expense type associated with the spinner selection
     *
     * @param spinnerSelectedItem index of the spinner selection
     * @return the corresponding expense type
     */
    fun getRecurringTypeFromSpinnerSelection(spinnerSelectedItem: Int): RecurringBudgetType {
        return when (spinnerSelectedItem) {
            0 -> RecurringBudgetType.MONTHLY
            else -> RecurringBudgetType.MONTHLY
        }
    }

    fun setSpinnerSelectionFromRecurringType(
        type: RecurringBudgetType,
        spinner: Spinner
    ) {
        val selectionIndex = when (type) {
            RecurringBudgetType.MONTHLY -> 0
            else -> 0
        }
        spinner.setSelection(selectionIndex, false)
    }
}
