package com.mydo.app.domain.search

/**
 * Ranks how well a piece of text matches a raw query string, per specs08-search.md
 * ("Result Ranking"): exact match first, then prefix, then any substring, then a
 * secondary-field match (e.g. a task's description rather than its title). Lower is
 * better; ties are broken by recency by the caller.
 */
object SearchRanker {
    const val RANK_EXACT = 0
    const val RANK_PREFIX = 1
    const val RANK_PARTIAL = 2
    const val RANK_SECONDARY_FIELD = 3

    /**
     * Ranks [primary] (e.g. a task title) against [query], falling back to
     * [RANK_SECONDARY_FIELD] if only [secondary] (e.g. a description) contains it, and to
     * [RANK_PARTIAL] if neither raw string contains the query — which happens when a task
     * matched via `field:value` filter syntax rather than free text.
     */
    fun rank(query: String, primary: String, secondary: String? = null): Int {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return RANK_PARTIAL
        val p = primary.lowercase()
        return when {
            p == q -> RANK_EXACT
            p.startsWith(q) -> RANK_PREFIX
            p.contains(q) -> RANK_PARTIAL
            secondary != null && secondary.lowercase().contains(q) -> RANK_SECONDARY_FIELD
            else -> RANK_PARTIAL
        }
    }
}
