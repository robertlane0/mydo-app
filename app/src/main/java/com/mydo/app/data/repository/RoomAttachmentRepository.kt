package com.mydo.app.data.repository

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.errors.DatabaseError
import com.mydo.app.data.local.MydoDatabase
import com.mydo.app.data.local.entity.AttachmentEntity
import com.mydo.app.data.local.mapper.toDomain
import com.mydo.app.data.local.mapper.toEntity
import com.mydo.app.data.local.mapper.toUUIDString
import com.mydo.app.domain.model.Attachment
import com.mydo.app.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomAttachmentRepository(private val db: MydoDatabase) : AttachmentRepository {
    private val dao = db.attachmentDao()

    override fun observeByTask(taskId: UUID): Flow<AppResult<List<Attachment>>> =
        dao.observeByTask(taskId.toUUIDString()).map<List<AttachmentEntity>, AppResult<List<Attachment>>> { list ->
            AppResult.Success(list.map { it.toDomain() })
        }.catch { e ->
            emit(AppResult.Failure(DatabaseError("db_error", "Failed to load attachments", e)))
        }

    override suspend fun getById(id: UUID): AppResult<Attachment?> = try {
        AppResult.Success(dao.getById(id.toUUIDString())?.toDomain())
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to load attachment", e))
    }

    override suspend fun create(attachment: Attachment): AppResult<Unit> = try {
        dao.insert(attachment.toEntity())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to create attachment", e))
    }

    override suspend fun delete(id: UUID): AppResult<Unit> = try {
        dao.deleteById(id.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete attachment", e))
    }

    override suspend fun deleteByTask(taskId: UUID): AppResult<Unit> = try {
        dao.deleteByTask(taskId.toUUIDString())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DatabaseError("db_error", "Failed to delete attachments", e))
    }
}