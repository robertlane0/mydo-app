package com.mydo.app.domain.model

import java.util.UUID

data class ActivityEvent(
    val id: UUID,
    val objectId: UUID,
    val objectType: ObjectType,
    val eventType: EventType,
    val timestampUtcMillis: Long,
)
