package com.mydo.app.domain.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.BackupManifest
import com.mydo.app.domain.model.ImportStrategy
import com.mydo.app.domain.model.ImportSummary

/**
 * Manual data export/import (specs10-settings.md, "Data"; specs11-data-model.md, "Data
 * Portability"). Every entity specified in the data model is included, plus preferences.
 * MyDo never uploads or syncs this data anywhere on its own — export and import both happen
 * entirely through a user-chosen [com.mydo.app.platform.ShareGateway] destination/source.
 */
interface BackupRepository {
    /** Serializes the entire local database into one versioned JSON document with integrity
     *  metadata (a checksum plus per-entity counts). */
    suspend fun exportBackup(): AppResult<String>

    /** Validates [rawJson] — format version, structural shape, and checksum — and returns
     *  its manifest *without* touching the local database. Always call this before
     *  [importBackup] so the user can see what they're about to import. */
    suspend fun inspectBackup(rawJson: String): AppResult<BackupManifest>

    /**
     * Imports [rawJson] using [strategy]. Re-validates from scratch (never trusts a prior
     * [inspectBackup] call) and leaves the local database completely unchanged if validation
     * fails. For [ImportStrategy.REPLACE], the entire swap runs in one database transaction,
     * so a failure partway through leaves the *original* data intact rather than a mix of
     * old and new rows.
     */
    suspend fun importBackup(rawJson: String, strategy: ImportStrategy): AppResult<ImportSummary>

    /** Wipes every row from every table covered by backups (specs10-settings.md, "Clear
     *  local data"). Does not touch attachment files on disk — those are owned by the OS
     *  document providers they came from, not by MyDo. */
    suspend fun clearLocalData(): AppResult<Unit>
}
