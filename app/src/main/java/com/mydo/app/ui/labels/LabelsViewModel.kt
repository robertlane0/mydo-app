package com.mydo.app.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.repository.LabelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LabelsViewModel(
    private val labelRepository: LabelRepository,
) : ViewModel() {

    val uiState = labelRepository.observeAll()
        .map { result ->
            when (result) {
                is AppResult.Failure -> LabelsUiState.Error(result.error.userMessage)
                is AppResult.Success -> LabelsUiState.Ready(result.value)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = LabelsUiState.Loading,
        )

    fun onCreateLabel(name: String, color: String) {
        // TODO: Implement create label
    }

    fun onEditLabel(label: Label, name: String, color: String) {
        // TODO: Implement edit label
    }

    fun onDeleteLabel(label: Label) {
        // TODO: Implement delete label
    }

    class Factory(
        private val labelRepository: LabelRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LabelsViewModel::class.java)) {
                return LabelsViewModel(labelRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

sealed interface LabelsUiState {
    data object Loading : LabelsUiState
    data class Ready(val labels: List<Label>) : LabelsUiState
    data class Error(val message: String) : LabelsUiState
}