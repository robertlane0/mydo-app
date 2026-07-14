package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun observePreferences(): Flow<AppResult<Map<String, String>>>

    suspend fun setPreference(key: String, value: String): AppResult<Unit>
}
