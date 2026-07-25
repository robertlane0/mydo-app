package com.mydo.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Project
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.usecase.CreateTaskUseCase
import com.mydo.app.domain.usecase.DeleteTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** One-shot outcomes the Task Composer sheet should react to once and forget. */
sealed interface ComposerEvent {
    /** [projectName] is null when the task landed in the Inbox. */
    data class Created(val task: Task, val projectName: String?) : ComposerEvent
    data class Failed(val message: String) : ComposerEvent
}

/**
 * Backs the global Quick Add bottom sheet (specs12-user-flows.md, "Core Flow: Quick Add
 * Task"). Presets let a screen inherit context (e.g. tapping + under a specific Upcoming
 * date, or adding from within a project) without the sheet needing to know who opened it.
 */
class TaskComposerViewModel(
    private val createTaskUseCase: CreateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : ViewModel() {

    var presetDueAtUtcMillis: Long? = null
    var presetProjectId: UUID? = null
    var presetSectionId: UUID? = null

    val events = MutableSharedFlow<ComposerEvent>(extraBufferCapacity = 1)

    /**
     * Creates a task from the composer's raw input. [projectId]/[dueAtUtcMillis]/[priority]
     * come from [parseQuickAdd] when it recognized inline tokens; otherwise the preset
     * context (if any) is used. [projectName] is passed straight through for the
     * confirmation message so callers don't need to re-look it up.
     */
    fun createTask(
        title: String,
        projectId: UUID? = null,
        projectName: String? = null,
        dueAtUtcMillis: Long? = null,
        priority: Priority = Priority.P4,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val resolvedProjectId = projectId ?: presetProjectId
        val resolvedSectionId = if (projectId == null) presetSectionId else null
        val resolvedDueAt = dueAtUtcMillis ?: presetDueAtUtcMillis
        viewModelScope.launch {
            when (
                val result = createTaskUseCase(
                    cleanTitle,
                    projectId = resolvedProjectId,
                    sectionId = resolvedSectionId,
                    dueAtUtcMillis = resolvedDueAt,
                    priority = priority,
                )
            ) {
                is AppResult.Success -> {
                    presetDueAtUtcMillis = null
                    presetProjectId = null
                    presetSectionId = null
                    events.tryEmit(ComposerEvent.Created(result.value, projectName))
                }
                is AppResult.Failure -> events.tryEmit(ComposerEvent.Failed(result.error.userMessage))
            }
        }
    }

    /** Reverses a just-created task within the Undo window (specs12, "Resolution": Undo). */
    fun undoCreate(taskId: UUID) {
        viewModelScope.launch { deleteTaskUseCase(taskId) }
    }

    class Factory(
        private val createTaskUseCase: CreateTaskUseCase,
        private val deleteTaskUseCase: DeleteTaskUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskComposerViewModel::class.java)) {
                return TaskComposerViewModel(createTaskUseCase, deleteTaskUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/** Result of scanning quick-add raw text for inline context tokens. */
data class ParsedQuickAdd(
    val title: String,
    val projectId: UUID? = null,
    val projectName: String? = null,
    val dueAtUtcMillis: Long? = null,
    val dueLabel: String? = null,
    val priority: Priority? = null,
)

private val WEEKDAY_NAMES = mapOf(
    "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
    "thursday" to DayOfWeek.THURSDAY, "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY,
    "sunday" to DayOfWeek.SUNDAY,
)

/**
 * Lightweight, local, non-NLP token scan for the Quick Add field (specs12-user-flows.md,
 * "Parsing (NLP)": "#Work" -> project, "tomorrow" -> due date). This intentionally covers
 * only unambiguous, easily-reversible tokens (project tag, priority flag, a handful of
 * relative-date words) rather than full natural-language date/time parsing, so a token is
 * only ever consumed when it's a confident, near-certain match.
 */
fun parseQuickAdd(raw: String, projects: List<Project>, zoneId: ZoneId = ZoneId.systemDefault()): ParsedQuickAdd {
    var projectId: UUID? = null
    var projectName: String? = null
    var dueAtUtcMillis: Long? = null
    var dueLabel: String? = null
    var priority: Priority? = null

    val today = LocalDate.now(zoneId)
    fun atNoon(date: LocalDate): Long = date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

    val tokens = raw.split(" ")
    val kept = mutableListOf<String>()
    for (token in tokens) {
        val bare = token.trimEnd(',', '.', ';', '!', '?')
        val lower = bare.lowercase()
        when {
            projectId == null && bare.startsWith("#") && bare.length > 1 -> {
                val name = bare.substring(1)
                val match = projects.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (match != null) {
                    projectId = match.id
                    projectName = match.name
                    continue
                }
            }
            priority == null && lower in setOf("p1", "p2", "p3", "p4") -> {
                priority = when (lower) {
                    "p1" -> Priority.P1
                    "p2" -> Priority.P2
                    "p3" -> Priority.P3
                    else -> Priority.P4
                }
                continue
            }
            dueAtUtcMillis == null && lower == "today" -> {
                dueAtUtcMillis = atNoon(today)
                dueLabel = "Today"
                continue
            }
            dueAtUtcMillis == null && lower == "tomorrow" -> {
                dueAtUtcMillis = atNoon(today.plusDays(1))
                dueLabel = "Tomorrow"
                continue
            }
            dueAtUtcMillis == null && WEEKDAY_NAMES.containsKey(lower) -> {
                val target = WEEKDAY_NAMES.getValue(lower)
                var candidate = today.plusDays(1)
                while (candidate.dayOfWeek != target) candidate = candidate.plusDays(1)
                dueAtUtcMillis = atNoon(candidate)
                dueLabel = bare.replaceFirstChar { it.uppercase() }
                continue
            }
        }
        kept.add(token)
    }

    return ParsedQuickAdd(
        title = kept.joinToString(" ").trim(),
        projectId = projectId,
        projectName = projectName,
        dueAtUtcMillis = dueAtUtcMillis,
        dueLabel = dueLabel,
        priority = priority,
    )
}
