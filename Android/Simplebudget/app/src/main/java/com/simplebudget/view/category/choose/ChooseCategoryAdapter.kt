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
package com.simplebudget.view.category.choose

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.model.category.Category

class ChooseCategoryAdapter(
    private val categoriesList: List<Category>,
    private val onCategorySelected: (selectedCategory: Category) -> Unit,
    private val onCategoryChosen: (selectedCategory: Category) -> Unit = {},
    private val isMultiSelect: Boolean = false// Add this flag to determine selection mode
) : RecyclerView.Adapter<ChooseCategoryAdapter.SearchViewHolder>() {

    private val selectedCategories = mutableSetOf<Category>()

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.search_title_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_category, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val category = categoriesList[position]
        holder.titleTextView.text = category.name
        // Show the checkbox only if it's multi-select mode
        holder.itemView.isSelected = selectedCategories.contains(category)

        holder.itemView.setOnClickListener {
            if (isMultiSelect) {
                onCategoryChosen.invoke(category)
                if (selectedCategories.contains(category)) {
                    selectedCategories.remove(category)
                } else {
                    selectedCategories.add(category)
                }
                notifyDataSetChanged()
            } else {
                selectedCategories.clear()
                selectedCategories.add(category)
                onCategorySelected(category)

            }
        }

    }

    fun getSelectedCategories() = selectedCategories.toList()
    override fun getItemCount() = categoriesList.size
}