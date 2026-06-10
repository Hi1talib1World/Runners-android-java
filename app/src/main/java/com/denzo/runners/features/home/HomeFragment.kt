package com.denzo.runners.features.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denzo.runners.R
import com.denzo.runners.databinding.FragmentHomeBinding
import com.denzo.runners.services.LocationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTrackingService()
        } else {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMap()
        setupClickListeners()
        observeUiState()
    }

    private fun setupMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(17.5)
        binding.map.controller.setCenter(GeoPoint(0.0, 0.0)) // Default
    }

    private fun setupClickListeners() {
        binding.buttonLeft.setOnClickListener {
            checkPermissionsAndToggle()
        }
    }

    private fun checkPermissionsAndToggle() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.toggleTracking()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
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

    private fun updateUi(state: HomeUiState) {
        // State Management: Loading & Execution
        binding.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.pauseIcon.visibility = if (state.isLoading) View.GONE else View.VISIBLE
        binding.buttonLeft.isEnabled = !state.isLoading

        // Dynamic Data Mapping
        binding.dataTime.text = state.currentActivity.duration
        binding.dataLength.text = state.currentActivity.distance
        binding.dataPace.text = state.currentActivity.pace
        binding.dataBpm.text = "${state.currentActivity.heartRate} BPM"
        binding.dataCadence.text = "${state.currentActivity.cadence} RPM CADENCE"

        // Transitions: Toggle Button State
        if (state.isTracking) {
            binding.buttonLeft.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_accent_red))
            binding.pauseIcon.setImageResource(android.R.drawable.ic_media_pause)
            startTrackingService()
        } else {
            binding.buttonLeft.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
            binding.pauseIcon.setImageResource(android.R.drawable.ic_media_play)
            stopTrackingService()
        }
        
        state.error?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }
    }

    private fun startTrackingService() {
        val intent = Intent(requireContext(), LocationService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopTrackingService() {
        val intent = Intent(requireContext(), LocationService::class.java)
        requireContext().stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
