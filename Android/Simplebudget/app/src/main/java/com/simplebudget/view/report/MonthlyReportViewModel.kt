/*
 *   Copyright 2021 Benoit LETONDOR
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
package com.simplebudget.view.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.model.Expense
import com.simplebudget.db.DB
import com.simplebudget.helper.CurrencyHelper.getFormattedAmountValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Constants Values
 */
const val CSV_TITLE_EXPENSE_TOTAL = "Expenses Total"
const val CSV_TITLE_INCOMES_TOTAL = "Revenues Total"
const val CSV_TITLE_BALANCE = "Balance"
const val CSV_HEADER_EXPENSE = "Expenses"
const val CSV_HEADER_INCOMES = "Revenues"
const val CSV_HEADER = "Title,Amount,Date"

class MonthlyReportViewModel(private val db: DB) : ViewModel() {

    val monthlyReportDataLiveData = MutableLiveData<MonthlyReportData>()
    val expenses = mutableListOf<Expense>()
    val revenues = mutableListOf<Expense>()
    var revenuesAmount = 0.0
    var expensesAmount = 0.0
    var balance = 0.0


    sealed class MonthlyReportData {
        object Empty : MonthlyReportData()
        class Data(
            val expenses: List<Expense>,
            val revenues: List<Expense>,
            val expensesAmount: Double,
            val revenuesAmount: Double
        ) : MonthlyReportData()
    }

    fun loadDataForMonth(month: Date) {
        viewModelScope.launch {
            val expensesForMonth = withContext(Dispatchers.Default) {
                db.getExpensesForMonth(month)
            }

            if (expensesForMonth.isEmpty()) {
                monthlyReportDataLiveData.value = MonthlyReportData.Empty
                return@launch
            }

            expenses.clear()
            revenues.clear()
            revenuesAmount = 0.0
            expensesAmount = 0.0

            withContext(Dispatchers.Default) {
                for (expense in expensesForMonth) {
                    if (expense.isRevenue()) {
                        revenues.add(expense)
                        revenuesAmount -= expense.amount
                    } else {
                        expenses.add(expense)
                        expensesAmount += expense.amount
                    }
                }
            }

            balance = revenuesAmount - expensesAmount

            monthlyReportDataLiveData.value =
                MonthlyReportData.Data(expenses, revenues, expensesAmount, revenuesAmount)
        }
    }

    private val exportStatus: MutableLiveData<ExportStatus> = MutableLiveData()
    val observeExportStatus: LiveData<ExportStatus> = exportStatus

    /**
     *
     */
    fun exportCSV(currency: Currency, month: Date, file: File) {
        try {
            val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

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
                fileWriter.append(getFormattedAmountValue(revenuesAmount))
                fileWriter.append("\n")

                // Expense Total
                fileWriter.append("$CSV_TITLE_EXPENSE_TOTAL (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(getFormattedAmountValue(expensesAmount))
                fileWriter.append("\n")

                // Balance Total
                fileWriter.append("$CSV_TITLE_BALANCE (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(getFormattedAmountValue(balance))
                fileWriter.append("\n")
                fileWriter.append("\n")

                // Incomes
                fileWriter.append(CSV_HEADER_INCOMES)
                fileWriter.append("\n")
                fileWriter.append("Title,Amount(${currency.symbol ?: currency.displayName}),Date")
                fileWriter.append("\n")

                for (data in revenues) {
                    fileWriter.append(data.title)
                    fileWriter.append(",")
                    fileWriter.append(getFormattedAmountValue(-data.amount))
                    fileWriter.append(",")
                    fileWriter.append(String.format("%s", format.format(data.date)))
                    fileWriter.append("\n")
                }
                fileWriter.append("\n\n")

                // Expenses
                fileWriter.append(CSV_HEADER_EXPENSE)
                fileWriter.append("\n")
                for (data in expenses) {
                    fileWriter.append(data.title)
                    fileWriter.append(",")
                    fileWriter.append(getFormattedAmountValue(-data.amount))
                    fileWriter.append(",")
                    fileWriter.append(String.format("%s", format.format(data.date)))
                    fileWriter.append("\n")
                }
                exportStatus.value =
                    ExportStatus(true, "Successfully exported file!", file.absolutePath, file)
            } catch (e: Exception) {
                exportStatus.value =
                    ExportStatus(
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
                    exportStatus.value =
                        ExportStatus(
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

    private val generatePDFReport: MutableLiveData<String> = MutableLiveData()
    val observeGeneratePDFReport: LiveData<String> = generatePDFReport

    /**
     * Generate HTML for printing a PDF report
     */
    fun generateHtml(currency: Currency, month: Date) {
        viewModelScope.launch {
            val contents: StringBuilder = StringBuilder()
            val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

            // Table start
            contents.append("<html><head><body>")
            contents.append("<p>")
            // Month Title
            contents.append("<h1 style=\"color:black;\">${(String.format("Budget Report Of %s", monthFormat.format(month)))}</h1>")
            contents.append("<hr>")

            // Incomes Total
            contents.append("<h2 style=\"color:green;\">${("$CSV_TITLE_INCOMES_TOTAL (${currency.symbol ?: currency.displayName}) = ")}" +
                    "<b style=\"color:black;\">${getFormattedAmountValue(revenuesAmount)}</b></h2>")

            // Expense Total
            contents.append("<h2 style=\"color:red;\">${"$CSV_TITLE_EXPENSE_TOTAL (${currency.symbol ?: currency.displayName}) = "}" +
                    "<b style=\"color:black;\">${getFormattedAmountValue(expensesAmount)}</b></h2>")

            // Balance Total
            val color = if(balance>0) "green" else "red"
            contents.append("<h2 style=\"color:${color};\">${"$CSV_TITLE_BALANCE (${currency.symbol ?: currency.displayName}) = "}" +
                    "<b style=\"color:black;\">${getFormattedAmountValue(balance)}</b></h2>")


            // Incomes
            contents.append("<h2 style=\"color:black;background-color:LightGray\">${CSV_HEADER_INCOMES}</h2>")
            for (data in revenues) {
                contents.append("" +
                        "<p style=\"font-size:140%;color:black\"><b>${(data.title)}</b></p>" +
                        "<p style=\"font-size:140%;color:grey\">${(String.format("%s", format.format(data.date)))} / ${(data.category)}</p>" +
                        "<p style=\"font-size:140%;color:green\">${(getFormattedAmountValue(-data.amount))}</p>" +
                        "")
                contents.append("<hr>")
            }

            // Expenses
            contents.append("<h2 style=\"color:black;background-color:LightGray;\">${CSV_HEADER_EXPENSE}</h2>")
            for (data in expenses) {
                contents.append("" +
                        "<p style=\"font-size:140%;color:black\"><b>${(data.title)}</b></p>" +
                        "<p style=\"font-size:140%;color:grey\">${(String.format("%s", format.format(data.date)))} / ${(data.category)}</p>" +
                        "<p style=\"font-size:140%;color:red\">${(getFormattedAmountValue(-data.amount))}</p>" +
                        "")
                contents.append("<hr>")
            }
            // Table end
            contents.append("</p>")
            contents.append("<br><br>")
            contents.append("<p style=\"font-size:120%;color:grey;text-align:center;\">Generated with SimpleBudget!</b>")
            contents.append("<p style=\"font-size:140%;color:black;text-align:center;\"><b>------------------THANK YOU------------------</b></p>")
            contents.append("</body>")
            contents.append("</html>")

            generatePDFReport.value = contents.toString()
        }
    }

    /**
     *
     */
    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}