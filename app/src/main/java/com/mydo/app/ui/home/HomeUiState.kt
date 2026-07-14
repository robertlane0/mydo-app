package com.mydo.app.ui.home

import com.mydo.app.domain.model.TaskSummary

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val tasks: List<TaskSummary>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
