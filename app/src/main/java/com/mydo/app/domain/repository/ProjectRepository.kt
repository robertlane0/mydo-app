package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Project
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ProjectRepository {
    fun observeActive(): Flow<AppResult<List<Project>>>
    fun observeArchived(): Flow<AppResult<List<Project>>>
    fun observeFavorites(): Flow<AppResult<List<Project>>>
    fun observeById(id: UUID): Flow<AppResult<Project?>>
    suspend fun getById(id: UUID): AppResult<Project?>
    suspend fun create(project: Project): AppResult<Unit>
    suspend fun update(project: Project): AppResult<Unit>
    suspend fun delete(id: UUID): AppResult<Unit>
    suspend fun reorder(id: UUID, sortOrder: Int): AppResult<Unit>
}
