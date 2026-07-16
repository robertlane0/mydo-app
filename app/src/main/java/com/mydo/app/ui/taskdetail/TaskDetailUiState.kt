package com.mydo.app.ui.taskdetail

import com.mydo.app.domain.model.Attachment
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.model.Task

sealed interface TaskDetailUiState {
    data object Loading : TaskDetailUiState
    data object NotFound : TaskDetailUiState
    data class Error(val message: String) : TaskDetailUiState
    data class Ready(
        val task: Task,
        val allProjects: List<Project> = emptyList(),
        val allLabels: List<Label> = emptyList(),
        val reminders: List<Reminder> = emptyList(),
        val attachments: List<Attachment> = emptyList(),
    ) : TaskDetailUiState
}
