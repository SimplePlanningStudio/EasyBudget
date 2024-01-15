/*
 *   Copyright 2024 Waheed Nazir
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
package com.simplebudget.view.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.db.DB
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.DateHelper
import com.simplebudget.model.account.appendAccount
import com.simplebudget.model.expense.Expense
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccount
import com.simplebudget.prefs.activeAccountLabel
import com.simplebudget.view.report.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

/**
 * Constants Values
 */

class SearchViewModel(
    private val db: DB, private val appPreferences: AppPreferences
) : ViewModel() {

    private val allExpensesLiveData = MutableLiveData<List<Expense>>()
    val allExpenses: LiveData<List<Expense>> = allExpensesLiveData

    private val loadingMutableLiveData = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = loadingMutableLiveData


    // Searched results in case of search use case
    val monthlyReportDataLiveData = MutableLiveData<DataModels.MonthlyReportData>()

    val expenses = mutableListOf<Expense>()
    val revenues = mutableListOf<Expense>()
    private val allExpensesOfThisMonth = mutableListOf<DataModels.SuperParent>()
    private val allExpensesParentList = mutableListOf<DataModels.CustomTriple.Data>()
    var revenuesAmount = 0.0
    var expensesAmount = 0.0
    var balance = 0.0
    private val hashMap = hashMapOf<String, DataModels.CustomTriple.Data>()


    init {
        loadThisMonthExpenses()
    }

    /**
     * Search expenses
     */
    fun searchExpenses(search_query: String) {
        loadingMutableLiveData.value = true
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                db.searchExpenses(search_query)
            }
            allExpensesLiveData.postValue(results)
            loadingMutableLiveData.postValue(false)
            loadDataForReports(results)
        }
    }

    /**
     * Load Today's expenses
     */
    fun loadTodayExpenses() {
        loadExpensesForADate(DateHelper.today)
    }

    /**
     * Load Yesterday's expenses
     */
    fun loadYesterdayExpenses() {
        loadExpensesForADate(DateHelper.yesterday)
    }

    /**
     * Load expenses for this week
     */
    fun loadThisWeekExpenses() {
        // Starting of week to Today's date
        loadExpensesForGivenDates(DateHelper.firstDayOfThisWeek, DateHelper.today)
    }

    /**
     * Load expenses for this month
     */
    fun loadThisMonthExpenses() {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                db.getExpensesForMonth(DateHelper.startDayOfMonth)
            }
            allExpensesLiveData.postValue(results)
            loadingMutableLiveData.postValue(false)
            loadDataForReports(results)
        }
    }

    /**
     * Load expenses for last week
     */
    fun loadLastWeekExpenses() {
        loadExpensesForGivenDates(DateHelper.firstDayOfLastWeek, DateHelper.lastDayOfLastWeek)
    }

    /**
     * Load Tomorrow's expenses
     */
    fun loadTomorrowExpenses() {
        loadExpensesForADate(DateHelper.tomorrow)
    }

    /**
     *
     */
    fun loadExpensesForADate(date: LocalDate) {
        loadingMutableLiveData.value = true
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                db.getExpensesForDay(date, appPreferences.activeAccount())
            }
            allExpensesLiveData.postValue(results)
            loadingMutableLiveData.postValue(false)
            loadDataForReports(results)
        }
    }

    /**
     *
     */
    fun loadExpensesForGivenDates(startDate: LocalDate, endDate: LocalDate) {
        loadingMutableLiveData.value = true
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                db.getAllExpenses(startDate, endDate)
            }
            allExpensesLiveData.postValue(results)
            loadingMutableLiveData.postValue(false)
            loadDataForReports(results)
        }
    }


    private val exportStatus: MutableLiveData<ExportStatus> = MutableLiveData()
    val observeExportStatus: LiveData<ExportStatus> = exportStatus

    /**
     *
     */
    fun exportCSV(currency: Currency, month: LocalDate, file: File) {
        try {
            if (expenses.isEmpty() && revenues.isEmpty()) {
                exportStatus.value = ExportStatus(
                    false, "Please add expenses of this month to generate report.", "", file
                )
                return
            }
            val format = DateTimeFormatter.ofPattern(
                "dd MMM yyyy", Locale.getDefault()
            )

            val monthFormat = DateTimeFormatter.ofPattern(
                "MMM yyyy", Locale.getDefault()
            )

            var fileWriter: FileWriter? = null
            try {
                file.createNewFile()
                fileWriter = FileWriter(file)

                // Month Title
                fileWriter.append(",")
                fileWriter.append(String.format("Report of %s", monthFormat.format(month)))
                fileWriter.append(",")
                fileWriter.append("\n")

                // Incomes Total
                fileWriter.append("$CSV_TITLE_INCOMES_TOTAL (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(CurrencyHelper.getFormattedAmountValue(revenuesAmount))
                fileWriter.append("\n")

                // Expense Total
                fileWriter.append("$CSV_TITLE_EXPENSE_TOTAL (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(CurrencyHelper.getFormattedAmountValue(expensesAmount))
                fileWriter.append("\n")

                // Balance Total
                fileWriter.append("$CSV_TITLE_BALANCE (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(CurrencyHelper.getFormattedAmountValue(balance))
                fileWriter.append("\n")
                fileWriter.append("\n")

                // Incomes
                fileWriter.append(CSV_HEADER_INCOMES)
                fileWriter.append("\n")
                fileWriter.append("Title,Amount(${currency.symbol ?: currency.displayName}),Date,Category")
                fileWriter.append("\n")

                for (data in revenues) {
                    fileWriter.append(data.title)
                    fileWriter.append(",")
                    fileWriter.append(CurrencyHelper.getFormattedAmountValue(-data.amount))
                    fileWriter.append(",")
                    fileWriter.append(String.format("%s", format.format(data.date)))
                    fileWriter.append(",")
                    fileWriter.append(data.category)
                    fileWriter.append("\n")
                }
                fileWriter.append("\n\n")

                // Expenses
                fileWriter.append(CSV_HEADER_EXPENSE)
                fileWriter.append("\n")
                for (data in expenses) {
                    fileWriter.append(data.title)
                    fileWriter.append(",")
                    fileWriter.append(CurrencyHelper.getFormattedAmountValue(-data.amount))
                    fileWriter.append(",")
                    fileWriter.append(String.format("%s", format.format(data.date)))
                    fileWriter.append(",")
                    fileWriter.append(data.category)
                    fileWriter.append("\n")
                }
                exportStatus.value =
                    ExportStatus(true, "Successfully exported file!", file.absolutePath, file)
            } catch (e: Exception) {
                exportStatus.value = ExportStatus(
                    false,
                    "An error occurred while exporting CSV file ${e.localizedMessage}",
                    "",
                    file
                )
                e.printStackTrace()
            } finally {
                try {
                    fileWriter?.flush()
                    fileWriter?.close()
                } catch (e: IOException) {
                    exportStatus.value = ExportStatus(
                        false,
                        "An error occurred while exporting CSV file ${e.localizedMessage}",
                        "",
                        file
                    )
                    e.printStackTrace()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    data class SearchReport(val html: String, val isEmpty: Boolean)

    private val generatePDFReport: MutableLiveData<SearchReport> = MutableLiveData()
    val observeGeneratePDFReport: LiveData<SearchReport> = generatePDFReport

    /**
     * This method will load data into lists.
     * So we can use this for printing / downloading reports.
     */
    private fun loadDataForReports(expensesResults: List<Expense>) {
        if (expensesResults.isEmpty()) {
            monthlyReportDataLiveData.value = DataModels.MonthlyReportData.Empty
            return
        }
        viewModelScope.launch {
            expenses.clear()
            revenues.clear()
            allExpensesParentList.clear()
            allExpensesOfThisMonth.clear()
            revenuesAmount = 0.0
            expensesAmount = 0.0
            hashMap.clear()

            withContext(Dispatchers.IO) {
                for (expense in expensesResults) {
                    // Adding category into map with empty list
                    if (!hashMap.containsKey(expense.category)) hashMap[expense.category] =
                        DataModels.CustomTriple.Data(
                            expense.category,
                            0.0,
                            0.0,
                            0.0,
                            ArrayList<Expense>()
                        )
                    var tCredit: Double = hashMap[expense.category]?.totalCredit ?: 0.0
                    var tDebit: Double = hashMap[expense.category]?.totalDebit ?: 0.0

                    if (expense.isRevenue()) {
                        revenues.add(expense)
                        revenuesAmount -= expense.amount
                        tCredit -= expense.amount
                    } else {
                        expenses.add(expense)
                        expensesAmount += expense.amount
                        tDebit += expense.amount
                    }
                    hashMap[expense.category]?.totalCredit = tCredit
                    hashMap[expense.category]?.totalDebit = tDebit
                    hashMap[expense.category]?.expenses?.add(expense)
                }

                hashMap.keys.forEach { key ->
                    val tCredit = hashMap[key]?.totalCredit ?: 0.0
                    val tDebit = hashMap[key]?.totalDebit ?: 0.0
                    allExpensesOfThisMonth.add(
                        DataModels.Parent(
                            hashMap[key]?.category!!,
                            tCredit,
                            tDebit
                        )
                    )
                    val amountSpend =
                        if (tCredit > tDebit) (tCredit - tDebit) else (tDebit - tCredit)
                    allExpensesParentList.add(
                        DataModels.CustomTriple.Data(
                            hashMap[key]?.category!!,
                            tCredit,
                            tDebit,
                            amountSpend,
                            hashMap[key]?.expenses ?: ArrayList()
                        )
                    )
                    hashMap[key]?.expenses?.forEach { expense ->
                        allExpensesOfThisMonth.add(DataModels.Child(expense))
                    }
                }
                balance = revenuesAmount - expensesAmount
            }

            allExpensesParentList.sortByDescending { it.amountSpend }

            monthlyReportDataLiveData.postValue(
                DataModels.MonthlyReportData.Data(
                    expenses,
                    revenues,
                    allExpensesOfThisMonth,
                    allExpensesParentList,
                    expensesAmount,
                    revenuesAmount
                )
            )
        }
    }

    /**
     * Generate HTML for printing a PDF report
     */
    fun generateHtml(currency: Currency, month: LocalDate) {
        viewModelScope.launch {
            val contents: StringBuilder = StringBuilder()

            val format = DateTimeFormatter.ofPattern(
                "dd MMM yyyy", Locale.getDefault()
            )

            val monthFormat = DateTimeFormatter.ofPattern(
                "MMM yyyy", Locale.getDefault()
            )

            // Table start
            contents.append("<html><head><body>")
            contents.append("<p>")
            // Month Title
            contents.append(
                "<h1 style=\"color:black;\">${
                    (String.format(
                        "Budget Report Of %s", monthFormat.format(month)
                    ))
                }</h1>"
            )
            contents.append("<hr>")

            val activeAccount = appPreferences.activeAccountLabel().appendAccount()
            contents.append("<h2 style=\"color:blue;\">${activeAccount}</h2>")

            // Incomes Total
            val revTotalFormattedAmount =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, revenuesAmount)
            contents.append(
                "<h2 style=\"color:green;\">${("$CSV_TITLE_INCOMES_TOTAL (${currency.symbol ?: currency.displayName}) = ")}" + "<b style=\"color:black;\">${revTotalFormattedAmount}</b></h2>"
            )

            // Expense Total
            val expTotalFormattedAmount =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, expensesAmount)
            contents.append(
                "<h2 style=\"color:red;\">${"$CSV_TITLE_EXPENSE_TOTAL (${currency.symbol ?: currency.displayName}) = "}" + "<b style=\"color:black;\">${expTotalFormattedAmount}</b></h2>"
            )

            // Balance Total
            val balanceColor = if (balance > 0) "green" else "red"
            val balanceFormattedAmount =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, balance)
            contents.append(
                "<h2 style=\"color:${balanceColor};\">${"$CSV_TITLE_BALANCE (${currency.symbol ?: currency.displayName}) = "}" + "<b style=\"color:black;\">${balanceFormattedAmount}</b></h2>"
            )
            // All expenses
            for (data in allExpensesOfThisMonth) {
                if (data is DataModels.Parent) {
                    val amountSpend = CurrencyHelper.getFormattedCurrencyString(
                        appPreferences,
                        (if (data.totalCredit > data.totalDebit) (data.totalCredit - data.totalDebit) else (data.totalDebit - data.totalCredit))
                    )
                    contents.append("<h2 style=\"color:white;background-color:mediumblue\">${data.category} ($amountSpend)</h2>")
                } else {
                    if (data is DataModels.Child) {
                        val formattedAmount = CurrencyHelper.getFormattedCurrencyString(
                            appPreferences, -(data.expense.amount)
                        )
                        val expenseColor = if (data.expense.isRevenue()) "green" else "red"
                        val futureExpense =
                            if (data.expense.isFutureExpense()) "/ (Upcoming expense)"
                            else if (data.expense.isPastExpense()) "/ (Past expense)" else ""

                        contents.append(
                            "<p style=\"font-size:140%;color:black\"><b>${(data.expense.title)}</b></p>" + "<p style=\"font-size:140%;color:grey\">${
                                (String.format(
                                    "%s", format.format(data.expense.date)
                                ))
                            } / ${(data.expense.category)}${futureExpense}</p>" + "<p style=\"font-size:140%;color:$expenseColor\">${
                                formattedAmount
                            }</p>"
                        )
                        contents.append("<hr>")
                    }
                }
            }
            // Table end
            contents.append("</p>")
            contents.append("<br><br>")
            contents.append("<p style=\"font-size:120%;color:grey;text-align:center;\">Generated with SimpleBudget!</b>")
            contents.append("<p style=\"font-size:140%;color:black;text-align:center;\"><b>------------------THANK YOU------------------</b></p>")
            contents.append("</body>")
            contents.append("</html>")

            generatePDFReport.value =
                SearchReport(contents.toString(), allExpensesOfThisMonth.isEmpty())
        }
    }

    override fun onCleared() {
        hashMap.clear()
        super.onCleared()
    }
}