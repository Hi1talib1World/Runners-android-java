package com.denzo.runners.features.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Pillar 4: Structural Data Strategy & Payload Separation
 * Immutable data model for onboarding content.
 */
data class OnboardingStep(
    @StringRes val titleResId: Int,
    @StringRes val descResId: Int,
    @DrawableRes val imageResId: Int,
    val backgroundColor: Int? = null
)
