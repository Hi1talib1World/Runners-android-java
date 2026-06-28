package com.denzo.runners.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.R
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.data.local.entities.ChallengeEntity
import com.denzo.runners.databinding.FragmentRanksBinding
import com.denzo.runners.databinding.ItemChallengeBinding
import com.denzo.runners.databinding.ItemRankingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RanksFragment : Fragment() {

    private var _binding: FragmentRanksBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RanksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRanksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.tabGlobal.setOnClickListener { viewModel.onTabSelected(RanksTab.GLOBAL) }
        binding.tabClubs.setOnClickListener { viewModel.onTabSelected(RanksTab.CLUBS) }
        binding.tabFriends.setOnClickListener { viewModel.onTabSelected(RanksTab.FRIENDS) }
        binding.tabChallenges.setOnClickListener { viewModel.onTabSelected(RanksTab.CHALLENGES) }

        binding.btnSearch.setOnClickListener {
            findNavController().navigate(R.id.navigation_search)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: RanksUiState) {
        binding.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.ranksContent.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        updateTabs(state.selectedTab)

        if (!state.isLoading) {
            if (state.selectedTab == RanksTab.CHALLENGES) {
                renderChallenges(state.challenges)
            } else {
                renderRankings(state.podium, state.rankings)
            }
        }
    }

    private fun renderRankings(podium: List<AthleteRank>, rankings: List<AthleteRank>) {
        binding.podiumContainer.visibility = View.VISIBLE
        
        podium.forEachIndexed { _, athlete ->
            when (athlete.rank) {
                "01" -> binding.name1.text = athlete.name
                "02" -> binding.name2.text = athlete.name
                "03" -> binding.name3.text = athlete.name
            }
        }

        binding.rankingsList.removeAllViews()
        rankings.forEach { athlete ->
            val itemBinding = ItemRankingBinding.inflate(layoutInflater, binding.rankingsList, false)
            itemBinding.textRank.text = athlete.rank
            itemBinding.textName.text = athlete.name
            itemBinding.textTeam.text = athlete.team
            itemBinding.textDistance.text = athlete.distance
            
            if (athlete.isMe) {
                itemBinding.rankingCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                itemBinding.textRank.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                itemBinding.textName.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                itemBinding.textTeam.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                itemBinding.textDistance.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
            }
            binding.rankingsList.addView(itemBinding.root)
        }
    }

    private fun renderChallenges(challenges: List<ChallengeEntity>) {
        binding.podiumContainer.visibility = View.GONE
        binding.rankingsList.removeAllViews()
        
        challenges.forEach { challenge ->
            val itemBinding = ItemChallengeBinding.inflate(layoutInflater, binding.rankingsList, false)
            itemBinding.tvChallengeName.text = challenge.name
            itemBinding.tvChallengeDesc.text = challenge.description
            itemBinding.ivChallengeMedal.setImageResource(challenge.medalIconResId)
            
            if (challenge.isJoined) {
                itemBinding.btnJoinChallenge.text = "JOINED"
                itemBinding.btnJoinChallenge.isEnabled = false
                itemBinding.btnJoinChallenge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_card_bg))
                itemBinding.btnJoinChallenge.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_text_secondary))
                
                itemBinding.challengeProgress.visibility = View.VISIBLE
                itemBinding.tvChallengeStatus.visibility = View.VISIBLE
            } else {
                itemBinding.btnJoinChallenge.text = "JOIN"
                itemBinding.btnJoinChallenge.isEnabled = true
                itemBinding.btnJoinChallenge.setOnClickListener { viewModel.onJoinChallenge(challenge.id) }
                
                itemBinding.challengeProgress.visibility = View.GONE
                itemBinding.tvChallengeStatus.visibility = View.GONE
            }
            
            binding.rankingsList.addView(itemBinding.root)
        }
    }

    private fun updateTabs(selected: RanksTab) {
        val tabs = listOf(binding.tabGlobal, binding.tabClubs, binding.tabFriends, binding.tabChallenges)
        tabs.forEach { it.setBackgroundColor(0) }
        tabs.forEach { it.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_text_secondary)) }

        val activeTab = when (selected) {
            RanksTab.GLOBAL -> binding.tabGlobal
            RanksTab.CLUBS -> binding.tabClubs
            RanksTab.FRIENDS -> binding.tabFriends
            RanksTab.CHALLENGES -> binding.tabChallenges
        }

        activeTab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
        activeTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
