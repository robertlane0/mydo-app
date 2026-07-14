package com.mydo.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.usecase.SearchTasksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val searchTasks: SearchTasksUseCase,
) : ViewModel() {
    
    private val _query = MutableStateFlow("")
    val query: MutableStateFlow<String> = _query
    
    val uiState = _query
        .flatMapLatest { query ->
            if (query.isBlank()) {
                kotlinx.coroutines.flow.flowOf(AppResult.Success(emptyList<TaskSummary>()))
            } else {
                searchTasks(query)
            }
        }
        .map { result ->
            when (result) {
                is AppResult.Failure -> SearchUiState.Error(result.error.userMessage)
                is AppResult.Success -> SearchUiState.Ready(_query.value, result.value)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SearchUiState.Loading,
        )
    
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
    
    fun clearQuery() {
        _query.value = ""
    }

    class Factory(
        private val searchTasks: SearchTasksUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(searchTasks) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

sealed interface SearchUiState {
    data object Loading : SearchUiState
    data class Ready(val query: String, val tasks: List<TaskSummary>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}