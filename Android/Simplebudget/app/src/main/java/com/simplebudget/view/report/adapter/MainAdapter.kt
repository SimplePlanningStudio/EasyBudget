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
package com.simplebudget.view.report.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplebudget.R
import com.simplebudget.databinding.RecyclerviewMonthlyReportHeaderCellBinding
import com.simplebudget.helper.CurrencyHelper
import com.simplebudget.helper.banner.AppBanner
import com.simplebudget.prefs.AppPreferences
import com.simplebudget.view.report.DataModels
import com.simplebudget.view.report.MonthlyReportViewModel


private const val TYPE_CONTENT = 0
private const val TYPE_BANNER = 1

class MainAdapter(
    private val allExpensesParentList: List<DataModels.SuperParent>,
    private val appPreferences: AppPreferences,
    private val onBannerClick: (AppBanner) -> Unit, // Callback function
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var context: Context


    override fun getItemViewType(position: Int): Int {
        return when (allExpensesParentList[position]) {
            is DataModels.CustomTriple.Data -> TYPE_CONTENT
            else -> TYPE_BANNER

        }
    }

    class CollectionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RecyclerviewMonthlyReportHeaderCellBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_BANNER) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
            BannerViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_monthly_report_header_cell, parent, false)
            context = parent.context
            CollectionsViewHolder(view)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = allExpensesParentList[position]) {
            is DataModels.CustomTriple.Data -> {
                (holder as CollectionsViewHolder).binding.apply {
                    val obj = allExpensesParentList[position] as DataModels.CustomTriple.Data
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

            is DataModels.BannerItem -> (holder as BannerViewHolder).bind(
                item.banner!!
            )
        }
    }

    override fun getItemCount() = allExpensesParentList.size

    // Banner ViewHolder
    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val bannerImage: ImageView = view.findViewById(R.id.banner_image)
        private val bannerTitle: TextView = view.findViewById(R.id.banner_title)
        private val bannerDescription: TextView = view.findViewById(R.id.banner_description)

        fun bind(banner: AppBanner?) {
            banner?.let {
                bannerTitle.text = banner.title
                bannerDescription.text = banner.description

                Glide.with(bannerTitle.context).load(banner.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).into(bannerImage)

                itemView.setOnClickListener {
                    onBannerClick.invoke(banner)
                }
            }
        }
    }
}