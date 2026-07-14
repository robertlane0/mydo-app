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
)
