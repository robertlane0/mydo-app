package com.mydo.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mydo.app.data.local.dao.ProjectDao
import com.mydo.app.data.local.dao.TaskDao
import com.mydo.app.data.local.entity.ProjectEntity
import com.mydo.app.data.local.entity.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: MydoDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MydoDatabase::class.java
        ).allowMainThreadQueries().build()
        projectDao = database.projectDao()
        taskDao = database.taskDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndGetTask() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = taskId,
            projectId = null,
            sectionId = null,
            parentTaskId = null,
            title = "Test Task",
            description = "Description",
            completed = false,
            priority = "P1",
            dueAtUtcMillis = null,
            recurringRule = null,
            sortOrder = 0,
            createdAtUtcMillis = 0,
            updatedAtUtcMillis = 0,
            completedAtUtcMillis = null
        )

        taskDao.insert(task)
        val loaded = taskDao.getById(taskId)
        assertEquals(task.title, loaded?.title)
    }

    @Test
    fun testProjectDeletionSetsTaskProjectIdToNull() = runBlocking {
        val projectId = UUID.randomUUID().toString()
        val project = ProjectEntity(
            id = projectId,
            name = "Project 1",
            description = "",
            color = "#fff",
            icon = "",
            archived = false,
            favorite = false,
            sortOrder = 0,
            createdAtUtcMillis = 0,
            updatedAtUtcMillis = 0
        )
        projectDao.insert(project)

        val taskId = UUID.randomUUID().toString()
        val task = TaskEntity(
            id = taskId,
            projectId = projectId,
            sectionId = null,
            parentTaskId = null,
            title = "Test Task",
            description = "Description",
            completed = false,
            priority = "P1",
            dueAtUtcMillis = null,
            recurringRule = null,
            sortOrder = 0,
            createdAtUtcMillis = 0,
            updatedAtUtcMillis = 0,
            completedAtUtcMillis = null
        )
        taskDao.insert(task)

        // Delete the project
        projectDao.deleteById(projectId)

        // Task should still exist but projectId should be null
        val loadedTask = taskDao.getById(taskId)
        assertEquals(task.title, loadedTask?.title)
        assertNull(loadedTask?.projectId)
    }

    @Test
    fun testTaskDeletionCascadesToSubtasks() = runBlocking {
        val parentId = UUID.randomUUID().toString()
        val parentTask = TaskEntity(
            id = parentId,
            projectId = null, sectionId = null, parentTaskId = null,
            title = "Parent", description = "", completed = false, priority = "P1",
            dueAtUtcMillis = null, recurringRule = null, sortOrder = 0,
            createdAtUtcMillis = 0, updatedAtUtcMillis = 0, completedAtUtcMillis = null
        )
        taskDao.insert(parentTask)

        val childId = UUID.randomUUID().toString()
        val childTask = TaskEntity(
            id = childId,
            projectId = null, sectionId = null, parentTaskId = parentId,
            title = "Child", description = "", completed = false, priority = "P1",
            dueAtUtcMillis = null, recurringRule = null, sortOrder = 0,
            createdAtUtcMillis = 0, updatedAtUtcMillis = 0, completedAtUtcMillis = null
        )
        taskDao.insert(childTask)

        // Delete parent
        taskDao.deleteById(parentId)

        // Child should be deleted
        val loadedChild = taskDao.getById(childId)
        assertNull(loadedChild)
    }
}
