package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = SET_NULL),
        ForeignKey(entity = SectionEntity::class, parentColumns = ["id"], childColumns = ["sectionId"], onDelete = SET_NULL),
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["parentTaskId"], onDelete = CASCADE)
    ],
    indices = [
        Index("projectId"),
        Index("sectionId"),
        Index("parentTaskId"),
        Index("completed"),
        Index("dueAtUtcMillis"),
        Index("priority"),
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val sectionId: String?,
    val parentTaskId: String?,
    val title: String,
    val description: String,
    val completed: Boolean,
    val priority: String,
    val dueAtUtcMillis: Long?,
    val recurringRule: String?,
    val sortOrder: Int,
    val createdAtUtcMillis: Long,
    val updatedAtUtcMillis: Long,
    val completedAtUtcMillis: Long?,
    // Basis for "next occurrence after X" calculations. Ad-hoc rescheduling of the
    // *current* occurrence (drag/date-picker) updates dueAtUtcMillis but intentionally
    // leaves this untouched, matching specs16-recurring-tasks.md: "Weekly Monday task
    // moved to Wednesday -> next still Monday". Null means "same as dueAtUtcMillis".
    // Placed at the end with a default so existing positional TaskEntity(...) call
    // sites (tests, etc.) predating step 4 keep compiling.
    val recurrenceAnchorUtcMillis: Long? = null,
    // 1-based position of this occurrence within its recurring series; compared against
    // RRULE's COUNT=N to know when the series is exhausted.
    val occurrenceNumber: Int = 1,
    // Links a generated occurrence back to the task it was generated from, for history
    // (specs16-recurring-tasks.md: "parentTaskId = completed task's ID (for history)").
    // Deliberately a separate column from `parentTaskId`, which is reserved for the
    // subtask hierarchy (CASCADE delete + countSubtasks()) — reusing it for recurrence
    // would make each generated occurrence look like a subtask of its predecessor and
    // would cascade-delete it when the predecessor is removed.
    val previousOccurrenceTaskId: String? = null,
)
