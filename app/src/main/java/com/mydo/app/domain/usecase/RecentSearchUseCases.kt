package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.RecentSearch
import com.mydo.app.domain.repository.RecentSearchRepository
import kotlinx.coroutines.flow.Flow

class ObserveRecentSearchesUseCase(private val recentSearchRepository: RecentSearchRepository) {
    operator fun invoke(limit: Int = 10): Flow<AppResult<List<RecentSearch>>> = recentSearchRepository.observeRecent(limit)
}

class RecordRecentSearchUseCase(private val recentSearchRepository: RecentSearchRepository) {
    suspend operator fun invoke(query: String): AppResult<Unit> = recentSearchRepository.record(query)
}

class RemoveRecentSearchUseCase(private val recentSearchRepository: RecentSearchRepository) {
    suspend operator fun invoke(query: String): AppResult<Unit> = recentSearchRepository.remove(query)
}

class ClearRecentSearchesUseCase(private val recentSearchRepository: RecentSearchRepository) {
    suspend operator fun invoke(): AppResult<Unit> = recentSearchRepository.clear()
}
