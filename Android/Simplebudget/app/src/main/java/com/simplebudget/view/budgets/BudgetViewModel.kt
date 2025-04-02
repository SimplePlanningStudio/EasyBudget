package com.simplebudget.view.budgets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.extensions.toBudget
import com.simplebudget.helper.extensions.toBudgetEntity
import com.simplebudget.model.budget.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Constants Values
 */
class BudgetViewModel(private val db: DB) : ViewModel() {

    private val budgetsMutableLiveData = MutableLiveData<List<Budget>>()
    val budgetsLiveData: LiveData<List<Budget>> = budgetsMutableLiveData

    private val loadingMutableLiveData = MutableLiveData<Boolean>()
    val loadingLiveData: LiveData<Boolean> = loadingMutableLiveData

    /**
     *
     */
    fun loadBudgets(monthStartDate: LocalDate) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                loadingMutableLiveData.postValue(true)
                //Update budgets spent amount starting this month until Today
                db.updateBudgetsSpentAmount(monthStartDate, DateHelper.today)
                //Get budgets starting this month until Today
                val budgetWithCategories = db.getBudgetsWithCategoriesByAccount(monthStartDate)
                val budgets = budgetWithCategories.map { it.toBudget(db) }.reversed()
                budgetsMutableLiveData.postValue(budgets)
                loadingMutableLiveData.postValue(false)
            }
        }
    }

    /**
     * Delete budget from DB
     */
    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            db.deleteBudget(budget)
        }
    }
}