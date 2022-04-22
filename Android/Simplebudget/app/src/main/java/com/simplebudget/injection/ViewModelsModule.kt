/*
 *   Copyright 2022 Benoit LETONDOR
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

import com.simplebudget.view.CategoriesViewModel
import com.simplebudget.view.expenseedit.ExpenseEditViewModel
import com.simplebudget.view.main.MainViewModel
import com.simplebudget.view.premium.PremiumViewModel
import com.simplebudget.view.recurringexpenseadd.RecurringExpenseEditViewModel
import com.simplebudget.view.report.MonthlyReportViewModel
import com.simplebudget.view.report.base.MonthlyReportBaseViewModel
import com.simplebudget.view.selectcurrency.SelectCurrencyViewModel
import com.simplebudget.view.settings.backup.BackupSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { CategoriesViewModel(get()) }
    viewModel { SelectCurrencyViewModel() }
    viewModel { MonthlyReportViewModel(get(), get()) }
    viewModel { MonthlyReportBaseViewModel(get()) }
    viewModel { ExpenseEditViewModel(get(), get(), get()) }
    viewModel { RecurringExpenseEditViewModel(get(), get(), get()) }
    viewModel { PremiumViewModel(get()) }
    viewModel { BackupSettingsViewModel(get(), get(), get()) }
}