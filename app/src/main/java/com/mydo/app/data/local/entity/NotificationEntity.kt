package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [Index("read"), Index("createdAtUtcMillis")]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val taskId: String?,
    val title: String,
    val read: Boolean,
    val createdAtUtcMillis: Long,
)
