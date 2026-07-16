package com.denzo.runners.features.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.databinding.ItemFeedActivityBinding

class FeedAdapter(
    private val onKudosClick: (String) -> Unit
) : ListAdapter<FeedActivity, FeedAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemFeedActivityBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeedActivity) {
            b.tvAthleteName.text = item.athleteName
            b.tvMetrics.text = "${item.distanceKm} KM • ${item.duration}"
            b.tvKudosCount.text = item.kudosCount.toString()
            b.btnKudos.setOnClickListener { onKudosClick(item.id) }
            
            b.liveBadge.visibility = if (item.isLive) View.VISIBLE else View.GONE
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FeedActivity>() {
        override fun areItemsTheSame(oldItem: FeedActivity, newItem: FeedActivity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FeedActivity, newItem: FeedActivity): Boolean {
            return oldItem == newItem
        }
    }
}
