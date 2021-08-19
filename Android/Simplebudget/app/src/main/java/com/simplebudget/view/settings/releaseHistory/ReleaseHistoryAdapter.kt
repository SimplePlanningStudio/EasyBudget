package com.simplebudget.view.settings.releaseHistory

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simplebudget.R

/**
 */
class ReleaseHistoryAdapter(
    private val layoutInflater: LayoutInflater,
    private val releaseHistory: List<ReleaseHistory>,
    @param:LayoutRes private val rowLayout: Int
) : RecyclerView.Adapter<ReleaseHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = layoutInflater.inflate(
            rowLayout,
            parent,
            false
        )
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val singer = releaseHistory[position]
        holder.fullName.text = singer.details
    }

    override fun getItemCount(): Int = releaseHistory.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fullName: TextView = view.findViewById<View>(R.id.full_name_tv) as TextView
    }
}
