package com.mydo.app.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.Section
import com.mydo.app.domain.usecase.BulkActionOutcome
import com.mydo.app.domain.usecase.BulkAddLabelsOutcome
import com.mydo.app.domain.usecase.BulkAddLabelsUseCase
import com.mydo.app.domain.usecase.BulkCompleteTasksUseCase
import com.mydo.app.domain.usecase.BulkDeleteTasksUseCase
import com.mydo.app.domain.usecase.BulkMoveTasksUseCase
import com.mydo.app.domain.usecase.BulkSetDueDateUseCase
import com.mydo.app.domain.usecase.BulkSetPriorityUseCase
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.CreateSectionUseCase
import com.mydo.app.domain.usecase.DeleteProjectUseCase
import com.mydo.app.domain.usecase.DeleteSectionUseCase
import com.mydo.app.domain.usecase.ObserveProjectTasksUseCase
import com.mydo.app.domain.usecase.ObserveProjectUseCase
import com.mydo.app.domain.usecase.ObserveSectionsUseCase
import com.mydo.app.domain.usecase.RenameSectionUseCase
import com.mydo.app.domain.usecase.ReorderSectionsUseCase
import com.mydo.app.domain.usecase.ReorderTasksUseCase
import com.mydo.app.domain.usecase.UndoBulkAddLabelsUseCase
import com.mydo.app.domain.usecase.UndoBulkTaskOperationUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import com.mydo.app.domain.usecase.UpdateProjectUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** One-shot feedback for the Project Detail screen. */
sealed interface ProjectDetailEvent {
    data class TaskCompleted(val outcome: CompleteTaskUseCase.Outcome) : ProjectDetailEvent
    data class BulkActionDone(val label: String, val outcome: BulkActionOutcome) : ProjectDetailEvent
    data class BulkLabelsAdded(val outcome: BulkAddLabelsOutcome) : ProjectDetailEvent
    data class SectionCreated(val name: String) : ProjectDetailEvent
    data object SectionDeleted : ProjectDetailEvent

    /** Emitted after the project itself is deleted, so the screen can navigate back. */
    data object ProjectDeleted : ProjectDetailEvent
}

/**
 * Backs a single project's detail screen (specs06-projects.md, "Sections"): tasks grouped
 * by section plus an "unsectioned" bucket, with the same completion/bulk-action/manual
 * reorder toolkit as Inbox and Today.
 */
