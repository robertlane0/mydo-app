package com.mydo.app.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Filter
import com.mydo.app.domain.usecase.CreateFilterUseCase
import com.mydo.app.domain.usecase.DeleteFilterUseCase
import com.mydo.app.domain.usecase.ObserveFiltersUseCase
import com.mydo.app.domain.usecase.ToggleFilterFavoriteUseCase
import com.mydo.app.domain.usecase.UpdateFilterUseCase
import com.mydo.app.domain.usecase.ValidateFilterQueryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface FiltersUiState {
    data object Loading : FiltersUiState
    data class Ready(val filters: List<Filter>) : FiltersUiState
    data class Error(val message: String) : FiltersUiState
}

class FiltersViewModel(
    observeFiltersUseCase: ObserveFiltersUseCase,
    private val createFilterUseCase: CreateFilterUseCase,
    private val updateFilterUseCase: UpdateFilterUseCase,
    private val deleteFilterUseCase: DeleteFilterUseCase,
    private val toggleFilterFavoriteUseCase: ToggleFilterFavoriteUseCase,
    val validateFilterQueryUseCase: ValidateFilterQueryUseCase,
) : ViewModel() {

    val uiState: StateFlow<FiltersUiState> = observeFiltersUseCase().map {
        when (it) {
            is AppResult.Success -> FiltersUiState.Ready(it.value.sortedWith(compareByDescending<Filter> { f -> f.favorite }.thenBy { f -> f.name }))
            is AppResult.Failure -> FiltersUiState.Error(it.error.userMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FiltersUiState.Loading)

    val errors = MutableSharedFlow<String>()

    fun createFilter(name: String, query: String) {
        viewModelScope.launch {
            val result = createFilterUseCase(name, query)
            if (result is AppResult.Failure) errors.emit(result.error.userMessage)
        }
    }

    fun updateFilter(filter: Filter) {
        viewModelScope.launch {
            val result = updateFilterUseCase(filter)
            if (result is AppResult.Failure) errors.emit(result.error.userMessage)
        }
    }

    fun deleteFilter(id: UUID) {
        viewModelScope.launch { deleteFilterUseCase(id) }
    }

    fun toggleFavorite(filter: Filter) {
        viewModelScope.launch { toggleFilterFavoriteUseCase(filter) }
    }

    class Factory(
        private val observeFiltersUseCase: ObserveFiltersUseCase,
        private val createFilterUseCase: CreateFilterUseCase,
        private val updateFilterUseCase: UpdateFilterUseCase,
        private val deleteFilterUseCase: DeleteFilterUseCase,
        private val toggleFilterFavoriteUseCase: ToggleFilterFavoriteUseCase,
        private val validateFilterQueryUseCase: ValidateFilterQueryUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FiltersViewModel(
                observeFiltersUseCase, createFilterUseCase, updateFilterUseCase,
                deleteFilterUseCase, toggleFilterFavoriteUseCase, validateFilterQueryUseCase,
            ) as T
        }
    }
}
