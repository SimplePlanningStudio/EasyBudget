/*
 *   Copyright 2023 Waheed Nazir
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

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.simplebudget.R
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.model.Expense
import com.simplebudget.model.RecurringExpenseType
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Type of cell for an [Expense]
 */
private const val EXPENSE_VIEW_TYPE = 1

/**
 * Type of cell for a header
 */
private const val HEADER_VIEW_TYPE = 2

/**
 * The adapter for the [MonthlyReportFragment] recycler view.
 *
 * @author Benoit LETONDOR
 */
class MonthlyReportRecyclerViewAdapter(
    private val expenses: List<Expense>,
    private val revenues: List<Expense>,
    private val allExpensesOfThisMonth: List<DataModels.SuperParent>,
    private val appPreferences: AppPreferences
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Formatter to get day number for each date
     */
    private val dayFormatter = DateTimeFormatter.ofPattern("dd", Locale.getDefault())

// --------------------------------------->

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (HEADER_VIEW_TYPE == viewType) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_monthly_report_header_cell, parent, false)
            return HeaderViewHolder(v)
        }

        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_monthly_report_expense_cell, parent, false)
        return ExpenseViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val obj = getExpense(position) as DataModels.Parent
            val amountSpend =
                if (obj.totalCredit > obj.totalDebit) (obj.totalCredit - obj.totalDebit) else (obj.totalDebit - obj.totalCredit)
            holder.headerTitle.text = String.format(
                "%s (%s)",
                obj.category,
                CurrencyHelper.getFormattedCurrencyString(appPreferences, amountSpend)
            )
        } else {
            val viewHolder = holder as ExpenseViewHolder
            val obj = getExpense(position) as DataModels.Child
            viewHolder.expenseTitleTextView.text = obj.expense.title
            viewHolder.categoryTypeTextView.text = obj.expense.category
            viewHolder.expenseAmountTextView.text =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, -obj.expense.amount)
            viewHolder.expenseAmountTextView.setTextColor(
                ContextCompat.getColor(
                    viewHolder.view.context,
                    if (obj.expense.isRevenue()) R.color.budget_green else R.color.budget_red
                )
            )
            viewHolder.monthlyIndicator.visibility =
                if (obj.expense.isRecurring()) View.VISIBLE else View.GONE

            if (obj.expense.isRecurring()) {
                when (obj.expense.associatedRecurringExpense?.type
                    ?: RecurringExpenseType.NOTHING) {
                    RecurringExpenseType.DAILY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.daily)
                    RecurringExpenseType.WEEKLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.weekly)
                    RecurringExpenseType.BI_WEEKLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.bi_weekly)
                    RecurringExpenseType.TER_WEEKLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.ter_weekly)
                    RecurringExpenseType.FOUR_WEEKLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.four_weekly)
                    RecurringExpenseType.MONTHLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.monthly)
                    RecurringExpenseType.BI_MONTHLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.bi_monthly)
                    RecurringExpenseType.TER_MONTHLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.ter_monthly)
                    RecurringExpenseType.SIX_MONTHLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.six_monthly)
                    RecurringExpenseType.YEARLY -> viewHolder.recurringExpenseTypeTextView.text =
                        viewHolder.view.context.getString(R.string.yearly)
                    else -> {}
                }
            }
            viewHolder.dateTextView.text = dayFormatter.format(obj.expense.date)
        }
    }

    override fun getItemCount() = allExpensesOfThisMonth.size

    override fun getItemViewType(position: Int) =
        if (getExpense(position) is DataModels.Parent) HEADER_VIEW_TYPE else EXPENSE_VIEW_TYPE

    /**
     * Get the expense for the given position
     *
     * @param position the position
     * @return the expense for that position
     */
    private fun getExpense(position: Int): DataModels.SuperParent =
        allExpensesOfThisMonth[position]

// --------------------------------------->

    class ExpenseViewHolder internal constructor(internal val view: View) :
        RecyclerView.ViewHolder(view) {
        internal val expenseTitleTextView: TextView = view.findViewById(R.id.expense_title)
        internal val expenseAmountTextView: TextView = view.findViewById(R.id.expense_amount)
        internal val monthlyIndicator: ViewGroup = view.findViewById(R.id.recurring_indicator)
        internal val dateTextView: TextView = view.findViewById(R.id.date_tv)
        internal val recurringExpenseTypeTextView: TextView =
            view.findViewById(R.id.recurring_expense_type)
        internal val categoryTypeTextView: TextView = view.findViewById(R.id.category_type)
    }

    class HeaderViewHolder internal constructor(internal val view: View) :
        RecyclerView.ViewHolder(view) {
        internal val headerTitle: TextView = view.findViewById(R.id.monthly_recycler_view_header_tv)
    }
}
