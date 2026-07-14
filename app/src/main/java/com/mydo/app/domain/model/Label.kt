package com.mydo.app.domain.model

import java.util.UUID

data class Label(
    val id: UUID,
    val name: String,
    val color: String,
    val createdAtUtcMillis: Long,
)
