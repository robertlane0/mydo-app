package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.SearchResult
import com.mydo.app.domain.model.SearchResults
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.FilterRepository
import com.mydo.app.domain.repository.LabelRepository
import com.mydo.app.domain.repository.ProjectRepository
import com.mydo.app.domain.repository.SectionRepository
import com.mydo.app.domain.repository.TaskRepository
import com.mydo.app.domain.search.FilterQuery
import com.mydo.app.domain.search.FilterQueryEvaluator
import com.mydo.app.domain.search.FilterQueryParseException
import com.mydo.app.domain.search.FilterQueryParser
import com.mydo.app.domain.search.SearchRanker
import com.mydo.app.domain.search.TaskFilterContext
import java.time.ZoneId

private const val MAX_TASK_RESULTS = 50
private const val MAX_SECONDARY_RESULTS = 5

/**
 * Runs one search across tasks, projects, sections, labels, and saved filters
 * (specs08-search.md, "Search Scope"). Accepts either plain free text or the same
 * `field:value` query syntax as Saved Filters ("Advanced Filter Syntax in Search") — a
 * query that doesn't parse as a filter expression falls back to a plain-text match so a
 * stray colon never zeroes out results.
 */
class SearchUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val sectionRepository: SectionRepository,
    private val labelRepository: LabelRepository,
    private val filterRepository: FilterRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(rawQuery: String): AppResult<SearchResults> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return AppResult.Success(SearchResults.empty(query))

        val parsedQuery = try {
            FilterQueryParser.parse(query)
        } catch (e: FilterQueryParseException) {
            FilterQuery.Text(query)
        }

        val contexts = when (val result = taskRepository.getFilterContexts()) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val evaluator = FilterQueryEvaluator(zoneId)
        val taskResults = contexts.asSequence()
            .filter { evaluator.matches(parsedQuery, it) }
            .map { ctx -> ctx to SearchRanker.rank(query, ctx.title, ctx.description) }
            .sortedWith(compareBy({ it.second }, { -it.first.updatedAtUtcMillis }))
            .take(MAX_TASK_RESULTS)
            .map { (ctx, rank) -> SearchResult.TaskResult(ctx.toSummary(), rank) }
            .toList()

        val projectResults = (projectRepository.search(query) as? AppResult.Success)?.value.orEmpty()
            .take(MAX_SECONDARY_RESULTS)
            .map { project ->
                val count = (projectRepository.countActiveTasks(project.id) as? AppResult.Success)?.value ?: 0
                SearchResult.ProjectResult(project, count, SearchRanker.rank(query, project.name))
            }
            .sortedBy { it.rank }

        val sectionResults = (sectionRepository.search(query) as? AppResult.Success)?.value.orEmpty()
            .take(MAX_SECONDARY_RESULTS)
            .map { section ->
                val projectName = (projectRepository.getById(section.projectId) as? AppResult.Success)?.value?.name.orEmpty()
                SearchResult.SectionResult(section, projectName, SearchRanker.rank(query, section.name))
            }
            .sortedBy { it.rank }

        val labelResults = (labelRepository.search(query) as? AppResult.Success)?.value.orEmpty()
            .take(MAX_SECONDARY_RESULTS)
            .map { label ->
                val count = (labelRepository.countTasks(label.id) as? AppResult.Success)?.value ?: 0
                SearchResult.LabelResult(label, count, SearchRanker.rank(query, label.name))
            }
            .sortedBy { it.rank }

        val filterResults = (filterRepository.search(query) as? AppResult.Success)?.value.orEmpty()
            .take(MAX_SECONDARY_RESULTS)
            .map { filter -> SearchResult.FilterResult(filter, SearchRanker.rank(query, filter.name)) }
            .sortedBy { it.rank }

        return AppResult.Success(SearchResults(query, taskResults, projectResults, labelResults, filterResults, sectionResults))
    }

    private fun TaskFilterContext.toSummary() = TaskSummary(
        id = taskId,
        title = title,
        completed = completed,
        priority = priority,
        dueAtUtcMillis = dueAtUtcMillis,
        projectPath = projectName,
        recurring = recurring,
    )
}
