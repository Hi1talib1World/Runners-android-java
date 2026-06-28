package com.denzo.runners.features.home

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.denzo.runners.data.local.entities.GearEntity
import com.denzo.runners.databinding.FragmentGearManagementBinding
import com.denzo.runners.databinding.ItemGearBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GearManagementFragment : Fragment() {

    private var _binding: FragmentGearManagementBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GearViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGearManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInteractions()
        observeUiState()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.fabAddGear.setOnClickListener {
            showAddGearDialog()
        }
    }

    private fun showAddGearDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_gear, null)
        val etBrand = dialogView.findViewById<EditText>(R.id.et_brand)
        val etModel = dialogView.findViewById<EditText>(R.id.et_model)
        val etMaxKm = dialogView.findViewById<EditText>(R.id.et_max_km)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Gear")
            .setView(dialogView)
            .setPositiveButton("ADD") { _: DialogInterface, _: Int ->
                val brand = etBrand.text.toString()
                val model = etModel.text.toString()
                val maxKm = etMaxKm.text.toString().toDoubleOrNull() ?: 500.0
                viewModel.onAddGear(brand, model, maxKm)
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
        binding.gearLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        val adapter = GearAdapter(state.gearList, state.isMetric) { gear ->
            showGearActions(gear)
        }
        binding.rvGearList.adapter = adapter
    }

    private fun showGearActions(gear: GearEntity) {
        val options = mutableListOf<String>()
        if (!gear.isActive && !gear.isRetired) options.add("Set as Active")
        if (!gear.isRetired) options.add("Retire Gear")
        options.add("Delete")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${gear.brand} ${gear.model}")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Set as Active" -> viewModel.onSetActive(gear)
                    "Retire Gear" -> viewModel.onRetire(gear)
                    "Delete" -> { /* Delete logic */ }
                }
            }
            .show()
    }

    inner class GearAdapter(
        private val items: List<GearEntity>,
        private val isMetric: Boolean,
        private val onGearClick: (GearEntity) -> Unit
    ) : RecyclerView.Adapter<GearAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemGearBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val b: ItemGearBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(gear: GearEntity) {
                b.tvGearName.text = "${gear.brand} ${gear.model}"
                
                val progress = (gear.currentMileageMeters / gear.maxMileageMeters * 100).toInt().coerceIn(0, 100)
                b.gearMileageProgress.progress = progress
                
                val currentText = UnitConverter.formatDistance(gear.currentMileageMeters, isMetric)
                val maxText = UnitConverter.formatDistance(gear.maxMileageMeters, isMetric)
                b.tvMileageText.text = "$currentText / $maxText"

                if (gear.isActive) {
                    b.chipStatus.visibility = View.VISIBLE
                    b.chipStatus.text = "ACTIVE"
                    b.chipStatus.setChipBackgroundColorResource(R.color.runners_volt)
                } else if (gear.isRetired) {
                    b.chipStatus.visibility = View.VISIBLE
                    b.chipStatus.text = "RETIRED"
                    b.chipStatus.setChipBackgroundColorResource(R.color.runners_card_bg)
                } else {
                    b.chipStatus.visibility = View.GONE
                }

                b.root.setOnClickListener { onGearClick(gear) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
