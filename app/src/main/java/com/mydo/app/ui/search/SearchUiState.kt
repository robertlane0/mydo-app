package com.mydo.app.ui.search

import com.mydo.app.domain.model.RecentSearch
import com.mydo.app.domain.model.SearchResults

sealed interface SearchUiState {
    /** Empty query — shows recent searches only. */
    data class Idle(val recentSearches: List<RecentSearch>) : SearchUiState
    data object Searching : SearchUiState
    data class Results(val results: SearchResults) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
