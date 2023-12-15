/*
 *   Copyright 2023 Benoit LETONDOR
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

import android.content.Context
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.MaterialDatePicker

import com.simplebudget.R
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.ArrayList
import java.util.Locale


object DateHelper {
    /**
     * Get the list of future months available for the user for the monthly report view.
     *
     * @return a list of Date object set at the 1st day of the month 00:00:00:000
     */
    fun getListOfFutureMonthsAvailableForUser(): List<LocalDate> {
        val today = LocalDate.now()
        val months = ArrayList<LocalDate>()
        var currentDate = LocalDate.of(today.year, today.month, 1)

        for (i in 1..10) {
            months.add(currentDate)
            currentDate = currentDate.plusMonths(1)
        }
        return months
    }

    val today = LocalDate.now()

    val yesterday = today.minusDays(1)

    val tomorrow = today.plusDays(1)

    val firstDayOfThisWeek: LocalDate = today.with(ChronoField.DAY_OF_WEEK, 1)

    val firstDayOfLastWeek: LocalDate = firstDayOfThisWeek.minusWeeks(1)

    val lastDayOfLastWeek: LocalDate = firstDayOfThisWeek.minusDays(1)

    val startDayOfMonth: LocalDate = today.withDayOfMonth(1)

    val lastThreeMonth: LocalDate = today.minusMonths(3)

    val lastOneYear: LocalDate = today.minusYears(1)

    val endDayOfMonth: LocalDate = startDayOfMonth.plusMonths(1).minusDays(1)
}

/**
 * Get the list of months available for the user for the monthly report view.
 *
 * @return a list of Date object set at the 1st day of the month 00:00:00:000
 */
fun AppPreferences.getListOfMonthsAvailableForUser(): Pair<List<LocalDate>, Int> {
    var currentMonthPosition = 0
    val initDate = getInitDate() ?: DateHelper.today
    val today = LocalDate.now()
    val months = ArrayList<LocalDate>()
    var currentDate = LocalDate.of(initDate.year, initDate.month, 1)
    // Minus 24 months for past So that user can see report of past 24 month
    currentDate = currentDate.minusMonths(24)

    while (currentDate.isBefore(today) || currentDate == today) {
        months.add(currentDate)
        currentDate = currentDate.plusMonths(1)
    }
    // Add 24 more months for future so that user can see future months report.
    for (i in 1..24) {
        months.add(currentDate)
        currentDate = currentDate.plusMonths(1)
    }

    months.forEachIndexed { index, localDate ->
        if (localDate.month == today.month && localDate.year == today.year) {
            currentMonthPosition = index
        }
    }

    return Pair(months, currentMonthPosition)
}

fun LocalDate.computeCalendarMinDateFromInitDate(): LocalDate = minusYears(1)

/**
 * Get the title of the month to display in the report view
 *
 * @param context non null context
 * @return a formatted string like "January 2016"
 */
fun LocalDate.getMonthTitle(context: Context): String {
    val format = DateTimeFormatter.ofPattern(
        context.resources.getString(R.string.monthly_report_month_title_format),
        Locale.getDefault()
    )
    return format.format(this)
}

/**
 * Get the formatted date
 *
 * @param context non null context
 * @return a formatted string like "04 Dec 2023"
 */
fun LocalDate.getFormattedDate(context: Context): String {
    val format = DateTimeFormatter.ofPattern(
        context.resources.getString(R.string.budgets_date_format),
        Locale.getDefault()
    )
    return format.format(this)
}

/**
 * Get the title of the month to display in the report view
 *
 * @param context non null context
 * @return a formatted string like "January 2016"
 */
fun LocalDate.getMonthTitleWithPastAndFuture(context: Context): String {
    val format = DateTimeFormatter.ofPattern(
        context.resources.getString(R.string.monthly_report_month_title_format),
        Locale.getDefault()
    )
    val dateTitle = format.format(this)
    val today = LocalDate.now()
    return if (this.month == today.month && this.year == today.year)
        dateTitle
    else if (LocalDate.now().minusMonths(1).isBefore(this))
        String.format("%s %s", dateTitle, "(Future)")
    else String.format("%s %s", dateTitle, "(Past)")
}


/**
 * Single Material date picker
 * Your app or activity theme must be material in order to use this date picker
 */
fun FragmentActivity.pickSingleDate(onDateSet: (LocalDate) -> Unit) {
    val datePicker: MaterialDatePicker<Long> = MaterialDatePicker
        .Builder
        .datePicker()
        .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
        .setTitleText("Pick a date")
        .build()
    datePicker.isCancelable = false
    datePicker.show(this.supportFragmentManager, "DATE_PICKER")
    datePicker.addOnPositiveButtonClickListener { long ->
        onDateSet.invoke(localDateFromTimestamp(long))
    }
}

/**
 * Your app or activity theme must be material in order to use this date picker
 */
fun FragmentActivity.pickDateRange(onDateSet: (Pair<LocalDate, LocalDate>) -> Unit) {
    val dateRange: MaterialDatePicker<Pair<Long, Long>> = MaterialDatePicker
        .Builder
        .dateRangePicker()
        .setTitleText("Select date range")
        .build()
    dateRange.show(this.supportFragmentManager, "DATE_RANGE_PICKER")
    dateRange.addOnPositiveButtonClickListener { dates ->
        onDateSet.invoke(
            Pair(
                localDateFromTimestamp(dates.first),
                localDateFromTimestamp(dates.second)
            )
        )
    }
}