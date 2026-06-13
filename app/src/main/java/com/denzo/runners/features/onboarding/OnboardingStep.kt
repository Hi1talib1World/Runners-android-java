package com.denzo.runners.features.onboarding

import androidx.annotation.DrawableRes

/**
 * Pillar 4: Structural Data Strategy & Payload Separation
 * Immutable data model for onboarding content.
 */
data class OnboardingStep(
    val title: String,
    val description: String,
    @DrawableRes val imageResId: Int,
    val backgroundColor: Int? = null
)
