package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun observeByProject(projectId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getById(id: String): SectionEntity?

    @Query("SELECT * FROM sections ORDER BY sortOrder ASC")
    suspend fun getAllSnapshot(): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 20): List<SectionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(section: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<SectionEntity>)

    @Update
    suspend fun update(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sections")
    suspend fun clearAll()

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM sections WHERE projectId = :projectId")
    suspend fun nextSortOrder(projectId: String): Int

    @Query("UPDATE sections SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}
