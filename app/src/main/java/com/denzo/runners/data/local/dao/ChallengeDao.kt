package com.denzo.runners.data.local.dao

import androidx.room.*
import com.denzo.runners.data.local.entities.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity)

    @Query("SELECT * FROM challenges")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE isJoined = 1")
    fun getJoinedChallenges(): Flow<List<ChallengeEntity>>

    @Query("UPDATE challenges SET isJoined = 1 WHERE id = :challengeId")
    suspend fun joinChallenge(challengeId: Int)

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)
}
