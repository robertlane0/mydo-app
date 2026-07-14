package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_events",
    indices = [Index("objectId"), Index("timestampUtcMillis")]
)
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val objectId: String,
    val objectType: String,
    val eventType: String,
    val timestampUtcMillis: Long,
)
