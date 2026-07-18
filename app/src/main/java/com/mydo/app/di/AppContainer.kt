package com.mydo.app.di

import android.content.Context
import androidx.room.Room
import com.mydo.app.core.errors.ErrorReporter
import com.mydo.app.core.errors.LogcatErrorReporter
import com.mydo.app.core.time.SystemTimeProvider
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.MIGRATION_1_2
import com.mydo.app.data.local.MIGRATION_2_3
import com.mydo.app.data.repository.RoomAttachmentRepository
import com.mydo.app.data.repository.RoomBackupRepository
import com.mydo.app.data.repository.RoomFilterRepository
import com.mydo.app.data.repository.RoomLabelRepository
import com.mydo.app.data.repository.RoomNotificationRepository
import com.mydo.app.data.repository.RoomPreferenceRepository
import com.mydo.app.data.repository.RoomProjectRepository
import com.mydo.app.data.repository.RoomRecentSearchRepository
import com.mydo.app.data.repository.RoomReminderRepository
import com.mydo.app.data.repository.RoomSectionRepository
import com.mydo.app.data.repository.RoomTaskRepository
import com.mydo.app.domain.repository.AttachmentRepository
import com.mydo.app.domain.repository.BackupRepository
import com.mydo.app.domain.repository.FilterRepository
import com.mydo.app.domain.repository.LabelRepository
import com.mydo.app.domain.repository.NotificationRepository
import com.mydo.app.domain.repository.PreferenceRepository
import com.mydo.app.domain.repository.ProjectRepository
import com.mydo.app.domain.repository.RecentSearchRepository
import com.mydo.app.domain.repository.ReminderRepository
import com.mydo.app.domain.repository.SectionRepository
import com.mydo.app.domain.repository.TaskRepository
import com.mydo.app.domain.usecase.*
import com.mydo.app.platform.AndroidAttachmentGateway
import com.mydo.app.platform.AndroidDocumentPicker
import com.mydo.app.platform.AndroidNotificationScheduler
import com.mydo.app.platform.AndroidShareGateway
import com.mydo.app.platform.AttachmentGateway
import com.mydo.app.platform.DocumentPicker
import com.mydo.app.platform.NotificationScheduler
import com.mydo.app.platform.ShareGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Hand-rolled dependency graph (no DI framework). Everything is a `val` built once and
 * shared for the process lifetime, wired from an [android.app.Application] (see
 * MydoApplication) so it outlives any single Activity.
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    /** Process-lifetime scope for fire-and-forget background work (e.g. re-arming reminder
     *  alarms at startup) that shouldn't be tied to any single screen's lifecycle. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: MydoDatabase = Room.databaseBuilder(
        applicationContext,
        MydoDatabase::class.java,
        "mydo.db",
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    val errorReporter: ErrorReporter = LogcatErrorReporter()
    val timeProvider: TimeProvider = SystemTimeProvider()

    // -- Repositories --
    val taskRepository: TaskRepository = RoomTaskRepository(database)
    val preferenceRepository: PreferenceRepository = RoomPreferenceRepository(database, timeProvider)
    val projectRepository: ProjectRepository = RoomProjectRepository(database)
    val sectionRepository: SectionRepository = RoomSectionRepository(database)
    val labelRepository: LabelRepository = RoomLabelRepository(database)
    val reminderRepository: ReminderRepository = RoomReminderRepository(database)
    val attachmentRepository: AttachmentRepository = RoomAttachmentRepository(database)
    val filterRepository: FilterRepository = RoomFilterRepository(database)
    val notificationRepository: NotificationRepository = RoomNotificationRepository(database)
    val recentSearchRepository: RecentSearchRepository = RoomRecentSearchRepository(database, timeProvider)
    val backupRepository: BackupRepository = RoomBackupRepository(
        database,
        timeProvider,
        precautionaryBackupDir = File(applicationContext.filesDir, "backups"),
    )

    // -- Platform --
    val notificationScheduler: NotificationScheduler = AndroidNotificationScheduler(applicationContext)
    val documentPicker: DocumentPicker = AndroidDocumentPicker()
    val shareGateway: ShareGateway = AndroidShareGateway(applicationContext)
    val attachmentGateway: AttachmentGateway = AndroidAttachmentGateway(applicationContext)

    // -- Reminder alarm scheduling (AGENTS.md step 5) --
    val reminderAlarmCoordinator = ReminderAlarmCoordinator(reminderRepository, taskRepository, notificationScheduler, timeProvider)

    // -- Task use cases --
    val createTaskUseCase = CreateTaskUseCase(taskRepository, timeProvider)
    val observeInboxTasks = ObserveInboxTasksUseCase(taskRepository)
    val observeActiveProjectsUseCase = ObserveActiveProjectsUseCase(projectRepository)
    val observeTaskUseCase = ObserveTaskUseCase(taskRepository)
    val updateTaskUseCase = UpdateTaskUseCase(taskRepository, reminderAlarmCoordinator)
    val deleteTaskUseCase = DeleteTaskUseCase(taskRepository, reminderAlarmCoordinator)
    val completeTaskUseCase = CompleteTaskUseCase(taskRepository, reminderRepository, timeProvider, reminderAlarmCoordinator = reminderAlarmCoordinator)
    val undoCompleteTaskUseCase = UndoCompleteTaskUseCase(taskRepository, timeProvider, reminderAlarmCoordinator)
    val reorderTasksUseCase = ReorderTasksUseCase(taskRepository)

    // -- Recurrence use cases --
    val setRecurrenceUseCase = SetRecurrenceUseCase(taskRepository, timeProvider)
    val removeRecurrenceUseCase = RemoveRecurrenceUseCase(taskRepository, timeProvider)
    val skipNextOccurrenceUseCase = SkipNextOccurrenceUseCase(taskRepository, timeProvider)
    val rescheduleTaskUseCase = RescheduleTaskUseCase(taskRepository, timeProvider)

    // -- Reminder use cases --
    val observeRemindersUseCase = ObserveRemindersUseCase(reminderRepository)
    val createAbsoluteReminderUseCase = CreateAbsoluteReminderUseCase(reminderRepository, reminderAlarmCoordinator)
    val createRelativeReminderUseCase = CreateRelativeReminderUseCase(taskRepository, reminderRepository, reminderAlarmCoordinator)
    val updateReminderUseCase = UpdateReminderUseCase(reminderRepository, reminderAlarmCoordinator)
    val deleteReminderUseCase = DeleteReminderUseCase(reminderRepository, reminderAlarmCoordinator)

    // -- Attachment use cases --
    val observeAttachmentsUseCase = ObserveAttachmentsUseCase(attachmentRepository)
    val addAttachmentsUseCase = AddAttachmentsUseCase(attachmentRepository, attachmentGateway)
    val removeAttachmentUseCase = RemoveAttachmentUseCase(attachmentRepository, attachmentGateway)

    // -- Bulk operations --
    val bulkSetPriorityUseCase = BulkSetPriorityUseCase(taskRepository, timeProvider)
    val bulkSetDueDateUseCase = BulkSetDueDateUseCase(taskRepository, timeProvider)
    val bulkMoveTasksUseCase = BulkMoveTasksUseCase(taskRepository, timeProvider)
    val bulkCompleteTasksUseCase = BulkCompleteTasksUseCase(taskRepository, reminderRepository, timeProvider, reminderAlarmCoordinator)
    val bulkDeleteTasksUseCase = BulkDeleteTasksUseCase(taskRepository, reminderAlarmCoordinator)
    val undoBulkTaskOperationUseCase = UndoBulkTaskOperationUseCase(taskRepository, reminderAlarmCoordinator)
    val bulkAddLabelsUseCase = BulkAddLabelsUseCase(labelRepository)
    val undoBulkAddLabelsUseCase = UndoBulkAddLabelsUseCase(labelRepository)

    // -- Upcoming --
    val observeUpcomingUseCase = ObserveUpcomingUseCase(taskRepository, timeProvider)

    // -- Search --
    val searchUseCase = SearchUseCase(taskRepository, projectRepository, sectionRepository, labelRepository, filterRepository)
    val observeRecentSearchesUseCase = ObserveRecentSearchesUseCase(recentSearchRepository)
    val recordRecentSearchUseCase = RecordRecentSearchUseCase(recentSearchRepository)
    val removeRecentSearchUseCase = RemoveRecentSearchUseCase(recentSearchRepository)
    val clearRecentSearchesUseCase = ClearRecentSearchesUseCase(recentSearchRepository)

    // -- Saved filters --
    val observeFiltersUseCase = ObserveFiltersUseCase(filterRepository)
    val validateFilterQueryUseCase = ValidateFilterQueryUseCase()
    val createFilterUseCase = CreateFilterUseCase(filterRepository)
    val updateFilterUseCase = UpdateFilterUseCase(filterRepository)
    val deleteFilterUseCase = DeleteFilterUseCase(filterRepository)
    val toggleFilterFavoriteUseCase = ToggleFilterFavoriteUseCase(filterRepository)
    val runFilterUseCase = RunFilterUseCase(taskRepository)

    // -- Labels --
    val observeLabelsUseCase = ObserveLabelsUseCase(labelRepository)
    val observeTaskLabelsUseCase = ObserveTaskLabelsUseCase(labelRepository)
    val createLabelUseCase = CreateLabelUseCase(labelRepository, timeProvider)
    val updateLabelUseCase = UpdateLabelUseCase(labelRepository)
    val deleteLabelUseCase = DeleteLabelUseCase(labelRepository)
    val assignLabelUseCase = AssignLabelUseCase(labelRepository)
    val unassignLabelUseCase = UnassignLabelUseCase(labelRepository)
    val observeTasksForLabelUseCase = ObserveTasksForLabelUseCase(taskRepository, labelRepository)

    // -- Notifications --
    val observeNotificationsUseCase = ObserveNotificationsUseCase(notificationRepository)
    val observeUnreadNotificationCountUseCase = ObserveUnreadNotificationCountUseCase(notificationRepository)
    val markNotificationReadUseCase = MarkNotificationReadUseCase(notificationRepository)
    val markAllNotificationsReadUseCase = MarkAllNotificationsReadUseCase(notificationRepository)
    val clearNotificationsUseCase = ClearNotificationsUseCase(notificationRepository)

    // -- Settings --
    val observeSettingsUseCase = ObserveSettingsUseCase(preferenceRepository)
    val updateSettingUseCase = UpdateSettingUseCase(preferenceRepository)

    // -- Data (backup/export/import), specs10-settings.md "Data" --
    val exportBackupUseCase = ExportBackupUseCase(backupRepository)
    val inspectBackupUseCase = InspectBackupUseCase(backupRepository)
    val importBackupUseCase = ImportBackupUseCase(backupRepository, reminderAlarmCoordinator)
    val clearLocalDataUseCase = ClearLocalDataUseCase(backupRepository, reminderAlarmCoordinator)
}
