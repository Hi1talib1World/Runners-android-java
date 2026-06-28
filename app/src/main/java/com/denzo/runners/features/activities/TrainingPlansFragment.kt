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
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.TrainingPlanEntity
import com.denzo.runners.data.local.entities.WorkoutEntity
import com.denzo.runners.databinding.FragmentTrainingPlansBinding
import com.denzo.runners.databinding.ItemTrainingPlanBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrainingPlansFragment : Fragment() {

    private var _binding: FragmentTrainingPlansBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TrainingPlansViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeUiState()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCreateCustom.setOnClickListener {
            findNavController().navigate(R.id.navigation_workout_builder)
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

    private fun updateUi(state: TrainingPlansUiState) {
        binding.plansLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        val adapter = PlansAdapter(state.plans, state.customWorkouts, state.activePlan?.id) { planId ->
            viewModel.onEnroll(planId)
        }
        binding.rvPlansList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class PlansAdapter(
        private val plans: List<TrainingPlanEntity>,
        private val customWorkouts: List<WorkoutEntity>,
        private val activePlanId: Int?,
        private val onEnrollClick: (Int) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            return if (position < plans.size) 0 else 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val itemBinding = ItemTrainingPlanBinding.inflate(layoutInflater, parent, false)
            return PlanViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == 0) {
                (holder as PlanViewHolder).bind(plans[position])
            } else {
                (holder as PlanViewHolder).bindCustom(customWorkouts[position - plans.size])
            }
        }

        override fun getItemCount() = plans.size + customWorkouts.size

        inner class PlanViewHolder(private val b: ItemTrainingPlanBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(plan: TrainingPlanEntity) {
                b.tvPlanName.text = plan.name
                b.tvPlanDesc.text = plan.description
                b.tvTotalWeeks.text = "${plan.totalWeeks} WEEKS"
                b.chipDifficulty.text = plan.difficulty

                val isActive = plan.id == activePlanId
                if (isActive) {
                    b.btnEnroll.text = "ENROLLED"
                    b.btnEnroll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_card_bg))
                    b.btnEnroll.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                    b.btnEnroll.isEnabled = false
                } else {
                    b.btnEnroll.text = "ENROLL"
                    b.btnEnroll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                    b.btnEnroll.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                    b.btnEnroll.isEnabled = true
                }

                b.btnEnroll.setOnClickListener { onEnrollClick(plan.id) }
            }

            fun bindCustom(workout: WorkoutEntity) {
                b.tvPlanName.text = workout.title
                b.tvPlanDesc.text = "Custom User Workout"
                b.tvTotalWeeks.text = "${workout.steps.size} STEPS"
                b.chipDifficulty.text = "Custom"
                b.btnEnroll.visibility = View.GONE
            }
        }
    }
}
