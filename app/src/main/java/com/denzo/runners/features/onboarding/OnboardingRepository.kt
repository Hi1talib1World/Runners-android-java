package com.denzo.runners.features.onboarding

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pillar 1: The First-Run Guard & Session Persistence
 * Persistence Layer for managing onboarding lifecycle.
 */
@Singleton
class OnboardingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    fun isFirstRunCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false)
    }

    fun setFirstRunCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_RUN_COMPLETED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "runners_onboarding_prefs"
        private const val KEY_FIRST_RUN_COMPLETED = "is_first_run_completed"
    }
}
