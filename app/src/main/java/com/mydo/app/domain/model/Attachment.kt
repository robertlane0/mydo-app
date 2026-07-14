package com.mydo.app.domain.model

import java.util.UUID

data class Attachment(
    val id: UUID,
    val taskId: UUID,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String,
)
