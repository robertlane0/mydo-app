package com.mydo.app.domain.model

import java.util.UUID

data class RecentSearch(
    val id: UUID,
    val query: String,
    val searchedAtUtcMillis: Long,
)
