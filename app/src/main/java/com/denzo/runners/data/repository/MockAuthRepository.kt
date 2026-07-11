package com.denzo.runners.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {
    private var _userLoggedIn = false
    private val _displayName = MutableStateFlow<String?>("Test Runner")

    override val currentUserEmail: String? = "test@example.com"
    override val isUserLoggedIn: Boolean get() = _userLoggedIn
    override val userId: String? = "mock_user_123"
    override val displayName: Flow<String?> = _displayName.asStateFlow()

    override suspend fun login(email: String, password: String): Result<Unit> {
        _userLoggedIn = true
        return Result.success(Unit)
    }

    override suspend fun signup(email: String, password: String): Result<Unit> {
        _userLoggedIn = true
        return Result.success(Unit)
    }

    override fun logout() {
        _userLoggedIn = false
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> {
        _displayName.value = name
        return Result.success(Unit)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        _userLoggedIn = true
        return Result.success(Unit)
    }
}
