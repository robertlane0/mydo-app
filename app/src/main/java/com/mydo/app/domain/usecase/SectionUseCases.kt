package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Section
import com.mydo.app.domain.repository.SectionRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Section CRUD/organization use cases (specs06-projects.md, "Sections"). */

class ObserveSectionsUseCase(private val sectionRepository: SectionRepository) {
    operator fun invoke(projectId: UUID): Flow<AppResult<List<Section>>> = sectionRepository.observeByProject(projectId)
}

class CreateSectionUseCase(private val sectionRepository: SectionRepository) {
    suspend operator fun invoke(projectId: UUID, name: String): AppResult<Unit> =
        sectionRepository.create(Section(id = UUID.randomUUID(), projectId = projectId, name = name.trim(), sortOrder = 0))
}

class RenameSectionUseCase(private val sectionRepository: SectionRepository) {
    suspend operator fun invoke(section: Section, name: String): AppResult<Unit> =
        sectionRepository.update(section.copy(name = name.trim()))
}

/**
 * Deletes a section. `tasks.sectionId` is ON DELETE SET NULL (see TaskEntity), so contained
 * tasks are never lost — they become unsectioned within the same project, matching
 * specs06-projects.md: "Deleting a section... leave [tasks] unsectioned in the project."
 */
class DeleteSectionUseCase(private val sectionRepository: SectionRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = sectionRepository.delete(id)
}

class ReorderSectionsUseCase(private val sectionRepository: SectionRepository) {
    suspend operator fun invoke(orderedIds: List<UUID>): AppResult<Unit> {
        orderedIds.forEachIndexed { index, id ->
            when (val result = sectionRepository.reorder(id, index)) {
                is AppResult.Failure -> return result
                is AppResult.Success -> Unit
            }
        }
        return AppResult.Success(Unit)
    }
}
