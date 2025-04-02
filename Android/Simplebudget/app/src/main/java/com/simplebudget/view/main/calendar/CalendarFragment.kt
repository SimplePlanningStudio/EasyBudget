/*
 *   Copyright 2025 Benoit LETONDOR
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
package com.simplebudget.view.main.calendar

import com.simplebudget.db.DB
import com.roomorama.caldroid.CaldroidFragment
import com.roomorama.caldroid.CaldroidGridAdapter
import com.simplebudget.prefs.AppPreferences
import org.koin.android.ext.android.inject
import java.time.LocalDate


class CalendarFragment : CaldroidFragment() {
    private var mSelectedDate = LocalDate.now()

    private val db: DB by inject()

    private val appPreferences: AppPreferences by inject()

// --------------------------------------->

    override fun getNewDatesGridAdapter(month: Int, year: Int): CaldroidGridAdapter {
        return CalendarGridAdapter(
            requireContext(),
            db,
            appPreferences,
            month,
            year,
            getCaldroidData(),
            extraData
        )
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    override fun setSelectedDate(date: LocalDate) {
        this.mSelectedDate = date

        super.clearSelectedDates()
        super.setSelectedDate(date)

        try {
            // Exception that occurs if we call this code before the calendar being initialized
            super.moveToDate(date)
        } catch (ignored: Exception) {
        }
    }

    fun getSelectedDate() = mSelectedDate

    fun setFirstDayOfWeek(firstDayOfWeek: Int) {
        if (firstDayOfWeek != startDayOfWeek) {
            startDayOfWeek = firstDayOfWeek
            val weekdaysAdapter = getNewWeekdayAdapter(themeResource)
            weekdayGridView.adapter = weekdaysAdapter
            nextMonth()
            prevMonth()
        }
    }

    fun goToCurrentMonth() {
        try {
            // Exception that occurs if we call this code before the calendar being initialized
            super.moveToDate(LocalDate.now())
        } catch (ignored: Exception) {
        }
    }
}
