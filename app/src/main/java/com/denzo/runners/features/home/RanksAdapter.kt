package com.denzo.runners.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.R
import com.denzo.runners.databinding.ItemPodiumBinding
import com.denzo.runners.databinding.ItemRankingBinding

sealed class RankingItem {
    data class Podium(val athletes: List<AthleteRank>) : RankingItem()
    data class Rank(val athlete: AthleteRank) : RankingItem()

    val id: String
        get() = when (this) {
            is Podium -> "podium_header"
            is Rank -> athlete.rank + athlete.name
        }
}

class RanksAdapter : ListAdapter<RankingItem, RecyclerView.ViewHolder>(DiffCallback) {

    private enum class ViewType { PODIUM, RANK }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RankingItem.Podium -> ViewType.PODIUM.ordinal
            is RankingItem.Rank -> ViewType.RANK.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (ViewType.values()[viewType]) {
            ViewType.PODIUM -> {
                PodiumViewHolder(ItemPodiumBinding.inflate(inflater, parent, false))
            }
            ViewType.RANK -> {
                RankViewHolder(ItemRankingBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PodiumViewHolder -> holder.bind((item as RankingItem.Podium).athletes)
            is RankViewHolder -> holder.bind((item as RankingItem.Rank).athlete)
        }
    }

    class PodiumViewHolder(private val binding: ItemPodiumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(athletes: List<AthleteRank>) {
            athletes.forEach { athlete ->
                when (athlete.rank) {
                    "01" -> binding.name1.text = athlete.name
                    "02" -> binding.name2.text = athlete.name
                    "03" -> binding.name3.text = athlete.name
                }
            }
        }
    }

    class RankViewHolder(private val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(athlete: AthleteRank) {
            binding.textRank.text = athlete.rank
            binding.textName.text = athlete.name
            binding.textTeam.text = athlete.team
            binding.textDistance.text = athlete.distance

            val context = binding.root.context
            if (athlete.isMe) {
                binding.rankingCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.runners_volt))
                binding.textRank.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                binding.textName.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                binding.textTeam.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                binding.textDistance.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
            } else {
                binding.rankingCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.runners_surface))
                binding.textRank.setTextColor(ContextCompat.getColor(context, R.color.runners_text_secondary))
                binding.textName.setTextColor(ContextCompat.getColor(context, R.color.runners_text_primary))
                binding.textTeam.setTextColor(ContextCompat.getColor(context, R.color.runners_text_muted))
                binding.textDistance.setTextColor(ContextCompat.getColor(context, R.color.runners_text_primary))
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RankingItem>() {
        override fun areItemsTheSame(oldItem: RankingItem, newItem: RankingItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RankingItem, newItem: RankingItem): Boolean {
            return oldItem == newItem
        }
    }
}
