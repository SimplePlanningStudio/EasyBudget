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