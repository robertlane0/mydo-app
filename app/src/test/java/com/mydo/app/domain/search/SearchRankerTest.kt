package com.mydo.app.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankerTest {

    @Test
    fun exactMatchRanksBestAndPrefixBeatsPartial() {
        val exact = SearchRanker.rank("milk", "Milk")
        val prefix = SearchRanker.rank("milk", "Milkshake recipe")
        val partial = SearchRanker.rank("milk", "Buy oat milk")
        assertEquals(SearchRanker.RANK_EXACT, exact)
        assertEquals(SearchRanker.RANK_PREFIX, prefix)
        assertEquals(SearchRanker.RANK_PARTIAL, partial)
        assertTrue(exact < prefix)
        assertTrue(prefix < partial)
    }

    @Test
    fun descriptionOnlyMatchRanksBelowTitleMatches() {
        val rank = SearchRanker.rank("recipe", primary = "Groceries", secondary = "Find a pasta recipe")
        assertEquals(SearchRanker.RANK_SECONDARY_FIELD, rank)
    }

    @Test
    fun noRawMatchStillRanksSomehow() {
        // Matched via field:value syntax rather than free text — falls back to partial tier.
        val rank = SearchRanker.rank("project:Work", primary = "Buy milk")
        assertEquals(SearchRanker.RANK_PARTIAL, rank)
    }
}
