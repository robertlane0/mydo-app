package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>>
}
