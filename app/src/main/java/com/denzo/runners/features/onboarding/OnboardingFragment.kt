package com.denzo.runners.features.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denzo.runners.R
import com.denzo.runners.databinding.FragmentOnboardingBinding
import com.denzo.runners.features.home.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Pillar 2: UI Passive Observables
 * Pillar 3: Micro-Feedback & Step Locks
 */
@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeState()
    }

    private fun setupInteractions() {
        binding.btnNext.setOnClickListener { viewModel.onNextClicked() }
        binding.btnBack.setOnClickListener { viewModel.onBackClicked() }
        binding.btnSkip.setOnClickListener { viewModel.onSkipClicked() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isCompleted) {
                        navigateToMain()
                    } else {
                        updateUi(state)
                    }
                }
            }
        }
    }

    /**
     * Pillar 4: Zero Hardcoding Rules
     * The view controller binds layout variables reactively to payload models.
     */
    private fun updateUi(state: OnboardingUiState) {
        val step = state.currentStep ?: return

        // Hydrate content
        binding.tvOnboardingTitle.text = step.title
        binding.tvOnboardingDesc.text = step.description
        binding.ivOnboardingIllustration.setImageResource(step.imageResId)

        // Pillar 2: Button Morphing Contracts
        binding.btnNext.text = if (state.isLastStep) "Get Started" else "Next"
        binding.btnBack.visibility = if (state.currentStepIndex == 0) View.GONE else View.VISIBLE
        binding.btnSkip.visibility = if (state.isLastStep) View.GONE else View.VISIBLE

        // Pillar 3: Active Navigation Guards
        val navigationEnabled = !state.isTransitioning
        binding.btnNext.isEnabled = navigationEnabled
        binding.btnBack.isEnabled = navigationEnabled
        binding.btnSkip.isEnabled = navigationEnabled

        // Pillar 3: Visual Feedback (Dot Indicators)
        updateIndicators(state.currentStepIndex, state.totalSteps)
        
        // Background transition feedback
        step.backgroundColor?.let { colorRes ->
            binding.onboardingRoot.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
        }
    }

    private fun updateIndicators(currentIndex: Int, count: Int) {
        binding.indicatorLayout.removeAllViews()
        for (i in 0 until count) {
            val dot = ImageView(requireContext())
            val size = resources.getDimensionPixelSize(R.dimen.dot_size)
            val params = LinearLayout.LayoutParams(size, size).apply {
                setMargins(8, 0, 8, 0)
            }
            dot.layoutParams = params
            val color = if (i == currentIndex) R.color.runners_volt else R.color.runners_text_secondary
            dot.setImageResource(R.drawable.ic_dot)
            dot.setColorFilter(ContextCompat.getColor(requireContext(), color))
            binding.indicatorLayout.addView(dot)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
