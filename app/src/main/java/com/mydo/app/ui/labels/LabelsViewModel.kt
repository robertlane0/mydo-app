package com.mydo.app.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Label
import com.mydo.app.domain.usecase.CreateLabelUseCase
import com.mydo.app.domain.usecase.DeleteLabelUseCase
import com.mydo.app.domain.usecase.ObserveLabelsUseCase
import com.mydo.app.domain.usecase.UpdateLabelUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface LabelsUiState {
    data object Loading : LabelsUiState
    data class Ready(val labels: List<Label>) : LabelsUiState
    data class Error(val message: String) : LabelsUiState
}

class LabelsViewModel(
    observeLabelsUseCase: ObserveLabelsUseCase,
    private val createLabelUseCase: CreateLabelUseCase,
    private val updateLabelUseCase: UpdateLabelUseCase,
    private val deleteLabelUseCase: DeleteLabelUseCase,
) : ViewModel() {

    val uiState: StateFlow<LabelsUiState> = observeLabelsUseCase().map {
        when (it) {
            is AppResult.Success -> LabelsUiState.Ready(it.value)
            is AppResult.Failure -> LabelsUiState.Error(it.error.userMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LabelsUiState.Loading)

    val errors = MutableSharedFlow<String>()

    fun createLabel(name: String, color: String) {
        viewModelScope.launch {
            val result = createLabelUseCase(name, color)
            if (result is AppResult.Failure) errors.emit(result.error.userMessage)
        }
    }

    fun updateLabel(label: Label) {
        viewModelScope.launch {
            val result = updateLabelUseCase(label)
            if (result is AppResult.Failure) errors.emit(result.error.userMessage)
        }
    }

    fun deleteLabel(id: UUID) {
        viewModelScope.launch { deleteLabelUseCase(id) }
    }

    class Factory(
        private val observeLabelsUseCase: ObserveLabelsUseCase,
        private val createLabelUseCase: CreateLabelUseCase,
        private val updateLabelUseCase: UpdateLabelUseCase,
        private val deleteLabelUseCase: DeleteLabelUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LabelsViewModel(observeLabelsUseCase, createLabelUseCase, updateLabelUseCase, deleteLabelUseCase) as T
        }
    }
}
