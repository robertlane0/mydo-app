package com.mydo.app.ui.taskdetail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.usecase.AddAttachmentsUseCase
import com.mydo.app.domain.usecase.AssignLabelUseCase
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.CreateAbsoluteReminderUseCase
import com.mydo.app.domain.usecase.CreateRelativeReminderUseCase
import com.mydo.app.domain.usecase.DeleteReminderUseCase
import com.mydo.app.domain.usecase.DeleteTaskUseCase
import com.mydo.app.domain.usecase.ObserveActiveProjectsUseCase
import com.mydo.app.domain.usecase.ObserveAttachmentsUseCase
import com.mydo.app.domain.usecase.ObserveLabelsUseCase
import com.mydo.app.domain.usecase.ObserveRemindersUseCase
import com.mydo.app.domain.usecase.ObserveTaskUseCase
import com.mydo.app.domain.usecase.RemoveAttachmentUseCase
import com.mydo.app.domain.usecase.RemoveRecurrenceUseCase
import com.mydo.app.domain.usecase.RescheduleTaskUseCase
import com.mydo.app.domain.usecase.SetRecurrenceUseCase
import com.mydo.app.domain.usecase.SkipNextOccurrenceUseCase
import com.mydo.app.domain.usecase.UnassignLabelUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import com.mydo.app.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface TaskDetailEvent {
    data class Completed(val outcome: CompleteTaskUseCase.Outcome) : TaskDetailEvent
    data class Message(val text: String) : TaskDetailEvent
    data object Deleted : TaskDetailEvent
}

