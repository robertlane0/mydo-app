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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(filter: FilterEntity)

    @Update
    suspend fun update(filter: FilterEntity)

    @Query("DELETE FROM filters WHERE id = :id")
    suspend fun deleteById(id: String)
}
