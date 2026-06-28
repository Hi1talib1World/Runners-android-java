package com.denzo.runners.features.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.databinding.ItemRunHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onDeleteClick: (RunEntity) -> Unit
) : ListAdapter<RunEntity, HistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRunHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val run = getItem(position)
        holder.bind(run)
    }

    inner class ViewHolder(private val binding: ItemRunHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(run: RunEntity) {
            binding.textDate.text = dateFormat.format(Date(run.timestamp))
            binding.textDistance.text = String.format("%.2f km", run.distanceMeters / 1000)
            binding.textCalories.text = String.format("%.0f kcal", run.caloriesBurned)
            
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(run)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RunEntity>() {
        override fun areItemsTheSame(oldItem: RunEntity, newItem: RunEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RunEntity, newItem: RunEntity): Boolean {
            return oldItem == newItem
        }
    }
}
