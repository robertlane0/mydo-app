package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Project CRUD/organization use cases (specs06-projects.md). The Room repository layer
 * already implements everything these wrap (including SET_NULL foreign keys so deleting a
 * project moves its tasks to the Inbox rather than losing them); this file is the thin
 * domain-layer surface the UI was missing.
 */

class ObserveArchivedProjectsUseCase(private val projectRepository: ProjectRepository) {
    operator fun invoke(): Flow<AppResult<List<Project>>> = projectRepository.observeArchived()
}

class ObserveProjectUseCase(private val projectRepository: ProjectRepository) {
    operator fun invoke(id: UUID): Flow<AppResult<Project?>> = projectRepository.observeById(id)
}

class CreateProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(name: String, color: String, icon: String = ""): AppResult<Project> {
        val now = timeProvider.nowUtcMillis()
        val project = Project(
            id = UUID.randomUUID(),
            name = name.trim(),
            description = "",
            color = color,
            icon = icon,
            archived = false,
            favorite = false,
            sortOrder = 0,
            createdAtUtcMillis = now,
            updatedAtUtcMillis = now,
        )
        return when (val result = projectRepository.create(project)) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(project)
        }
    }
}

class UpdateProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(project: Project, name: String, color: String): AppResult<Unit> =
        projectRepository.update(project.copy(name = name.trim(), color = color, updatedAtUtcMillis = timeProvider.nowUtcMillis()))
}

class SetProjectArchivedUseCase(
    private val projectRepository: ProjectRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(project: Project, archived: Boolean): AppResult<Unit> =
        projectRepository.update(project.copy(archived = archived, updatedAtUtcMillis = timeProvider.nowUtcMillis()))
}

class ToggleProjectFavoriteUseCase(
    private val projectRepository: ProjectRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(project: Project): AppResult<Unit> =
        projectRepository.update(project.copy(favorite = !project.favorite, updatedAtUtcMillis = timeProvider.nowUtcMillis()))
}

/** Reports how many active (incomplete) tasks a project holds, for delete-confirmation copy. */
class CountActiveTasksInProjectUseCase(private val projectRepository: ProjectRepository) {
    suspend operator fun invoke(projectId: UUID): AppResult<Int> = projectRepository.countActiveTasks(projectId)
}

/**
 * Deletes a project. The `tasks.projectId` foreign key is ON DELETE SET NULL (see
 * TaskEntity), so this can never silently discard tasks — they simply move to the Inbox,
 * matching specs06-projects.md: "Deletion requires confirmation and explains what happens
 * to contained tasks; it must not silently discard data."
 */
class DeleteProjectUseCase(private val projectRepository: ProjectRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = projectRepository.delete(id)
}

class ReorderProjectsUseCase(private val projectRepository: ProjectRepository) {
    suspend operator fun invoke(orderedIds: List<UUID>): AppResult<Unit> {
        orderedIds.forEachIndexed { index, id ->
            when (val result = projectRepository.reorder(id, index)) {
                is AppResult.Failure -> return result
                is AppResult.Success -> Unit
            }
        }
        return AppResult.Success(Unit)
    }
}
