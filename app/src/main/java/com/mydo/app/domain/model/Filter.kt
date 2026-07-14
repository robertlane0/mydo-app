package com.mydo.app.domain.model

import java.util.UUID

data class Filter(
    val id: UUID,
    val name: String,
    val query: String,
    val favorite: Boolean,
)
