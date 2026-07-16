package com.mydo.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.SearchResults
import com.mydo.app.domain.usecase.ClearRecentSearchesUseCase
import com.mydo.app.domain.usecase.ObserveRecentSearchesUseCase
import com.mydo.app.domain.usecase.RecordRecentSearchUseCase
import com.mydo.app.domain.usecase.RemoveRecentSearchUseCase
import com.mydo.app.domain.usecase.SearchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DEBOUNCE_MILLIS = 300L

/**
 * Backs the Search screen (specs08-search.md): incremental, debounced, case-insensitive
 * search across tasks/projects/sections/labels/filters, plus locally-persisted recent
 * searches.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val searchUseCase: SearchUseCase,
    observeRecentSearchesUseCase: ObserveRecentSearchesUseCase,
    private val recordRecentSearchUseCase: RecordRecentSearchUseCase,
    private val removeRecentSearchUseCase: RemoveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val recentSearches = observeRecentSearchesUseCase()
        .map { (it as? AppResult.Success)?.value.orEmpty() }

    val uiState: StateFlow<SearchUiState> = query
        .debounce(DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                recentSearches.map { SearchUiState.Idle(it) }
            } else {
                flowOf(q).map { runSearch(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle(emptyList()))

    private var lastRecordedQuery: String? = null

    private suspend fun runSearch(q: String): SearchUiState {
        val result = searchUseCase(q)
        return when (result) {
            is AppResult.Failure -> SearchUiState.Error(result.error.userMessage)
            is AppResult.Success -> {
                if (lastRecordedQuery != q) {
                    lastRecordedQuery = q
                    recordRecentSearchUseCase(q)
                }
                SearchUiState.Results(result.value)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun removeRecentSearch(text: String) {
        viewModelScope.launch { removeRecentSearchUseCase(text) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { clearRecentSearchesUseCase() }
    }

    class Factory(
        private val searchUseCase: SearchUseCase,
        private val observeRecentSearchesUseCase: ObserveRecentSearchesUseCase,
        private val recordRecentSearchUseCase: RecordRecentSearchUseCase,
        private val removeRecentSearchUseCase: RemoveRecentSearchUseCase,
        private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(
                searchUseCase, observeRecentSearchesUseCase, recordRecentSearchUseCase,
                removeRecentSearchUseCase, clearRecentSearchesUseCase,
            ) as T
        }
    }
}
