package com.mydo.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Projects table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `projects` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `color` TEXT NOT NULL,
                `icon` TEXT NOT NULL,
                `archived` INTEGER NOT NULL,
                `favorite` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAtUtcMillis` INTEGER NOT NULL,
                `updatedAtUtcMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_archived` ON `projects` (`archived`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_favorite` ON `projects` (`favorite`)")

        // Sections table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `sections` (
                `id` TEXT NOT NULL,
                `projectId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sections_projectId` ON `sections` (`projectId`)")

        // Tasks table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` TEXT NOT NULL,
                `projectId` TEXT,
                `sectionId` TEXT,
                `parentTaskId` TEXT,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `priority` TEXT NOT NULL,
                `dueAtUtcMillis` INTEGER,
                `recurringRule` TEXT,
                `sortOrder` INTEGER NOT NULL,
                `createdAtUtcMillis` INTEGER NOT NULL,
                `updatedAtUtcMillis` INTEGER NOT NULL,
                `completedAtUtcMillis` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`sectionId`) REFERENCES `sections`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`parentTaskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId` ON `tasks` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sectionId` ON `tasks` (`sectionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentTaskId` ON `tasks` (`parentTaskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_completed` ON `tasks` (`completed`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueAtUtcMillis` ON `tasks` (`dueAtUtcMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_priority` ON `tasks` (`priority`)")

        // Labels table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `labels` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `color` TEXT NOT NULL,
                `createdAtUtcMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // Task-Label cross-reference table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `task_labels` (
                `taskId` TEXT NOT NULL,
                `labelId` TEXT NOT NULL,
                PRIMARY KEY(`taskId`, `labelId`),
                FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`labelId`) REFERENCES `labels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_labels_taskId` ON `task_labels` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_labels_labelId` ON `task_labels` (`labelId`)")

        // Filters table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `filters` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `query` TEXT NOT NULL,
                `favorite` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // Reminders table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `triggerAtUtcMillis` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `enabled` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_taskId` ON `reminders` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_triggerAtUtcMillis` ON `reminders` (`triggerAtUtcMillis`)")

        // Attachments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `attachments` (
                `id` TEXT NOT NULL,
                `taskId` TEXT NOT NULL,
                `filename` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `sizeBytes` INTEGER NOT NULL,
                `localUri` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_taskId` ON `attachments` (`taskId`)")

        // Activity events table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `activity_events` (
                `id` TEXT NOT NULL,
                `objectId` TEXT NOT NULL,
                `objectType` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `timestampUtcMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_objectId` ON `activity_events` (`objectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_timestampUtcMillis` ON `activity_events` (`timestampUtcMillis`)")

        // Notifications table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `taskId` TEXT,
                `title` TEXT NOT NULL,
                `read` INTEGER NOT NULL,
                `createdAtUtcMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_read` ON `notifications` (`read`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_createdAtUtcMillis` ON `notifications` (`createdAtUtcMillis`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recurrence tracking (specs16-recurring-tasks.md): the anchor is the basis for
        // "next occurrence" math and is left untouched by ad-hoc reschedules of the
        // current occurrence; occurrenceNumber is compared against RRULE's COUNT=N.
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceAnchorUtcMillis INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN occurrenceNumber INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN previousOccurrenceTaskId TEXT")

        // Recent searches table (local search history, specs08-search.md)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recent_searches` (
                `id` TEXT NOT NULL,
                `query` TEXT NOT NULL,
                `searchedAtUtcMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_searches_searchedAtUtcMillis` ON `recent_searches` (`searchedAtUtcMillis`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recent_searches_query` ON `recent_searches` (`query`)")
    }
}
