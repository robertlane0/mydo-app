package com.mydo.app.domain.model

import java.util.UUID

data class Section(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val sortOrder: Int,
)
