package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Label
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LabelRepository {
    fun observeAll(): Flow<AppResult<List<Label>>>
    fun observeByTask(taskId: UUID): Flow<AppResult<List<Label>>>
    suspend fun create(label: Label): AppResult<Unit>
    suspend fun update(label: Label): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    suspend fun assignToTask(taskId: UUID, labelId: UUID): AppResult<Unit>
    suspend fun unassignFromTask(taskId: UUID, labelId: UUID): AppResult<Unit>
}
