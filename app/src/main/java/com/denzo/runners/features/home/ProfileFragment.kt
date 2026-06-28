package com.denzo.runners.features.home

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
import com.denzo.runners.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.gearTrackerCard.setOnClickListener {
            findNavController().navigate(R.id.navigation_gear)
        }

        binding.retireGearButton.setOnClickListener {
            viewModel.retireGear()
        }
        
        binding.editProfileButton.setOnClickListener {
            // Demonstrate atomic mutation
            viewModel.updateProfileName("Elite Pro Runner")
        }

        binding.goProButton.setOnClickListener {
            // This is a direct call to BillingManager for simulation
            // In a real app, the ViewModel would trigger this via a side effect or event.
            viewModel.goPro(requireActivity())
        }
    }

    /**
     * Pillar 3: Reactive UI & Interaction Interlocking
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: ProfileUiState) {
        // Micro-Feedback: Global Loading State
        binding.globalLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        // Interaction Interlocking
        val isInteracting = !state.isLoading
        binding.retireGearButton.isEnabled = isInteracting
        binding.editProfileButton.isEnabled = isInteracting

        // Dynamic Data: Identity
        binding.athleteName.text = state.name
        binding.athleteEmail.text = state.email
        
        val memberText = if (state.isPro) {
            getString(R.string.member_since_pro, state.memberSince)
        } else {
            getString(R.string.member_since_free, state.memberSince)
        }
        binding.memberSince.text = memberText
        binding.proBadge.visibility = if (state.isPro) View.VISIBLE else View.GONE
        binding.goProButton.visibility = if (state.isPro) View.GONE else View.VISIBLE
        
        binding.textLifetimeDistance.text = state.lifetimeDistanceKm
        binding.textTotalRuns.text = state.totalRuns.toString()

        // Training Intelligence
        binding.tvTrainingLoad.text = state.trainingLoad.toString()
        val loadStatus = when {
            state.trainingLoad < 200 -> "RECOVERY"
            state.trainingLoad < 600 -> "OPTIMAL"
            else -> "OVERREACHING"
        }
        binding.tvLoadStatus.text = loadStatus
        binding.loadProgress.progress = (state.trainingLoad / 10).coerceIn(0, 100)

        // Race Predictions
        binding.predictionsContainer.removeAllViews()
        state.predictions.forEach { pred ->
            val predView = layoutInflater.inflate(R.layout.item_race_prediction, binding.predictionsContainer, false)
            predView.findViewById<android.widget.TextView>(R.id.tv_pred_distance).text = pred.distance
            predView.findViewById<android.widget.TextView>(R.id.tv_pred_time).text = pred.time
            binding.predictionsContainer.addView(predView)
        }

        // Populate Achievements
        binding.achievementsContainer.removeAllViews()
        state.achievements.forEach { achievement ->
            val badgeView = layoutInflater.inflate(R.layout.item_achievement_badge, binding.achievementsContainer, false)
            badgeView.findViewById<android.widget.ImageView>(R.id.iv_badge_icon).apply {
                setImageResource(achievement.iconResId)
                if (achievement.isUnlocked) {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                } else {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_card_bg))
                    setColorFilter(ContextCompat.getColor(requireContext(), R.color.runners_text_secondary))
                }
            }
            badgeView.findViewById<android.widget.TextView>(R.id.tv_badge_title).text = achievement.title
            binding.achievementsContainer.addView(badgeView)
        }

        // Populate Records Grid
        binding.recordsGrid.removeAllViews()
        state.records.forEach { record ->
            val itemView = layoutInflater.inflate(R.layout.item_profile_record, binding.recordsGrid, false)
            itemView.findViewById<android.widget.TextView>(R.id.record_value).text = record.value
            itemView.findViewById<android.widget.TextView>(R.id.record_label).text = record.label
            binding.recordsGrid.addView(itemView)
        }
        
        // State Management: Gear visibility
        if (state.gear != null) {
            binding.gearTrackerCard.visibility = View.VISIBLE
            binding.gearEmptyState.visibility = View.GONE
            binding.gearModelName.text = "${state.gear.brand} ${state.gear.model}"
            binding.gearProgress.progress = (state.gear.currentMileageMeters * 100 / state.gear.maxMileageMeters).toInt()
        } else {
            binding.gearTrackerCard.visibility = View.GONE
            binding.gearEmptyState.visibility = View.VISIBLE
            binding.gearEmptyState.setText(R.string.gear_empty_state)
        }

        // Pillar 4: Feedback (Error/Success)
        state.errorEvent?.let { error ->
            showSnackbar(error)
            viewModel.clearError()
        }

        state.successMessage?.let { message ->
            showSnackbar(message)
            viewModel.clearSuccess()
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
