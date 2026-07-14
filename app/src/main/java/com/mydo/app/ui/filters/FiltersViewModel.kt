package com.mydo.app.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Filter
import com.mydo.app.domain.repository.FilterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FiltersViewModel(
    private val filterRepository: FilterRepository,
) : ViewModel() {

    val uiState = filterRepository.observeAll()
        .map { result ->
            when (result) {
                is AppResult.Failure -> FiltersUiState.Error(result.error.userMessage)
                is AppResult.Success -> FiltersUiState.Ready(result.value)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FiltersUiState.Loading,
        )

    fun onCreateFilter(name: String, query: String, favorite: Boolean) {
        // TODO: Implement create filter
    }

    fun onEditFilter(filter: Filter, name: String, query: String, favorite: Boolean) {
        // TODO: Implement edit filter
    }

    fun onDeleteFilter(filter: Filter) {
        // TODO: Implement delete filter
    }

    class Factory(
        private val filterRepository: FilterRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FiltersViewModel::class.java)) {
                return FiltersViewModel(filterRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

sealed interface FiltersUiState {
    data object Loading : FiltersUiState
    data class Ready(val filters: List<Filter>) : FiltersUiState
    data class Error(val message: String) : FiltersUiState
}