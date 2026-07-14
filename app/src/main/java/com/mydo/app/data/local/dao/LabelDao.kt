package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.LabelEntity
import com.mydo.app.data.local.entity.TaskLabelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun observeAll(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE id = :id")
    suspend fun getById(id: String): LabelEntity?

    @Query("SELECT l.* FROM labels l INNER JOIN task_labels tl ON l.id = tl.labelId WHERE tl.taskId = :taskId ORDER BY l.name ASC")
    fun observeByTask(taskId: String): Flow<List<LabelEntity>>

    @Query("SELECT l.* FROM labels l INNER JOIN task_labels tl ON l.id = tl.labelId WHERE tl.taskId = :taskId ORDER BY l.name ASC")
    suspend fun getByTask(taskId: String): List<LabelEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(label: LabelEntity)

    @Update
    suspend fun update(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskLabel(crossRef: TaskLabelCrossRef)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId AND labelId = :labelId")
    suspend fun deleteTaskLabel(taskId: String, labelId: String)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId")
    suspend fun deleteAllTaskLabels(taskId: String)

    @Query("SELECT * FROM labels WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<LabelEntity>
}
