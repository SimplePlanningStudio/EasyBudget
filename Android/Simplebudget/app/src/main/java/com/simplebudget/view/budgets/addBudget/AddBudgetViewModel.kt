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
package com.simplebudget.view.budgets.addBudget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.db.impl.accounts.AccountTypeEntity
import com.simplebudget.db.impl.budgets.BudgetEntity
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.Logger
import com.simplebudget.helper.SingleLiveEvent
import com.simplebudget.helper.extensions.toAccount
import com.simplebudget.helper.extensions.toBudgetEntity
import com.simplebudget.helper.extensions.toCategoriesFromCategoryEntity
import com.simplebudget.helper.extensions.toCategoriesId
import com.simplebudget.iab.Iab
import com.simplebudget.model.account.Account
import com.simplebudget.model.budget.Budget
import com.simplebudget.model.budget.RecurringBudget
import com.simplebudget.model.budget.RecurringBudgetType
import com.simplebudget.model.category.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * BudgetsViewModel to handle budgets.
 */
class AddBudgetViewModel(
    private val db: DB, private val iab: Iab,
) : ViewModel() {

    private var editingBudget: Budget? = null

    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    val expenseFirstInstanceDateLiveData = MutableLiveData<LocalDate>()

    /**
     * Expense that is being edited (will be null if it's a new one)
     */
    val expenseLastInstanceDateLiveData = MutableLiveData<LocalDate>()

    /**
     *
     */
    private val doneAddingWithBudgetLiveData = MutableLiveData<Budget?>()
    val doneAddingWithBudget: LiveData<Budget?> = doneAddingWithBudgetLiveData

    /**
     *
     */
    val existingBudgetEventStream = SingleLiveEvent<ExistingBudgetData?>()

    /**
     *
     */
    private val budgetsLiveData = MutableLiveData<List<BudgetEntity>>()
    val budgets: LiveData<List<BudgetEntity>> = budgetsLiveData

    val finishLiveData = MutableLiveData<Unit?>()
    val errorEventStream = SingleLiveEvent<Unit?>()
    val savingBudgetEventStream = SingleLiveEvent<Boolean>()

    /**
     * Premium Status
     */
    val premiumStatusLiveData = MutableLiveData<Boolean>()
    fun onIabStatusChanged() {
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    fun isUserPremium(): Boolean = premiumStatusLiveData.value ?: false


    /**
     * For existing budget to edit we'll pass the budget from listing
     * screen to here to get required data.
     */
    fun initExistingBudgetToEdit(editingBudget: Budget?) {
        this.editingBudget = editingBudget
        if (editingBudget != null) {
            viewModelScope.launch {
                val account: AccountTypeEntity? = db.getAccount(editingBudget.accountId)
                val categories =
                    db.getBudgetWithCategories(editingBudget.id!!).categories.toCategoriesFromCategoryEntity()
                existingBudgetEventStream.postValue(
                    ExistingBudgetData(
                        editingBudget = Budget(
                            id = editingBudget.id,
                            goal = editingBudget.goal,
                            accountId = editingBudget.accountId,
                            budgetAmount = editingBudget.budgetAmount,
                            remainingAmount = editingBudget.remainingAmount,
                            spentAmount = editingBudget.spentAmount,
                            startDate = editingBudget.startDate,
                            endDate = editingBudget.endDate,
                            associatedRecurringBudget = editingBudget.associatedRecurringBudget,
                            categories = categories
                        ),
                        account = account?.toAccount(),
                        categories = categories,
                        type = editingBudget.associatedRecurringBudget?.type
                            ?: RecurringBudgetType.ONE_TIME
                    )
                )
            }
        } else {
            existingBudgetEventStream.value = null
        }
    }

    /**
     *
     */
    private fun loadBudgets() {
        viewModelScope.launch {
            val budgetEntityList = db.getBudgets()
            budgetsLiveData.postValue(budgetEntityList)
        }
    }

    fun onUpdateFirstInstance(date: LocalDate?) {
        this.expenseFirstInstanceDateLiveData.value = date ?: LocalDate.now()
    }


    fun onUpdateLastInstance(date: LocalDate?) {
        this.expenseLastInstanceDateLiveData.value = date ?: LocalDate.now()
    }


    /**
     * Save budget
     */
    fun saveBudget(
        goal: String,
        accountId: Long,
        budgetAmount: Double,
        type: RecurringBudgetType,
        categories: ArrayList<Category>,

        ) {
        val startDate = expenseFirstInstanceDateLiveData.value ?: return
        val endDate = expenseLastInstanceDateLiveData.value
        doSaveBudget(
            goal = goal,
            accountId = accountId,
            budgetAmount = budgetAmount,
            recurringBudgetType = type,
            startDate = startDate,
            endDate = endDate,
            categories = categories
        )
    }


    private fun doSaveBudget(
        goal: String,
        accountId: Long,
        budgetAmount: Double,
        recurringBudgetType: RecurringBudgetType,
        startDate: LocalDate,
        endDate: LocalDate?,
        categories: List<Category>,
    ) {
        savingBudgetEventStream.value = true
        var insertedBudget: RecurringBudget? = null
        viewModelScope.launch {
            val inserted = withContext(Dispatchers.IO) {
                if (editingBudget == null) {
                    // Adding new budget
                    if (recurringBudgetType != RecurringBudgetType.ONE_TIME) {
                        //No need to add into recurringBudget because its not repeating.
                        insertedBudget = try {
                            db.persistRecurringBudget(
                                RecurringBudget(
                                    goal = goal,
                                    accountId = accountId,
                                    budgetAmount = budgetAmount,
                                    type = recurringBudgetType,
                                    recurringDate = startDate,
                                    modified = false
                                )
                            )
                        } catch (t: Throwable) {
                            Logger.error(
                                false,
                                "Error while inserting recurring expense into DB: addRecurringExpense returned false"
                            )
                            return@withContext false
                        }
                    } else {
                        // Without adding to db just create an object
                        insertedBudget = RecurringBudget(
                            goal = goal,
                            accountId = accountId,
                            budgetAmount = budgetAmount,
                            type = recurringBudgetType,
                            recurringDate = startDate,
                            modified = false
                        )
                    }
                    if (!flattenBudgetsAndSave(
                            recurringBudgetType, startDate, endDate, insertedBudget!!, categories
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
                    // Editing existing budget
                    if (recurringBudgetType == RecurringBudgetType.ONE_TIME) {
                        //No need to edit into recurringBudget because its not repeating.
                        editingBudget?.let { editingBudget ->
                            val budget = Budget(
                                id = editingBudget.id,
                                goal = goal,
                                accountId = accountId,
                                budgetAmount = budgetAmount,
                                remainingAmount = budgetAmount,
                                spentAmount = 0.0,
                                startDate = startDate,
                                endDate = endDate!!,
                                associatedRecurringBudget = null, // Monthly id would be null for one time expense
                                categories = categories
                            )
                            //First do cleanup and delete current budget
                            db.deleteBudget(editingBudget)
                            //Then add new budget
                            db.insertBudgetWithCategories(
                                budget.toBudgetEntity(),
                                categories.toCategoriesId()
                            )
                        }
                    } else {
                        val recurringBudget = try {
                            val recurringBudget = editingBudget!!.associatedRecurringBudget!!
                            db.deleteBudget(editingBudget!!)

                            val newRecurringExpense = recurringBudget.copy(
                                modified = true,
                                type = recurringBudgetType,
                                recurringDate = startDate,
                                goal = goal,
                                budgetAmount = budgetAmount,
                                accountId = accountId
                            )
                            db.persistRecurringBudget(newRecurringExpense)
                        } catch (t: Throwable) {
                            Logger.error(
                                false,
                                "Error while editing recurring budget into DB: addRecurringBudget returned false"
                            )
                            return@withContext false
                        }

                        if (!flattenBudgetsAndSave(
                                recurringBudgetType, startDate, endDate, recurringBudget, categories
                            )
                        ) {
                            Logger.error(
                                false,
                                "Error while flattening budgets for recurring budget edit: flattenBudgetsForRecurringBudget returned false"
                            )
                            return@withContext false
                        }
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

    /**
     *
     */
    private suspend fun flattenBudgetsAndSave(
        type: RecurringBudgetType,
        startDate: LocalDate,
        endDate: LocalDate?,
        recurringBudget: RecurringBudget,
        categories: List<Category>,
    ): Boolean {
        var currentDate = startDate
        var lastDate = endDate ?: startDate
        val noOfYears = 2

        when (type) {
            RecurringBudgetType.ONE_TIME -> {
                //No repeat: Add only 1 expense as its one time only ( No repeat)
                val budget = Budget(
                    goal = recurringBudget.goal,
                    accountId = recurringBudget.accountId,
                    budgetAmount = recurringBudget.budgetAmount,
                    remainingAmount = recurringBudget.budgetAmount,
                    spentAmount = 0.0,
                    startDate = currentDate,
                    endDate = lastDate,
                    associatedRecurringBudget = null, // Monthly id would be null for one time expense
                    categories = categories
                )
                db.insertBudgetWithCategories(budget.toBudgetEntity(), categories.toCategoriesId())
            }

            RecurringBudgetType.MONTHLY -> {
                // Add up to noOfYears years of expenses
                for (i in 0 until 12 * noOfYears) {
                    try {
                        val budget = Budget(
                            goal = recurringBudget.goal,
                            accountId = recurringBudget.accountId,
                            budgetAmount = recurringBudget.budgetAmount,
                            remainingAmount = recurringBudget.budgetAmount,
                            spentAmount = 0.0,
                            startDate = currentDate,
                            endDate = DateHelper.lastDateOfMonth(currentDate),
                            associatedRecurringBudget = recurringBudget,
                            categories = categories
                        )
                        db.insertBudgetWithCategories(
                            budget.toBudgetEntity(),
                            categories.toCategoriesId()
                        )
                    } catch (t: Throwable) {
                        logError(t)
                        return false
                    }
                    currentDate = currentDate.plusMonths(1)
                }
            }
        }
        return true
    }

    private fun logError(t: Throwable) {
        Logger.error(
            false,
            "Error while inserting budget for recurring budget into DB: persistExpense returned false",
            t
        )
    }

    init {
        loadBudgets()
        premiumStatusLiveData.value = iab.isUserPremium()
    }

    data class ExistingBudgetData(
        val editingBudget: Budget?,
        val account: Account?,
        val categories: List<Category>,
        val type: RecurringBudgetType,
    )
}