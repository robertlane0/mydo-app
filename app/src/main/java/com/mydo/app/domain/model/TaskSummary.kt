package com.mydo.app.domain.model

import java.util.UUID

data class TaskSummary(
    val id: UUID,
    val title: String,
    val completed: Boolean,
    val priority: Priority,
    val dueAtUtcMillis: Long?,
    val projectPath: String?,
    val recurring: Boolean = false,
    val sortOrder: Int = 0,
)
