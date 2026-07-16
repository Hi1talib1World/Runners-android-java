package com.denzo.runners.features.activities

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
import com.denzo.runners.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

enum class JourneyTab { MY_RUNS, GLOBAL_FEED }

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var feedAdapter: FeedAdapter

    private var currentTab = JourneyTab.MY_RUNS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupClickListeners()
        setupSwipeRefresh()
        observeState()
    }

    private fun setupAdapters() {
        historyAdapter = HistoryAdapter(
            onItemClick = { run ->
                findNavController().navigate(R.id.run_summary, Bundle().apply { putInt("runId", run.id) })
            },
            onDeleteClick = { run ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Activity")
                    .setMessage("Are you sure you want to delete this run?")
                    .setPositiveButton("DELETE") { _, _ -> historyViewModel.deleteRun(run) }
                    .setNegativeButton("CANCEL", null)
                    .show()
            }
        )
        
        feedAdapter = FeedAdapter { activityId -> 
            feedViewModel.onKudos(activityId) 
        }
        
        binding.rvHistory.adapter = if (currentTab == JourneyTab.MY_RUNS) historyAdapter else feedAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.tabGlobal.setOnClickListener { switchTab(JourneyTab.GLOBAL_FEED) }
        binding.tabFriends.setOnClickListener { switchTab(JourneyTab.MY_RUNS) } 
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            if (currentTab == JourneyTab.MY_RUNS) {
                // historyViewModel.refreshRuns() 
                historyViewModel.clearError() 
            } else {
                feedViewModel.loadFeed()
            }
        }
    }

    private fun switchTab(tab: JourneyTab) {
        if (currentTab == tab) return
        currentTab = tab
        binding.rvHistory.adapter = if (tab == JourneyTab.MY_RUNS) historyAdapter else feedAdapter
        updateTabUi()
    }

    private fun updateTabUi() {
        val context = requireContext()
        if (currentTab == JourneyTab.MY_RUNS) {
            binding.tabGlobal.setBackgroundColor(0)
            binding.tabGlobal.setTextColor(ContextCompat.getColor(context, R.color.runners_text_secondary))
            binding.tabFriends.setBackgroundColor(ContextCompat.getColor(context, R.color.runners_volt))
            binding.tabFriends.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
        } else {
            binding.tabGlobal.setBackgroundColor(ContextCompat.getColor(context, R.color.runners_volt))
            binding.tabGlobal.setTextColor(ContextCompat.getColor(context, R.color.onPrimary))
            binding.tabFriends.setBackgroundColor(0)
            binding.tabFriends.setTextColor(ContextCompat.getColor(context, R.color.runners_text_secondary))
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    historyViewModel.uiState.collect { state ->
                        updateHistoryUi(state)
                    }
                }
                launch {
                    feedViewModel.uiState.collect { state ->
                        updateFeedUi(state)
                    }
                }
            }
        }
    }

    private fun updateHistoryUi(state: HistoryUiState) {
        if (currentTab == JourneyTab.MY_RUNS) {
            binding.historyLoading.isVisible = state.isLoading
            binding.swipeRefresh.isRefreshing = state.isLoading
            binding.tvEmptyHistory.isVisible = state.runs.isEmpty() && !state.isLoading
        }
        
        binding.tvTotalDistance.text = String.format("%.1f", state.totalDistanceKm)
        binding.tvTotalRuns.text = state.totalRuns.toString()

        historyAdapter.setUnitSystem(state.isMetric)
        historyAdapter.submitList(state.runs)
    }

    private fun updateFeedUi(state: FeedUiState) {
        if (currentTab == JourneyTab.GLOBAL_FEED) {
            binding.historyLoading.isVisible = state.isLoading
            binding.swipeRefresh.isRefreshing = state.isLoading
            binding.tvEmptyHistory.isVisible = state.feed.isEmpty() && !state.isLoading
            feedAdapter.submitList(state.feed)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
