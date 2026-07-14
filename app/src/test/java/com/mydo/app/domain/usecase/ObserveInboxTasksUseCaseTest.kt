package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveInboxTasksUseCaseTest {
    @Test
    fun delegatesToRepository() = runTest {
        val useCase = ObserveInboxTasksUseCase(
            taskRepository = object : TaskRepository {
                override fun observeInboxTasks(): Flow<AppResult<List<TaskSummary>>> {
                    return flowOf(AppResult.Success(emptyList()))
                }
            },
        )

        useCase().collect { result ->
            assertEquals(AppResult.Success(emptyList<TaskSummary>()), result)
        }
    }
}
