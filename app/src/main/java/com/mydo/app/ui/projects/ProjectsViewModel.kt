package com.mydo.app.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.usecase.CountActiveTasksInProjectUseCase
import com.mydo.app.domain.usecase.CreateProjectUseCase
import com.mydo.app.domain.usecase.DeleteProjectUseCase
import com.mydo.app.domain.usecase.ObserveActiveProjectsUseCase
import com.mydo.app.domain.usecase.ObserveArchivedProjectsUseCase
import com.mydo.app.domain.usecase.ReorderProjectsUseCase
import com.mydo.app.domain.usecase.SetProjectArchivedUseCase
import com.mydo.app.domain.usecase.ToggleProjectFavoriteUseCase
import com.mydo.app.domain.usecase.UpdateProjectUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** One-shot feedback events for the Projects list. */
sealed interface ProjectsEvent {
    data class ProjectCreated(val project: Project) : ProjectsEvent
    data class ProjectDeleted(val name: String, val movedTaskCount: Int) : ProjectsEvent
}

/**
 * Backs the Projects list (specs06-projects.md): favorites pinned above the rest of the
 * active projects, with an archived view toggle. Deleting a project never discards its
 * tasks — the SET_NULL foreign key on `tasks.projectId` moves them to the Inbox, and the
 * confirmation dialog says so up front (see [DeleteProjectUseCase]).
 */
class ProjectsViewModel(
    observeActiveProjectsUseCase: ObserveActiveProjectsUseCase,
    observeArchivedProjectsUseCase: ObserveArchivedProjectsUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val setProjectArchivedUseCase: SetProjectArchivedUseCase,
    private val toggleProjectFavoriteUseCase: ToggleProjectFavoriteUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val countActiveTasksInProjectUseCase: CountActiveTasksInProjectUseCase,
    private val reorderProjectsUseCase: ReorderProjectsUseCase,
) : ViewModel() {

    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<ProjectsUiState> = combine(
        observeActiveProjectsUseCase(),
        observeArchivedProjectsUseCase(),
        showArchived,
    ) { activeResult, archivedResult, showingArchived ->
        if (activeResult is AppResult.Failure) return@combine ProjectsUiState.Error(activeResult.error.userMessage)
        if (archivedResult is AppResult.Failure) return@combine ProjectsUiState.Error(archivedResult.error.userMessage)
        val active = (activeResult as AppResult.Success).value.sortedBy { it.sortOrder }
        val archived = (archivedResult as AppResult.Success).value.sortedBy { it.sortOrder }
        val (favorites, others) = active.partition { it.favorite }
        ProjectsUiState.Ready(favorites, others, showingArchived, archived)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectsUiState.Loading)

    val events = MutableSharedFlow<ProjectsEvent>(extraBufferCapacity = 1)

    fun toggleShowArchived() { showArchived.value = !showArchived.value }

    fun createProject(name: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = createProjectUseCase(trimmed, color)
            if (result is AppResult.Success) events.tryEmit(ProjectsEvent.ProjectCreated(result.value))
        }
    }

    fun renameProject(project: Project, name: String, color: String) {
        viewModelScope.launch { updateProjectUseCase(project, name, color) }
    }

    fun toggleFavorite(project: Project) {
        viewModelScope.launch { toggleProjectFavoriteUseCase(project) }
    }

    fun setArchived(project: Project, archived: Boolean) {
        viewModelScope.launch { setProjectArchivedUseCase(project, archived) }
    }

    /** Used by the delete confirmation dialog to explain what will happen to contained tasks. */
    suspend fun countActiveTasks(projectId: UUID): Int =
        (countActiveTasksInProjectUseCase(projectId) as? AppResult.Success)?.value ?: 0

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            val movedCount = countActiveTasks(project.id)
            val result = deleteProjectUseCase(project.id)
            if (result is AppResult.Success) events.tryEmit(ProjectsEvent.ProjectDeleted(project.name, movedCount))
        }
    }

    fun reorder(newOrder: List<UUID>) {
        viewModelScope.launch { reorderProjectsUseCase(newOrder) }
    }

    class Factory(
        private val observeActiveProjectsUseCase: ObserveActiveProjectsUseCase,
        private val observeArchivedProjectsUseCase: ObserveArchivedProjectsUseCase,
        private val createProjectUseCase: CreateProjectUseCase,
        private val updateProjectUseCase: UpdateProjectUseCase,
        private val setProjectArchivedUseCase: SetProjectArchivedUseCase,
        private val toggleProjectFavoriteUseCase: ToggleProjectFavoriteUseCase,
        private val deleteProjectUseCase: DeleteProjectUseCase,
        private val countActiveTasksInProjectUseCase: CountActiveTasksInProjectUseCase,
        private val reorderProjectsUseCase: ReorderProjectsUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProjectsViewModel(
                observeActiveProjectsUseCase, observeArchivedProjectsUseCase, createProjectUseCase, updateProjectUseCase,
                setProjectArchivedUseCase, toggleProjectFavoriteUseCase, deleteProjectUseCase,
                countActiveTasksInProjectUseCase, reorderProjectsUseCase,
            ) as T
        }
    }
}
