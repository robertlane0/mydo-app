package com.mydo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences ORDER BY key ASC")
    fun observeAll(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM preferences WHERE `key` = :key")
    suspend fun getByKey(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PreferenceEntity)
}
