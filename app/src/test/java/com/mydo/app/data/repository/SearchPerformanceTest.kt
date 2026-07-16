package com.mydo.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mydo.app.core.errors.AppResult
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.TaskEntity
import com.mydo.app.domain.search.FilterQueryEvaluator
import com.mydo.app.domain.search.FilterQueryParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Guards specs08-search.md's large-dataset performance requirement ("Query execution
 * < 200ms for 10k tasks") at the repository + query-engine layer. Robolectric's SQLite
 * bridge is slower than a real device, so the bound here is intentionally generous —
 * it exists to catch an accidentally-quadratic implementation (e.g. an N+1 query per
 * task), not to certify on-device latency.
 */
@RunWith(AndroidJUnit4::class)
class SearchPerformanceTest {

    private lateinit var database: MydoDatabase
    private lateinit var repository: RoomTaskRepository

    private val taskCount = 10_000

    @Before
    fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MydoDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomTaskRepository(database)

        val dao = database.taskDao()
        val now = System.currentTimeMillis()
        (0 until taskCount).forEach { i ->
            dao.insert(
                TaskEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = null,
                    sectionId = null,
                    parentTaskId = null,
                    title = "Task number $i covering topic ${i % 50}",
                    description = if (i % 7 == 0) "Needs review from the team" else "",
                    completed = i % 5 == 0,
                    priority = listOf("P1", "P2", "P3", "P4")[i % 4],
                    dueAtUtcMillis = if (i % 3 == 0) now + i * 60_000L else null,
                    recurringRule = null,
                    sortOrder = i,
                    createdAtUtcMillis = now,
                    updatedAtUtcMillis = now,
                    completedAtUtcMillis = null,
                )
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun filterContextsForTenThousandTasksBuildsWithinBudget() = runBlocking {
        val elapsedMillis = measureMillis {
            val result = repository.getFilterContexts()
            assertTrue(result is AppResult.Success)
            assertTrue((result as AppResult.Success).value.size == taskCount)
        }
        assertTrue("getFilterContexts() took ${elapsedMillis}ms for $taskCount tasks", elapsedMillis < 5_000)
    }

    @Test
    fun evaluatingAQueryAgainstTenThousandTasksBuildsWithinBudget() = runBlocking {
        val contexts = (repository.getFilterContexts() as AppResult.Success).value
        val query = FilterQueryParser.parse("priority:1 -completed:true")
        val evaluator = FilterQueryEvaluator()

        val elapsedMillis = measureMillis {
            val matches = contexts.filter { evaluator.matches(query, it) }
            assertTrue(matches.isNotEmpty())
        }
        assertTrue("Evaluating a query over $taskCount tasks took ${elapsedMillis}ms", elapsedMillis < 1_000)
    }

    private inline fun measureMillis(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }
}
