package com.denzo.runners.data.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {
    private var _userLoggedIn = false
    private var _displayName: String? = "Test Runner"

    override val currentUserEmail: String? = "test@example.com"
    override val isUserLoggedIn: Boolean get() = _userLoggedIn
    override val userId: String? = "mock_user_123"
    override val displayName: String? get() = _displayName

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
        _displayName = name
        return Result.success(Unit)
    }
}
