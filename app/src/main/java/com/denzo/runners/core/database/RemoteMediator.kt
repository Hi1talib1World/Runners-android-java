package com.denzo.runners.core.database

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.denzo.runners.data.local.entities.RunEntity

@OptIn(ExperimentalPagingApi::class)
class RunningRemoteMediator(
    private val database: AppDatabase
) : RemoteMediator<Int, RunEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RunEntity>
    ): MediatorResult {
        return try {
            // TODO: Implement syncing logic between Cloud API and Room
            MediatorResult.Success(endOfPaginationReached = true)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
