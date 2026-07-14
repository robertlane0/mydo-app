package com.mydo.app.data.local.dao

import androidx.room.*
import com.mydo.app.data.local.entity.ActivityEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Query("SELECT * FROM activity_events WHERE objectId = :objectId ORDER BY timestampUtcMillis DESC")
    fun observeByObject(objectId: String): Flow<List<ActivityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ActivityEventEntity)

    @Query("DELETE FROM activity_events WHERE objectId = :objectId")
    suspend fun deleteByObject(objectId: String)
}
