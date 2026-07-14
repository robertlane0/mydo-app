package com.mydo.app.domain.model

import java.util.UUID

data class Reminder(
    val id: UUID,
    val taskId: UUID,
    val triggerAtUtcMillis: Long,
    val type: ReminderType,
    val enabled: Boolean,
)
