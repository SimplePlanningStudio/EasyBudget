/*
 *   Copyright 2025 Benoit LETONDOR / Waheed Nazir
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
package com.simplebudget.injection

import com.simplebudget.view.accounts.AccountsViewModel
import com.simplebudget.view.category.choose.ChooseCategoryViewModel
import com.simplebudget.view.breakdown.base.BreakDownBaseViewModel
import com.simplebudget.view.expenseedit.ExpenseEditViewModel
import com.simplebudget.view.main.MainViewModel
import com.simplebudget.view.premium.PremiumViewModel
import com.simplebudget.view.recurringexpenseadd.RecurringExpenseEditViewModel
import com.simplebudget.view.report.MonthlyReportViewModel
import com.simplebudget.view.report.base.MonthlyReportBaseViewModel
import com.simplebudget.view.breakdown.BreakDownViewModel
import com.simplebudget.view.budgets.addBudget.AddBudgetViewModel
import com.simplebudget.view.budgets.BudgetViewModel
import com.simplebudget.view.budgets.base.BudgetBaseViewModel
import com.simplebudget.view.budgets.budgetDetails.BudgetDetailsViewModel
import com.simplebudget.view.category.manage.ManageCategoriesViewModel
import com.simplebudget.view.reset.ResetAppDataViewModel
import com.simplebudget.view.search.SearchViewModel
import com.simplebudget.view.selectcurrency.SelectCurrencyViewModel
import com.simplebudget.view.settings.backup.BackupSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    /**
     * Main activity view model
     */
    viewModel { MainViewModel(get(), get(), get(), get()) }

    /**
     * Categories view model
     */
    viewModel { ChooseCategoryViewModel(get(), get()) }

    viewModel { ManageCategoriesViewModel(get(), get()) }

    /**
     * Select currencies
     */
    viewModel { SelectCurrencyViewModel() }

    /**
     * Monthly report
     */
    viewModel { MonthlyReportViewModel(get(), get()) }
    viewModel { MonthlyReportBaseViewModel(get()) }

    /**
     * Monthly break down
     */
    viewModel { BreakDownViewModel(get()) }
    viewModel { BreakDownBaseViewModel(get()) }

    /**
     * Search Expenses
     */
    viewModel { SearchViewModel(get(), get()) }

    /**
     * Add / Edit Expenses
     */
    viewModel { ExpenseEditViewModel(get(), get(), get()) }
    viewModel { RecurringExpenseEditViewModel(get(), get(), get()) }

    /**
     * Premium view model
     */
    viewModel { PremiumViewModel(get()) }

    /**
     * Backup view model
     */
    viewModel { BackupSettingsViewModel(get(), get(), get()) }

    /**
     * Reset app data view model
     */
    viewModel { ResetAppDataViewModel(get(), get()) }

    /**
     * Accounts view model
     */
    viewModel { AccountsViewModel(get(), get()) }

    /**
     *  Add Budget view model
     */
    viewModel { AddBudgetViewModel(get(), get()) }
    /**
     *  Budget base view model
     */
    viewModel { BudgetBaseViewModel(get(), get()) }
    /**
     *  Budget view model
     */
    viewModel { BudgetViewModel(get()) }
    /**
     *  Budget details view model
     */
    viewModel { BudgetDetailsViewModel(get()) }
}