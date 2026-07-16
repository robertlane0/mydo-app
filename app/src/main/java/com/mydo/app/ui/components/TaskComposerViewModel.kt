package com.mydo.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.domain.usecase.CreateTaskUseCase
import kotlinx.coroutines.launch

class TaskComposerViewModel(
    private val createTaskUseCase: CreateTaskUseCase
) : ViewModel() {

    var presetDueAtUtcMillis: Long? = null
    var presetProjectId: java.util.UUID? = null

    fun createTask(title: String) {
        if (title.isBlank()) return
        val dueAtUtcMillis = presetDueAtUtcMillis
        val projectId = presetProjectId
        viewModelScope.launch {
            createTaskUseCase(title, projectId = projectId, dueAtUtcMillis = dueAtUtcMillis)
            presetDueAtUtcMillis = null
            presetProjectId = null
        }
    }

    class Factory(
        private val createTaskUseCase: CreateTaskUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskComposerViewModel::class.java)) {
                return TaskComposerViewModel(createTaskUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
