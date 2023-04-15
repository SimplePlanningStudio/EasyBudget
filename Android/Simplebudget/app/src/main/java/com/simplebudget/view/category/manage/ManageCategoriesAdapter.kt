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
import com.simplebudget.model.category.ExpenseCategoryType

class ManageCategoriesAdapter(
    private val categoriesList: ArrayList<Category>, private val listener: ManageCategoriesListener
) : RecyclerView.Adapter<ManageCategoriesAdapter.ManageCategoriesViewHolder>() {

    class ManageCategoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val editIcon: ImageView = itemView.findViewById(R.id.edit_icon)
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
        val categoryName: String = selectedCategory.name
        holder.tvCategoryName.text = categoryName
        holder.editIcon.visibility =
            if (categoryName == ExpenseCategoryType.MISCELLANEOUS.name) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener {
            listener.onCategorySelected(selectedCategory, position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun delete(category: Category) {
        if (categoriesList.contains(category)) categoriesList.remove(category)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun delete(position: Int) {
        if (categoriesList.size >= position) categoriesList.removeAt(position)
        notifyDataSetChanged()
    }

    override fun getItemCount() = categoriesList.size

    interface ManageCategoriesListener {
        fun onCategorySelected(selectedCategory: Category, position: Int)
    }
}