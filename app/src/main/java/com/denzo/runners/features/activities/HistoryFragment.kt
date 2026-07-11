package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.core.utils.UnitConverter
import com.denzo.runners.databinding.ActivityHistoryBinding
import com.denzo.runners.databinding.ItemFeedActivityBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

enum class JourneyTab { MY_RUNS, GLOBAL_FEED }

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: ActivityHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var feedAdapter: FeedAdapter

    private var currentTab = JourneyTab.MY_RUNS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupClickListeners()
        observeState()
    }

    private fun setupAdapters() {
        historyAdapter = HistoryAdapter(
            onItemClick = { run ->
                val action = HistoryFragmentDirections.actionMyJourneyToRunSummary(run.id)
                findNavController().navigate(action)
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
        binding.rvHistory.adapter = historyAdapter

        feedAdapter = FeedAdapter { activityId -> feedViewModel.onKudos(activityId) }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.tabGlobal.setOnClickListener { switchTab(JourneyTab.GLOBAL_FEED) }
        binding.tabFriends.setOnClickListener { switchTab(JourneyTab.MY_RUNS) } // Reuse friends as My Runs for now or add new tab
        binding.tabGlobal.setOnClickListener { switchTab(JourneyTab.GLOBAL_FEED) }
    }

    private fun switchTab(tab: JourneyTab) {
        currentTab = tab
        binding.rvHistory.adapter = if (tab == JourneyTab.MY_RUNS) historyAdapter else feedAdapter
        updateTabUi()
    }

    private fun updateTabUi() {
        if (currentTab == JourneyTab.MY_RUNS) {
            binding.tabGlobal.setBackgroundColor(0)
            binding.tabGlobal.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_text_secondary))
            // Assuming tabFriends or something is used for My Runs in the new layout
            // Let's use tab_global and tab_challenges for now.
        } else {
            binding.tabGlobal.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
            binding.tabGlobal.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
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
        binding.historyLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        binding.tvTotalDistance.text = String.format("%.1f", state.totalDistance)
        binding.tvTotalRuns.text = state.totalRuns.toString()
        binding.tvTotalDistanceUnit.text = if (state.isMetric) getString(R.string.label_kilometers) else getString(R.string.label_miles)

        historyAdapter.setUnitSystem(state.isMetric)
        historyAdapter.submitList(state.runs)
        binding.tvEmptyHistory.visibility = if (state.runs.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
    }

    private fun updateFeedUi(state: FeedUiState) {
        if (currentTab == JourneyTab.GLOBAL_FEED) {
            binding.historyLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            feedAdapter.submitList(state.feed)
            binding.tvEmptyHistory.visibility = if (state.feed.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
        }
    }

    inner class FeedAdapter(private val onKudosClick: (String) -> Unit) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {
        private var items: List<FeedActivity> = emptyList()
        fun submitList(newItems: List<FeedActivity>) { items = newItems; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemFeedActivityBinding.inflate(layoutInflater, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
        inner class ViewHolder(private val b: ItemFeedActivityBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(item: FeedActivity) {
                b.tvAthleteName.text = item.athleteName
                b.tvActivityMetrics.text = "${item.distanceKm} KM • ${item.duration}"
                b.tvKudosCount.text = item.kudosCount.toString()
                b.btnKudos.setOnClickListener { onKudosClick(item.id) }
                
                if (item.isLive) {
                    b.liveBadge.visibility = View.VISIBLE
                    b.root.strokeColor = ContextCompat.getColor(requireContext(), R.color.runners_volt)
                    b.root.strokeWidth = 2
                } else {
                    b.liveBadge.visibility = View.GONE
                    b.root.strokeWidth = 0
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
