package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = CASCADE)],
    indices = [Index("taskId"), Index("triggerAtUtcMillis")]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val triggerAtUtcMillis: Long,
    val type: String,
    val enabled: Boolean,
)
