package com.mydo.app.domain.model

import java.util.UUID

/**
 * A single ranked entry in search results. [rank] is lower-is-better and follows the
 * priority order from specs08-search.md ("Result Ranking"): exact match, prefix match,
 * partial match, then recency — [SearchRanker] assigns it; screens should not re-sort.
 */
sealed interface SearchResult {
    val rank: Int

    data class TaskResult(
        val task: TaskSummary,
        override val rank: Int,
    ) : SearchResult

    data class ProjectResult(
        val project: Project,
        val taskCount: Int,
        override val rank: Int,
    ) : SearchResult

    data class LabelResult(
        val label: Label,
        val taskCount: Int,
        override val rank: Int,
    ) : SearchResult

    data class FilterResult(
        val filter: Filter,
        override val rank: Int,
    ) : SearchResult

    data class SectionResult(
        val section: Section,
        val projectName: String,
        override val rank: Int,
    ) : SearchResult
}

data class SearchResults(
    val query: String,
    val tasks: List<SearchResult.TaskResult>,
    val projects: List<SearchResult.ProjectResult>,
    val labels: List<SearchResult.LabelResult>,
    val filters: List<SearchResult.FilterResult>,
    val sections: List<SearchResult.SectionResult>,
) {
    val isEmpty: Boolean
        get() = tasks.isEmpty() && projects.isEmpty() && labels.isEmpty() && filters.isEmpty() && sections.isEmpty()

    companion object {
        fun empty(query: String) = SearchResults(query, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}
