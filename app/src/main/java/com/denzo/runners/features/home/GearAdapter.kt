package com.denzo.runners.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.databinding.ItemGearBinding

class GearAdapter(
    private val onGearClick: (GearEntity) -> Unit
) : ListAdapter<GearEntity, GearAdapter.ViewHolder>(DiffCallback) {

    private var isMetric: Boolean = true

    fun setUnitSystem(isMetric: Boolean) {
        if (this.isMetric != isMetric) {
            this.isMetric = isMetric
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ItemGearBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemGearBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(gear: GearEntity) {
            b.tvShoeName.text = gear.model
            b.tvShoeBrand.text = gear.brand.uppercase()
            
            val progress = (gear.currentMileageMeters * 100 / gear.maxMileageMeters).toInt()
            b.pbShoeLife.progress = progress
            
            val current = UnitConverter.formatDistance(gear.currentMileageMeters, isMetric)
            val total = UnitConverter.formatDistance(gear.maxMileageMeters, isMetric)
            b.tvShoeMileage.text = "$current / $total"

            b.chipStatus.text = if (gear.isActive) "ACTIVE" else if (gear.isRetired) "RETIRED" else "INACTIVE"
            
            b.root.setOnClickListener { onGearClick(gear) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<GearEntity>() {
        override fun areItemsTheSame(oldItem: GearEntity, newItem: GearEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GearEntity, newItem: GearEntity): Boolean {
            return oldItem == newItem
        }
    }
}
