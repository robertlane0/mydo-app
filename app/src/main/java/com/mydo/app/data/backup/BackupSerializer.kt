package com.mydo.app.data.backup

import com.mydo.app.data.local.PreferenceEntity
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
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Current backup file format. Bump only when an entity's on-disk shape changes in a way
 *  that would break parsing an older file — adding an optional field does not require a bump
 *  since every reader falls back to a default for keys it doesn't find. */
const val BACKUP_FORMAT_VERSION = 1

/** One full snapshot of every table a backup covers, as typed entity lists. */
data class BackupSnapshot(
    val preferences: List<PreferenceEntity>,
    val projects: List<ProjectEntity>,
    val sections: List<SectionEntity>,
    val tasks: List<TaskEntity>,
    val labels: List<LabelEntity>,
    val taskLabels: List<TaskLabelCrossRef>,
    val filters: List<FilterEntity>,
    val reminders: List<ReminderEntity>,
    val attachments: List<AttachmentEntity>,
    val activityEvents: List<ActivityEventEntity>,
    val notifications: List<NotificationEntity>,
)

/** A [BackupSnapshot] plus the file-level metadata that wraps it. */
data class BackupDocument(
    val formatVersion: Int,
    val exportedAtUtcMillis: Long,
    val appVersionName: String,
    val checksumSha256: String,
    val snapshot: BackupSnapshot,
)

/**
 * Converts between [BackupSnapshot] and the on-disk JSON format, and computes/verifies the
 * integrity checksum (specs11-data-model.md, "Data Portability": "a complete, versioned
 * snapshot ... with integrity metadata"). Uses `org.json` (bundled with Android) rather than
 * a third-party JSON library, so backups need no extra dependency.
 */
object BackupSerializer {

    fun write(snapshot: BackupSnapshot, exportedAtUtcMillis: Long, appVersionName: String): String {
        val entitiesJson = snapshotToJson(snapshot)
        val checksum = checksumOf(entitiesJson)
        val root = JSONObject()
            .put("formatVersion", BACKUP_FORMAT_VERSION)
            .put("exportedAtUtcMillis", exportedAtUtcMillis)
            .put("appVersionName", appVersionName)
            .put("checksumSha256", checksum)
            .put("entities", entitiesJson)
        return root.toString(2)
    }

    /** @throws BackupParseException if [rawJson] isn't valid JSON, is missing required
     *  fields, or its checksum doesn't match its contents. */
    fun read(rawJson: String): BackupDocument {
        val root = try {
            JSONObject(rawJson)
        } catch (e: Exception) {
            throw BackupParseException("not_json", "This file isn't a valid MyDo backup.", e)
        }

        val formatVersion = root.optInt("formatVersion", -1)
        if (formatVersion !in 1..BACKUP_FORMAT_VERSION) {
            throw BackupParseException(
                "unsupported_version",
                if (formatVersion > BACKUP_FORMAT_VERSION) "This backup was made by a newer version of MyDo." else "This backup's format isn't recognized.",
            )
        }
        val exportedAtUtcMillis = if (root.has("exportedAtUtcMillis")) root.optLong("exportedAtUtcMillis") else
            throw BackupParseException("missing_field", "This backup is missing its export date.")
        val appVersionName = root.optString("appVersionName", "")
        val declaredChecksum = root.optString("checksumSha256", "")
        val entitiesJson = root.optJSONObject("entities")
            ?: throw BackupParseException("missing_field", "This backup has no data in it.")

        val snapshot = try {
            jsonToSnapshot(entitiesJson)
        } catch (e: Exception) {
            throw BackupParseException("malformed_entities", "This backup's data is corrupted.", e)
        }

        val recomputedChecksum = checksumOf(snapshotToJson(snapshot))
        if (declaredChecksum.isBlank() || !declaredChecksum.equals(recomputedChecksum, ignoreCase = true)) {
            throw BackupParseException("checksum_mismatch", "This backup failed an integrity check — it may be corrupted or tampered with.")
        }

        return BackupDocument(formatVersion, exportedAtUtcMillis, appVersionName, declaredChecksum, snapshot)
    }

