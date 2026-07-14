package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.PreferenceEntity
import com.mydo.app.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomPreferenceRepository(
    private val database: MydoDatabase,
    private val timeProvider: TimeProvider,
) : PreferenceRepository {
    override fun observePreferences(): Flow<AppResult<Map<String, String>>> {
        return database.preferenceDao()
            .observeAll()
            .map<List<PreferenceEntity>, AppResult<Map<String, String>>> { entities ->
                AppResult.Success(entities.associate { it.key to it.value })
            }
            .catch { throwable ->
                emit(
                    AppResult.Failure(
                        DatabaseError(
                            code = "preferences_observe_failed",
                            userMessage = "Unable to load local preferences.",
                            cause = throwable,
                        ),
                    ),
                )
            }
    }

    override suspend fun setPreference(key: String, value: String): AppResult<Unit> {
        return try {
            database.preferenceDao().upsert(
                PreferenceEntity(
                    key = key,
                    value = value,
                    updatedAtUtcMillis = timeProvider.nowUtcMillis(),
                ),
            )
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Failure(
                DatabaseError(
                    code = "preferences_save_failed",
                    userMessage = "Unable to save local preferences.",
                    cause = throwable,
                ),
            )
        }
    }
}
