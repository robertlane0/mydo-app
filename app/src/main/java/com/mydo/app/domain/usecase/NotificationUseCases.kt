package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Notification
import com.mydo.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveNotificationsUseCase(private val notificationRepository: NotificationRepository) {
    operator fun invoke(): Flow<AppResult<List<Notification>>> = notificationRepository.observeAll()
}

class ObserveUnreadNotificationCountUseCase(private val notificationRepository: NotificationRepository) {
    operator fun invoke(): Flow<AppResult<Int>> = notificationRepository.observeUnreadCount()
}

class MarkNotificationReadUseCase(private val notificationRepository: NotificationRepository) {
    suspend operator fun invoke(id: UUID): AppResult<Unit> = notificationRepository.markRead(id)
}

class MarkAllNotificationsReadUseCase(private val notificationRepository: NotificationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = notificationRepository.markAllRead()
}

class ClearNotificationsUseCase(private val notificationRepository: NotificationRepository) {
    suspend operator fun invoke(): AppResult<Unit> = notificationRepository.clearAll()
}
