package com.mydo.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mydo.app.data.local.dao.RecentSearchDao
import com.mydo.app.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecentSearchDaoTest {

    private lateinit var database: MydoDatabase
    private lateinit var dao: RecentSearchDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MydoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.recentSearchDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun entry(query: String, searchedAt: Long) =
        RecentSearchEntity(id = UUID.randomUUID().toString(), query = query, searchedAtUtcMillis = searchedAt)

    @Test
    fun observeRecentOrdersByMostRecentFirst() = runBlocking {
        dao.upsert(entry("milk", 100L))
        dao.upsert(entry("bananas", 300L))
        dao.upsert(entry("eggs", 200L))

        val recent = dao.observeRecent(10).first()
        assertEquals(listOf("bananas", "eggs", "milk"), recent.map { it.query })
    }

    @Test
    fun reSearchingSameQueryReplacesInsteadOfDuplicating() = runBlocking {
        dao.upsert(entry("milk", 100L))
        dao.upsert(entry("milk", 500L))

        assertEquals(1, dao.count())
        assertEquals(500L, dao.getRecent(10).first().searchedAtUtcMillis)
    }

    @Test
    fun trimToKeepsOnlyMostRecentEntries() = runBlocking {
        (1..5).forEach { i -> dao.upsert(entry("query$i", i.toLong())) }
        dao.trimTo(2)

        val remaining = dao.getRecent(10)
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.query == "query5" || it.query == "query4" })
    }

    @Test
    fun clearRemovesEverything() = runBlocking {
        dao.upsert(entry("milk", 100L))
        dao.clear()
        assertEquals(0, dao.count())
    }
}
