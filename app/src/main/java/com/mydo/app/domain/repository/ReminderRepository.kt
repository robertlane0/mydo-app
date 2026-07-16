package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ReminderRepository {
    fun observeByTask(taskId: UUID): Flow<AppResult<List<Reminder>>>
    suspend fun getByTask(taskId: UUID): AppResult<List<Reminder>>
    suspend fun create(reminder: Reminder): AppResult<Unit>
    suspend fun update(reminder: Reminder): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    suspend fun deleteByTask(taskId: UUID): AppResult<Unit>
}
