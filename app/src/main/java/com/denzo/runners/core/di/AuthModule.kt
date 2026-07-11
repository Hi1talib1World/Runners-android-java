package com.denzo.runners.core.di

import com.denzo.runners.data.repository.AuthRepository
import com.denzo.runners.data.repository.FirebaseAuthRepository
import com.denzo.runners.data.repository.MockAuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth?,
        mockRepo: MockAuthRepository
    ): AuthRepository {
        // FORCE Mock if API key is the placeholder one from google-services.json
        val isPlaceholder = firebaseAuth?.app?.options?.apiKey == "placeholder_api_key"
        
        if (firebaseAuth == null || isPlaceholder) {
            return mockRepo
        }
        
        return try {
            firebaseAuth.currentUser
            FirebaseAuthRepository(firebaseAuth)
        } catch (e: Exception) {
            mockRepo
        }
    }
}
