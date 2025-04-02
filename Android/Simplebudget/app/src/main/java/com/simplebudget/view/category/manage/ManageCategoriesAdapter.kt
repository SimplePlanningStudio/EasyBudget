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
package com.simplebudget.view.category.manage

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.model.category.Category

@SuppressLint("NotifyDataSetChanged")
class ManageCategoriesAdapter(
    private val categoriesList: ArrayList<Category>,
    private val listener: ManageCategoriesListener,
    private val currencyCode: String
) : RecyclerView.Adapter<ManageCategoriesAdapter.ManageCategoriesViewHolder>() {

    class ManageCategoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvCategoryBudget: TextView = itemView.findViewById(R.id.tvCategoryBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageCategoriesViewHolder {
        return ManageCategoriesViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_edit_category, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ManageCategoriesViewHolder, position: Int) {
        val selectedCategory = categoriesList[position]
        holder.tvCategoryName.text = selectedCategory.name
        holder.itemView.setOnClickListener {
            listener.onCategorySelected(selectedCategory, position)
        }
    }

    fun delete(category: Category) {
        if (categoriesList.contains(category)) categoriesList.remove(category)
        notifyDataSetChanged()
    }

    fun delete(position: Int) {
        if (categoriesList.size >= position) categoriesList.removeAt(position)
        notifyDataSetChanged()
    }

    override fun getItemCount() = categoriesList.size

    interface ManageCategoriesListener {
        fun onCategorySelected(selectedCategory: Category, position: Int)
    }
}