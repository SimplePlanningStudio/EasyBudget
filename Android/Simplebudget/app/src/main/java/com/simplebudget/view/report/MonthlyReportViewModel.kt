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

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.model.Expense
import com.simplebudget.db.DB
import com.simplebudget.helper.CurrencyHelper.getFormattedAmountValue
import kotlinx.android.synthetic.main.activity_main.*
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

    /**
     * Generate HTML for printing a PDF report
     */
    fun generateHtml(currency: Currency, month: Date): String {
        val contents: StringBuilder = StringBuilder()
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        // Style
        contents.append("<style>\n" +
                "table {\n" +
                "  border-collapse: collapse;\n" +
                "  width: 100%;\n" +
                "}\n" +
                "\n" +
                "tr {\n" +
                "  border-bottom: 1px solid #ddd;\n" +
                "}\n" +
                "</style>")

        // Table start
        contents.append("<table style=\"width:100%\">")

        // Month Title
        contents.append("<tr><td><h1 style=\"color:black;\">${(String.format("Budget report of %s", monthFormat.format(month)))}</h1></td><td></td><td></td></tr>")

        // Incomes Total
        contents.append("<tr><td><h3 style=\"color:black;\">${("$CSV_TITLE_INCOMES_TOTAL (${currency.symbol ?: currency.displayName})")}</h3></td><td><h2 style=\"color:green;\">${getFormattedAmountValue(revenuesAmount)}</h2></td><td></td></tr>")

        // Expense Total
        contents.append("<tr><td><h3 style=\"color:black;\">${"$CSV_TITLE_EXPENSE_TOTAL (${currency.symbol ?: currency.displayName})"}</h3></td><td><h2 style=\"color:red;\">${getFormattedAmountValue(expensesAmount)}</h2></td><td></td></tr>")

        // Balance Total
        contents.append("<tr><td><h3 style=\"color:black;\">${"$CSV_TITLE_BALANCE (${currency.symbol ?: currency.displayName})"}</h3></td><td><h2 style=\"color:green;\">${getFormattedAmountValue(balance)}</h2></td><td></td></tr>")

        // Incomes
        contents.append("<tr><td><h2 style=\"color:green;\">${CSV_HEADER_INCOMES}</h2></td><td></td><td></td></tr>")
        // "Description,Amount,Date"
        contents.append("<tr style=\"color:green;\">" +
                "<td><h3>Description</h3></td>" +
                "<td><h3>Amount</h3></td>" +
                "<td><h3>Date</h3></td>" +
                "</tr>")
        for (data in revenues) {
            contents.append("<tr>")
            contents.append("<td><h4>${(data.title?:"")}</h4></td>")
            contents.append("<td><h4 style=\"color:green;\">${(getFormattedAmountValue(-data.amount))}</h4></td>")
            contents.append("<td><h4>${(String.format("%s", format.format(data.date)))}</h4></td>")
            contents.append("</tr>")
        }
        // Expenses
        contents.append("<tr><td><h2 style=\"color:red;\">${CSV_HEADER_EXPENSE}</h2></td><td></td><td></td></tr>")

        // "Description,Amount,Date"
        contents.append("<tr style=\"color:red;\">" +
                "<td><h3>Description</h3></td>" +
                "<td><h3>Amount</h3></td>" +
                "<td><h3>Date</h3></td>" +
                "</tr>")
        for (data in expenses) {
            contents.append("<tr>")
            contents.append("<td><h4>${(data.title?:"")}</h4></td>")
            contents.append("<td><h4 style=\"color:red;\">${(getFormattedAmountValue(-data.amount))}</h4></td>")
            contents.append("<td><h4>${(String.format("%s", format.format(data.date)))}</h4></td>")
            contents.append("</tr>")
        }
        // Table end
        contents.append("</table>")

        return  contents.toString()
    }

    /**
     *
     */
    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}