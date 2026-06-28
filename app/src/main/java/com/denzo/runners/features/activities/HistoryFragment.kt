package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.databinding.ActivityHistoryBinding
import com.denzo.runners.databinding.ItemFeedActivityBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class JourneyTab { MY_RUNS, GLOBAL_FEED }

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.activity_history) {

    private var _binding: ActivityHistoryBinding? = null
    private val binding get() = _binding!!

    private val historyViewModel: HistoryViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()
    
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var feedAdapter: FeedAdapter
    
    private var currentTab = JourneyTab.MY_RUNS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityHistoryBinding.bind(view)

        setupAdapters()
        setupClickListeners()
        observeState()
    }

    private fun setupAdapters() {
        historyAdapter = HistoryAdapter(
            onItemClick = { run ->
                val bundle = Bundle().apply { putInt("runId", run.id) }
                findNavController().navigate(R.id.run_summary, bundle)
            },
            onDeleteClick = { run ->
                historyViewModel.deleteRun(run)
            }
        )
        
        feedAdapter = FeedAdapter { id -> feedViewModel.toggleKudos(id) }
        
        binding.recyclerViewHistory.adapter = historyAdapter
    }

    private fun setupClickListeners() {
        binding.buttonClearAll.setOnClickListener {
            historyViewModel.clearHistory()
        }

        binding.tabMyHistory.setOnClickListener { switchTab(JourneyTab.MY_RUNS) }
        binding.tabGlobalFeed.setOnClickListener { switchTab(JourneyTab.GLOBAL_FEED) }
    }

    private fun switchTab(tab: JourneyTab) {
        currentTab = tab
        updateTabUi()
        binding.recyclerViewHistory.adapter = if (tab == JourneyTab.MY_RUNS) historyAdapter else feedAdapter
        binding.buttonClearAll.visibility = if (tab == JourneyTab.MY_RUNS) View.VISIBLE else View.GONE
    }

    private fun updateTabUi() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.runners_volt)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.runners_text_secondary)
        val onPrimary = ContextCompat.getColor(requireContext(), R.color.onPrimary)

        binding.tabMyHistory.apply {
            setBackgroundColor(if (currentTab == JourneyTab.MY_RUNS) activeColor else 0)
            setTextColor(if (currentTab == JourneyTab.MY_RUNS) onPrimary else inactiveColor)
        }
        binding.tabGlobalFeed.apply {
            setBackgroundColor(if (currentTab == JourneyTab.GLOBAL_FEED) activeColor else 0)
            setTextColor(if (currentTab == JourneyTab.GLOBAL_FEED) onPrimary else inactiveColor)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    historyViewModel.historyState.collectLatest { state ->
                        if (currentTab == JourneyTab.MY_RUNS) {
                            updateHistoryUi(state)
                        }
                    }
                }
                launch {
                    feedViewModel.uiState.collectLatest { state ->
                        if (currentTab == JourneyTab.GLOBAL_FEED) {
                            updateFeedUi(state)
                        }
                    }
                }
            }
        }
    }

    private fun updateHistoryUi(state: HistoryUiState) {
        binding.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.buttonClearAll.isEnabled = !state.isLoading && state.runs.isNotEmpty()
        historyAdapter.setUnitSystem(state.isMetric)
        historyAdapter.submitList(state.runs)
        binding.textEmptyHistory.visibility = if (state.runs.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
    }

    private fun updateFeedUi(state: FeedUiState) {
        binding.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        feedAdapter.submitList(state.activities)
        binding.textEmptyHistory.visibility = if (state.activities.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
    }

    // Inner Feed Adapter for unified management
    inner class FeedAdapter(private val onKudosClick: (String) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<FeedAdapter.ViewHolder>() {
        private var items = listOf<FeedActivity>()
        fun submitList(newItems: List<FeedActivity>) { items = newItems; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) = ViewHolder(ItemFeedActivityBinding.inflate(layoutInflater, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
        inner class ViewHolder(private val b: ItemFeedActivityBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root) {
            fun bind(a: FeedActivity) {
                b.tvAthleteName.text = a.athleteName
                b.tvTimestamp.text = a.timestamp
                b.tvActivityTitle.text = a.title
                b.tvDistance.text = a.distance
                b.tvPace.text = a.pace
                b.tvDuration.text = a.duration
                b.tvKudosCount.text = a.kudosCount.toString()
                b.tvCommentCount.text = a.commentCount.toString()
                val color = ContextCompat.getColor(requireContext(), if (a.isKudoed) R.color.runners_volt else R.color.runners_text_secondary)
                b.ivKudos.setColorFilter(color); b.tvKudosCount.setTextColor(color)
                b.btnKudos.setOnClickListener { onKudosClick(a.id) }

                b.btnCheer.visibility = if (a.isLive) View.VISIBLE else View.GONE
                b.btnCheer.setOnClickListener {
                    Snackbar.make(b.root, "Cheer sent to ${a.athleteName}!", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
