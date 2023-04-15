package com.simplebudget.view.report.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.databinding.RecyclerviewMonthlyReportExpenseCellBinding
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.model.expense.Expense
import com.simplebudget.model.recurringexpense.RecurringExpenseType
import com.simplebudget.prefs.AppPreferences
import java.time.format.DateTimeFormatter
import java.util.*

class SubItemAdapter(
    private val subItemModel: List<Expense>,
    private val appPreferences: AppPreferences
) :
    RecyclerView.Adapter<SubItemAdapter.ViewHolder>() {

    /**
     * Formatter to get day number for each date
     */
    private val dayFormatter = DateTimeFormatter.ofPattern("dd", Locale.getDefault())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RecyclerviewMonthlyReportExpenseCellBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_monthly_report_expense_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            val obj = subItemModel[position]
            expenseTitle.text = obj.title
            categoryType.text = String.format("%s", obj.category)
            expenseAmount.text =
                CurrencyHelper.getFormattedCurrencyString(appPreferences, -obj.amount)
            expenseAmount.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (obj.isRevenue()) R.color.budget_green else R.color.budget_red
                )
            )
            recurringIndicator.visibility =
                if (obj.isRecurring()) View.VISIBLE else View.GONE

            if (obj.isRecurring()) {
                when (obj.associatedRecurringExpense?.type
                    ?: RecurringExpenseType.NOTHING) {
                    RecurringExpenseType.DAILY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.daily)
                    RecurringExpenseType.WEEKLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.weekly)
                    RecurringExpenseType.BI_WEEKLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.bi_weekly)
                    RecurringExpenseType.TER_WEEKLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.ter_weekly)
                    RecurringExpenseType.FOUR_WEEKLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.four_weekly)
                    RecurringExpenseType.MONTHLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.monthly)
                    RecurringExpenseType.BI_MONTHLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.bi_monthly)
                    RecurringExpenseType.TER_MONTHLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.ter_monthly)
                    RecurringExpenseType.SIX_MONTHLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.six_monthly)
                    RecurringExpenseType.YEARLY -> recurringExpenseType.text =
                        holder.itemView.context.getString(R.string.yearly)
                    else -> {}
                }
            }
            dateTv.text = dayFormatter.format(obj.date)
        }
    }

    override fun getItemCount() = subItemModel.size
}