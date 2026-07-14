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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(section: SectionEntity)

    @Update
    suspend fun update(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM sections WHERE projectId = :projectId")
    suspend fun nextSortOrder(projectId: String): Int

    @Query("UPDATE sections SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}
