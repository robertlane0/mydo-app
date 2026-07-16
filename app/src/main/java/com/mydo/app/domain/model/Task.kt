package com.mydo.app.domain.model

import java.util.UUID

data class Task(
    val id: UUID,
    val projectId: UUID?,
    val sectionId: UUID?,
    val parentTaskId: UUID?,
    val title: String,
    val description: String,
    val completed: Boolean,
    val priority: Priority,
    val dueAtUtcMillis: Long?,
    val recurringRule: String?,
    val sortOrder: Int,
    val createdAtUtcMillis: Long,
    val updatedAtUtcMillis: Long,
    val completedAtUtcMillis: Long?,
    val labels: List<Label>,
    val subtaskCount: Int,
    val completedSubtaskCount: Int,
    // See TaskEntity for why these three are separate from the original field set and
    // what each means; kept at the end (with defaults) for the same positional-argument
    // compatibility reason.
    val recurrenceAnchorUtcMillis: Long? = null,
    val occurrenceNumber: Int = 1,
    val previousOccurrenceTaskId: UUID? = null,
)
