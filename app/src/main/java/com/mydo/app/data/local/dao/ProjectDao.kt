package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE archived = 1 ORDER BY name ASC")
    fun observeArchived(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE favorite = 1 AND archived = 0 ORDER BY sortOrder ASC")
    fun observeFavorites(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM projects")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE projects SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}
