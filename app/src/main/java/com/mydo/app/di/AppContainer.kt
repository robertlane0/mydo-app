package com.mydo.app.di

import android.content.Context
import androidx.room.Room
import com.mydo.app.core.errors.ErrorReporter
import com.mydo.app.core.errors.LogcatErrorReporter
import com.mydo.app.core.time.SystemTimeProvider
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.MIGRATION_1_2
import com.mydo.app.data.repository.RoomAttachmentRepository
import com.mydo.app.data.repository.RoomFilterRepository
import com.mydo.app.data.repository.RoomLabelRepository
import com.mydo.app.data.repository.RoomPreferenceRepository
import com.mydo.app.data.repository.RoomProjectRepository
import com.mydo.app.data.repository.RoomTaskRepository
import com.mydo.app.domain.repository.AttachmentRepository
import com.mydo.app.domain.repository.FilterRepository
import com.mydo.app.domain.repository.LabelRepository
import com.mydo.app.domain.repository.PreferenceRepository
import com.mydo.app.domain.repository.ProjectRepository
import com.mydo.app.domain.repository.TaskRepository
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.ObserveInboxTasksUseCase
import com.mydo.app.domain.usecase.ObserveUpcomingTasksUseCase
import com.mydo.app.domain.usecase.SearchTasksUseCase
import com.mydo.app.platform.AndroidAttachmentGateway
import com.mydo.app.platform.AndroidDocumentPicker
import com.mydo.app.platform.AndroidNotificationScheduler
import com.mydo.app.platform.AndroidShareGateway
import com.mydo.app.platform.AttachmentGateway
import com.mydo.app.platform.DocumentPicker
import com.mydo.app.platform.NotificationScheduler
import com.mydo.app.platform.ShareGateway

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: MydoDatabase = Room.databaseBuilder(
        applicationContext,
        MydoDatabase::class.java,
        "mydo.db",
    )
    .addMigrations(MIGRATION_1_2)
    .build()

    val errorReporter: ErrorReporter = LogcatErrorReporter()
    val timeProvider: TimeProvider = SystemTimeProvider()

    val taskRepository: TaskRepository = RoomTaskRepository(database)
    val labelRepository: LabelRepository = RoomLabelRepository(database)
    val filterRepository: FilterRepository = RoomFilterRepository(database)
    val attachmentRepository: AttachmentRepository = RoomAttachmentRepository(database)
    val preferenceRepository: PreferenceRepository = RoomPreferenceRepository(database, timeProvider)
    val projectRepository: ProjectRepository = RoomProjectRepository(database)

    val notificationScheduler: NotificationScheduler = AndroidNotificationScheduler()
    val documentPicker: DocumentPicker = AndroidDocumentPicker()
    val shareGateway: ShareGateway = AndroidShareGateway()
    val attachmentGateway: AttachmentGateway = AndroidAttachmentGateway()

    val observeInboxTasks = ObserveInboxTasksUseCase(taskRepository)
    val observeUpcomingTasks = ObserveUpcomingTasksUseCase(taskRepository, timeProvider)
    val searchTasks = SearchTasksUseCase(taskRepository)
    val completeTask = CompleteTaskUseCase(taskRepository, timeProvider)
}
