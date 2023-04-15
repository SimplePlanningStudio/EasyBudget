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
package com.simplebudget.view.search

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.simplebudget.R
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpenseType
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The adapter for the [SearchRecyclerViewAdapter] recycler view.
 *
 * @author Benoit LETONDOR
 */
class SearchRecyclerViewAdapter(
    private var allExpensesOfThisMonth: List<Expense>,
    private val appPreferences: AppPreferences
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Formatter to get day number for each date
     */
    private val dayFormatter = DateTimeFormatter.ofPattern("E,dd/MMM/yyyy", Locale.getDefault())

// --------------------------------------->

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_search_item_expense_cell, parent, false)
        return ExpenseViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = holder as ExpenseViewHolder
        val expense = getExpense(position)
        viewHolder.expenseTitleTextView.text = expense.title
        viewHolder.categoryTypeTextView.text = expense.category
        viewHolder.expenseAmountTextView.text =
            CurrencyHelper.getFormattedCurrencyString(appPreferences, -expense.amount)
        viewHolder.expenseAmountTextView.setTextColor(
            ContextCompat.getColor(
                viewHolder.view.context,
                if (expense.isRevenue()) R.color.budget_green else R.color.budget_red
            )
        )
        viewHolder.monthlyIndicator.visibility =
            if (expense.isRecurring()) View.VISIBLE else View.GONE

        val expenseType: String = if (expense.isFutureExpense()) {
            viewHolder.view.context.getString(R.string.future_expense)
        } else if (expense.isPastExpense()) {
            viewHolder.view.context.getString(R.string.past_expense)
        } else {
            viewHolder.view.context.getString(R.string.today_expense)
        }
        viewHolder.futureExpense.text = expenseType

        if (expense.isRecurring()) {
            when (expense.associatedRecurringExpense?.type
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
        viewHolder.dateTextView.text = dayFormatter.format(expense.date)
    }

    /**
     * Items count
     */
    override fun getItemCount() = allExpensesOfThisMonth.size

    /**
     * Get the expense for the given position
     *
     * @param position the position
     * @return the expense for that position
     */
    private fun getExpense(position: Int): Expense = allExpensesOfThisMonth[position]


// --------------------------------------->

    class ExpenseViewHolder internal constructor(internal val view: View) :
        RecyclerView.ViewHolder(view) {
        internal val expenseTitleTextView: TextView = view.findViewById(R.id.expense_title)
        internal val expenseAmountTextView: TextView = view.findViewById(R.id.expense_amount)
        internal val monthlyIndicator: ViewGroup = view.findViewById(R.id.recurring_indicator)
        internal val futureExpense: TextView = view.findViewById(R.id.future_expense)
        internal val dateTextView: TextView = view.findViewById(R.id.date_tv)
        internal val recurringExpenseTypeTextView: TextView =
            view.findViewById(R.id.recurring_expense_type)
        internal val categoryTypeTextView: TextView = view.findViewById(R.id.category_type)
    }
}
