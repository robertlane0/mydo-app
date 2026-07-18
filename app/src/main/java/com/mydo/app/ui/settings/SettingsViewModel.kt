package com.mydo.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.AppSettings
import com.mydo.app.domain.model.BackupManifest
import com.mydo.app.domain.model.DefaultHomeView
import com.mydo.app.domain.model.ImportStrategy
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.StartOfWeek
import com.mydo.app.domain.model.ThemeMode
import com.mydo.app.domain.usecase.ClearLocalDataUseCase
import com.mydo.app.domain.usecase.ExportBackupUseCase
import com.mydo.app.domain.usecase.ImportBackupUseCase
import com.mydo.app.domain.usecase.InspectBackupUseCase
import com.mydo.app.domain.usecase.ObserveSettingsUseCase
import com.mydo.app.domain.usecase.UpdateSettingUseCase
import com.mydo.app.platform.ShareGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val settings: AppSettings) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

/**
 * State for the "Data" section (specs10-settings.md, "Data"): export, import, and clear
 * local data. Kept separate from [SettingsUiState] since it drives its own dialogs/progress
 * independent of whether preferences have loaded.
 */
sealed interface DataOperationState {
    data object Idle : DataOperationState
    data object InProgress : DataOperationState
    /** Export finished; [json] is ready to be written wherever the user picks via the
     *  system save chooser. */
    data class ExportReady(val json: String, val suggestedFilename: String) : DataOperationState
    /** A chosen file passed validation; only [ImportStrategy.REPLACE] is offered — MyDo
     *  doesn't yet support merge import, so there's no conflict-resolution step. */
    data class ImportPreview(val rawJson: String, val manifest: BackupManifest) : DataOperationState
    data class Message(val text: String, val isError: Boolean = false) : DataOperationState
}

/** Every setter here fires immediately — there is no explicit Save button (specs10-settings.md). */
class SettingsViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateSettingUseCase: UpdateSettingUseCase,
    private val exportBackupUseCase: ExportBackupUseCase? = null,
    private val inspectBackupUseCase: InspectBackupUseCase? = null,
    private val importBackupUseCase: ImportBackupUseCase? = null,
    private val clearLocalDataUseCase: ClearLocalDataUseCase? = null,
    private val shareGateway: ShareGateway? = null,
    private val timeProvider: TimeProvider? = null,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeSettingsUseCase().map {
        when (it) {
            is AppResult.Success -> SettingsUiState.Ready(it.value)
            is AppResult.Failure -> SettingsUiState.Error(it.error.userMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState.Loading)

    private val _dataState = MutableStateFlow<DataOperationState>(DataOperationState.Idle)
    val dataState: StateFlow<DataOperationState> = _dataState.asStateFlow()

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

    // -- Data: export --

    /** Step 1 of export: build the backup JSON. The screen follows this by launching the
     *  system CreateDocument chooser, then calling [writeExport] with the chosen [Uri]. */
    fun startExport() {
        val export = exportBackupUseCase ?: return
        _dataState.value = DataOperationState.InProgress
        viewModelScope.launch {
            when (val result = export()) {
                is AppResult.Success -> {
                    val filename = shareGateway?.suggestedBackupFilename(timeProvider?.nowUtcMillis() ?: System.currentTimeMillis())
                        ?: "mydo-backup.json"
                    _dataState.value = DataOperationState.ExportReady(result.value, filename)
                }
                is AppResult.Failure -> _dataState.value = DataOperationState.Message(result.error.userMessage, isError = true)
            }
        }
    }

    /** Step 2 of export: write the already-built JSON to the destination the user picked. */
    fun writeExport(uri: Uri) {
        val json = (_dataState.value as? DataOperationState.ExportReady)?.json ?: return
        val gateway = shareGateway ?: return
        _dataState.value = DataOperationState.InProgress
        viewModelScope.launch {
            val wrote = gateway.writeText(uri, json)
            _dataState.value = if (wrote) {
                DataOperationState.Message("Backup saved.")
            } else {
                DataOperationState.Message("Couldn't save the backup — check storage space and try again.", isError = true)
            }
        }
    }

    // -- Data: import --

    /** Step 1 of import: read and validate a chosen file without touching the database. */
    fun onImportFileChosen(uri: Uri) {
        val gateway = shareGateway ?: return
        val inspect = inspectBackupUseCase ?: return
        _dataState.value = DataOperationState.InProgress
        viewModelScope.launch {
            val text = gateway.readText(uri)
            if (text == null) {
                _dataState.value = DataOperationState.Message("Couldn't open that file.", isError = true)
                return@launch
            }
            when (val result = inspect(text)) {
                is AppResult.Success -> _dataState.value = DataOperationState.ImportPreview(text, result.value)
                is AppResult.Failure -> _dataState.value = DataOperationState.Message(result.error.userMessage, isError = true)
            }
        }
    }

    /** Step 2 of import: the user confirmed the Replace warning shown for [DataOperationState.ImportPreview]. */
    fun confirmReplaceImport() {
        val preview = _dataState.value as? DataOperationState.ImportPreview ?: return
        val importBackup = importBackupUseCase ?: return
        _dataState.value = DataOperationState.InProgress
        viewModelScope.launch {
            when (val result = importBackup(preview.rawJson, ImportStrategy.REPLACE)) {
                is AppResult.Success -> _dataState.value = DataOperationState.Message(
                    "Import complete: ${result.value.counts.tasks} tasks, ${result.value.counts.projects} projects restored."
                )
                is AppResult.Failure -> _dataState.value = DataOperationState.Message(result.error.userMessage, isError = true)
            }
        }
    }

    // -- Data: clear --

    fun clearLocalData() {
        val clear = clearLocalDataUseCase ?: return
        _dataState.value = DataOperationState.InProgress
        viewModelScope.launch {
            when (val result = clear()) {
                is AppResult.Success -> _dataState.value = DataOperationState.Message("All local data cleared.")
                is AppResult.Failure -> _dataState.value = DataOperationState.Message(result.error.userMessage, isError = true)
            }
        }
    }

    fun dismissDataState() {
        _dataState.value = DataOperationState.Idle
    }

    class Factory(
        private val observeSettingsUseCase: ObserveSettingsUseCase,
        private val updateSettingUseCase: UpdateSettingUseCase,
        private val exportBackupUseCase: ExportBackupUseCase? = null,
        private val inspectBackupUseCase: InspectBackupUseCase? = null,
        private val importBackupUseCase: ImportBackupUseCase? = null,
        private val clearLocalDataUseCase: ClearLocalDataUseCase? = null,
        private val shareGateway: ShareGateway? = null,
        private val timeProvider: TimeProvider? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                observeSettingsUseCase, updateSettingUseCase,
                exportBackupUseCase, inspectBackupUseCase, importBackupUseCase, clearLocalDataUseCase,
                shareGateway, timeProvider,
            ) as T
        }
    }
}
