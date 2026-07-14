package com.mydo.app.domain.model

import java.util.UUID

data class Project(
    val id: UUID,
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
