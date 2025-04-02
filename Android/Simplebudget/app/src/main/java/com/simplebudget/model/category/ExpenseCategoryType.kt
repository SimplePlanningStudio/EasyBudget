/*
 *   Copyright 2025 Waheed Nazir
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
package com.simplebudget.model.category

import java.util.ArrayList


enum class ExpenseCategoryType {
    GROCERY, UTILITIES, SHOPPING, CLOTHING, ENTERTAINMENT, TRANSPORTATION, EDUCATION, MEDICAL, HOUSEHOLD, FOOD, BREAKFAST, DINNER, LUNCH, PERSONAL, INSURANCE, RETIREMENT, DONATIONS, SALARY, INCOME, PROFIT, DEBIT, CREDIT, DEBT, BALANCE, SAVINGS, RENT, AIRBNB, MORTGAGE, FURNITURE, GYM, TAXES, MEMBERSHIP, FUEL, TRAVEL, GIFT, CAR_PAYMENT, PARKING_FEE, REPAIRS, DENTAL_CARE, BOOKS, WEDDING, BIRTHDAY, GAMES, MOVIES, CHARITIES, SUBSCRIPTIONS, COLLEGE, HAIRCUTS, CLEANING, HEALTH_INSURANCE, LIFE_INSURANCE, AUTO_INSURANCE, MISCELLANEOUS
}


object ExpenseCategories {
    fun contains(category: String): Boolean =
        ExpenseCategoryType.values().none { this.equals(category) }

    fun getCategoriesArrayList(): ArrayList<String> {
        val categories: ArrayList<String> = ArrayList()
        ExpenseCategoryType.values().forEach {
            categories.add(it.name)
        }
        return categories
    }

    fun getCategoriesList(): List<Category> {
        val categories: ArrayList<Category> = ArrayList()
        ExpenseCategoryType.values().forEach {
            categories.add(Category(it.name))
        }
        return categories
    }
}