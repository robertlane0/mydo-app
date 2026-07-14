package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.PreferenceEntity
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomTaskRepository(
    private val database: MydoDatabase,
) : TaskRepository {
    override fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>> {
        return database.preferenceDao()
            .observeAll()
            .map<List<PreferenceEntity>, AppResult<List<TaskSummary>>> {
                AppResult.Success(emptyList())
            }
            .catch { throwable ->
                emit(
                    AppResult.Failure(
                        DatabaseError(
                            code = "inbox_observe_failed",
                            userMessage = "Unable to load local tasks.",
                            cause = throwable,
                        ),
                    ),
                )
            }
    }
}
