package com.simplebudget.helper

import com.simplebudget.model.ExpenseCategoryType

object GetPositionOfCategoryTypeForSpinner {

    fun get(type: ExpenseCategoryType): Int {
        return when (type) {
            ExpenseCategoryType.MISCELLANEOUS -> 0
            ExpenseCategoryType.GROCERY -> 1
            ExpenseCategoryType.UTILITIES -> 2
            ExpenseCategoryType.SHOPPING -> 3
            ExpenseCategoryType.CLOTHING -> 4
            ExpenseCategoryType.ENTERTAINMENT -> 5
            ExpenseCategoryType.TRANSPORTATION -> 6
            ExpenseCategoryType.EDUCATION -> 7
            ExpenseCategoryType.MEDICAL -> 8
            ExpenseCategoryType.HOUSEHOLD -> 9
            ExpenseCategoryType.FOOD -> 10
            ExpenseCategoryType.BREAKFAST -> 11
            ExpenseCategoryType.DINNER -> 12
            ExpenseCategoryType.LUNCH -> 13
            ExpenseCategoryType.PERSONAL -> 14
            ExpenseCategoryType.INSURANCE -> 15
            ExpenseCategoryType.RETIREMENT -> 16
            ExpenseCategoryType.DONATIONS -> 17
            ExpenseCategoryType.SALARY -> 18
            ExpenseCategoryType.INCOME -> 19
            ExpenseCategoryType.PROFIT -> 20
            ExpenseCategoryType.DEBIT -> 21
            ExpenseCategoryType.CREDIT -> 22
            ExpenseCategoryType.DEBT -> 23
            ExpenseCategoryType.BALANCE -> 24
            ExpenseCategoryType.SAVINGS -> 25
        }
    }
}