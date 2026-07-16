package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveProjectsUseCase(private val projectRepository: ProjectRepository) {
    operator fun invoke(): Flow<AppResult<List<Project>>> = projectRepository.observeActive()
}
