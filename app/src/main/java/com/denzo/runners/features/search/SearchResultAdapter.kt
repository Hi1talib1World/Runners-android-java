package com.denzo.runners.features.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private val onActionClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(result: SearchResult) {
            b.tvResultName.text = result.name
            b.tvResultSub.text = result.description
            
            if (result.isActionTaken) {
                b.btnAction.text = "FOLLOWING"
                b.btnAction.alpha = 0.5f
            } else {
                b.btnAction.text = "FOLLOW"
                b.btnAction.alpha = 1.0f
            }

            b.btnAction.setOnClickListener { onActionClick(result) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
