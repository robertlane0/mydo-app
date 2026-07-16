package com.mydo.app.domain.search

import com.mydo.app.domain.model.Priority
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class FilterQueryEvaluatorTest {

    private val zoneId = ZoneId.of("UTC")
    private val fixedNow = Instant.parse("2026-06-15T12:00:00Z").toEpochMilli()
    private val evaluator = FilterQueryEvaluator(zoneId) { fixedNow }

    private fun task(
        title: String = "Buy groceries",
        description: String = "",
        projectName: String? = "Personal",
        priority: Priority = Priority.P3,
        dueAtUtcMillis: Long? = null,
        completed: Boolean = false,
        labelNames: Set<String> = emptySet(),
        recurring: Boolean = false,
        hasAttachment: Boolean = false,
        hasSubtasks: Boolean = false,
    ) = TaskFilterContext(
        taskId = UUID.randomUUID(),
        title = title,
        description = description,
        projectName = projectName,
        sectionName = null,
        labelNames = labelNames,
        priority = priority,
        dueAtUtcMillis = dueAtUtcMillis,
        completed = completed,
        createdAtUtcMillis = fixedNow,
        updatedAtUtcMillis = fixedNow,
        recurring = recurring,
        hasAttachment = hasAttachment,
        hasSubtasks = hasSubtasks,
    )

    @Test
    fun textMatchesTitleCaseInsensitive() {
        val ctx = task(title = "Buy Groceries")
        assertTrue(evaluator.matches(FilterQuery.Text("groceries"), ctx))
        assertFalse(evaluator.matches(FilterQuery.Text("bananas"), ctx))
    }

    @Test
    fun textMatchesDescriptionToo() {
        val ctx = task(title = "Errand", description = "Pick up dry cleaning")
        assertTrue(evaluator.matches(FilterQuery.Text("dry cleaning"), ctx))
    }

    @Test
    fun priorityMatches() {
        val ctx = task(priority = Priority.P1)
        assertTrue(evaluator.matches(FilterQuery.PriorityIs(Priority.P1), ctx))
        assertFalse(evaluator.matches(FilterQuery.PriorityIs(Priority.P2), ctx))
    }

    @Test
    fun labelMatchesIsCaseInsensitive() {
        val ctx = task(labelNames = setOf("urgent"))
        assertTrue(evaluator.matches(FilterQuery.LabelIs("Urgent"), ctx))
    }

    @Test
    fun dueOnMatchesSameLocalDay() {
        val dueAt = Instant.parse("2026-06-20T09:00:00Z").toEpochMilli()
        val ctx = task(dueAtUtcMillis = dueAt)
        assertTrue(evaluator.matches(FilterQuery.DueOn(DateSpec.Absolute(2026, 6, 20)), ctx))
        assertFalse(evaluator.matches(FilterQuery.DueOn(DateSpec.Absolute(2026, 6, 21)), ctx))
    }

    @Test
    fun overdueMatchesPastDueIncompleteTasks() {
        val overdue = task(dueAtUtcMillis = Instant.parse("2026-06-01T09:00:00Z").toEpochMilli(), completed = false)
        val notYetDue = task(dueAtUtcMillis = Instant.parse("2026-06-20T09:00:00Z").toEpochMilli(), completed = false)
        val overdueQuery = FilterQueryParser.parse("due:overdue")
        assertTrue(evaluator.matches(overdueQuery, overdue))
        assertFalse(evaluator.matches(overdueQuery, notYetDue))
    }

    @Test
    fun noDueMatchesTasksWithoutDueDate() {
        val ctx = task(dueAtUtcMillis = null)
        assertTrue(evaluator.matches(FilterQuery.NoDue(true), ctx))
        assertFalse(evaluator.matches(FilterQuery.NoDue(false), ctx))
    }

    @Test
    fun notNegatesInnerTerm() {
        val ctx = task(completed = true)
        assertFalse(evaluator.matches(FilterQuery.Not(FilterQuery.CompletedIs(true)), ctx))
        assertTrue(evaluator.matches(FilterQuery.Not(FilterQuery.CompletedIs(false)), ctx))
    }

    @Test
    fun andRequiresAllTermsToMatch() {
        val ctx = task(priority = Priority.P1, labelNames = setOf("urgent"))
        val query = FilterQuery.And(listOf(FilterQuery.PriorityIs(Priority.P1), FilterQuery.LabelIs("urgent")))
        assertTrue(evaluator.matches(query, ctx))
        val queryMismatch = FilterQuery.And(listOf(FilterQuery.PriorityIs(Priority.P1), FilterQuery.LabelIs("home")))
        assertFalse(evaluator.matches(queryMismatch, ctx))
    }

    @Test
    fun inInboxMatchesTasksWithoutProject() {
        val inboxTask = task(projectName = null)
        val projectTask = task(projectName = "Work")
        assertTrue(evaluator.matches(FilterQuery.InInbox(true), inboxTask))
        assertFalse(evaluator.matches(FilterQuery.InInbox(true), projectTask))
    }

    @Test
    fun hasAttachmentAndHasSubtasksMatchFlags() {
        val ctx = task(hasAttachment = true, hasSubtasks = false)
        assertTrue(evaluator.matches(FilterQuery.HasAttachment(true), ctx))
        assertFalse(evaluator.matches(FilterQuery.HasSubtasks(true), ctx))
    }
}
