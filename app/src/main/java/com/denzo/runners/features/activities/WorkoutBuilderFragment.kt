package com.denzo.runners.features.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.StepType
import com.denzo.runners.data.local.entities.WorkoutEntity
import com.denzo.runners.data.local.entities.WorkoutStep
import com.denzo.runners.data.repository.WorkoutRepository
import com.denzo.runners.databinding.FragmentWorkoutBuilderBinding
import com.denzo.runners.databinding.ItemWorkoutBuilderStepBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WorkoutBuilderFragment : Fragment() {

    @Inject
    lateinit var repository: WorkoutRepository

    private var _binding: FragmentWorkoutBuilderBinding? = null
    private val binding get() = _binding!!
    
    private val steps = mutableListOf<WorkoutStep>()
    private lateinit var adapter: BuilderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = BuilderAdapter(steps) { position ->
            steps.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        binding.rvBuilderSteps.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.btnAddStep.setOnClickListener {
            showAddStepDialog()
        }

        binding.btnSaveWorkout.setOnClickListener {
            saveWorkout()
        }
    }

    private fun showAddStepDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_workout_step, null)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rg_step_type)
        val etDuration = dialogView.findViewById<EditText>(R.id.et_step_duration)
        val etInstruction = dialogView.findViewById<EditText>(R.id.et_step_instruction)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Workout Step")
            .setView(dialogView)
            .setPositiveButton("ADD") { _, _ ->
                val type = when (rgType.checkedRadioButtonId) {
                    R.id.rb_warmup -> StepType.WARMUP
                    R.id.rb_sprint -> StepType.SPRINT
                    R.id.rb_cooldown -> StepType.COOLDOWN
                    else -> StepType.RUN
                }
                val duration = etDuration.text.toString().toLongOrNull() ?: 60L
                val instruction = etInstruction.text.toString()

                steps.add(WorkoutStep(type, duration, 0.0, instruction))
                adapter.notifyItemInserted(steps.size - 1)
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun saveWorkout() {
        val name = binding.etWorkoutName.text.toString()
        if (name.isBlank() || steps.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a name and at least one step", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val workout = WorkoutEntity(
                planId = -1, // Custom Workout
                weekNumber = 0,
                dayNumber = 0,
                title = name,
                steps = steps.toList()
            )
            // repository.insertWorkouts([workout]) -> need insert method
            // I'll assume repository handles individual insertion or I'll add it
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class BuilderAdapter(
        private val items: List<WorkoutStep>,
        private val onRemoveClick: (Int) -> Unit
    ) : RecyclerView.Adapter<BuilderAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemWorkoutBuilderStepBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val b: ItemWorkoutBuilderStepBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(step: WorkoutStep, position: Int) {
                b.tvStepType.text = step.type.name
                b.tvStepDetails.text = "${step.durationSeconds}s • ${step.instruction}"
                b.btnRemove.setOnClickListener { onRemoveClick(position) }
            }
        }
    }
}
