package com.denzo.runners.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserEmail: String?
    val isUserLoggedIn: Boolean
    val userId: String?
    val displayName: String?

    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signup(email: String, password: String): Result<Unit>
    fun logout()
    suspend fun updateDisplayName(name: String): Result<Unit>
}
