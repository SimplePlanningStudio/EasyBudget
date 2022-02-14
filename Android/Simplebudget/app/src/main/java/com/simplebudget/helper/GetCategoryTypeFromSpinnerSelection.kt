package com.simplebudget.helper

import com.simplebudget.model.ExpenseCategoryType

object CategoryTypeFromSpinnerSelection {

    fun get(spinnerSelectedItem: Int): ExpenseCategoryType {
        when (spinnerSelectedItem) {
            0 -> return ExpenseCategoryType.MISCELLANEOUS
            1 -> return ExpenseCategoryType.GROCERY
            2 -> return ExpenseCategoryType.UTILITIES
            3 -> return ExpenseCategoryType.SHOPPING
            4 -> return ExpenseCategoryType.CLOTHING
            5 -> return ExpenseCategoryType.ENTERTAINMENT
            6 -> return ExpenseCategoryType.TRANSPORTATION
            7 -> return ExpenseCategoryType.EDUCATION
            8 -> return ExpenseCategoryType.MEDICAL
            9 -> return ExpenseCategoryType.HOUSEHOLD
            10 -> return ExpenseCategoryType.FOOD
            11 -> return ExpenseCategoryType.BREAKFAST
            12 -> return ExpenseCategoryType.DINNER
            13 -> return ExpenseCategoryType.LUNCH
            14 -> return ExpenseCategoryType.PERSONAL
            15 -> return ExpenseCategoryType.INSURANCE
            16 -> return ExpenseCategoryType.RETIREMENT
            17 -> return ExpenseCategoryType.DONATIONS
            18 -> return ExpenseCategoryType.SALARY
            19 -> return ExpenseCategoryType.INCOME
            20 -> return ExpenseCategoryType.PROFIT
            21 -> return ExpenseCategoryType.DEBIT
            22 -> return ExpenseCategoryType.CREDIT
            23 -> return ExpenseCategoryType.DEBT
            24 -> return ExpenseCategoryType.BALANCE
            else -> return ExpenseCategoryType.SAVINGS
        }
    }
}