class ProjectDetailViewModel(
    private val projectId: UUID,
    observeProjectUseCase: ObserveProjectUseCase,
    observeSectionsUseCase: ObserveSectionsUseCase,
    observeProjectTasksUseCase: ObserveProjectTasksUseCase,
    private val createSectionUseCase: CreateSectionUseCase,
    private val renameSectionUseCase: RenameSectionUseCase,
    private val deleteSectionUseCase: DeleteSectionUseCase,
    private val reorderSectionsUseCase: ReorderSectionsUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val bulkSetPriorityUseCase: BulkSetPriorityUseCase,
    private val bulkSetDueDateUseCase: BulkSetDueDateUseCase,
    private val bulkMoveTasksUseCase: BulkMoveTasksUseCase,
    private val bulkCompleteTasksUseCase: BulkCompleteTasksUseCase,
    private val bulkDeleteTasksUseCase: BulkDeleteTasksUseCase,
    private val undoBulkTaskOperationUseCase: UndoBulkTaskOperationUseCase,
    private val bulkAddLabelsUseCase: BulkAddLabelsUseCase,
    private val undoBulkAddLabelsUseCase: UndoBulkAddLabelsUseCase,
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val selectionMode = MutableStateFlow(false)

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        observeProjectUseCase(projectId),
        observeSectionsUseCase(projectId),
        observeProjectTasksUseCase(projectId),
        selectedIds,
        selectionMode,
    ) { projectResult, sectionsResult, tasksResult, selected, selecting ->
        if (projectResult is AppResult.Failure) return@combine ProjectDetailUiState.Error(projectResult.error.userMessage)
        if (sectionsResult is AppResult.Failure) return@combine ProjectDetailUiState.Error(sectionsResult.error.userMessage)
        if (tasksResult is AppResult.Failure) return@combine ProjectDetailUiState.Error(tasksResult.error.userMessage)
        val project = (projectResult as AppResult.Success).value ?: return@combine ProjectDetailUiState.NotFound
        val sections = (sectionsResult as AppResult.Success).value
        val tasks = (tasksResult as AppResult.Success).value
        val unsectioned = tasks.filter { it.sectionId == null }
        val bySection = tasks.filter { it.sectionId != null }.groupBy { it.sectionId as UUID }
        ProjectDetailUiState.Ready(project, sections, unsectioned, bySection, selected, selecting)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectDetailUiState.Loading)

    val events = MutableSharedFlow<ProjectDetailEvent>(extraBufferCapacity = 1)

    fun toggleSelectionMode() {
        selectionMode.value = !selectionMode.value
        if (!selectionMode.value) selectedIds.value = emptySet()
    }

    fun toggleSelected(id: UUID) {
        selectedIds.value = if (selectedIds.value.contains(id)) selectedIds.value - id else selectedIds.value + id
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        selectionMode.value = false
    }

    fun completeTask(id: UUID) {
        viewModelScope.launch {
            val result = completeTaskUseCase(id)
            if (result is AppResult.Success) events.tryEmit(ProjectDetailEvent.TaskCompleted(result.value))
        }
    }

    fun undoComplete(outcome: CompleteTaskUseCase.Outcome) {
        viewModelScope.launch { undoCompleteTaskUseCase(outcome) }
    }

    /** Persists a drag-reorder within one section (or the unsectioned bucket). */
    fun reorderWithin(newOrder: List<UUID>) {
        viewModelScope.launch { reorderTasksUseCase(newOrder) }
    }

    fun moveTaskToSection(taskId: UUID, sectionId: UUID?) {
        viewModelScope.launch { bulkMoveTasksUseCase(listOf(taskId), projectId, sectionId) }
    }

    fun createSection(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = createSectionUseCase(projectId, trimmed)
            if (result is AppResult.Success) events.tryEmit(ProjectDetailEvent.SectionCreated(trimmed))
        }
    }

    fun renameSection(section: Section, name: String) {
        viewModelScope.launch { renameSectionUseCase(section, name) }
    }

    /** Tasks in the deleted section become unsectioned (see [DeleteSectionUseCase]); nothing is lost. */
    fun deleteSection(id: UUID) {
        viewModelScope.launch {
            val result = deleteSectionUseCase(id)
            if (result is AppResult.Success) events.tryEmit(ProjectDetailEvent.SectionDeleted)
        }
    }

    fun reorderSections(newOrder: List<UUID>) {
        viewModelScope.launch { reorderSectionsUseCase(newOrder) }
    }

    fun renameProject(project: Project, name: String, color: String) {
        viewModelScope.launch { updateProjectUseCase(project, name, color) }
    }

    /** Contained tasks move to the Inbox rather than being deleted (see [DeleteProjectUseCase]). */
    fun deleteProject() {
        viewModelScope.launch {
            val result = deleteProjectUseCase(projectId)
            if (result is AppResult.Success) events.tryEmit(ProjectDetailEvent.ProjectDeleted)
        }
    }

    fun bulkComplete() = runBulk("Marked complete") { bulkCompleteTasksUseCase(it) }
    fun bulkSetPriority(priority: Priority) = runBulk("Priority updated") { bulkSetPriorityUseCase(it, priority) }
    fun bulkSetDueDate(dueAtUtcMillis: Long?) = runBulk("Due date updated") { bulkSetDueDateUseCase(it, dueAtUtcMillis) }
    fun bulkMoveToSection(sectionId: UUID?) = runBulk("Moved") { bulkMoveTasksUseCase(it, projectId, sectionId) }
    fun bulkMoveToProject(targetProjectId: UUID?) = runBulk("Moved") { bulkMoveTasksUseCase(it, targetProjectId, null) }

    fun bulkAddLabels(labelIds: List<UUID>) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = bulkAddLabelsUseCase(ids, labelIds)
            if (result is AppResult.Success) events.tryEmit(ProjectDetailEvent.BulkLabelsAdded(result.value))
            clearSelection()
        }
    }

    fun undoBulkLabels(outcome: BulkAddLabelsOutcome) {
        viewModelScope.launch { undoBulkAddLabelsUseCase(outcome) }
    }

    fun bulkDelete() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            bulkDeleteTasksUseCase(ids)
            clearSelection()
        }
    }

    fun undoBulk(outcome: BulkActionOutcome) {
        viewModelScope.launch { undoBulkTaskOperationUseCase(outcome) }
    }

    private fun runBulk(label: String, action: suspend (List<UUID>) -> AppResult<BulkActionOutcome>) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = action(ids)
            if (result is AppResult.Success) events.tryEmit(ProjectDetailEvent.BulkActionDone(label, result.value))
            clearSelection()
        }
    }

    class Factory(
        private val projectId: UUID,
        private val observeProjectUseCase: ObserveProjectUseCase,
        private val observeSectionsUseCase: ObserveSectionsUseCase,
        private val observeProjectTasksUseCase: ObserveProjectTasksUseCase,
        private val createSectionUseCase: CreateSectionUseCase,
        private val renameSectionUseCase: RenameSectionUseCase,
        private val deleteSectionUseCase: DeleteSectionUseCase,
        private val reorderSectionsUseCase: ReorderSectionsUseCase,
        private val updateProjectUseCase: UpdateProjectUseCase,
        private val deleteProjectUseCase: DeleteProjectUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
        private val reorderTasksUseCase: ReorderTasksUseCase,
        private val bulkSetPriorityUseCase: BulkSetPriorityUseCase,
        private val bulkSetDueDateUseCase: BulkSetDueDateUseCase,
        private val bulkMoveTasksUseCase: BulkMoveTasksUseCase,
        private val bulkCompleteTasksUseCase: BulkCompleteTasksUseCase,
        private val bulkDeleteTasksUseCase: BulkDeleteTasksUseCase,
        private val undoBulkTaskOperationUseCase: UndoBulkTaskOperationUseCase,
        private val bulkAddLabelsUseCase: BulkAddLabelsUseCase,
        private val undoBulkAddLabelsUseCase: UndoBulkAddLabelsUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProjectDetailViewModel(
                projectId, observeProjectUseCase, observeSectionsUseCase, observeProjectTasksUseCase,
                createSectionUseCase, renameSectionUseCase, deleteSectionUseCase, reorderSectionsUseCase,
                updateProjectUseCase, deleteProjectUseCase, completeTaskUseCase, undoCompleteTaskUseCase,
                reorderTasksUseCase, bulkSetPriorityUseCase, bulkSetDueDateUseCase, bulkMoveTasksUseCase,
                bulkCompleteTasksUseCase, bulkDeleteTasksUseCase, undoBulkTaskOperationUseCase,
                bulkAddLabelsUseCase, undoBulkAddLabelsUseCase,
            ) as T
        }
    }
}
