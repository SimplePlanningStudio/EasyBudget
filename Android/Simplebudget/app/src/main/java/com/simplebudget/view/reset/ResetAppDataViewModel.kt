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
package com.simplebudget.view.reset

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ResetAppDataViewModel(
    private val db: DB, private val appPreferences: AppPreferences
) : ViewModel() {
    /**
     * Clear all app data stream
     */
    val clearDataEventStream = MutableLiveData<Boolean>()
    val progress = MutableLiveData<Boolean?>()


    /**
     * Clear database tables, data remove all preferences data as well
     *
     */
    fun clearAppData() {
        progress.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.clearAllTables()
                appPreferences.clearAllPreferencesData()
                clearDataEventStream.postValue(true)
                progress.postValue(false)
            }
        }
    }
}