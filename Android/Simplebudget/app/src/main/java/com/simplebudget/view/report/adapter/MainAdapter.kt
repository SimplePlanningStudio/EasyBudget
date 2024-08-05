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
package com.simplebudget.view.report.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.databinding.RecyclerviewMonthlyReportHeaderCellBinding
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.report.DataModels
import com.simplebudget.view.report.MonthlyReportViewModel

class MainAdapter(
    private val allExpensesParentList: List<DataModels.CustomTriple.Data>,
    private val appPreferences: AppPreferences
) :
    RecyclerView.Adapter<MainAdapter.CollectionsViewHolder>() {

    lateinit var context: Context

    class CollectionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RecyclerviewMonthlyReportHeaderCellBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionsViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_monthly_report_header_cell, parent, false)
        context = parent.context
        return CollectionsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionsViewHolder, position: Int) {
        holder.binding.apply {
            val obj = allExpensesParentList[position]
            monthlyRecyclerViewHeaderTv.text = String.format(
                "%s (%s)",
                obj.category,
                CurrencyHelper.getFormattedCurrencyString(appPreferences, obj.amountSpend)
            )

            val subItemAdapter = SubItemAdapter(obj.expenses, appPreferences)
            rvSubItem.adapter = subItemAdapter
            rvSubItem.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            ivExpandableIcon.setBackgroundResource(if (rvSubItem.isShown) R.drawable.ic_expandable_opened else R.drawable.ic_expandable_closed)
            headerCardView.setOnClickListener {
                rvSubItem.visibility = if (rvSubItem.isShown) View.GONE else View.VISIBLE
                ivExpandableIcon.setBackgroundResource(if (rvSubItem.isShown) R.drawable.ic_expandable_opened else R.drawable.ic_expandable_closed)
            }
        }
    }

    override fun getItemCount() = allExpensesParentList.size
}