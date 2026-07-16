package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.AppSettings
import com.mydo.app.domain.model.DefaultHomeView
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.SettingsKeys
import com.mydo.app.domain.model.StartOfWeek
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.domain.model.toAppSettings
import com.mydo.app.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveSettingsUseCase(private val preferenceRepository: PreferenceRepository) {
    operator fun invoke(): Flow<AppResult<AppSettings>> =
        preferenceRepository.observePreferences().map { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.value.toAppSettings())
                is AppResult.Failure -> result
            }
        }
}

/**
 * Typed setters over the flat preference store; each takes effect immediately since
 * every setting is read live off [ObserveSettingsUseCase]'s Flow (specs10-settings.md:
 * "Changes take effect locally and immediately").
 */
class UpdateSettingUseCase(private val preferenceRepository: PreferenceRepository) {
    suspend fun setDefaultHomeView(value: DefaultHomeView): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.DEFAULT_HOME_VIEW, value.name)

    suspend fun setDefaultPriority(value: Priority): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.DEFAULT_PRIORITY, value.name)

    suspend fun setDefaultReminderMinutesBefore(value: Int): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.DEFAULT_REMINDER_MINUTES, value.toString())

    suspend fun setStartOfWeek(value: StartOfWeek): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.START_OF_WEEK, value.name)

    suspend fun setThemeMode(value: ThemeMode): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.THEME_MODE, value.name)

    suspend fun setUseDynamicColor(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.USE_DYNAMIC_COLOR, value.toString())

    suspend fun setCompactTaskRows(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.COMPACT_TASK_ROWS, value.toString())

    suspend fun setDailyGoal(value: Int): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.DAILY_GOAL, value.toString())

    suspend fun setShowCompletedTasks(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.SHOW_COMPLETED_TASKS, value.toString())

    suspend fun setShowWeekends(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.SHOW_WEEKENDS, value.toString())

    suspend fun setCompletionAnimationEnabled(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.COMPLETION_ANIMATION, value.toString())

    suspend fun setNotificationsEnabled(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.NOTIFICATIONS_ENABLED, value.toString())

    suspend fun setReminderSoundEnabled(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.REMINDER_SOUND_ENABLED, value.toString())

    suspend fun setDailySummaryEnabled(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.DAILY_SUMMARY_ENABLED, value.toString())

    suspend fun setAnalyticsEnabled(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.ANALYTICS_ENABLED, value.toString())

    suspend fun setCrashReportingEnabled(value: Boolean): AppResult<Unit> =
        preferenceRepository.setPreference(SettingsKeys.CRASH_REPORTING_ENABLED, value.toString())
}
