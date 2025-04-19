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
package com.simplebudget.view.report

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplebudget.R
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.CurrencyHelper.getFormattedAmountValue
import com.simplebudget.helper.Logger
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.prefs.activeAccountLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.core.graphics.scale
import com.simplebudget.helper.DateHelper
import java.io.FileOutputStream
import com.simplebudget.helper.extensions.truncateWithEllipsis
import com.simplebudget.helper.formatLocalDate
import com.simplebudget.helper.getMonthTitle

/**
 * Constants Values
 */
const val CSV_TITLE_EXPENSE_TOTAL = "Expenses Total"
const val CSV_TITLE_INCOMES_TOTAL = "Revenues Total"
const val CSV_TITLE_BALANCE = "Balance"
const val CSV_HEADER_EXPENSE = "Expenses"
const val CSV_HEADER_INCOMES = "Revenues"
private const val startOfPageX = 30f
private const val pageWidthX = 555f
private const val pageHeightY = 820f
private const val expenseItemDateStartX = 320f
private const val expenseItemAmountStartX = 410f

// Header paint (bold, larger text)
private val headerPaint = Paint().apply {
    color = Color.BLACK
    textSize = 16f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

// Footer paint (smaller, italic text)
private val footerPaint = Paint().apply {
    color = Color.GRAY
    textSize = 10f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    strokeWidth = 1.5f
    isAntiAlias = false
}

class PDFViewModel(private val appPreferences: AppPreferences) : ViewModel() {

    private val exportStatus: MutableLiveData<ExportStatus> = MutableLiveData()
    val observeExportStatus: LiveData<ExportStatus> = exportStatus

    /**
     *
     */
    fun exportCSV(
        currency: Currency,
        month: LocalDate,
        file: File,
        data: DataModels.MonthlyReportData.Data,
    ) {
        try {
            if (data.expenses.isEmpty() && data.revenues.isEmpty()) {
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
                fileWriter.append(getFormattedAmountValue(data.revenuesAmount))
                fileWriter.append("\n")

                // Expense Total
                fileWriter.append("$CSV_TITLE_EXPENSE_TOTAL (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(getFormattedAmountValue(data.expensesAmount))
                fileWriter.append("\n")

                // Balance Total
                fileWriter.append("$CSV_TITLE_BALANCE (${currency.symbol ?: currency.displayName})")
                fileWriter.append(",")
                fileWriter.append(getFormattedAmountValue((data.revenuesAmount - data.expensesAmount)))
                fileWriter.append("\n")
                fileWriter.append("\n")

                // Incomes
                fileWriter.append(CSV_HEADER_INCOMES)
                fileWriter.append("\n")
                fileWriter.append("Title,Amount(${currency.symbol ?: currency.displayName}),Date,Category")
                fileWriter.append("\n")

                for (data in data.revenues) {
                    fileWriter.append(data.title)
                    fileWriter.append(",")
                    fileWriter.append(getFormattedAmountValue(-data.amount))
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
                for (data in data.expenses) {
                    fileWriter.append(data.title)
                    fileWriter.append(",")
                    fileWriter.append(getFormattedAmountValue(-data.amount))
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
                Logger.error(
                    MonthlyReportViewModel::class.java.simpleName,
                    "An error occurred while exporting CSV file ${e.localizedMessage}",
                    e
                )
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
                    Logger.error(
                        MonthlyReportViewModel::class.java.simpleName,
                        "An error occurred while exporting CSV file ${e.localizedMessage}",
                        e
                    )
                }
            }

        } catch (_: Exception) {
        }
    }

    private val generatePDFReport: MutableLiveData<Uri?> = MutableLiveData()
    val observeGeneratePDFReport: LiveData<Uri?> = generatePDFReport


    private fun drawPageWithHeaderFooter(
        page: PdfDocument.Page,
        pageNumber: Int,
    ): Canvas {
        val canvas = page.canvas
        try {
            // --- Draw Header ---
            canvas.drawText("Simple Budget Expenses Report", startOfPageX, 40f, headerPaint)
            canvas.drawLine(
                startOfPageX,
                50f,
                pageWidthX,
                50f,
                Paint().apply { color = Color.LTGRAY })

            // --- Draw Footer ---
            val footerText =
                "Page ${pageNumber + 1} • Generated with SimpleBudget on ${DateHelper.today.formatLocalDate()}"
            canvas.drawText(footerText, startOfPageX, pageHeightY, footerPaint) // Bottom of page
            canvas.drawLine(
                startOfPageX,
                (pageHeightY - 20f),
                pageWidthX,
                (pageHeightY - 20f),
                Paint().apply { color = Color.LTGRAY })
        } catch (_: Exception) {
        }
        return canvas
    }

    /**
     * Generate PDF report and return the Uri
     */
    fun generatePdfReport(
        context: Context,
        month: LocalDate,
        data: DataModels.MonthlyReportData.Data,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var pdfDocument: PdfDocument? = null
            var page: PdfDocument.Page? = null
            var outputStream: FileOutputStream? = null

            try {
                var pageNumber = 0
                pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                page = pdfDocument.startPage(pageInfo)
                var canvas = drawPageWithHeaderFooter(page, pageNumber)
                val paint = Paint()

                try {
                    val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo)
                    val scaledLogo = logo.scale(100, 49)
                    canvas.drawBitmap(scaledLogo, 450f, 50f, null)
                } catch (e: Exception) {
                    Logger.debug("Pdf_Logo", "Failed to draw logo", e)
                }

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 12f
                canvas.drawText(
                    String.format("%s%s", "Month: ", month.getMonthTitle(context)),
                    startOfPageX,
                    70f,
                    paint
                )
                var yPosition = 100f
                val lines = listOf(
                    "Account: ${appPreferences.activeAccountLabel()}",
                    "Total Income: ${
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences,
                            data.revenuesAmount
                        )
                    }",
                    "Total Expense: ${
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences,
                            data.expensesAmount
                        )
                    }",
                    "Balance: ${
                        CurrencyHelper.getFormattedCurrencyString(
                            appPreferences,
                            (data.revenuesAmount - data.expensesAmount)
                        )
                    }"
                )

                for (line in lines) {
                    canvas.drawText(line, startOfPageX, yPosition, paint)
                    yPosition += 20f
                }

                // Safely iterate through expenses
                data.allExpensesOfThisMonth.forEach { data ->
                    try {
                        // Check if we need a new page
                        if (yPosition > 750f) {
                            pageNumber++
                            pdfDocument.finishPage(page)
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page?.canvas!!
                            canvas = drawPageWithHeaderFooter(page, pageNumber)
                            yPosition = 70f  // Reset Y position for new page
                        }

                        when (data) {
                            is DataModels.Parent -> {
                                val amountSpend = CurrencyHelper.getFormattedCurrencyString(
                                    appPreferences,
                                    if (data.totalCredit > data.totalDebit)
                                        data.totalCredit - data.totalDebit
                                    else data.totalDebit - data.totalCredit
                                )
                                // Background strip settings ---
                                val stripPaint = Paint().apply {
                                    color = Color.LTGRAY  // Light gray strip
                                    style = Paint.Style.FILL
                                }
                                val stripHeight = 18f      // Height of the strip
                                val stripTopMargin = 8f    // Space above the strip
                                val stripBottomMargin =
                                    6f // Extra space BELOW the strip (fixes overlap!)

                                // Draw the strip (slightly indented from page edges)
                                canvas.drawRect(
                                    22f,
                                    yPosition - stripTopMargin,
                                    570f,
                                    yPosition + stripHeight,
                                    stripPaint
                                )
                                // Draw the parent text (centered vertically in the strip)
                                paint.color = Color.BLACK
                                val textY = yPosition + (stripHeight / 2)  // Vertically center text
                                canvas.drawText(
                                    "${data.category} ($amountSpend)",
                                    startOfPageX,
                                    textY,
                                    paint
                                )
                                yPosition += stripHeight + stripBottomMargin  // Critical fix!
                            }

                            is DataModels.Child -> {
                                yPosition += 5f
                                val expense = data.expense
                                val formattedAmount = CurrencyHelper.getFormattedCurrencyString(
                                    appPreferences, -(expense.amount)
                                )
                                val expenseColor =
                                    if (expense.isRevenue()) Color.GREEN else Color.RED
                                val childPaint = Paint(paint).apply { color = expenseColor }

                                canvas.drawText(
                                    expense.title.truncateWithEllipsis(maxLength = 50),
                                    startOfPageX,
                                    yPosition,
                                    paint
                                )
                                canvas.drawText(
                                    expense.date.formatLocalDate("dd MMM yyyy"),
                                    expenseItemDateStartX,
                                    yPosition,
                                    paint
                                )
                                canvas.drawText(
                                    formattedAmount,
                                    expenseItemAmountStartX,
                                    yPosition,
                                    childPaint
                                )
                                yPosition += 10f
                            }

                            else -> {
                                Logger.debug(
                                    "PDF_REPORT",
                                    "Unknown data type: ${data.javaClass.simpleName}"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Logger.debug("PDF_REPORT", "Error processing expense item", e)
                    }
                    yPosition += 5f
                }
                // Final page operations
                page?.let { pdfDocument.finishPage(it) }

                val fileName = "SimpleBudget_Report_${
                    LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM_yyyy"))
                }_${System.currentTimeMillis()}.pdf"
                val file =
                    File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                outputStream = FileOutputStream(file)
                pdfDocument.writeTo(outputStream)

                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    generatePDFReport.value = uri
                }

            } catch (e: Exception) {
                Logger.error("PDF_REPORT", "Failed at step: ${e.stackTraceToString()}", e)
                generatePDFReport.value = null
            } finally {
                try {
                    page?.let { pdfDocument?.finishPage(it) }  // Finish the current page if it exists
                    pdfDocument?.close()
                    outputStream?.close()
                } catch (e: Exception) {
                    Logger.debug("PDF_REPORT", "Error closing resources", e)
                }
            }
        }
    }
}