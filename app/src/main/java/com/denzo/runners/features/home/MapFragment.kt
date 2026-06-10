package com.denzo.runners.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.denzo.runners.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMap()
        setupClickListeners()
    }

    private fun setupMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(15.0)
    }

    private fun setupClickListeners() {
        binding.btnJoinSession.setOnClickListener {
            joinSession()
        }
    }

    private fun joinSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Micro-Feedback: Loading State
            binding.btnJoinSession.isEnabled = false
            binding.btnJoinSession.text = "JOINING..."
            
            delay(1200) // Mock API network delay
            
            Toast.makeText(requireContext(), "Joined live session! 🏃", Toast.LENGTH_SHORT).show()
            
            binding.btnJoinSession.text = "JOINED"
            binding.btnJoinSession.alpha = 0.5f
        }
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
