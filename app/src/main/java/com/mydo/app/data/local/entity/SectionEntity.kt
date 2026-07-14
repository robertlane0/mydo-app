package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = CASCADE)],
    indices = [Index("projectId")]
)
data class SectionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val sortOrder: Int,
)
