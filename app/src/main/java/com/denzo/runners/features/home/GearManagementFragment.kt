package com.denzo.runners.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.databinding.FragmentGearManagementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GearManagementFragment : Fragment() {

    private var _binding: FragmentGearManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GearViewModel by viewModels()
    private lateinit var gearAdapter: GearAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGearManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInteractions()
        observeUiState()
    }

    private fun setupRecyclerView() {
        gearAdapter = GearAdapter { gear ->
            showGearActions(gear)
        }
        binding.rvGearList.adapter = gearAdapter
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.fabAddGear.setOnClickListener { showAddGearDialog() }
    }

    private fun showAddGearDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gear, null)
        val etBrand = dialogView.findViewById<EditText>(R.id.et_brand)
        val etModel = dialogView.findViewById<EditText>(R.id.et_model)
        val etLimit = dialogView.findViewById<EditText>(R.id.et_max_km)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Gear")
            .setView(dialogView)
            .setPositiveButton("ADD") { _, _ ->
                val brand = etBrand.text.toString()
                val model = etModel.text.toString()
                val limit = etLimit.text.toString().toDoubleOrNull() ?: 800.0
                viewModel.onAddGear(brand, model, limit * 1000)
            }
            .setNegativeButton("CANCEL", null)
            .show()
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

    private fun updateUi(state: GearUiState) {
        binding.gearLoading.isVisible = state.isLoading
        gearAdapter.setUnitSystem(state.isMetric)
        gearAdapter.submitList(state.gearList)
    }

    private fun showGearActions(gear: GearEntity) {
        val options = if (gear.isActive) arrayOf("Retire Gear") else arrayOf("Set as Active")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${gear.brand} ${gear.model}")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Retire Gear" -> viewModel.onRetire(gear)
                    "Set as Active" -> viewModel.onSetActive(gear)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
