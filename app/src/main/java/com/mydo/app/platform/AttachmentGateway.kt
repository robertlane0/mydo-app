package com.mydo.app.platform

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns

/** Metadata read from a picked SAF document (specs15-attachments.md, "Metadata Extraction"). */
data class PickedDocument(
    val uri: Uri,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
)

/**
 * Bridges Android's Storage Access Framework for task attachments. The system picker
 * itself is launched from Compose with
 * `rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments())`
 * (a picker launch can only happen from an Activity-scoped launcher); this port handles
 * everything after a `content://` URI comes back — reading its metadata and taking the
 * persistable permission grant that lets MyDo reopen it after a reboot, per specs15's
 * "MyDo never uploads, syncs, or moves files" and "Persisted URI Permission" sections.
 */
interface AttachmentGateway {
    /** Reads filename/MIME type/size for a freshly picked SAF URI, or null if unreadable. */
    fun inspect(uri: Uri): PickedDocument?

    /** Takes a persistable read grant (survives reboots) for [uri]. */
    fun persistReadPermission(uri: Uri)

    /** Releases a previously persisted grant; called when an attachment is removed. */
    fun releaseReadPermission(uri: Uri)

    /** True if [uri] can currently be opened (grant still valid, document still exists). */
    fun canOpen(uri: Uri): Boolean

    /** Builds the ACTION_VIEW intent used to hand the attachment off to a system app. */
    fun buildOpenIntent(uri: Uri, mimeType: String): Intent
}

class AndroidAttachmentGateway(private val context: Context) : AttachmentGateway {
    private val resolver: ContentResolver get() = context.contentResolver

    override fun inspect(uri: Uri): PickedDocument? {
        val cursor: Cursor = resolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return null
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            val filename = if (nameIndex >= 0) it.getString(nameIndex) else uri.lastPathSegment ?: "Attachment"
            val sizeBytes = if (sizeIndex >= 0 && !it.isNull(sizeIndex)) it.getLong(sizeIndex) else 0L
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            PickedDocument(uri = uri, filename = filename, mimeType = mimeType, sizeBytes = sizeBytes)
        }
    }

    override fun persistReadPermission(uri: Uri) {
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some providers (e.g. camera roll shortcuts) don't support persistable grants;
            // the attachment still works for this session, and canOpen() will reflect
            // whether it survives a restart.
        }
    }

    override fun releaseReadPermission(uri: Uri) {
        try {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Permission was already gone (file moved/deleted) — nothing to release.
        }
    }

    override fun canOpen(uri: Uri): Boolean {
        val hasGrant = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        if (!hasGrant) return false
        return try {
            resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        } catch (_: Exception) {
            false
        }
    }

    override fun buildOpenIntent(uri: Uri, mimeType: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}

/**
 * Describes the SAF picker configuration (specs15-attachments.md, "Document Picker
 * Configuration"). The picker launch itself lives in Compose UI (see [AttachmentGateway]
 * doc); this just centralizes the accepted-type policy so it isn't duplicated per screen.
 */
interface DocumentPicker {
    val acceptedMimeTypes: Array<String>
}

class AndroidDocumentPicker : DocumentPicker {
    override val acceptedMimeTypes: Array<String> = arrayOf("*/*")
}
