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
import com.denzo.runners.R
import com.denzo.runners.databinding.FragmentRanksBinding
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
        setupTabs()
        observeUiState()
    }

    private fun setupTabs() {
        val tabs = listOf(binding.tabGlobal, binding.tabClubs, binding.tabFriends)
        tabs.forEach { tab ->
            tab.setOnClickListener {
                tabs.forEach { t ->
                    t.setBackgroundColor(0)
                    t.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_text_secondary))
                }
                tab.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                // API call would go here to reload rankings
            }
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

        if (!state.isLoading) {
            // Dynamic Data: Podium
            state.podium.forEachIndexed { index, athlete ->
                when (athlete.rank) {
                    "01" -> binding.name1.text = athlete.name
                    "02" -> binding.name2.text = athlete.name
                    "03" -> binding.name3.text = athlete.name
                }
            }

            // Dynamic Data: Ranking List
            binding.rankingsList.removeAllViews()
            state.rankings.forEach { athlete ->
                val itemBinding = ItemRankingBinding.inflate(layoutInflater, binding.rankingsList, false)
                itemBinding.textRank.text = athlete.rank
                itemBinding.textName.text = athlete.name
                itemBinding.textTeam.text = athlete.team
                itemBinding.textDistance.text = athlete.distance
                
                // State Management: Highlight "Me"
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
