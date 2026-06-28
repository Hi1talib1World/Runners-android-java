package com.denzo.runners

import android.app.Application
import com.denzo.runners.data.local.dao.ChallengeDao
import com.denzo.runners.data.repository.WorkoutRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    @Inject
    lateinit var challengeDao: ChallengeDao

    override fun onCreate() {
        super.onCreate()
        
        GlobalScope.launch {
            workoutRepository.seedMockData(challengeDao)
        }
    }
}
