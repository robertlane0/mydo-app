package com.mydo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PreferenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MydoDatabase : RoomDatabase() {
    abstract fun preferenceDao(): PreferenceDao
}
