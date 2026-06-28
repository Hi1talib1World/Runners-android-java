package com.denzo.runners.features.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.navigation.fragment.findNavController
import com.denzo.runners.R
import com.denzo.runners.databinding.FragmentHomeBinding
import com.denzo.runners.services.LocationService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()

    private var trackPolyline: Polyline? = null
    private var ghostMarker: Marker? = null
    private val liveMarkers = mutableMapOf<String, Marker>()
    private var holdJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTracking()
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
        observeState()
    }

    private fun setupMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(18.0)
        
        trackPolyline = Polyline(binding.map).apply {
            outlinePaint.color = android.graphics.Color.RED
            outlinePaint.strokeWidth = 10f
        }
        binding.map.overlays.add(trackPolyline)

        ghostMarker = Marker(binding.map).apply {
            icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces)
            title = "Personal Best Ghost"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    private fun setupClickListeners() {
        binding.buttonLeft.setOnClickListener {
            checkPermissionsAndToggle()
        }
        
        binding.activityStatusBar.setOnClickListener {
            viewModel.joinSession()
        }

        binding.btnSearch.setOnClickListener {
            findNavController().navigate(R.id.navigation_search)
        }

        binding.btnPlans.setOnClickListener {
            findNavController().navigate(R.id.navigation_plans)
        }

        binding.cardTodayWorkout.setOnClickListener {
            viewModel.onTodayWorkoutClicked()
        }

        binding.chipGroupGoals.setOnCheckedStateChangeListener { _, checkedIds ->
            val goal = when (checkedIds.firstOrNull()) {
                R.id.chip_5k -> RunGoal.DISTANCE_5K
                R.id.chip_10k -> RunGoal.DISTANCE_10K
                R.id.chip_30m -> RunGoal.TIME_30M
                R.id.chip_route -> {
                    showRouteSelector()
                    RunGoal.ROUTE
                }
                else -> RunGoal.FREE
            }
            viewModel.onGoalSelected(goal)
        }

        binding.chipGroupEnvironment.setOnCheckedStateChangeListener { _, checkedIds ->
            val env = when (checkedIds.firstOrNull()) {
                R.id.chip_trail -> "TRAIL"
                R.id.chip_indoor -> "TREADMILL"
                else -> "ROAD"
            }
            viewModel.onEnvironmentSelected(env)
        }

        setupHoldToFinish()
    }

    private fun showRouteSelector() {
        val routes = viewModel.uiState.value.availableRoutes
        if (routes.isEmpty()) {
            Toast.makeText(requireContext(), "No saved routes found!", Toast.LENGTH_SHORT).show()
            binding.chipFree.isChecked = true
            return
        }

        val names = routes.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Route")
            .setItems(names) { _, which ->
                viewModel.onRouteSelected(routes[which])
            }
            .setNegativeButton("Cancel") { _, _ -> binding.chipFree.isChecked = true }
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupHoldToFinish() {
        binding.finishContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startHoldTimer()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHoldTimer()
                    true
                }
                else -> false
            }
        }
    }

    private fun startHoldTimer() {
        holdJob?.cancel()
        holdJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.finishProgress.visibility = View.VISIBLE
            for (i in 0..100 step 5) {
                binding.finishProgress.progress = i
                delay(100)
            }
            binding.finishProgress.visibility = View.INVISIBLE
            viewModel.stopAndSaveRun()
        }
    }

    private fun cancelHoldTimer() {
        holdJob?.cancel()
        binding.finishProgress.progress = 0
        binding.finishProgress.visibility = View.INVISIBLE
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
            if (viewModel.uiState.value.isTracking) {
                // Handled by Hold to Finish
            } else {
                startTracking()
            }
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startTracking() {
        val intent = Intent(requireContext(), LocationService::class.java).apply {
            putExtra("routeId", viewModel.uiState.value.selectedRoute?.id ?: -1)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUi(state)
                    }
                }
                launch {
                    viewModel.uiEvent.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUi(state: HomeUiState) {
        _binding?.let { b ->
            b.loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            b.pauseIcon.visibility = if (state.isLoading) View.GONE else View.VISIBLE
            b.buttonLeft.isEnabled = !state.isLoading

            b.dataTime.text = state.currentActivity.duration
            b.dataLength.text = state.currentActivity.distance
            b.dataPace.text = state.currentActivity.pace
            b.dataBpm.text = "${state.currentActivity.heartRate} BPM"
            b.dataCadence.text = "${state.currentActivity.cadence} RPM"

            // Today's Workout Card
            if (!state.isTracking && state.todayWorkout != null) {
                b.cardTodayWorkout.visibility = View.VISIBLE
                b.tvTodayWorkoutTitle.text = "${state.todayWorkout.title} (Week ${state.todayWorkout.weekNumber})"
                if (state.selectedGoal == RunGoal.WORKOUT) {
                    b.cardTodayWorkout.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.runners_volt))
                } else {
                    b.cardTodayWorkout.setStrokeColor(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent))
                }
            } else {
                b.cardTodayWorkout.visibility = View.GONE
            }

            // Active Workout HUD
            if (state.isTracking && state.activeWorkoutStep != null) {
                b.cardActiveWorkoutStep.visibility = View.VISIBLE
                b.tvStepInstruction.text = state.activeWorkoutStep.instruction.uppercase()
                b.tvStepTimer.text = "${formatTime(state.stepRemainingSeconds.toInt())} REMAINING"
            } else {
                b.cardActiveWorkoutStep.visibility = View.GONE
            }

            // Live Activity Bar
            if (state.isLiveGroupJoined) {
                b.topTimer.text = "RUNNING WITH ${state.liveAthletes.size} OTHERS"
                b.topTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
            } else {
                b.topTimer.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_text_primary))
            }

            if (state.isTracking) {
                b.buttonLeft.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_accent_red))
                b.pauseIcon.setImageResource(android.R.drawable.ic_media_pause)
                b.goalSelector.visibility = View.GONE
                
                if (state.selectedGoal != RunGoal.FREE && state.selectedGoal != RunGoal.WORKOUT) {
                    b.goalProgressTop.visibility = View.VISIBLE
                    b.goalProgressTop.progress = state.goalProgress
                }

                if (state.selectedGoal == RunGoal.ROUTE && state.ghostPosition != null) {
                    b.cardPacerDelta.visibility = View.VISIBLE
                    b.textPacerDelta.text = "GHOST ACTIVE" 
                    
                    ghostMarker?.position = state.ghostPosition
                    if (!b.map.overlays.contains(ghostMarker)) {
                        b.map.overlays.add(ghostMarker)
                    }
                }
                
                updateLiveAthletes(state.liveAthletes)
            } else {
                b.buttonLeft.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                b.pauseIcon.setImageResource(android.R.drawable.ic_media_play)
                b.goalSelector.visibility = View.VISIBLE
                b.goalProgressTop.visibility = View.GONE
                b.cardPacerDelta.visibility = View.GONE
                b.map.overlays.remove(ghostMarker)
                clearLiveAthletes()
                
                val intent = Intent(requireContext(), LocationService::class.java)
                requireContext().stopService(intent)
            }

            if (state.pathPoints.isNotEmpty()) {
                trackPolyline?.setPoints(state.pathPoints)
                b.map.controller.animateTo(state.pathPoints.last())
                b.map.invalidate()
            }
        }
    }

    private fun updateLiveAthletes(athletes: List<LiveAthlete>) {
        athletes.forEach { athlete ->
            val marker = liveMarkers.getOrPut(athlete.id) {
                Marker(binding.map).apply {
                    icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_gallery)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        showAthleteCheerDialog(athlete)
                        true
                    }
                    binding.map.overlays.add(this)
                }
            }
            marker.position = athlete.position
            marker.title = "${athlete.name} (${athlete.pace})"
        }
        binding.map.invalidate()
    }

    private fun clearLiveAthletes() {
        liveMarkers.values.forEach { binding.map.overlays.remove(it) }
        liveMarkers.clear()
        binding.map.invalidate()
    }

    private fun showAthleteCheerDialog(athlete: LiveAthlete) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(athlete.name)
            .setMessage("Currently running at ${athlete.pace} pace. Send a cheer?")
            .setPositiveButton("SEND CHEER") { _, _ ->
                viewModel.sendCheer(athlete.id)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun handleEvent(event: UiEvent) {
        val view = view ?: return
        when (event) {
            is UiEvent.RunSaved -> Snackbar.make(view, "Run Saved Successfully!", Snackbar.LENGTH_SHORT).show()
            is UiEvent.ShowError -> Snackbar.make(view, event.message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.runners_accent_red))
                .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                .show()
            is UiEvent.ReceivedCheer -> {
                Snackbar.make(view, "${event.from} sent you a cheer!", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                    .show()
            }
            is UiEvent.NewStep -> {
                // Fragment can show a brief alert or rely on the HUD/Service audio
                Snackbar.make(view, "New Step: ${event.instruction}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val remainingSecs = seconds % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, remainingSecs)
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
