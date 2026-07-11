package com.denzo.runners.features.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.data.local.entities.RunEntity
import com.denzo.runners.databinding.ItemRunHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (RunEntity) -> Unit,
    private val onDeleteClick: (RunEntity) -> Unit
) : ListAdapter<RunEntity, HistoryAdapter.ViewHolder>(DiffCallback) {

    private var isMetric: Boolean = true

    fun setUnitSystem(isMetric: Boolean) {
        this.isMetric = isMetric
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRunHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRunHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault())

        fun bind(run: RunEntity) {
            binding.textDate.text = dateFormat.format(Date(run.timestamp))
            binding.textDistance.text = UnitConverter.formatDistance(run.distanceMeters, isMetric)
            binding.textDuration.text = UnitConverter.formatDuration(run.durationSeconds)
            binding.textPace.text = UnitConverter.formatPace(run.avgPace, isMetric)

            binding.root.setOnClickListener { onItemClick(run) }
            binding.btnDelete.setOnClickListener { onDeleteClick(run) }
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
