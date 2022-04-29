/*
 *   Copyright 2022 Waheed Nazir
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
package com.simplebudget.view.futurepayments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.helper.DateHelper.getListOfFutureMonthsAvailableForUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FutureBaseViewModel() : ViewModel() {
    /**
     * The current selected position
     */
    val selectedPositionLiveData = MutableLiveData<FutureSelectedPosition>()
    val datesLiveData = MutableLiveData<List<Date>>()

    fun loadData() {
        viewModelScope.launch {
            val dates = withContext(Dispatchers.IO) {
                return@withContext getListOfFutureMonthsAvailableForUser()
            }

            datesLiveData.value = dates
            selectedPositionLiveData.value = FutureSelectedPosition(
                0, dates[0], showNextButton = true,
                showPreviousButton = false
            )
        }
    }

    fun onPreviousMonthButtonClicked() {
        val (selectedPosition) = selectedPositionLiveData.value ?: return
        if (selectedPosition > 0) {
            onPageSelected(selectedPosition - 1)
        }
    }

    fun onNextMonthButtonClicked() {
        val dates = datesLiveData.value ?: return
        val (selectedPosition) = selectedPositionLiveData.value ?: return
        if (selectedPosition < dates.size - 1) {
            onPageSelected(selectedPosition + 1)
        }
    }

    fun onPageSelected(position: Int) {
        val dates = datesLiveData.value ?: return
        selectedPositionLiveData.value =
            FutureSelectedPosition(
                position,
                dates[position],
                (dates.size != position + 1),
                (position != 0)
            )
    }
}

data class FutureSelectedPosition(
    val position: Int,
    val date: Date,
    val showNextButton: Boolean,
    val showPreviousButton: Boolean
)