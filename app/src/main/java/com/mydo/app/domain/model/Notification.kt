package com.mydo.app.domain.model

import java.util.UUID

data class Notification(
    val id: UUID,
    val type: NotificationType,
    val taskId: UUID?,
    val title: String,
    val read: Boolean,
    val createdAtUtcMillis: Long,
)
