package com.mydo.app.ui.upcoming

import com.mydo.app.domain.model.TaskSummary
import java.time.LocalDate

data class UpcomingDay(val date: LocalDate, val tasks: List<TaskSummary>)

sealed interface UpcomingUiState {
    data object Loading : UpcomingUiState
    data class Error(val message: String) : UpcomingUiState
    data class Ready(
        val overdue: List<TaskSummary>,
        val days: List<UpcomingDay>,
    ) : UpcomingUiState
}
