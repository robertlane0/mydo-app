package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY filename ASC")
    fun observeByTask(taskId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: String): AttachmentEntity?

    @Query("SELECT COUNT(*) FROM attachments WHERE taskId = :taskId")
    suspend fun countByTask(taskId: String): Int

    @Query("SELECT DISTINCT taskId FROM attachments")
    suspend fun getTaskIdsWithAttachments(): List<String>

    @Query("SELECT * FROM attachments ORDER BY filename ASC")
    suspend fun getAllSnapshot(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attachments WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)

    @Query("DELETE FROM attachments")
    suspend fun clearAll()
}
