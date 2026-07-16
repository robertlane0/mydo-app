package com.mydo.app.domain.model

/**
 * How a task list is ordered. [MANUAL] is the only mode where drag reordering is enabled
 * and `Task.sortOrder` is authoritative; the other modes derive an order on the fly and
 * leave `sortOrder` untouched so the manual order is preserved when the user switches back
 * (specs18-drag-reorder.md, "Sorting vs Manual Order").
 */
enum class SortMode {
    MANUAL,
    DUE_DATE,
    PRIORITY,
    NAME,
    CREATED,
}
