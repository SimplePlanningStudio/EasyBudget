package com.simplebudget.view.category.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.model.category.Category

class CategorySearchAdapter(
    private val categoriesList: List<Category>,
    private val listener: CategoryAdapterListener
) : RecyclerView.Adapter<CategorySearchAdapter.SearchViewHolder>() {

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
        holder.itemView.setOnClickListener {
            listener.onCategorySelected(category)
        }
    }

    override fun getItemCount() = categoriesList.size

    interface CategoryAdapterListener {
        fun onCategorySelected(selectedCategory: Category)
    }
}