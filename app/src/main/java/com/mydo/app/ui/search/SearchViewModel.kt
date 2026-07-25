package com.mydo.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.usecase.ClearRecentSearchesUseCase
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.ObserveRecentSearchesUseCase
import com.mydo.app.domain.usecase.RecordRecentSearchUseCase
import com.mydo.app.domain.usecase.RemoveRecentSearchUseCase
import com.mydo.app.domain.usecase.SearchUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val DEBOUNCE_MILLIS = 300L

/** One-shot event (an Undo snackbar) the Search screen should surface once and forget. */
sealed interface SearchEvent {
    data class TaskCompleted(val outcome: CompleteTaskUseCase.Outcome) : SearchEvent
}

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
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    // Search results come from a one-shot suspend query rather than a reactive Flow, so
    // completing a task from a result row wouldn't otherwise cause the list to refresh.
    // Bumping this alongside the (debounced, deduplicated) query re-runs the same search.
    private val refreshTick = MutableStateFlow(0)

    private val recentSearches = observeRecentSearchesUseCase()
        .map { (it as? AppResult.Success)?.value.orEmpty() }

    val uiState: StateFlow<SearchUiState> = combine(
        query.debounce(DEBOUNCE_MILLIS).distinctUntilChanged(),
        refreshTick,
    ) { q, _ -> q }
        .flatMapLatest { q ->
            if (q.isBlank()) {
                recentSearches.map { SearchUiState.Idle(it) }
            } else {
                flowOf(q).map { runSearch(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle(emptyList()))

    val events = MutableSharedFlow<SearchEvent>(extraBufferCapacity = 1)

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

    fun completeTask(id: UUID) {
        viewModelScope.launch {
            val result = completeTaskUseCase(id)
            if (result is AppResult.Success) {
                events.tryEmit(SearchEvent.TaskCompleted(result.value))
                refreshTick.value += 1
            }
        }
    }

    fun undoComplete(outcome: CompleteTaskUseCase.Outcome) {
        viewModelScope.launch {
            undoCompleteTaskUseCase(outcome)
            refreshTick.value += 1
        }
    }

    class Factory(
        private val searchUseCase: SearchUseCase,
        private val observeRecentSearchesUseCase: ObserveRecentSearchesUseCase,
        private val recordRecentSearchUseCase: RecordRecentSearchUseCase,
        private val removeRecentSearchUseCase: RemoveRecentSearchUseCase,
        private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(
                searchUseCase, observeRecentSearchesUseCase, recordRecentSearchUseCase,
                removeRecentSearchUseCase, clearRecentSearchesUseCase, completeTaskUseCase, undoCompleteTaskUseCase,
            ) as T
        }
    }
}
