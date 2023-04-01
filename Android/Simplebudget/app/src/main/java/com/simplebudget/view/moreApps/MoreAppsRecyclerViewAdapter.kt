/*
 *   Copyright 2023 Waheed Nazir
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
package com.simplebudget.view.moreApps

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.simplebudget.view.moreApps.AppModel.AppItem
import com.simplebudget.view.moreApps.MoreAppsFragment.OnListFragmentInteractionListener
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.simplebudget.R

private class MoreAppsRecyclerViewAdapter internal constructor(
    private val mValues: List<AppItem>,
    private val mListener: OnListFragmentInteractionListener?,
    private val column_count: Int
) : RecyclerView.Adapter<MoreAppsRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = if (column_count == 1) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_more_apps, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item_more_apps, parent, false)
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.appName.text = mValues[position].title
        holder.appMessage.text = mValues[position].message
        Glide.with(holder.mView.context)
            .load(Uri.parse("file:///android_asset/" + mValues[position].imagelink))
            .into(holder.categoryImage)
        holder.mView.setOnClickListener { mListener?.onListFragmentInteraction(holder.mItem) }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    internal inner class ViewHolder(val mView: View) :
        RecyclerView.ViewHolder(
            mView
        ) {
        val appName: TextView = mView.findViewById(R.id.app_name)
        val appMessage: TextView = mView.findViewById(R.id.app_message)
        val categoryImage: ImageView = mView.findViewById(R.id.category_image)
        var mItem: AppItem? = null

    }
}