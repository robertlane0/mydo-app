package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.repository.LabelRepository
import java.util.UUID

/**
 * Which (task, label) pairs a bulk "Add labels" actually created — tasks that already
 * had a given label are skipped, and undo only removes what was newly added
 * (specs17-bulk-operations.md: "Undo removes added labels only").
 */
data class BulkAddLabelsOutcome(val addedPairs: List<Pair<UUID, UUID>>)

class BulkAddLabelsUseCase(private val labelRepository: LabelRepository) {
    suspend operator fun invoke(taskIds: List<UUID>, labelIds: List<UUID>): AppResult<BulkAddLabelsOutcome> {
        val added = mutableListOf<Pair<UUID, UUID>>()
        for (taskId in taskIds) {
            val existing = when (val result = labelRepository.getByTask(taskId)) {
                is AppResult.Failure -> return result
                is AppResult.Success -> result.value.map { it.id }.toSet()
            }
            for (labelId in labelIds) {
                if (labelId in existing) continue
                when (labelRepository.assignToTask(taskId, labelId)) {
                    is AppResult.Failure -> Unit // best-effort across the batch
                    is AppResult.Success -> added += taskId to labelId
                }
            }
        }
        return AppResult.Success(BulkAddLabelsOutcome(added))
    }
}

class UndoBulkAddLabelsUseCase(private val labelRepository: LabelRepository) {
    suspend operator fun invoke(outcome: BulkAddLabelsOutcome): AppResult<Unit> {
        outcome.addedPairs.forEach { (taskId, labelId) -> labelRepository.unassignFromTask(taskId, labelId) }
        return AppResult.Success(Unit)
    }
}
