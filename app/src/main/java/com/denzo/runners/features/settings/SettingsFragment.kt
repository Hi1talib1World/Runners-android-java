package com.denzo.runners.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denzo.runners.databinding.FragmentSettingsBinding
import com.denzo.runners.features.auth.LoginActivity
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

    /**
     * Pillar 2: Event Handlers & Atomic Mutations
     */
    private fun setupInteractions() {
        binding.themeSwitch.setOnClickListener {
            viewModel.toggleTheme(binding.themeSwitch.isChecked)
        }

        binding.telemetrySwitch.setOnClickListener {
            viewModel.toggleTelemetry(binding.telemetrySwitch.isChecked)
        }

        binding.unitSwitch.setOnClickListener {
            viewModel.toggleUnitSystem(binding.unitSwitch.isChecked)
        }

        binding.socialNotificationsSwitch.setOnClickListener {
            viewModel.toggleSocialNotifications(binding.socialNotificationsSwitch.isChecked)
        }

        binding.frequencySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.updateSyncFrequency(value.toInt())
            }
        }

        binding.usernameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.updateUsername(binding.usernameInput.text.toString())
                true
            } else {
                false
            }
        }

        binding.maxHrInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.maxHrInput.text.toString().toIntOrNull()?.let {
                    viewModel.updateMaxHr(it)
                }
                true
            } else {
                false
            }
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }
    }

    /**
     * Pillar 3: Reactive UI & Interaction Interlocking
     */
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoggedOut) {
                        navigateToLogin()
                    } else {
                        updateUi(state)
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun updateUi(state: SettingsUiState) {
        // Hydrate UI from SSOT
        // Prevent recursive updates by checking current value
        if (binding.themeSwitch.isChecked != state.isDarkMode) {
            binding.themeSwitch.isChecked = state.isDarkMode
        }
        if (binding.telemetrySwitch.isChecked != state.isTelemetryEnabled) {
            binding.telemetrySwitch.isChecked = state.isTelemetryEnabled
        }
        if (binding.unitSwitch.isChecked != state.isMetric) {
            binding.unitSwitch.isChecked = state.isMetric
        }
        if (binding.socialNotificationsSwitch.isChecked != state.isSocialNotificationsEnabled) {
            binding.socialNotificationsSwitch.isChecked = state.isSocialNotificationsEnabled
        }
        
        binding.frequencySlider.value = state.syncFrequencyMinutes.toFloat()
        binding.frequencyLabel.text = getString(com.denzo.runners.R.string.sync_frequency, state.syncFrequencyMinutes)
        
        if (binding.usernameInput.text.toString() != state.username && !binding.usernameInput.hasFocus()) {
            binding.usernameInput.setText(state.username)
        }

        if (binding.maxHrInput.text.toString() != state.maxHeartRate.toString() && !binding.maxHrInput.hasFocus()) {
            binding.maxHrInput.setText(state.maxHeartRate.toString())
        }

        // Interaction Interlocking: Disable controls during processing
        val controlsEnabled = !state.isProcessing
        binding.themeSwitch.isEnabled = controlsEnabled
        binding.telemetrySwitch.isEnabled = controlsEnabled
        binding.unitSwitch.isEnabled = controlsEnabled
        binding.socialNotificationsSwitch.isEnabled = controlsEnabled
        binding.frequencySlider.isEnabled = controlsEnabled
        binding.usernameInput.isEnabled = controlsEnabled
        binding.logoutButton.isEnabled = controlsEnabled
        
        binding.syncProgress.visibility = if (state.isProcessing) View.VISIBLE else View.INVISIBLE

        // Pillar 4: Feedback (Error/Success)
        state.errorEvent?.let { error ->
            showSnackbar(error, true)
            viewModel.clearError()
        }

        state.successMessage?.let { message ->
            showSnackbar(message, false)
            viewModel.clearSuccess()
        }
    }

    private fun showSnackbar(message: String, isError: Boolean) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
