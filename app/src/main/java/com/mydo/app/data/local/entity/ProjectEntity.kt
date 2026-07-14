package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects", indices = [Index("archived"), Index("favorite")])
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val color: String,
    val icon: String,
    val archived: Boolean,
    val favorite: Boolean,
    val sortOrder: Int,
    val createdAtUtcMillis: Long,
    val updatedAtUtcMillis: Long,
)
