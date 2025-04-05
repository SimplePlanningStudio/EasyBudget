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
package com.simplebudget.view.breakdown

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.simplebudget.R
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.prefs.AppPreferences

/**
 *
 */
class BreakDownRecyclerViewAdapter(
    private val allExpensesOfThisMonth: List<BreakDownViewModel.CategoryWiseExpense>,
    private val totalExpenses: Double,
    private val appPreferences: AppPreferences
) : RecyclerView.Adapter<BreakDownRecyclerViewAdapter.BreakDownViewHolder>() {

    /**
     *
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakDownViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_monthly_breakdown_cell, parent, false)
        return BreakDownViewHolder(v)
    }

    override fun onBindViewHolder(holder: BreakDownViewHolder, position: Int) {
        val obj = getExpense(position)
        obj?.let {
            holder.headerTitle.text = String.format(
                "%s (%s)",
                obj.category,
                CurrencyHelper.getFormattedCurrencyString(appPreferences, obj.amountSpend)
            )
            val percent = ((obj.amountSpend / totalExpenses) * 100)
            holder.percentage.text = String.format(
                "%s%s",
                CurrencyHelper.getFormattedAmountValue(percent),
                "%"
            )
            holder.progress.setProgress(percent.toInt(), true)
        }
    }

    override fun getItemCount() = allExpensesOfThisMonth.size

    /**
     * Get the expense for the given position
     * @param position the position
     * @return the expense for that position
     */
    private fun getExpense(position: Int): BreakDownViewModel.CategoryWiseExpense? =
        if (itemCount > position) allExpensesOfThisMonth[position] else null

    // --------------------------------------->
    class BreakDownViewHolder internal constructor(internal val view: View) :
        RecyclerView.ViewHolder(view) {
        internal val headerTitle: TextView = view.findViewById(R.id.title)
        internal val percentage: TextView = view.findViewById(R.id.percentage)
        internal val progress: ProgressBar = view.findViewById(R.id.progress)
    }
}
