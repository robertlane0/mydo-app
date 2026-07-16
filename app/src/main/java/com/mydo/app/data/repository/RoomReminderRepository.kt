package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.ReminderEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Reminder
import com.mydo.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomReminderRepository(private val db: MydoDatabase) : ReminderRepository {
    private val dao = db.reminderDao()

    override fun observeByTask(taskId: UUID): Flow<AppResult<List<Reminder>>> =
        dao.observeByTask(taskId.toUUIDString())
            .map<List<ReminderEntity>, AppResult<List<Reminder>>> { list -> AppResult.Success(list.map { it.toDomain() }) }
            .catch { e -> emit(AppResult.Failure(DatabaseError("db_error", "Failed to load reminders", e))) }

    override suspend fun getByTask(taskId: UUID): AppResult<List<Reminder>> = try {
        AppResult.Success(dao.getByTaskSnapshot(taskId.toUUIDString()).map { it.toDomain() })
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load reminders", e))
    }

    override suspend fun create(reminder: Reminder): AppResult<Unit> = try {
        dao.insert(reminder.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create reminder", e))
    }

    override suspend fun update(reminder: Reminder): AppResult<Unit> = try {
        dao.update(reminder.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to update reminder", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete reminder", e))
    }

    override suspend fun deleteByTask(taskId: UUID): AppResult<Unit> = try {
        dao.deleteByTask(taskId.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete reminders", e))
    }
}
