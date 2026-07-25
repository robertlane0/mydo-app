package com.mydo.app.ui.projects

import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.Section
import com.mydo.app.domain.model.TaskSummary
import java.util.UUID

sealed interface ProjectDetailUiState {
    data object Loading : ProjectDetailUiState

    data class Ready(
        val project: Project,
        val sections: List<Section>,
        val unsectionedTasks: List<TaskSummary>,
        val tasksBySection: Map<UUID, List<TaskSummary>>,
        val selectedIds: Set<UUID> = emptySet(),
        val selectionMode: Boolean = false,
    ) : ProjectDetailUiState {
        fun tasksFor(sectionId: UUID): List<TaskSummary> = tasksBySection[sectionId].orEmpty().sortedBy { it.sortOrder }
        val orderedUnsectioned: List<TaskSummary> get() = unsectionedTasks.sortedBy { it.sortOrder }
        val orderedSections: List<Section> get() = sections.sortedBy { it.sortOrder }
        val isEmpty: Boolean get() = unsectionedTasks.isEmpty() && tasksBySection.values.all { it.isEmpty() } && sections.isEmpty()
    }

    /** The project id in the route didn't resolve to a project (e.g. it was just deleted). */
    data object NotFound : ProjectDetailUiState

    data class Error(val message: String) : ProjectDetailUiState
}
