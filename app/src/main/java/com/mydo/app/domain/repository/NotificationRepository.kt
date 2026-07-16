package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Notification
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface NotificationRepository {
    fun observeAll(): Flow<AppResult<List<Notification>>>
    fun observeUnreadCount(): Flow<AppResult<Int>>
    suspend fun create(notification: Notification): AppResult<Unit>
    suspend fun markRead(id: UUID): AppResult<Unit>
    suspend fun markAllRead(): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    suspend fun clearAll(): AppResult<Unit>
}
