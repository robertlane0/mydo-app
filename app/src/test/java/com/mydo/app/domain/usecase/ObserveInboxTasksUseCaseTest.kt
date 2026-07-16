package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.Task
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import com.mydo.app.domain.search.TaskFilterContext
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
        override fun observeOverdue(nowUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override fun observeScheduledWindow(sinceUtcMillis: Long, untilUtcMillis: Long): Flow<AppResult<List<TaskSummary>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun getById(id: UUID): AppResult<Task?> = AppResult.Success(null)
        override suspend fun getByIds(ids: List<UUID>): AppResult<List<Task>> = AppResult.Success(emptyList())
        override suspend fun create(task: Task): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun update(task: Task): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun delete(id: UUID): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCompletion(id: UUID, completed: Boolean, completedAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDueDate(id: UUID, dueAtUtcMillis: Long?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updatePriority(id: UUID, priority: Priority, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateRecurrence(id: UUID, recurringRule: String?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun moveToProject(id: UUID, projectId: UUID?, sectionId: UUID?, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun clearSection(sectionId: UUID, updatedAtUtcMillis: Long): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun search(query: String): AppResult<List<TaskSummary>> = AppResult.Success(emptyList())
        override suspend fun reorder(orderedIds: List<UUID>): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getFilterContexts(): AppResult<List<TaskFilterContext>> = AppResult.Success(emptyList())
    }

    @Test
    fun delegatesToRepository() = runTest {
        val useCase = ObserveInboxTasksUseCase(taskRepository = fakeRepository)

        useCase().collect { result ->
            assertEquals(AppResult.Success(emptyList<TaskSummary>()), result)
        }
    }
}
