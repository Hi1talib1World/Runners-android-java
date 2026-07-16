package com.denzo.runners.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.databinding.FragmentRanksBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RanksFragment : Fragment() {

    private var _binding: FragmentRanksBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RanksViewModel by viewModels()
    private lateinit var ranksAdapter: RanksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRanksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        observeUiState()
    }

    private fun setupRecyclerView() {
        ranksAdapter = RanksAdapter { challengeId ->
            viewModel.onJoinChallenge(challengeId)
        }
        binding.recyclerViewRanks.adapter = ranksAdapter
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

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onTabSelected(viewModel.uiState.value.selectedTab)
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
        binding.loadingIndicator.isVisible = state.isLoading
        binding.swipeRefresh.isRefreshing = state.isLoading
        binding.swipeRefresh.isVisible = !state.isLoading

        updateTabs(state.selectedTab)

        if (!state.isLoading) {
            val items = mutableListOf<RankingItem>()
            if (state.selectedTab == RanksTab.CHALLENGES) {
                items.addAll(state.challenges.map { RankingItem.Challenge(it) })
            } else {
                if (state.podium.isNotEmpty()) {
                    items.add(RankingItem.Podium(state.podium))
                }
                items.addAll(state.rankings.map { RankingItem.Rank(it) })
            }
            ranksAdapter.submitList(items)
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
