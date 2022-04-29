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
package com.simplebudget.view.search.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.helper.getListOfMonthsAvailableForUser
import com.simplebudget.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SearchBaseViewModel(private val appPreferences: AppPreferences) : ViewModel() {
    /**
     * The current selected position
     */
    val selectedPositionLiveData = MutableLiveData<SearchSelectedPosition>()
    val datesLiveData = MutableLiveData<List<Date>>()

    fun loadData(fromNotification: Boolean) {
        viewModelScope.launch {
            val dates = withContext(Dispatchers.IO) {
                return@withContext appPreferences.getListOfMonthsAvailableForUser()
            }

            datesLiveData.value = dates
            if (!fromNotification || dates.size == 1) {
                selectedPositionLiveData.value =
                    SearchSelectedPosition(dates.size - 1, dates[dates.size - 1], true)
            } else {
                selectedPositionLiveData.value =
                    SearchSelectedPosition(dates.size - 2, dates[dates.size - 2], false)
            }
        }
    }

    fun onPageSelected(position: Int) {
        val dates = datesLiveData.value ?: return

        selectedPositionLiveData.value =
            SearchSelectedPosition(position, dates[position], dates.size == position + 1)
    }
}

data class SearchSelectedPosition(val position: Int, val date: Date, val latest: Boolean)