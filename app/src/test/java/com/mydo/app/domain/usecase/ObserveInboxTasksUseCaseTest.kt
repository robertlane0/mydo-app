package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class ObserveInboxTasksUseCaseTest {
    private val fakeRepository = object : TaskRepository {
        override fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeTodayTasks(endOfDayUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeProjectTasks(projectId: UUID): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeSectionTasks(sectionId: UUID): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeById(id: UUID): Flow<AppResult<Task?>> = flowOf(AppResult.Success(null))
        override suspend fun getById(id: UUID): AppResult<Task?> = AppResult.Success(null)
        override suspend fun create(task: Task): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun update(task: Task): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun delete(id: UUID): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCompletion(id: UUID, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun moveToProject(id: UUID, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearSection(sectionId: UUID, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun search(query: String): AppResult<List<TaskSummary>> = AppResult.Success(emptyList())
        override suspend fun searchWithCompletion(query: String, completed: Boolean): AppResult<List<TaskSummary>> = AppResult.Success(emptyList())
        override fun observeAllScheduledTasks(): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeScheduledTasksFrom(startUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeOverdueTasks(nowUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeRecurringTasks(): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun bulkMoveToProject(ids: List<UUID>, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun bulkSetPriority(ids: List<UUID>, priority: String, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun bulkSetDueDate(ids: List<UUID>, dueAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun bulkComplete(ids: List<UUID>, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun bulkDelete(ids: List<UUID>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getRecurringSeries(parentTaskId: UUID): AppResult<List<TaskSummary>> = AppResult.Success(emptyList())
        override suspend fun completeRecurringTask(task: Task, completedAtUtcMillis: Long, updatedAtUtcMillis: Long): AppResult<Task> = AppResult.Success(Task(
            id = UUID.randomUUID(),
            projectId = null,
            sectionId = null,
            parentTaskId = null,
            title = "",
            description = "",
            completed = false,
            priority = Priority.P4,
            dueAtUtcMillis = null,
            recurringRule = null,
            sortOrder = 0,
            createdAtUtcMillis = 0,
            updatedAtUtcMillis = 0,
            completedAtUtcMillis = null,
            labels = emptyList(),
            subtaskCount = 0,
            completedSubtaskCount = 0,
        ))
    }

    @Test
    fun delegatesToRepository() = runTest {
        val useCase = ObserveInboxTasksUseCase(taskRepository = fakeRepository)

        useCase().collect { result ->
            assertEquals(AppResult.Success(emptyList<TaskSummary>()), result)
        }
    }
}
