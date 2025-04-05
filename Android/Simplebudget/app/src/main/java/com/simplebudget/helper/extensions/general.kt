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
package com.simplebudget.helper.extensions

import com.simplebudget.BuildConfig
import com.simplebudget.db.DB
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.budgets.BudgetEntity
import com.simplebudget.db.impl.budgets.BudgetWithCategories
import com.simplebudget.db.impl.budgets.RecurringBudgetEntity
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.db.impl.expenses.ExpenseEntity
import com.simplebudget.db.impl.profile.ProfileEntity
import com.simplebudget.db.impl.recurringexpenses.RecurringExpenseEntity
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.Logger
import com.simplebudget.model.account.Account
import com.simplebudget.model.budget.Budget
import com.simplebudget.model.budget.RecurringBudget
import com.simplebudget.model.category.Category
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.profile.Profile
import com.simplebudget.model.recurringexpense.RecurringExpense
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor


/**
 *
 */
fun List<CategoryEntity>.toCategories(): List<Category> {
    val list: ArrayList<Category> = ArrayList()
    this.forEach {
        list.add(Category(it.id, it.name))
    }
    return list
}

/**
 *
 */
fun List<Category>.toCategoriesNamesList(): List<String> {
    val list: ArrayList<String> = ArrayList()
    this.forEach {
        list.add(it.name)
    }
    return list
}

/**
 *
 */
fun List<Category>.toCategoryEntities(): List<CategoryEntity> {
    val list: ArrayList<CategoryEntity> = ArrayList()
    this.forEach {
        list.add(CategoryEntity(it.id, it.name))
    }
    return list
}


fun Category.toCategoryEntity() = CategoryEntity(
    id, name
)


fun CategoryEntity.toCategory() = Category(
    id, name
)


fun Account.toAccountEntity() = AccountTypeEntity(
    id, name, isActive
)

fun AccountTypeEntity.toAccount() = Account(
    id, name, isActive
)

fun List<Account>.toAccountTypeEntities(): List<AccountTypeEntity> {
    val list: ArrayList<AccountTypeEntity> = ArrayList()
    this.forEach {
        list.add(AccountTypeEntity(it.id, it.name, it.isActive))
    }
    return list
}

fun List<AccountTypeEntity>.toAccounts(): List<Account> {
    val list: ArrayList<Account> = ArrayList()
    this.forEach {
        list.add(Account(it.id, it.name, it.isActive))
    }
    return list
}

fun Expense.toExpenseEntity() = ExpenseEntity(
    id,
    title,
    amount.getDBValue(),
    date,
    associatedRecurringExpense?.id,
    category,
    accountId,
    categoryId
)


fun RecurringExpense.toRecurringExpenseEntity() = RecurringExpenseEntity(
    id, title, amount.getDBValue(), recurringDate, modified, type.name, category, accountId
)

fun Budget.toBudgetEntity() = BudgetEntity(
    id = id,
    goal = goal,
    accountId = accountId,
    budgetAmount = budgetAmount.getDBValue(),
    remainingAmount = remainingAmount.getDBValue(),
    spentAmount = spentAmount.getDBValue(),
    startDate = startDate,
    endDate = endDate,
    associatedRecurringBudgetId = associatedRecurringBudget?.id
)


fun RecurringBudget.toRecurringBudgetEntity() = RecurringBudgetEntity(
    id = id,
    goal = goal,
    accountId = accountId,
    originalAmount = budgetAmount.getDBValue(),
    type = type.name,
    recurringDate = recurringDate,
    modified = modified
)

fun Profile.toProfileEntity() = ProfileEntity(
    id,
    userName,
    email,
    fcmToken,
    loginId,
    isPremium,
    premiumType,
    appVersion
)

suspend fun List<BudgetEntity>.toBudgets(db: DB): List<Budget> {
    return map { it.toBudget(db) }
}

suspend fun List<CategoryEntity>.toCategoriesFromCategoryEntity(): List<Category> {
    return map { it.toCategory() }
}

suspend fun List<Category>.toCategoriesId(): List<Long> {
    return map { it.id!! }
}

fun List<Category>.namesAsCommaSeparatedString(): String {
    return if (this.size > 3) {
        this.take(3).joinToString(", ") {
            it.name.toLowerCase(Locale.getDefault()).replaceFirstChar { char -> char.uppercase() }
        } + ", etc."
    } else {
        this.joinToString(", ") {
            it.name.toLowerCase(Locale.getDefault()).replaceFirstChar { char -> char.uppercase() }
        }
    }
}

suspend fun BudgetEntity.toBudget(db: DB, categories: List<Category> = emptyList()): Budget {
    val recurringBudget = this.associatedRecurringBudgetId?.let { id ->
        db.findRecurringBudgetForId(id)
    }
    return toBudget(recurringBudget, categories)
}

suspend fun BudgetWithCategories.toBudget(db: DB): Budget {
    val categories = this.categories.toCategoriesFromCategoryEntity()
    val budget = this.budget.toBudget(db, categories)
    return budget
}

/**
 * Check the selected account default or not.
 */
fun Account.isDefault() = (this.id == 1L)

/**
 * Return the integer value of the double * 100 to store it as integer in DB. This is an ugly
 * method that shouldn't be there but rounding on doubles are a pain :/
 *
 * @return the corresponding int value (double * 100)
 */
fun Double.getDBValue(): Long {
    val stringValue = CurrencyHelper.getFormattedAmountValue(this)
    if (BuildConfig.DEBUG_LOG) {
        Logger.debug("getDBValueForDouble: $stringValue")
    }

    val ceiledValue = ceil(this * 100).toLong()
    val ceiledDoubleValue = ceiledValue / 100.0

    if (CurrencyHelper.getFormattedAmountValue(ceiledDoubleValue) == stringValue) {
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return ceiled value: $ceiledValue")
        }
        return ceiledValue
    }

    val normalValue = this.toLong() * 100
    val normalDoubleValue = normalValue / 100.0

    if (CurrencyHelper.getFormattedAmountValue(normalDoubleValue) == stringValue) {
        if (BuildConfig.DEBUG_LOG) {
            Logger.debug("getDBValueForDouble, return normal value: $normalValue")
        }
        return normalValue
    }

    val flooredValue = floor(this * 100).toLong()
    if (BuildConfig.DEBUG_LOG) {
        Logger.debug("getDBValueForDouble, return floored value: $flooredValue")
    }

    return flooredValue
}

fun Long?.getRealValueFromDB(): Double = if (this != null) this / 100.0 else 0.0