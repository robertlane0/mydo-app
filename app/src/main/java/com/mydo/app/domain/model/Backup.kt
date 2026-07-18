package com.mydo.app.domain.model

/**
 * Per-entity row counts for a backup. Doubles as the "what's in this file" summary shown to
 * the user before they commit to an import (specs11-data-model.md, "Data Portability":
 * exports must be "a complete, versioned snapshot ... with integrity metadata").
 */
data class BackupCounts(
    val preferences: Int,
    val projects: Int,
    val sections: Int,
    val tasks: Int,
    val labels: Int,
    val filters: Int,
    val reminders: Int,
    val attachments: Int,
    val activityEvents: Int,
    val notifications: Int,
)

/** Parsed, verified header of a backup file — safe to show to the user before importing. */
data class BackupManifest(
    val formatVersion: Int,
    val exportedAtUtcMillis: Long,
    val appVersionName: String,
    val counts: BackupCounts,
)

enum class ImportStrategy {
    /** Wipes the local database and replaces it with the backup's contents, atomically. */
    REPLACE,
}

/** Result of a successful import, for a confirmation summary in the UI. */
data class ImportSummary(
    val strategy: ImportStrategy,
    val counts: BackupCounts,
    val precautionaryBackupSaved: Boolean,
)
