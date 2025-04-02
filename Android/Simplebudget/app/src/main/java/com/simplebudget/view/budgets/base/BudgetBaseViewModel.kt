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
package com.simplebudget.view.budgets.base

import androidx.core.util.Pair
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.getListOfMonthsAvailableForUser
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.ArrayList

class BudgetBaseViewModel(private val appPreferences: AppPreferences, private val db: DB) :
    ViewModel() {
    /**
     * The current selected position
     */
    val selectedPositionLiveData = MutableLiveData<BudgetSelectedPosition>()
    val datesLiveData = MutableLiveData<List<LocalDate>>()


    fun loadData() {
        viewModelScope.launch {
            val pair = withContext(Dispatchers.IO) {
                var currentMonthPosition = 0
                val today = DateHelper.today
                val initDate = db.getOldestBudgetStartDate() ?: today
                val months = ArrayList<LocalDate>()
                var currentDate = LocalDate.of(initDate.year, initDate.month, 1)
                while (currentDate.isBefore(today) || currentDate == today) {
                    months.add(currentDate)
                    currentDate = currentDate.plusMonths(1)
                }
                months.forEachIndexed { index, localDate ->
                    if (localDate.month == today.month && localDate.year == today.year) {
                        currentMonthPosition = index
                    }
                }
                Pair(months, currentMonthPosition)
            }
            if (pair.first.isNotEmpty()) {
                datesLiveData.value = pair.first
                selectedPositionLiveData.value = BudgetSelectedPosition(
                    pair.second, pair.first[pair.second], (pair.second == pair.first.size - 1)
                )
            }
        }
    }

    fun onPreviousMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return

        if (selectedPosition > 0) {
            selectedPositionLiveData.value = BudgetSelectedPosition(
                selectedPosition - 1,
                dates[selectedPosition - 1],
                (selectedPosition - 1 == dates.size - 1)
            )
        }
    }

    fun onNextMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return

        if (selectedPosition < dates.size - 1) {
            selectedPositionLiveData.value = BudgetSelectedPosition(
                selectedPosition + 1,
                dates[selectedPosition + 1],
                (selectedPosition + 1 == dates.size - 1)
            )
        }
    }

    fun onPageSelected(position: Int) {
        val dates = datesLiveData.value ?: return

        selectedPositionLiveData.value =
            BudgetSelectedPosition(position, dates[position], (position == dates.size - 1))
    }
}

data class BudgetSelectedPosition(
    val position: Int,
    val date: LocalDate,
    val isLastMonth: Boolean,
)