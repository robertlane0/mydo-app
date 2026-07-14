package com.mydo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mydo.app.data.local.dao.ActivityEventDao
import com.mydo.app.data.local.dao.AttachmentDao
import com.mydo.app.data.local.dao.FilterDao
import com.mydo.app.data.local.dao.LabelDao
import com.mydo.app.data.local.dao.NotificationDao
import com.mydo.app.data.local.dao.ProjectDao
import com.mydo.app.data.local.dao.ReminderDao
import com.mydo.app.data.local.dao.SectionDao
import com.mydo.app.data.local.dao.TaskDao
import com.mydo.app.data.local.entity.ActivityEventEntity
import com.mydo.app.data.local.entity.AttachmentEntity
import com.mydo.app.data.local.entity.FilterEntity
import com.mydo.app.data.local.entity.LabelEntity
import com.mydo.app.data.local.entity.NotificationEntity
import com.mydo.app.data.local.entity.ProjectEntity
import com.mydo.app.data.local.entity.ReminderEntity
import com.mydo.app.data.local.entity.SectionEntity
import com.mydo.app.data.local.entity.TaskEntity
import com.mydo.app.data.local.entity.TaskLabelCrossRef

@Database(
    entities = [
        PreferenceEntity::class,
        ProjectEntity::class,
        SectionEntity::class,
        TaskEntity::class,
        LabelEntity::class,
        TaskLabelCrossRef::class,
        FilterEntity::class,
        ReminderEntity::class,
        AttachmentEntity::class,
        ActivityEventEntity::class,
        NotificationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MydoDatabase : RoomDatabase() {
    abstract fun preferenceDao(): PreferenceDao
    abstract fun projectDao(): ProjectDao
    abstract fun sectionDao(): SectionDao
    abstract fun taskDao(): TaskDao
    abstract fun labelDao(): LabelDao
    abstract fun filterDao(): FilterDao
    abstract fun reminderDao(): ReminderDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun activityEventDao(): ActivityEventDao
    abstract fun notificationDao(): NotificationDao
}
