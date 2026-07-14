package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index

@Entity(
    tableName = "task_labels",
    primaryKeys = ["taskId", "labelId"],
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = CASCADE),
        ForeignKey(entity = LabelEntity::class, parentColumns = ["id"], childColumns = ["labelId"], onDelete = CASCADE)
    ],
    indices = [Index("taskId"), Index("labelId")]
)
data class TaskLabelCrossRef(
    val taskId: String,
    val labelId: String,
)
