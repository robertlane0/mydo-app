package com.mydo.app.data.local.mapper

import com.mydo.app.data.local.entity.*
import com.mydo.app.domain.model.*
import java.util.UUID

fun String.toUUID(): UUID = java.util.UUID.fromString(this)
fun UUID.toUUIDString(): String = this.toString()

fun ProjectEntity.toDomain() = Project(
    id = id.toUUID(),
    name = name,
    description = description,
    color = color,
    icon = icon,
    archived = archived,
    favorite = favorite,
    sortOrder = sortOrder,
    createdAtUtcMillis = createdAtUtcMillis,
    updatedAtUtcMillis = updatedAtUtcMillis
)

fun Project.toEntity() = ProjectEntity(
    id = id.toUUIDString(),
    name = name,
    description = description,
    color = color,
    icon = icon,
    archived = archived,
    favorite = favorite,
    sortOrder = sortOrder,
    createdAtUtcMillis = createdAtUtcMillis,
    updatedAtUtcMillis = updatedAtUtcMillis
)

fun SectionEntity.toDomain() = Section(
    id = id.toUUID(),
    projectId = projectId.toUUID(),
    name = name,
    sortOrder = sortOrder
)

fun Section.toEntity() = SectionEntity(
    id = id.toUUIDString(),
    projectId = projectId.toUUIDString(),
    name = name,
    sortOrder = sortOrder
)

fun LabelEntity.toDomain() = Label(
    id = id.toUUID(),
    name = name,
    color = color,
    createdAtUtcMillis = createdAtUtcMillis
)

fun Label.toEntity() = LabelEntity(
    id = id.toUUIDString(),
    name = name,
    color = color,
    createdAtUtcMillis = createdAtUtcMillis
)

fun TaskEntity.toDomain(labels: List<LabelEntity> = emptyList(), subtaskCount: Int = 0, completedSubtaskCount: Int = 0) = Task(
    id = id.toUUID(),
    projectId = projectId?.toUUID(),
    sectionId = sectionId?.toUUID(),
    parentTaskId = parentTaskId?.toUUID(),
    title = title,
    description = description,
    completed = completed,
    priority = Priority.valueOf(priority),
    dueAtUtcMillis = dueAtUtcMillis,
    recurringRule = recurringRule,
    sortOrder = sortOrder,
    createdAtUtcMillis = createdAtUtcMillis,
    updatedAtUtcMillis = updatedAtUtcMillis,
    completedAtUtcMillis = completedAtUtcMillis,
    labels = labels.map { it.toDomain() },
    subtaskCount = subtaskCount,
    completedSubtaskCount = completedSubtaskCount
)

fun Task.toEntity() = TaskEntity(
    id = id.toUUIDString(),
    projectId = projectId?.toUUIDString(),
    sectionId = sectionId?.toUUIDString(),
    parentTaskId = parentTaskId?.toUUIDString(),
    title = title,
    description = description,
    completed = completed,
    priority = priority.name,
    dueAtUtcMillis = dueAtUtcMillis,
    recurringRule = recurringRule,
    sortOrder = sortOrder,
    createdAtUtcMillis = createdAtUtcMillis,
    updatedAtUtcMillis = updatedAtUtcMillis,
    completedAtUtcMillis = completedAtUtcMillis
)

fun TaskEntity.toSummary(projectName: String? = null) = TaskSummary(
    id = id.toUUID(),
    title = title,
    completed = completed,
    priority = Priority.valueOf(priority),
    dueAtUtcMillis = dueAtUtcMillis,
    projectPath = projectName, // Could be enhanced to show project/section
)

fun FilterEntity.toDomain() = Filter(
    id = id.toUUID(),
    name = name,
    query = query,
    favorite = favorite
)

fun Filter.toEntity() = FilterEntity(
    id = id.toUUIDString(),
    name = name,
    query = query,
    favorite = favorite
)

fun ReminderEntity.toDomain() = Reminder(
    id = id.toUUID(),
    taskId = taskId.toUUID(),
    triggerAtUtcMillis = triggerAtUtcMillis,
    type = ReminderType.valueOf(type),
    enabled = enabled
)

fun Reminder.toEntity() = ReminderEntity(
    id = id.toUUIDString(),
    taskId = taskId.toUUIDString(),
    triggerAtUtcMillis = triggerAtUtcMillis,
    type = type.name,
    enabled = enabled
)

fun AttachmentEntity.toDomain() = Attachment(
    id = id.toUUID(),
    taskId = taskId.toUUID(),
    filename = filename,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localUri = localUri
)

fun Attachment.toEntity() = AttachmentEntity(
    id = id.toUUIDString(),
    taskId = taskId.toUUIDString(),
    filename = filename,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localUri = localUri
)

fun ActivityEventEntity.toDomain() = ActivityEvent(
    id = id.toUUID(),
    objectId = objectId.toUUID(),
    objectType = ObjectType.valueOf(objectType),
    eventType = EventType.valueOf(eventType),
    timestampUtcMillis = timestampUtcMillis
)

fun ActivityEvent.toEntity() = ActivityEventEntity(
    id = id.toUUIDString(),
    objectId = objectId.toUUIDString(),
    objectType = objectType.name,
    eventType = eventType.name,
    timestampUtcMillis = timestampUtcMillis
)

fun NotificationEntity.toDomain() = Notification(
    id = id.toUUID(),
    type = NotificationType.valueOf(type),
    taskId = taskId?.toUUID(),
    title = title,
    read = read,
    createdAtUtcMillis = createdAtUtcMillis
)

fun Notification.toEntity() = NotificationEntity(
    id = id.toUUIDString(),
    type = type.name,
    taskId = taskId?.toUUIDString(),
    title = title,
    read = read,
    createdAtUtcMillis = createdAtUtcMillis
)
