package com.mydo.app.domain.usecase

import android.net.Uri
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.ValidationError
import com.mydo.app.domain.model.Attachment
import com.mydo.app.domain.repository.AttachmentRepository
import com.mydo.app.platform.AttachmentGateway
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Observes a task's attachments (specs15-attachments.md). */
class ObserveAttachmentsUseCase(private val attachmentRepository: AttachmentRepository) {
    operator fun invoke(taskId: UUID): Flow<AppResult<List<Attachment>>> = attachmentRepository.observeByTask(taskId)
}

/**
 * Persists one or more SAF URIs (already returned by the system document picker in
 * Compose) as attachments on a task: reads metadata, takes the durable read grant, and
 * writes an [Attachment] row per URI. Best-effort per URI — one bad file doesn't block
 * the rest of a multi-select pick.
 */
class AddAttachmentsUseCase(
    private val attachmentRepository: AttachmentRepository,
    private val attachmentGateway: AttachmentGateway,
) {
    suspend operator fun invoke(taskId: UUID, uris: List<Uri>): AppResult<Int> {
        if (uris.isEmpty()) return AppResult.Success(0)
        var added = 0
        for (uri in uris) {
            val metadata = attachmentGateway.inspect(uri) ?: continue
            attachmentGateway.persistReadPermission(uri)
            val result = attachmentRepository.create(
                Attachment(
                    id = UUID.randomUUID(),
                    taskId = taskId,
                    filename = metadata.filename,
                    mimeType = metadata.mimeType,
                    sizeBytes = metadata.sizeBytes,
                    localUri = uri.toString(),
                )
            )
            if (result is AppResult.Success) added++
        }
        return if (added == 0 && uris.isNotEmpty()) {
            AppResult.Failure(ValidationError("attachment_failed", "Couldn't attach the selected file(s)"))
        } else {
            AppResult.Success(added)
        }
    }
}

class RemoveAttachmentUseCase(
    private val attachmentRepository: AttachmentRepository,
    private val attachmentGateway: AttachmentGateway,
) {
    suspend operator fun invoke(attachment: Attachment): AppResult<Unit> {
        val result = attachmentRepository.delete(attachment.id)
        if (result is AppResult.Success) {
            attachmentGateway.releaseReadPermission(Uri.parse(attachment.localUri))
        }
        return result
    }
}
