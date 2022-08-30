/*
 *   Copyright 2022 Benoit LETONDOR
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

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker

import com.simplebudget.R
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitTimestamp

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Get the list of months available for the user for the monthly report view.
 *
 * @return a list of Date object set at the 1st day of the month 00:00:00:000
 */
fun AppPreferences.getListOfMonthsAvailableForUser(): List<Date> {
    val initDate = getInitTimestamp()

    val cal = Calendar.getInstance()
    cal.timeInMillis = initDate

    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val today = Date()

    val months = ArrayList<Date>()

    while (cal.time.before(today)) {
        months.add(cal.time)
        cal.add(Calendar.MONTH, 1)
    }

    return months
}

object DateHelper {
    /**
     * Get the list of future months available for the user for the monthly report view.
     *
     * @return a list of Date object set at the 1st day of the month 00:00:00:000
     */
    fun getListOfFutureMonthsAvailableForUser(): List<Date> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = Date().time // Set today date

        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // cal.add(Calendar.MONTH, 1) // To take only future months, Adding one month in advance.

        val months = ArrayList<Date>()
        for (i in 1..10) {
            months.add(cal.time)
            cal.add(Calendar.MONTH, 1)
        }
        return months
    }
}

/**
 * Get the title of the month to display in the report view
 *
 * @param context non null context
 * @return a formatted string like "January 2016"
 */
fun Date.getMonthTitle(context: Context): String {
    val format = SimpleDateFormat(
        context.resources.getString(R.string.monthly_report_month_title_format),
        Locale.getDefault()
    )
    return format.format(this)
}


/**
 * Single Material date picker
 * Your app or activity theme must be material in order to use this date picker
 */
fun FragmentActivity.pickSingleDate(onDateSet: (Date) -> Unit) {
    val datePicker: MaterialDatePicker<Long> = MaterialDatePicker
        .Builder
        .datePicker()
        .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
        .setTitleText("Pick a date")
        .build()
    datePicker.isCancelable = false
    datePicker.show(this.supportFragmentManager, "DATE_PICKER")
    datePicker.addOnPositiveButtonClickListener { long ->
        onDateSet.invoke(Date(long))
    }
}

/**
 * Your app or activity theme must be material in order to use this date picker
 */
fun FragmentActivity.pickDateRange(onDateSet: (Pair<Date, Date>) -> Unit) {
    val dateRange: MaterialDatePicker<Pair<Long, Long>> = MaterialDatePicker
        .Builder
        .dateRangePicker()
        .setTitleText("Select date range")
        .build()
    dateRange.show(this.supportFragmentManager, "DATE_RANGE_PICKER")
    dateRange.addOnPositiveButtonClickListener { dates ->
        onDateSet.invoke(Pair(Date(dates.first), Date(dates.second)))
    }
}