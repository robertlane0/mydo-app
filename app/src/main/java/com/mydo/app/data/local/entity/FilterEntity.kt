package com.mydo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filters")
data class FilterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val query: String,
    val favorite: Boolean,
)
