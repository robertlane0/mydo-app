package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Section
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SectionRepository {
    fun observeByProject(projectId: UUID): Flow<AppResult<List<Section>>>
    suspend fun getById(id: UUID): AppResult<Section?>
    suspend fun create(section: Section): AppResult<Unit>
    suspend fun update(section: Section): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    suspend fun reorder(id: UUID, sortOrder: Int): AppResult<Unit>
}
