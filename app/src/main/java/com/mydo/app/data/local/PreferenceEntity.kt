package com.mydo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAtUtcMillis: Long,
)
