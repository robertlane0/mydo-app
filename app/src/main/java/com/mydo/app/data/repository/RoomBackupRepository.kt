package com.mydo.app.data.repository

import androidx.room.withTransaction
import com.mydo.app.BuildConfig
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.data.backup.BackupParseException
import com.mydo.app.data.backup.BackupSerializer
import com.mydo.app.data.backup.BackupSnapshot
import com.mydo.app.data.backup.counts
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.TaskEntity
import com.mydo.app.domain.model.BackupManifest
import com.mydo.app.domain.model.ImportStrategy
import com.mydo.app.domain.model.ImportSummary
import com.mydo.app.domain.repository.BackupRepository
import java.io.File

/**
 * Room-backed manual export/import (specs10-settings.md, "Data"; specs11-data-model.md,
 * "Data Portability"). [precautionaryBackupDir], if provided, is where a safety-net copy of
 * the *current* data is written right before a destructive replace — internal app storage,
 * never anything MyDo shares off-device on its own.
 */
class RoomBackupRepository(
    private val db: MydoDatabase,
    private val timeProvider: TimeProvider,
    private val precautionaryBackupDir: File? = null,
) : BackupRepository {
    private val preferenceDao = db.preferenceDao()
    private val projectDao = db.projectDao()
    private val sectionDao = db.sectionDao()
    private val taskDao = db.taskDao()
    private val labelDao = db.labelDao()
    private val filterDao = db.filterDao()
    private val reminderDao = db.reminderDao()
    private val attachmentDao = db.attachmentDao()
    private val activityEventDao = db.activityEventDao()
    private val notificationDao = db.notificationDao()

    override suspend fun exportBackup(): AppResult<String> = try {
        val snapshot = readSnapshot()
        val json = BackupSerializer.write(snapshot, timeProvider.nowUtcMillis(), BuildConfig.VERSION_NAME)
        AppResult.Success(json)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to export your data", e))
    }

    override suspend fun inspectBackup(rawJson: String): AppResult<BackupManifest> = try {
        val document = BackupSerializer.read(rawJson)
        AppResult.Success(
            BackupManifest(
                formatVersion = document.formatVersion,
                exportedAtUtcMillis = document.exportedAtUtcMillis,
                appVersionName = document.appVersionName,
                counts = document.snapshot.counts(),
            )
        )
    } catch (e: BackupParseException) {
        AppResult.Failure(ValidationError(e.reason, e.message ?: "This backup can't be read.", e))
    } catch (e: Exception) {
        AppResult.Failure(ValidationError("malformed_backup", "This backup can't be read.", e))
    }

    override suspend fun importBackup(rawJson: String, strategy: ImportStrategy): AppResult<ImportSummary> {
        // Re-validate from scratch — never trust a prior inspectBackup() call, and never
        // touch the database until the file has fully checked out.
        val document = try {
            BackupSerializer.read(rawJson)
        } catch (e: BackupParseException) {
            return AppResult.Failure(ValidationError(e.reason, e.message ?: "This backup can't be read.", e))
        } catch (e: Exception) {
            return AppResult.Failure(ValidationError("malformed_backup", "This backup can't be read.", e))
        }

        val precautionaryBackupSaved = writePrecautionaryBackup()

        return try {
            when (strategy) {
                ImportStrategy.REPLACE -> replaceAll(document.snapshot)
            }
            AppResult.Success(ImportSummary(strategy, document.snapshot.counts(), precautionaryBackupSaved))
        } catch (e: Exception) {
            // The transaction below rolled back on failure, so the *original* data is
            // still intact — this is a failed import, not data loss.
            AppResult.Failure(DatabaseError("db_error", "Import failed; your existing data was not changed", e))
        }
    }

    override suspend fun clearLocalData(): AppResult<Unit> = try {
        db.withTransaction { clearAllTables() }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to clear local data", e))
    }

    private suspend fun readSnapshot(): BackupSnapshot = BackupSnapshot(
        preferences = preferenceDao.getAllSnapshot(),
        projects = projectDao.getAllSnapshot(),
        sections = sectionDao.getAllSnapshot(),
        tasks = taskDao.getAllSnapshot(),
        labels = labelDao.getAllSnapshot(),
        taskLabels = labelDao.getAllTaskLabelCrossRefs(),
        filters = filterDao.getAllSnapshot(),
        reminders = reminderDao.getAllSnapshot(),
        attachments = attachmentDao.getAllSnapshot(),
        activityEvents = activityEventDao.getAllSnapshot(),
        notifications = notificationDao.getAllSnapshot(),
    )

    private suspend fun writePrecautionaryBackup(): Boolean {
        val dir = precautionaryBackupDir ?: return false
        return try {
            dir.mkdirs()
            // A single fixed filename: this is a short-lived safety net for the import
            // that's about to happen, not a backup history feature, so each new attempt
            // simply replaces the last one.
            val file = File(dir, "pre-import-backup.json")
            // Reads the database fresh rather than reusing any cached snapshot, since it
            // must reflect the data as it is right before the wipe.
            val json = BackupSerializer.write(readSnapshot(), timeProvider.nowUtcMillis(), BuildConfig.VERSION_NAME)
            file.writeText(json, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun replaceAll(snapshot: BackupSnapshot) {
        db.withTransaction {
            clearAllTables()
            // Parents before children, matching each table's foreign keys.
            preferenceDao.insertAll(snapshot.preferences)
            projectDao.insertAll(snapshot.projects)
            sectionDao.insertAll(snapshot.sections)
            labelDao.insertAll(snapshot.labels)
            filterDao.insertAll(snapshot.filters)
            // tasks.parentTaskId is a *self*-referencing foreign key, and SQLite checks
            // foreign keys immediately per row rather than deferring to the end of the
            // transaction — a subtask inserted before its parent task would fail even
            // though the parent exists later in the same batch. `SELECT * FROM tasks` has
            // no ORDER BY, so nothing guarantees export order already satisfies this.
            taskDao.insertAll(orderedParentsBeforeChildren(snapshot.tasks))
            reminderDao.insertAll(snapshot.reminders)
            attachmentDao.insertAll(snapshot.attachments)
            labelDao.insertAllTaskLabels(snapshot.taskLabels)
            activityEventDao.insertAll(snapshot.activityEvents)
            notificationDao.insertAll(snapshot.notifications)
        }
    }

    /** Orders [tasks] so a parent task always precedes any of its subtasks (see [replaceAll]). */
    private fun orderedParentsBeforeChildren(tasks: List<TaskEntity>): List<TaskEntity> {
        val idsInBatch = tasks.mapTo(HashSet()) { it.id }
        val inserted = HashSet<String>()
        val ordered = ArrayList<TaskEntity>(tasks.size)
        var remaining = tasks
        while (remaining.isNotEmpty()) {
            val (ready, notReady) = remaining.partition { task ->
                val parentId = task.parentTaskId
                parentId == null || parentId !in idsInBatch || parentId in inserted
            }
            if (ready.isEmpty()) {
                // A genuine cycle shouldn't occur in legitimate MyDo data; bail out rather
                // than looping forever, and let the FK check surface the real problem.
                ordered += remaining
                break
            }
            ordered += ready
            ready.forEach { inserted += it.id }
            remaining = notReady
        }
        return ordered
    }

    private suspend fun clearAllTables() {
        // Children before parents.
        labelDao.clearAllTaskLabels()
        reminderDao.clearAll()
        attachmentDao.clearAll()
        activityEventDao.clearAll()
        notificationDao.clearAll()
        taskDao.clearAll()
        sectionDao.clearAll()
        projectDao.clearAll()
        labelDao.clearAll()
        filterDao.clearAll()
        preferenceDao.clearAll()
    }
}
