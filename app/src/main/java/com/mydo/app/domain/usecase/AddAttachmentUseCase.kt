package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.domain.model.Attachment
import com.mydo.app.domain.repository.AttachmentRepository
import java.util.UUID

class AddAttachmentUseCase(
    private val attachmentRepository: AttachmentRepository,
    private val attachmentGateway: com.mydo.app.platform.AttachmentGateway,
) {
    suspend operator fun invoke(
        taskId: java.util.UUID,
        uri: android.net.Uri,
        fileName: String,
        mimeType: String,
        size: Long,
    ): AppResult<Attachment> {
        return try {
            val attachment = Attachment(
                id = java.util.UUID.randomUUID(),
                taskId = taskId,
                filename = fileName,
                mimeType = mimeType,
                sizeBytes = size,
                localUri = uri.toString(),
            )
            
            val result = attachmentRepository.create(attachment)
            if (result is com.mydo.app.core.errors.AppResult.Failure) {
                result
            } else {
                com.mydo.app.core.errors.AppResult.Success(attachment)
            }
        } catch (e: Exception) {
            com.mydo.app.core.errors.AppResult.Failure(
                com.mydo.app.core.errors.DatabaseError("db_error", "Failed to add attachment", e)
            )
        }
    }
}