package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_searches",
    indices = [Index("searchedAtUtcMillis"), Index(value = ["query"], unique = true)]
)
data class RecentSearchEntity(
    @PrimaryKey val id: String,
    val query: String,
    val searchedAtUtcMillis: Long,
)
