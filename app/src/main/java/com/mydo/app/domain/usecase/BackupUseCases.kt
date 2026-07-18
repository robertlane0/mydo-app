package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.BackupManifest
import com.mydo.app.domain.model.ImportStrategy
import com.mydo.app.domain.model.ImportSummary
import com.mydo.app.domain.repository.BackupRepository

/** Serializes the entire local database into one versioned JSON backup (specs10-settings.md,
 *  "Data" -> Export). Handing the result off to a chosen destination is the caller's job —
 *  see [com.mydo.app.platform.ShareGateway]. */
class ExportBackupUseCase(private val backupRepository: BackupRepository) {
    suspend operator fun invoke(): AppResult<String> = backupRepository.exportBackup()
}

/** Validates a chosen file and returns what it contains, without touching the database —
 *  always call this before [ImportBackupUseCase] so the user can confirm what they're
 *  about to import. */
class InspectBackupUseCase(private val backupRepository: BackupRepository) {
    suspend operator fun invoke(rawJson: String): AppResult<BackupManifest> = backupRepository.inspectBackup(rawJson)
}

/** Imports a previously-inspected backup. Re-validates independently of any prior inspect
 *  call, and leaves the database untouched if that validation fails. */
class ImportBackupUseCase(
    private val backupRepository: BackupRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(rawJson: String, strategy: ImportStrategy = ImportStrategy.REPLACE): AppResult<ImportSummary> {
        // The replace about to happen removes every existing reminder row outright, so any
        // alarm armed for one of them becomes uncancellable by taskId lookup afterward —
        // capture which reminders are currently armed before the wipe, not after.
        val staleIds = reminderAlarmCoordinator?.pendingReminderIds().orEmpty()
        val result = backupRepository.importBackup(rawJson, strategy)
        if (result is AppResult.Success) {
            reminderAlarmCoordinator?.cancel(staleIds)
            reminderAlarmCoordinator?.rescheduleAll() // arms whatever the imported data brought in
        }
        return result
    }
}

/** Wipes all local data after an explicit, separate user confirmation (specs10-settings.md,
 *  "Data" -> Clear local data) — this is not part of import, since import's own confirmation
 *  is about *replacing with a backup*, not about deleting with nothing to replace it. */
class ClearLocalDataUseCase(
    private val backupRepository: BackupRepository,
    private val reminderAlarmCoordinator: ReminderAlarmCoordinator? = null,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val staleIds = reminderAlarmCoordinator?.pendingReminderIds().orEmpty()
        val result = backupRepository.clearLocalData()
        if (result is AppResult.Success) reminderAlarmCoordinator?.cancel(staleIds)
        return result
    }
}
