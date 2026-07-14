package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = CASCADE)],
    indices = [Index("taskId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String,
)
