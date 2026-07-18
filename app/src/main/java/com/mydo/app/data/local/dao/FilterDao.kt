package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.FilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM filters ORDER BY name ASC")
    fun observeAll(): Flow<List<FilterEntity>>

    @Query("SELECT * FROM filters WHERE favorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<FilterEntity>>

    @Query("SELECT * FROM filters WHERE id = :id")
    suspend fun getById(id: String): FilterEntity?

    @Query("SELECT * FROM filters WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): FilterEntity?

    @Query("SELECT * FROM filters WHERE name LIKE '%' || :query || '%' OR query LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 20): List<FilterEntity>

    @Query("SELECT COUNT(*) FROM filters")
    suspend fun count(): Int

    @Query("SELECT * FROM filters ORDER BY name ASC")
    suspend fun getAllSnapshot(): List<FilterEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(filter: FilterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filters: List<FilterEntity>)

    @Update
    suspend fun update(filter: FilterEntity)

    @Query("DELETE FROM filters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM filters")
    suspend fun clearAll()
}