class TaskDetailViewModel(
    private val taskId: UUID,
    observeTaskUseCase: ObserveTaskUseCase,
    observeActiveProjectsUseCase: ObserveActiveProjectsUseCase,
    observeLabelsUseCase: ObserveLabelsUseCase,
    observeRemindersUseCase: ObserveRemindersUseCase,
    observeAttachmentsUseCase: ObserveAttachmentsUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val setRecurrenceUseCase: SetRecurrenceUseCase,
    private val removeRecurrenceUseCase: RemoveRecurrenceUseCase,
    private val skipNextOccurrenceUseCase: SkipNextOccurrenceUseCase,
    private val rescheduleTaskUseCase: RescheduleTaskUseCase,
    private val createAbsoluteReminderUseCase: CreateAbsoluteReminderUseCase,
    private val createRelativeReminderUseCase: CreateRelativeReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val addAttachmentsUseCase: AddAttachmentsUseCase,
    private val removeAttachmentUseCase: RemoveAttachmentUseCase,
    private val assignLabelUseCase: AssignLabelUseCase,
    private val unassignLabelUseCase: UnassignLabelUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val events = MutableSharedFlow<TaskDetailEvent>()

    val uiState: StateFlow<TaskDetailUiState> = combine(
        observeTaskUseCase(taskId),
        observeActiveProjectsUseCase(),
        observeLabelsUseCase(),
        observeRemindersUseCase(taskId),
        observeAttachmentsUseCase(taskId),
    ) { taskResult, projectsResult, labelsResult, remindersResult, attachmentsResult ->
        if (taskResult is AppResult.Failure) return@combine TaskDetailUiState.Error(taskResult.error.userMessage)
        val task = (taskResult as AppResult.Success).value ?: return@combine TaskDetailUiState.NotFound
        TaskDetailUiState.Ready(
            task = task,
            allProjects = (projectsResult as? AppResult.Success)?.value.orEmpty(),
            allLabels = (labelsResult as? AppResult.Success)?.value.orEmpty(),
            reminders = (remindersResult as? AppResult.Success)?.value.orEmpty(),
            attachments = (attachmentsResult as? AppResult.Success)?.value.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskDetailUiState.Loading)

    fun updateTitle(title: String) = withTask { task -> updateTaskUseCase(task.copy(title = title, updatedAtUtcMillis = now())) }
    fun updateDescription(description: String) = withTask { task -> updateTaskUseCase(task.copy(description = description, updatedAtUtcMillis = now())) }
    fun updatePriority(priority: Priority) = withTask { task -> updateTaskUseCase(task.copy(priority = priority, updatedAtUtcMillis = now())) }

    fun updateDueDate(dueAtUtcMillis: Long?) {
        viewModelScope.launch { rescheduleTaskUseCase(taskId, dueAtUtcMillis) }
    }

    fun moveToProject(projectId: UUID?) = withTask { task -> updateTaskUseCase(task.copy(projectId = projectId, sectionId = null, updatedAtUtcMillis = now())) }

    fun toggleComplete() {
        val state = uiState.value as? TaskDetailUiState.Ready ?: return
        viewModelScope.launch {
            if (state.task.completed) {
                updateTaskUseCase(state.task.copy(completed = false, completedAtUtcMillis = null, updatedAtUtcMillis = now()))
            } else {
                val result = completeTaskUseCase(taskId)
                if (result is AppResult.Success) events.emit(TaskDetailEvent.Completed(result.value))
            }
        }
    }

    fun undoComplete(outcome: CompleteTaskUseCase.Outcome) {
        viewModelScope.launch { undoCompleteTaskUseCase(outcome) }
    }

    fun setRecurrence(ruleString: String) {
        viewModelScope.launch {
            val result = if (ruleString.isBlank()) removeRecurrenceUseCase(taskId) else setRecurrenceUseCase(taskId, ruleString)
            if (result is AppResult.Failure) events.emit(TaskDetailEvent.Message(result.error.userMessage))
        }
    }

    fun removeRecurrence() {
        viewModelScope.launch { removeRecurrenceUseCase(taskId) }
    }

    fun skipNextOccurrence() {
        viewModelScope.launch {
            val result = skipNextOccurrenceUseCase(taskId)
            if (result is AppResult.Failure) events.emit(TaskDetailEvent.Message(result.error.userMessage))
        }
    }

    fun addRelativeReminder(minutesBefore: Long) {
        viewModelScope.launch {
            val result = createRelativeReminderUseCase(taskId, minutesBefore)
            if (result is AppResult.Failure) events.emit(TaskDetailEvent.Message(result.error.userMessage))
        }
    }

    fun addAbsoluteReminder(triggerAtUtcMillis: Long) {
        viewModelScope.launch { createAbsoluteReminderUseCase(taskId, triggerAtUtcMillis) }
    }

    fun deleteReminder(id: UUID) {
        viewModelScope.launch { deleteReminderUseCase(id) }
    }

    fun addAttachments(uris: List<Uri>) {
        viewModelScope.launch {
            val result = addAttachmentsUseCase(taskId, uris)
            if (result is AppResult.Failure) events.emit(TaskDetailEvent.Message(result.error.userMessage))
        }
    }

    fun removeAttachment(attachment: com.mydo.app.domain.model.Attachment) {
        viewModelScope.launch { removeAttachmentUseCase(attachment) }
    }

    fun toggleLabel(labelId: UUID, currentlyApplied: Boolean) {
        viewModelScope.launch {
            if (currentlyApplied) unassignLabelUseCase(taskId, labelId) else assignLabelUseCase(taskId, labelId)
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            val result = deleteTaskUseCase(taskId)
            if (result is AppResult.Success) events.emit(TaskDetailEvent.Deleted)
        }
    }

    private fun now() = timeProvider.nowUtcMillis()

    private inline fun withTask(crossinline action: suspend (Task) -> AppResult<Unit>) {
        val state = uiState.value as? TaskDetailUiState.Ready ?: return
        viewModelScope.launch { action(state.task) }
    }

    class Factory(
        private val taskId: UUID,
        private val observeTaskUseCase: ObserveTaskUseCase,
        private val observeActiveProjectsUseCase: ObserveActiveProjectsUseCase,
        private val observeLabelsUseCase: ObserveLabelsUseCase,
        private val observeRemindersUseCase: ObserveRemindersUseCase,
        private val observeAttachmentsUseCase: ObserveAttachmentsUseCase,
        private val updateTaskUseCase: UpdateTaskUseCase,
        private val deleteTaskUseCase: DeleteTaskUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
        private val setRecurrenceUseCase: SetRecurrenceUseCase,
        private val removeRecurrenceUseCase: RemoveRecurrenceUseCase,
        private val skipNextOccurrenceUseCase: SkipNextOccurrenceUseCase,
        private val rescheduleTaskUseCase: RescheduleTaskUseCase,
        private val createAbsoluteReminderUseCase: CreateAbsoluteReminderUseCase,
        private val createRelativeReminderUseCase: CreateRelativeReminderUseCase,
        private val deleteReminderUseCase: DeleteReminderUseCase,
        private val addAttachmentsUseCase: AddAttachmentsUseCase,
        private val removeAttachmentUseCase: RemoveAttachmentUseCase,
        private val assignLabelUseCase: AssignLabelUseCase,
        private val unassignLabelUseCase: UnassignLabelUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TaskDetailViewModel(
                taskId, observeTaskUseCase, observeActiveProjectsUseCase, observeLabelsUseCase, observeRemindersUseCase,
                observeAttachmentsUseCase, updateTaskUseCase, deleteTaskUseCase, completeTaskUseCase, undoCompleteTaskUseCase,
                setRecurrenceUseCase, removeRecurrenceUseCase, skipNextOccurrenceUseCase, rescheduleTaskUseCase,
                createAbsoluteReminderUseCase, createRelativeReminderUseCase, deleteReminderUseCase, addAttachmentsUseCase,
                removeAttachmentUseCase, assignLabelUseCase, unassignLabelUseCase, timeProvider,
            ) as T
        }
    }
}
