package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Attachment
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AttachmentRepository {
    fun observeByTask(taskId: UUID): Flow<AppResult<List<Attachment>>>
    suspend fun create(attachment: Attachment): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    suspend fun getById(id: UUID): AppResult<Attachment?>
}
