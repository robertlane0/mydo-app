package com.mydo.app.domain.search

import com.mydo.app.domain.model.Priority
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Everything the query evaluator needs about one task, pre-joined so a whole-list
 * evaluation only requires one pass with no further DB access (see [FilterQueryEvaluator]).
 */
data class TaskFilterContext(
    val taskId: UUID,
    val title: String,
    val description: String,
    val projectName: String?,
    val sectionName: String?,
    val labelNames: Set<String>,
    val priority: Priority,
    val dueAtUtcMillis: Long?,
    val completed: Boolean,
    val createdAtUtcMillis: Long,
    val updatedAtUtcMillis: Long,
    val recurring: Boolean,
    val hasAttachment: Boolean,
    val hasSubtasks: Boolean,
)

/**
 * Evaluates a [FilterQuery] against a [TaskFilterContext]. Relative dates (`today`,
 * `week`, ...) are resolved against [zoneId] per specs16-recurring-tasks.md's timezone
 * rule ("due dates stored in UTC; ... calculated in user's local timezone"), which this
 * search/filter language follows for consistency.
 */
class FilterQueryEvaluator(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowUtcMillis: () -> Long = System::currentTimeMillis,
) {
    fun matches(query: FilterQuery, ctx: TaskFilterContext): Boolean = eval(query, ctx)

    private fun eval(query: FilterQuery, ctx: TaskFilterContext): Boolean = when (query) {
        FilterQuery.MatchAll -> true
        is FilterQuery.And -> query.terms.all { eval(it, ctx) }
        is FilterQuery.Or -> query.terms.any { eval(it, ctx) }
        is FilterQuery.Not -> !eval(query.term, ctx)
        is FilterQuery.Text -> {
            val needle = query.value.lowercase()
            ctx.title.lowercase().contains(needle) || ctx.description.lowercase().contains(needle)
        }
        is FilterQuery.ProjectIs -> ctx.projectName?.equals(query.name, ignoreCase = true) == true
        is FilterQuery.SectionIs -> ctx.sectionName?.equals(query.name, ignoreCase = true) == true
        is FilterQuery.LabelIs -> ctx.labelNames.contains(query.name.lowercase())
        is FilterQuery.PriorityIs -> ctx.priority == query.priority
        is FilterQuery.DueOn -> ctx.dueAtUtcMillis?.let { isSameLocalDay(it, resolveDate(query.date)) } == true
        is FilterQuery.DueBefore -> ctx.dueAtUtcMillis?.let { it < startOfDayUtcMillis(resolveDate(query.date)) } == true
        is FilterQuery.DueAfter -> ctx.dueAtUtcMillis?.let { it > endOfDayUtcMillis(resolveDate(query.date)) } == true
        is FilterQuery.DueBetween -> ctx.dueAtUtcMillis?.let {
            it in startOfDayUtcMillis(resolveDate(query.start))..endOfDayUtcMillis(resolveDate(query.endInclusive))
        } == true
        is FilterQuery.NoDue -> (ctx.dueAtUtcMillis == null) == query.value
        is FilterQuery.CreatedOn -> isSameLocalDay(ctx.createdAtUtcMillis, resolveDate(query.date))
        is FilterQuery.CreatedBetween -> ctx.createdAtUtcMillis in
            startOfDayUtcMillis(resolveDate(query.start))..endOfDayUtcMillis(resolveDate(query.endInclusive))
        is FilterQuery.CompletedIs -> ctx.completed == query.value
        is FilterQuery.RecurringIs -> ctx.recurring == query.value
        is FilterQuery.HasAttachment -> ctx.hasAttachment == query.value
        is FilterQuery.HasSubtasks -> ctx.hasSubtasks == query.value
        is FilterQuery.HasProject -> (ctx.projectName != null) == query.value
        is FilterQuery.InInbox -> (ctx.projectName == null) == query.value
    }

    private fun resolveDate(spec: DateSpec): LocalDate {
        val today = Instant.ofEpochMilli(nowUtcMillis()).atZone(zoneId).toLocalDate()
        return when (spec) {
            DateSpec.Today -> today
            DateSpec.Tomorrow -> today.plusDays(1)
            DateSpec.Yesterday -> today.minusDays(1)
            DateSpec.StartOfWeek -> today.with(DayOfWeek.MONDAY)
            DateSpec.EndOfWeek -> today.with(DayOfWeek.MONDAY).plusDays(6)
            DateSpec.StartOfMonth -> today.withDayOfMonth(1)
            DateSpec.EndOfMonth -> today.withDayOfMonth(today.lengthOfMonth())
            is DateSpec.Absolute -> LocalDate.of(spec.year, spec.month, spec.day)
        }
    }

    private fun startOfDayUtcMillis(date: LocalDate): Long = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun endOfDayUtcMillis(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

    private fun isSameLocalDay(utcMillis: Long, date: LocalDate): Boolean =
        Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDate() == date
}
