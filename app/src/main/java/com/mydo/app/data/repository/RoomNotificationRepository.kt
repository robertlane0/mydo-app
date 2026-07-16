package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.NotificationEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Notification
import com.mydo.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomNotificationRepository(private val db: MydoDatabase) : NotificationRepository {
    private val dao = db.notificationDao()

    override fun observeAll(): Flow<AppResult<List<Notification>>> =
        dao.observeAll().map<List<NotificationEntity>, AppResult<List<Notification>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load notifications", e))) }

    override fun observeUnreadCount(): Flow<AppResult<Int>> =
        dao.observeUnreadCount().map<Int, AppResult<Int>> { AppResult.Success(it) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load notifications", e))) }

    override suspend fun create(notification: Notification): AppResult<Unit> = try {
        dao.insert(notification.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create notification", e))
    }

    override suspend fun markRead(id: UUID): AppResult<Unit> = try {
        dao.markRead(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update notification", e))
    }

    override suspend fun markAllRead(): AppResult<Unit> = try {
        dao.markAllRead()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update notifications", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete notification", e))
    }

    override suspend fun clearAll(): AppResult<Unit> = try {
        dao.clearAll()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to clear notifications", e))
    }
}
