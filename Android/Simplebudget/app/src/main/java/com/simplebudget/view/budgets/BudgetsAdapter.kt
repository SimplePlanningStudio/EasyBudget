package com.simplebudget.view.budgets

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.extensions.getRealValueFromDB
import com.simplebudget.helper.extensions.namesAsCommaSeparatedString
import com.simplebudget.helper.getFormattedDate
import com.simplebudget.model.budget.Budget
import com.simplebudget.prefs.AppPreferences
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("NotifyDataSetChanged")
class BudgetsAdapter(
    private val context: Context,
    private val budgets: ArrayList<Budget>,
    private val appPreferences: AppPreferences,
    private val onBudgetSelected: (budget: Budget, position: Int) -> Unit,
) : RecyclerView.Adapter<BudgetsAdapter.ManageCategoriesViewHolder>() {

    class ManageCategoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvLimit: TextView = itemView.findViewById(R.id.tvLimit)
        val tvSpent: TextView = itemView.findViewById(R.id.tvSpent)
        val tvRemaining: TextView = itemView.findViewById(R.id.tvRemaining)
        val tvStartDate: TextView = itemView.findViewById(R.id.tvStartDate)
        val tvEndDate: TextView = itemView.findViewById(R.id.tvEndDate)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val budgetProgress: ProgressBar = itemView.findViewById(R.id.budgetProgress)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageCategoriesViewHolder {
        return ManageCategoriesViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_budget, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ManageCategoriesViewHolder, position: Int) {
        val budget = budgets[position]
        holder.tvDescription.text = budget.goal
        holder.tvCategory.text = String.format(
            "%s%s %s",
            context.getString(R.string.categories_for_budget),
            ":",
            budget.categories.namesAsCommaSeparatedString()
        )
        if (budget.associatedRecurringBudget == null) {
            holder.tvStartDate.visibility = View.VISIBLE
            holder.tvEndDate.visibility = View.VISIBLE
            holder.tvStartDate.text = String.format(
                "%s %s",
                context.getString(R.string.start),
                budget.startDate.getFormattedDate(context)
            )
            holder.tvEndDate.text = String.format(
                "%s %s",
                context.getString(R.string.end),
                budget.endDate.getFormattedDate(context)
            )
        } else {
            holder.tvStartDate.visibility = View.VISIBLE
            holder.tvEndDate.visibility = View.GONE
            holder.tvStartDate.text = context.getString(R.string.recurring_monthly)
        }

        val budgeAmount = CurrencyHelper.getFormattedCurrencyString(
            appPreferences,
            budget.budgetAmount
        )
        val spentAmount = if (budget.spentAmount < 0) -budget.spentAmount else budget.spentAmount
        val spentAmountDisplay = CurrencyHelper.getFormattedCurrencyString(
            appPreferences,
            if (budget.spentAmount < 0) -budget.spentAmount else budget.spentAmount
        )
        val remainingAmount = budget.budgetAmount - budget.spentAmount
        val remainingAmountDisplay =
            CurrencyHelper.getFormattedCurrencyString(appPreferences, remainingAmount)
        val progress: Int =
            (((if (budget.spentAmount < 0) 0.0 else budget.spentAmount) / budget.budgetAmount) * 100).roundToInt()

        holder.tvLimit.text = String.format("%s %s", context.getString(R.string.limit), budgeAmount)
        holder.tvSpent.text =
            String.format("%s %s", context.getString(R.string.spent), spentAmountDisplay)
        if (spentAmount != 0.0)
            holder.tvSpent.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (budget.spentAmount < 0) R.color.budget_green else R.color.budget_red
                )
            )
        holder.tvRemaining.text =
            String.format(
                "%s %s",
                if (remainingAmount > budget.budgetAmount) context.getString(R.string.surplus) else context.getString(
                    R.string.remaining
                ),
                remainingAmountDisplay
            )
        holder.budgetProgress.setProgress(progress, true)
        if (progress != 0) {
            holder.tvPercentage.text = String.format(Locale.getDefault(), "%d%s", progress, "%")
            holder.tvPercentage.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (progress <= 75) R.color.budget_green else R.color.budget_red
                )
            )
        }

        holder.tvStatus.text = BudgetStatus.get(budget.budgetAmount, budget.spentAmount)
        holder.itemView.setOnClickListener {
            onBudgetSelected(budget, position)
        }
    }

    fun delete(budget: Budget) {
        if (budgets.contains(budget)) budgets.remove(budget)
        notifyDataSetChanged()
    }

    fun delete(position: Int) {
        if (budgets.size >= position) budgets.removeAt(position)
        notifyDataSetChanged()
    }

    override fun getItemCount() = budgets.size
}