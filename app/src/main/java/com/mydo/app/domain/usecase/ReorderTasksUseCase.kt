package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.repository.TaskRepository
import java.util.UUID

/**
 * Persists a manually dragged task order in one transaction (specs18-drag-reorder.md,
 * "Persistence"). [orderedIds] is the full visible list in its new order; sortOrder is
 * reassigned by position so gaps from earlier deletes don't accumulate.
 */
class ReorderTasksUseCase(private val taskRepository: TaskRepository) {
    suspend operator fun invoke(orderedIds: List<UUID>): AppResult<Unit> = taskRepository.reorder(orderedIds)
}
