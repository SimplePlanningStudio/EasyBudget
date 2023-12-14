package com.simplebudget.helper.extensions

import com.simplebudget.BuildConfig
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.categories.CategoryEntity
import com.simplebudget.db.impl.expenses.ExpenseEntity
import com.simplebudget.db.impl.recurringexpenses.RecurringExpenseEntity
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.Logger
import com.simplebudget.model.account.Account
import com.simplebudget.model.category.Category
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpense
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
    id, title, amount.getDBValue(), date, associatedRecurringExpense?.id, category, accountId
)


fun RecurringExpense.toRecurringExpenseEntity() = RecurringExpenseEntity(
    id, title, amount.getDBValue(), recurringDate, modified, type.name, category, accountId
)

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