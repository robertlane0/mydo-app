package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY triggerAtUtcMillis ASC")
    fun observeByTask(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND triggerAtUtcMillis > :nowUtcMillis ORDER BY triggerAtUtcMillis ASC")
    fun observePending(nowUtcMillis: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND triggerAtUtcMillis > :nowUtcMillis ORDER BY triggerAtUtcMillis ASC")
    suspend fun getPending(nowUtcMillis: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: String)
}
