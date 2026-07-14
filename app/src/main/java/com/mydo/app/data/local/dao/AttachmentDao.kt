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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attachments WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
