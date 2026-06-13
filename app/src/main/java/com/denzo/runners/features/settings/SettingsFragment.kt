package com.denzo.runners.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denzo.runners.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeState()
    }

    private fun setupInteractions() {
        // Pillar 2: Atomic State Mutations
        binding.themeSwitch.setOnClickListener {
            viewModel.toggleTheme(binding.themeSwitch.isChecked)
        }

        binding.telemetrySwitch.setOnClickListener {
            viewModel.toggleTelemetry(binding.telemetrySwitch.isChecked)
        }
    }

    private fun observeState() {
        // Pillar 3: Reactive UI & Interaction Interlocking
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: SettingsUiState) {
        // Hydrate UI from SSOT
        binding.themeSwitch.isChecked = state.isDarkMode
        binding.telemetrySwitch.isChecked = state.isTelemetryEnabled

        // Pillar 3: Strict component interlocking
        val controlsEnabled = !state.isProcessing
        binding.themeSwitch.isEnabled = controlsEnabled
        binding.telemetrySwitch.isEnabled = controlsEnabled
        binding.syncProgress.visibility = if (state.isProcessing) View.VISIBLE else View.INVISIBLE

        // Pillar 4: Failure Safeguards & Rollbacks
        state.errorEvent?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                .setAction("DISMISS") { viewModel.clearError() }
                .show()
            viewModel.clearError()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
