package com.mydo.app.ui.projects

import com.mydo.app.domain.model.Project

sealed interface ProjectsUiState {
    data object Loading : ProjectsUiState

    data class Ready(
        val favorites: List<Project>,
        val others: List<Project>,
        val showingArchived: Boolean,
        val archived: List<Project> = emptyList(),
    ) : ProjectsUiState {
        val isEmpty: Boolean get() = favorites.isEmpty() && others.isEmpty() && (!showingArchived || archived.isEmpty())
    }

    data class Error(val message: String) : ProjectsUiState
}
