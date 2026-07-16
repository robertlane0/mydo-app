package com.mydo.app.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Filter
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.FilterRepository
import com.mydo.app.domain.usecase.RunFilterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface FilterResultsUiState {
    data object Loading : FilterResultsUiState
    data class Ready(val filter: Filter, val tasks: List<TaskSummary>) : FilterResultsUiState
    data class Error(val message: String) : FilterResultsUiState
}

/** Loads a saved filter by id, then runs its query (specs14-filters.md, "Filter Results"). */
class FilterResultsViewModel(
    private val filterId: UUID,
    private val filterRepository: FilterRepository,
    private val runFilterUseCase: RunFilterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FilterResultsUiState>(FilterResultsUiState.Loading)
    val uiState: StateFlow<FilterResultsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val filterResult = filterRepository.getById(filterId)
            val filter = when (filterResult) {
                is AppResult.Failure -> { _uiState.value = FilterResultsUiState.Error(filterResult.error.userMessage); return@launch }
                is AppResult.Success -> filterResult.value ?: run {
                    _uiState.value = FilterResultsUiState.Error("This filter no longer exists")
                    return@launch
                }
            }
            when (val result = runFilterUseCase(filter.query)) {
                is AppResult.Success -> _uiState.value = FilterResultsUiState.Ready(filter, result.value)
                is AppResult.Failure -> _uiState.value = FilterResultsUiState.Error(result.error.userMessage)
            }
        }
    }

    class Factory(
        private val filterId: UUID,
        private val filterRepository: FilterRepository,
        private val runFilterUseCase: RunFilterUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FilterResultsViewModel(filterId, filterRepository, runFilterUseCase) as T
        }
    }
}
