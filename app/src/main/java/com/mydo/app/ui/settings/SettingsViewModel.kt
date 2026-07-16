package com.mydo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.AppSettings
import com.mydo.app.domain.model.DefaultHomeView
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.StartOfWeek
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.domain.usecase.ObserveSettingsUseCase
import com.mydo.app.domain.usecase.UpdateSettingUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val settings: AppSettings) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

/** Every setter here fires immediately — there is no explicit Save button (specs10-settings.md). */
class SettingsViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateSettingUseCase: UpdateSettingUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeSettingsUseCase().map {
        when (it) {
            is AppResult.Success -> SettingsUiState.Ready(it.value)
            is AppResult.Failure -> SettingsUiState.Error(it.error.userMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState.Loading)

    fun setDefaultHomeView(v: DefaultHomeView) = update { updateSettingUseCase.setDefaultHomeView(v) }
    fun setDefaultPriority(v: Priority) = update { updateSettingUseCase.setDefaultPriority(v) }
    fun setDefaultReminderMinutesBefore(v: Int) = update { updateSettingUseCase.setDefaultReminderMinutesBefore(v) }
    fun setStartOfWeek(v: StartOfWeek) = update { updateSettingUseCase.setStartOfWeek(v) }
    fun setThemeMode(v: ThemeMode) = update { updateSettingUseCase.setThemeMode(v) }
    fun setUseDynamicColor(v: Boolean) = update { updateSettingUseCase.setUseDynamicColor(v) }
    fun setCompactTaskRows(v: Boolean) = update { updateSettingUseCase.setCompactTaskRows(v) }
    fun setDailyGoal(v: Int) = update { updateSettingUseCase.setDailyGoal(v) }
    fun setShowCompletedTasks(v: Boolean) = update { updateSettingUseCase.setShowCompletedTasks(v) }
    fun setShowWeekends(v: Boolean) = update { updateSettingUseCase.setShowWeekends(v) }
    fun setCompletionAnimationEnabled(v: Boolean) = update { updateSettingUseCase.setCompletionAnimationEnabled(v) }
    fun setNotificationsEnabled(v: Boolean) = update { updateSettingUseCase.setNotificationsEnabled(v) }
    fun setReminderSoundEnabled(v: Boolean) = update { updateSettingUseCase.setReminderSoundEnabled(v) }
    fun setDailySummaryEnabled(v: Boolean) = update { updateSettingUseCase.setDailySummaryEnabled(v) }
    fun setAnalyticsEnabled(v: Boolean) = update { updateSettingUseCase.setAnalyticsEnabled(v) }
    fun setCrashReportingEnabled(v: Boolean) = update { updateSettingUseCase.setCrashReportingEnabled(v) }

    private fun update(action: suspend () -> AppResult<Unit>) {
        viewModelScope.launch { action() }
    }

    class Factory(
        private val observeSettingsUseCase: ObserveSettingsUseCase,
        private val updateSettingUseCase: UpdateSettingUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(observeSettingsUseCase, updateSettingUseCase) as T
        }
    }
}
