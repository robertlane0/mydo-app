package com.mydo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mydo.app.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAtUtcMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<RecentSearchEntity>>

    @Query("SELECT * FROM recent_searches ORDER BY searchedAtUtcMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<RecentSearchEntity>

    // Replace-on-conflict for the unique `query` index means re-searching a term
    // refreshes its timestamp and bumps it back to the top instead of duplicating it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteByQuery(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM recent_searches")
    suspend fun count(): Int

    @Query(
        "DELETE FROM recent_searches WHERE id NOT IN (" +
            "SELECT id FROM recent_searches ORDER BY searchedAtUtcMillis DESC LIMIT :keep" +
            ")"
    )
    suspend fun trimTo(keep: Int)
}
