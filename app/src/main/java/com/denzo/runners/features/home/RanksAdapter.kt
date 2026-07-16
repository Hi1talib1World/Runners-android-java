package com.denzo.runners.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.ChallengeEntity
import com.denzo.runners.databinding.ItemChallengeBinding
import com.denzo.runners.databinding.ItemPodiumBinding
import com.denzo.runners.databinding.ItemRankingBinding

sealed class RankingItem {
    data class Podium(val athletes: List<AthleteRank>) : RankingItem()
    data class Rank(val athlete: AthleteRank) : RankingItem()
    data class Challenge(val challenge: ChallengeEntity) : RankingItem()

    val id: String
        get() = when (this) {
            is Podium -> "podium_header"
            is Rank -> "rank_${athlete.rank}_${athlete.name}"
            is Challenge -> "challenge_${challenge.id}"
        }
}

class RanksAdapter(
    private val onJoinChallenge: (Int) -> Unit
) : ListAdapter<RankingItem, RecyclerView.ViewHolder>(DiffCallback) {

    private enum class ViewType { PODIUM, RANK, CHALLENGE }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RankingItem.Podium -> ViewType.PODIUM.ordinal
            is RankingItem.Rank -> ViewType.RANK.ordinal
            is RankingItem.Challenge -> ViewType.CHALLENGE.ordinal
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
            ViewType.CHALLENGE -> {
                ChallengeViewHolder(ItemChallengeBinding.inflate(inflater, parent, false), onJoinChallenge)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PodiumViewHolder -> holder.bind((item as RankingItem.Podium).athletes)
            is RankViewHolder -> holder.bind((item as RankingItem.Rank).athlete)
            is ChallengeViewHolder -> holder.bind((item as RankingItem.Challenge).challenge)
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

    class ChallengeViewHolder(
        private val binding: ItemChallengeBinding,
        private val onJoinChallenge: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(challenge: ChallengeEntity) {
            binding.tvChallengeName.text = challenge.name
            binding.tvChallengeDesc.text = challenge.description
            binding.ivChallengeMedal.setImageResource(challenge.medalIconResId)
            
            val context = binding.root.context
            if (challenge.isJoined) {
                binding.btnJoinChallenge.text = "JOINED"
                binding.btnJoinChallenge.isEnabled = false
                binding.btnJoinChallenge.setBackgroundColor(ContextCompat.getColor(context, R.color.runners_card_bg))
                binding.btnJoinChallenge.setTextColor(ContextCompat.getColor(context, R.color.runners_text_secondary))
                
                binding.challengeProgress.visibility = View.VISIBLE
                binding.tvChallengeStatus.visibility = View.VISIBLE
            } else {
                binding.btnJoinChallenge.text = "JOIN"
                binding.btnJoinChallenge.isEnabled = true
                binding.btnJoinChallenge.setBackgroundColor(ContextCompat.getColor(context, R.color.runners_volt))
                binding.btnJoinChallenge.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
                binding.btnJoinChallenge.setOnClickListener { onJoinChallenge(challenge.id) }
                
                binding.challengeProgress.visibility = View.GONE
                binding.tvChallengeStatus.visibility = View.GONE
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