    private fun checksumOf(entitiesJson: JSONObject): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(entitiesJson.toString().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun snapshotToJson(snapshot: BackupSnapshot): JSONObject = JSONObject()
        .put("preferences", JSONArray(snapshot.preferences.map { it.toJson() }))
        .put("projects", JSONArray(snapshot.projects.map { it.toJson() }))
        .put("sections", JSONArray(snapshot.sections.map { it.toJson() }))
        .put("tasks", JSONArray(snapshot.tasks.map { it.toJson() }))
        .put("labels", JSONArray(snapshot.labels.map { it.toJson() }))
        .put("taskLabels", JSONArray(snapshot.taskLabels.map { it.toJson() }))
        .put("filters", JSONArray(snapshot.filters.map { it.toJson() }))
        .put("reminders", JSONArray(snapshot.reminders.map { it.toJson() }))
        .put("attachments", JSONArray(snapshot.attachments.map { it.toJson() }))
        .put("activityEvents", JSONArray(snapshot.activityEvents.map { it.toJson() }))
        .put("notifications", JSONArray(snapshot.notifications.map { it.toJson() }))

    private fun jsonToSnapshot(entities: JSONObject): BackupSnapshot = BackupSnapshot(
        preferences = entities.getJSONArray("preferences").objects().map { it.toPreferenceEntity() },
        projects = entities.getJSONArray("projects").objects().map { it.toProjectEntity() },
        sections = entities.getJSONArray("sections").objects().map { it.toSectionEntity() },
        tasks = entities.getJSONArray("tasks").objects().map { it.toTaskEntity() },
        labels = entities.getJSONArray("labels").objects().map { it.toLabelEntity() },
        taskLabels = entities.getJSONArray("taskLabels").objects().map { it.toTaskLabelCrossRef() },
        filters = entities.getJSONArray("filters").objects().map { it.toFilterEntity() },
        reminders = entities.getJSONArray("reminders").objects().map { it.toReminderEntity() },
        attachments = entities.getJSONArray("attachments").objects().map { it.toAttachmentEntity() },
        activityEvents = entities.getJSONArray("activityEvents").objects().map { it.toActivityEventEntity() },
        notifications = entities.getJSONArray("notifications").objects().map { it.toNotificationEntity() },
    )
}

class BackupParseException(val reason: String, message: String, cause: Throwable? = null) : Exception(message, cause)

// -- JSONArray helpers --

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

// -- Entity <-> JSON --

private fun PreferenceEntity.toJson(): JSONObject = JSONObject()
    .put("key", key)
    .put("value", value)
    .put("updatedAtUtcMillis", updatedAtUtcMillis)

private fun JSONObject.toPreferenceEntity() = PreferenceEntity(
    key = getString("key"),
    value = getString("value"),
    updatedAtUtcMillis = getLong("updatedAtUtcMillis"),
)

private fun ProjectEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("description", description)
    .put("color", color)
    .put("icon", icon)
    .put("archived", archived)
    .put("favorite", favorite)
    .put("sortOrder", sortOrder)
    .put("createdAtUtcMillis", createdAtUtcMillis)
    .put("updatedAtUtcMillis", updatedAtUtcMillis)

private fun JSONObject.toProjectEntity() = ProjectEntity(
    id = getString("id"),
    name = getString("name"),
    description = optString("description", ""),
    color = getString("color"),
    icon = optString("icon", ""),
    archived = optBoolean("archived", false),
    favorite = optBoolean("favorite", false),
    sortOrder = optInt("sortOrder", 0),
    createdAtUtcMillis = getLong("createdAtUtcMillis"),
    updatedAtUtcMillis = getLong("updatedAtUtcMillis"),
)

private fun SectionEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("projectId", projectId)
    .put("name", name)
    .put("sortOrder", sortOrder)

private fun JSONObject.toSectionEntity() = SectionEntity(
    id = getString("id"),
    projectId = getString("projectId"),
    name = getString("name"),
    sortOrder = optInt("sortOrder", 0),
)

private fun TaskEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .putNullable("projectId", projectId)
    .putNullable("sectionId", sectionId)
    .putNullable("parentTaskId", parentTaskId)
    .put("title", title)
    .put("description", description)
    .put("completed", completed)
    .put("priority", priority)
    .putNullable("dueAtUtcMillis", dueAtUtcMillis)
    .putNullable("recurringRule", recurringRule)
    .put("sortOrder", sortOrder)
    .put("createdAtUtcMillis", createdAtUtcMillis)
    .put("updatedAtUtcMillis", updatedAtUtcMillis)
    .putNullable("completedAtUtcMillis", completedAtUtcMillis)
    .putNullable("recurrenceAnchorUtcMillis", recurrenceAnchorUtcMillis)
    .put("occurrenceNumber", occurrenceNumber)
    .putNullable("previousOccurrenceTaskId", previousOccurrenceTaskId)

private fun JSONObject.toTaskEntity() = TaskEntity(
    id = getString("id"),
    projectId = stringOrNull("projectId"),
    sectionId = stringOrNull("sectionId"),
    parentTaskId = stringOrNull("parentTaskId"),
    title = getString("title"),
    description = optString("description", ""),
    completed = optBoolean("completed", false),
    priority = getString("priority"),
    dueAtUtcMillis = longOrNull("dueAtUtcMillis"),
    recurringRule = stringOrNull("recurringRule"),
    sortOrder = optInt("sortOrder", 0),
    createdAtUtcMillis = getLong("createdAtUtcMillis"),
    updatedAtUtcMillis = getLong("updatedAtUtcMillis"),
    completedAtUtcMillis = longOrNull("completedAtUtcMillis"),
    recurrenceAnchorUtcMillis = longOrNull("recurrenceAnchorUtcMillis"),
    occurrenceNumber = optInt("occurrenceNumber", 1),
    previousOccurrenceTaskId = stringOrNull("previousOccurrenceTaskId"),
)

private fun LabelEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("color", color)
    .put("createdAtUtcMillis", createdAtUtcMillis)

private fun JSONObject.toLabelEntity() = LabelEntity(
    id = getString("id"),
    name = getString("name"),
    color = getString("color"),
    createdAtUtcMillis = getLong("createdAtUtcMillis"),
)

private fun TaskLabelCrossRef.toJson(): JSONObject = JSONObject()
    .put("taskId", taskId)
    .put("labelId", labelId)

private fun JSONObject.toTaskLabelCrossRef() = TaskLabelCrossRef(
    taskId = getString("taskId"),
    labelId = getString("labelId"),
)

private fun FilterEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("query", query)
    .put("favorite", favorite)

private fun JSONObject.toFilterEntity() = FilterEntity(
    id = getString("id"),
    name = getString("name"),
    query = getString("query"),
    favorite = optBoolean("favorite", false),
)

private fun ReminderEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("taskId", taskId)
    .put("triggerAtUtcMillis", triggerAtUtcMillis)
    .put("type", type)
    .put("enabled", enabled)

private fun JSONObject.toReminderEntity() = ReminderEntity(
    id = getString("id"),
    taskId = getString("taskId"),
    triggerAtUtcMillis = getLong("triggerAtUtcMillis"),
    type = getString("type"),
    enabled = optBoolean("enabled", true),
)

private fun AttachmentEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("taskId", taskId)
    .put("filename", filename)
    .put("mimeType", mimeType)
    .put("sizeBytes", sizeBytes)
    .put("localUri", localUri)

private fun JSONObject.toAttachmentEntity() = AttachmentEntity(
    id = getString("id"),
    taskId = getString("taskId"),
    filename = getString("filename"),
    mimeType = getString("mimeType"),
    sizeBytes = optLong("sizeBytes", 0L),
    localUri = getString("localUri"),
)

private fun ActivityEventEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("objectId", objectId)
    .put("objectType", objectType)
    .put("eventType", eventType)
    .put("timestampUtcMillis", timestampUtcMillis)

private fun JSONObject.toActivityEventEntity() = ActivityEventEntity(
    id = getString("id"),
    objectId = getString("objectId"),
    objectType = getString("objectType"),
    eventType = getString("eventType"),
    timestampUtcMillis = getLong("timestampUtcMillis"),
)

private fun NotificationEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("type", type)
    .putNullable("taskId", taskId)
    .put("title", title)
    .put("read", read)
    .put("createdAtUtcMillis", createdAtUtcMillis)

private fun JSONObject.toNotificationEntity() = NotificationEntity(
    id = getString("id"),
    type = getString("type"),
    taskId = stringOrNull("taskId"),
    title = getString("title"),
    read = optBoolean("read", false),
    createdAtUtcMillis = getLong("createdAtUtcMillis"),
)

internal fun BackupSnapshot.counts() = com.mydo.app.domain.model.BackupCounts(
    preferences = preferences.size,
    projects = projects.size,
    sections = sections.size,
    tasks = tasks.size,
    labels = labels.size,
    filters = filters.size,
    reminders = reminders.size,
    attachments = attachments.size,
    activityEvents = activityEvents.size,
    notifications = notifications.size,
)
