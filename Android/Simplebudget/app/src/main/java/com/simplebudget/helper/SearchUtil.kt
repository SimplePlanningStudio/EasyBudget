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
package com.simplebudget.helper

object SearchUtil {

    const val TODAY = "Today"
    const val YESTERDAY = "Yesterday"
    const val TOMORROW = "Tomorrow"
    const val THIS_WEEK = "This week"
    const val LAST_WEEK = "Last week"
    const val THIS_MONTH = "This month"
    const val PICK_A_DATE = "Pick a date"
    const val PICK_A_DATE_RANGE = "Pick a date range"

    /**
     * Return list of hardcoded searches as a top searches
     */
    fun getTopSearches(): List<String> {
        return listOf(
            TODAY,
            YESTERDAY,
            TOMORROW,
            THIS_WEEK,
            LAST_WEEK,
            THIS_MONTH,
            PICK_A_DATE,
            PICK_A_DATE_RANGE
        )
    }
}