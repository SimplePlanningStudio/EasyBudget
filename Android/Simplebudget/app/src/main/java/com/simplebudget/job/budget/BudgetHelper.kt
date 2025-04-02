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
package com.simplebudget.job.budget

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.simplebudget.db.DB
import com.simplebudget.helper.DateHelper
import com.simplebudget.helper.DelayHelper
import com.simplebudget.helper.Logger
import com.simplebudget.helper.extensions.getDBValue
import com.simplebudget.helper.extensions.toBudget
import com.simplebudget.prefs.AppPreferences
import java.util.concurrent.TimeUnit

const val BUDGET_JOB_REQUEST_TAG = "budgetJobTag"

suspend fun updateBudget(
    db: DB,
    appPreferences: AppPreferences
): ListenableWorker.Result {
    //TODO Handle it for premium use case!
    /*if (appPreferences.isUserPremium()) {
        Log.e(
            "BudgetJob",
            "Not premium"
        )
        return ListenableWorker.Result.failure()
    }*/

    try {
        // TODO Update budgets
        /*db.use { openedDB ->
            val budgets = openedDB.getBudgetsOfActiveAccount()
            if (budgets.isNotEmpty()) {
                budgets.forEach { budgetEntity ->
                    val budget = budgetEntity.toBudget(openedDB)
                    openedDB.getBud
                    val expenseAmount = openedDB.getBalanceForACategory(
                        DateHelper.startDayOfMonth,
                        DateHelper.today,
                        budget.accountId,
                        budget.categoryName
                    )
                    val newBudget = budget.copy(
                        spentAmount = expenseAmount,
                        remainingAmount = (budget.budgetAmount - expenseAmount),
                    )
                    openedDB.updateBudget(
                        budget.id!!,
                        expenseAmount.getDBValue(),
                        (budget.budgetAmount - expenseAmount).getDBValue()
                    )
                    Logger.debug(
                        "budgetJobStateStream",
                        newBudget.toString()
                    )
                }
            }

        }*/
    } catch (error: Throwable) {
        Log.e(
            "BudgetJob",
            "Error updating budget into db",
            error
        )
        return ListenableWorker.Result.failure()
    }
    return ListenableWorker.Result.success()
}

fun scheduleBudgetJob(context: Context) {
    val budgetJobRequest = PeriodicWorkRequestBuilder<BudgetJob>(1, TimeUnit.DAYS)
        .setInitialDelay(
            DelayHelper.calculateInitialDelay(19),
            TimeUnit.MILLISECONDS
        ) // Run everyday at 7pm = 19
        .addTag(BUDGET_JOB_REQUEST_TAG)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        BUDGET_JOB_REQUEST_TAG,
        ExistingPeriodicWorkPolicy.KEEP,
        budgetJobRequest
    )
}

fun unScheduleBudgetJob(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(BUDGET_JOB_REQUEST_TAG)
}

fun getBudgetJobInfosLiveData(context: Context): LiveData<List<WorkInfo>> {
    return WorkManager.getInstance(context).getWorkInfosByTagLiveData(BUDGET_JOB_REQUEST_TAG)
}