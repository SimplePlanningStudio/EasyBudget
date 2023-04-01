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
package com.simplebudget.view.recurringexpenseadd

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.iab.Iab
import com.simplebudget.db.DB
import com.simplebudget.helper.Logger
import com.simplebudget.helper.SingleLiveEvent
import com.simplebudget.model.Expense
import com.simplebudget.model.RecurringExpense
import com.simplebudget.model.RecurringExpenseType
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.getInitDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RecurringExpenseEditViewModel(
    private val db: DB,
    private val appPreferences: AppPreferences,
    private val iab: Iab
) : ViewModel() {

    val premiumStatusLiveData = MutableLiveData<Boolean>()

    private var editedExpense: Expense? = null
    val expenseDateLiveData = MutableLiveData<LocalDate>()
    val editTypeLiveData = MutableLiveData<ExpenseEditType>()
    val existingExpenseEventStream = SingleLiveEvent<ExistingExpenseData?>()
    val savingIsRevenueEventStream = SingleLiveEvent<Boolean>()
    val finishLiveData = MutableLiveData<Unit?>()
    val expenseAddBeforeInitDateEventStream = SingleLiveEvent<Unit>()
    val errorEventStream = SingleLiveEvent<Unit?>()


    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }


    fun initWithDateAndExpense(date: LocalDate, expense: Expense?) {
        this.expenseDateLiveData.value = date
        this.editedExpense = expense
        this.editTypeLiveData.value = ExpenseEditType(
            editedExpense?.isRevenue() ?: false,
            editedExpense != null
        )

        existingExpenseEventStream.value = if (expense != null) ExistingExpenseData(
            expense.title,
            expense.amount,
            expense.associatedRecurringExpense!!.type,
            expense.associatedRecurringExpense.category
        ) else null
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeLiveData.value = ExpenseEditType(isRevenue, editedExpense != null)
    }

    fun onSave(
        value: Double, description: String, recurringExpenseType: RecurringExpenseType,
        expenseCategoryType: String
    ) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return
        val dateOfInstallation = appPreferences.getInitDate() ?: LocalDate.now()

        if (date.isBefore(dateOfInstallation)) {
            expenseAddBeforeInitDateEventStream.value = Unit
            return
        }

        doSaveExpense(
            value,
            description,
            recurringExpenseType,
            editedExpense,
            isRevenue,
            date,
            expenseCategoryType
        )
    }

    fun onAddExpenseBeforeInitDateConfirmed(
        value: Double,
        description: String,
        recurringExpenseType: RecurringExpenseType,
        expenseCategoryType: String
    ) {
        val isRevenue = editTypeLiveData.value?.isRevenue ?: return
        val date = expenseDateLiveData.value ?: return

        doSaveExpense(
            value,
            description,
            recurringExpenseType,
            editedExpense,
            isRevenue,
            date,
            expenseCategoryType
        )
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(
        value: Double,
        description: String,
        recurringExpenseType: RecurringExpenseType,
        editedExpense: Expense?,
        isRevenue: Boolean,
        date: LocalDate,
        expenseCategoryType: String
    ) {
        savingIsRevenueEventStream.value = isRevenue

        viewModelScope.launch {
            val inserted = withContext(Dispatchers.Default) {
                if (editedExpense == null) {
                    val insertedExpense = try {
                        db.persistRecurringExpense(
                            RecurringExpense(
                                description,
                                if (isRevenue) -value else value,
                                date,
                                recurringExpenseType,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting recurring expense into DB: addRecurringExpense returned false"
                        )
                        return@withContext false
                    }

                    if (!flattenExpensesForRecurringExpense(
                            insertedExpense,
                            date,
                            expenseCategoryType
                        )
                    ) {
                        Logger.error(
                            false,
                            "Error while flattening expenses for recurring expense: flattenExpensesForRecurringExpense returned false"
                        )
                        return@withContext false
                    }

                    return@withContext true
                } else {
                    val recurringExpense = try {
                        val recurringExpense = editedExpense.associatedRecurringExpense!!
                        db.deleteAllExpenseForRecurringExpenseFromDate(
                            recurringExpense,
                            editedExpense.date
                        )
                        db.deleteExpense(editedExpense)

                        val newRecurringExpense = recurringExpense.copy(
                            modified = true,
                            type = recurringExpenseType,
                            recurringDate = date,
                            title = description,
                            amount = if (isRevenue) -value else value,
                            category = expenseCategoryType
                        )
                        db.persistRecurringExpense(newRecurringExpense)
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while editing recurring expense into DB: addRecurringExpense returned false"
                        )
                        return@withContext false
                    }

                    if (!flattenExpensesForRecurringExpense(
                            recurringExpense,
                            date,
                            expenseCategoryType
                        )
                    ) {
                        Logger.error(
                            false,
                            "Error while flattening expenses for recurring expense edit: flattenExpensesForRecurringExpense returned false"
                        )
                        return@withContext false
                    }

                    return@withContext true
                }
            }

            if (inserted) {
                finishLiveData.value = null
            } else {
                errorEventStream.value = null
            }
        }
    }

    private suspend fun flattenExpensesForRecurringExpense(
        expense: RecurringExpense,
        date: LocalDate,
        expenseCategoryType: String
    ): Boolean {
        var currentDate = date

        when (expense.type) {
            RecurringExpenseType.DAILY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 365 * 5) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false"
                        )
                        return false
                    }
                    currentDate = currentDate.plusDays(1)
                }
            }
            RecurringExpenseType.WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }
                    currentDate = currentDate.plus(1, ChronoUnit.WEEKS)
                }
            }
            RecurringExpenseType.BI_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }
                    currentDate = currentDate.plus(2, ChronoUnit.WEEKS)
                }
            }
            RecurringExpenseType.TER_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }

                    currentDate = currentDate.plus(3, ChronoUnit.WEEKS)
                }
            }
            RecurringExpenseType.FOUR_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12 * 4 * 5) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }
                    currentDate = currentDate.plus(4, ChronoUnit.WEEKS)
                }
            }
            RecurringExpenseType.MONTHLY -> {
                // Add up to 10 years of expenses
                for (i in 0 until 12 * 10) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }
                    currentDate = currentDate.plusMonths(1)
                }
            }
            RecurringExpenseType.BI_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 4 * 25) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }
                    currentDate = currentDate.plusMonths(2)
                }
            }
            RecurringExpenseType.TER_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 4 * 25) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }

                    currentDate = currentDate.plusMonths(3)
                }
            }
            RecurringExpenseType.SIX_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 2 * 25) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false",
                            t
                        )
                        return false
                    }

                    currentDate = currentDate.plusMonths(6)
                }
            }
            RecurringExpenseType.YEARLY -> {
                // Add up to 100 years of expenses
                for (i in 0 until 100) {
                    try {
                        db.persistExpense(
                            Expense(
                                expense.title,
                                expense.amount,
                                currentDate,
                                expense,
                                expenseCategoryType
                            )
                        )
                    } catch (t: Throwable) {
                        Logger.error(
                            false,
                            "Error while inserting expense for recurring expense into DB: persistExpense returned false"
                        )
                        return false
                    }
                    currentDate = currentDate.plusYears(1)
                }
            }
            else -> {
            }
        }

        return true
    }

    fun onDateChanged(date: LocalDate) {
        this.expenseDateLiveData.value = date
    }

    init {
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}

data class ExpenseEditType(val isRevenue: Boolean, val editing: Boolean)

data class ExistingExpenseData(
    val title: String,
    val amount: Double,
    val type: RecurringExpenseType,
    val categoryType: String
)