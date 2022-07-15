package com.simplebudget.view.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R

class CategorySearchAdapter(
    private val categoriesList: List<String>,
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
        val categoryName: String = categoriesList[position]
        holder.titleTextView.text = categoryName
        holder.itemView.setOnClickListener {
            listener.onCategorySelected(categoryName)
        }
    }

    override fun getItemCount() = categoriesList.size

    interface CategoryAdapterListener {
        fun onCategorySelected(selectedCategory: String)
    }
}