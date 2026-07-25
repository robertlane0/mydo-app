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
    /** Null for tasks with no project, or for tasks in a project but not in any section. */
    val sectionId: UUID? = null,
)
