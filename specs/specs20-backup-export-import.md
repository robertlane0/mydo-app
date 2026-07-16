# Backup, Export, and Import Specification

## Purpose

MyDo provides manual, user-initiated export and import of the complete local database. Exports create a versioned, integrity-verified backup file. Imports validate the backup before any mutation, offer replace or merge modes, and never silently overwrite user data. All operations use Android's Storage Access Framework (SAF) for file access; no private file paths are exposed.

---

# Goals

- Export a complete, portable backup of all MyDo data (tasks, projects, labels, filters, reminders, preferences, activity history, attachment metadata)
- Import a validated backup with explicit user confirmation
- Support **Replace** (full restore) and **Merge** (additive, conflict-aware) modes
- Guarantee database integrity: invalid/corrupt backups leave local data unchanged
- Use platform-protected storage via SAF; no `MANAGE_EXTERNAL_STORAGE` required
- Work fully offline; no network access during export or import

---

# Data Model: Backup File Format

## File Structure

```
mydo-backup-v{version}.json
```

Single JSON file (optionally compressed as `.json.gz`). Not a SQLite dump — portable JSON with schema version.

## Top-Level Structure

```json
{
  "version": 4,
  "exportedAt": "2026-01-15T14:30:00Z",
  "exportedBy": "MyDo/1.2.0",
  "checksum": "sha256:abc123...",
  "entityCounts": {
    "projects": 12,
    "sections": 45,
    "tasks": 1250,
    "subtasks": 340,
    "labels": 28,
    "filters": 15,
    "reminders": 890,
    "attachments": 56,
    "activities": 12000,
    "notifications": 450,
    "preferences": 1
  },
  "data": {
    "projects": [...],
    "sections": [...],
    "tasks": [...],
    "subtasks": [...],
    "labels": [...],
    "filters": [...],
    "reminders": [...],
    "attachments": [...],
    "activities": [...],
    "notifications": [...],
    "preferences": {...}
  }
}
```

## Version History

| Version | MyDo Version | Changes |
|---------|--------------|---------|
| 1 | 1.0 | Initial format |
| 2 | 1.1 | Added `attachments`, `reminders`, `notifications` |
| 3 | 1.2 | Added `activities`, `subtasks` separate from tasks |
| 4 | 1.3 | Added `entityCounts`, `checksum`, `preferences` split by category |

## Checksum

- **Algorithm**: SHA-256 of canonicalized JSON (sorted keys, no whitespace) of the `data` object only
- **Purpose**: Detect corruption or tampering before import
- **Validation**: Import computes checksum of `data` and compares to `checksum` field; mismatch → reject

## Entity Serialization

All entities use the same field names as the Room database (snake_case). UUIDs as strings. Dates as ISO-8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`). Enums as uppercase strings.

### Example: Task

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "project_id": "660e8400-e29b-41d4-a716-446655440001",
  "section_id": null,
  "parent_task_id": null,
  "title": "Finish quarterly report",
  "description": "Include Q4 projections",
  "completed": false,
  "priority": "P2",
  "due_date": "2026-01-20",
  "due_time": "14:00:00",
  "recurring_rule": "FREQ=WEEKLY;BYDAY=MO",
  "created_at": "2026-01-01T10:00:00Z",
  "updated_at": "2026-01-15T14:30:00Z",
  "completed_at": null
}
```

### Example: Attachment

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "task_id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "budget.xlsx",
  "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "size": 512000,
  "local_uri": "content://com.android.providers.media.documents/document/12345"
}
```

### Example: Preferences

```json
{
  "general": {
    "defaultStartScreen": "TODAY",
    "defaultPriority": "P3",
    "defaultReminder": "AT_TIME",
    "dateFormat": "ISO",
    "timeFormat": "24H",
    "weekStart": "MONDAY",
    "language": "system",
    "timeZone": "system"
  },
  "notifications": {
    "taskRemindersEnabled": true,
    "dailySummaryEnabled": false,
    "snoozeMinutes": 10
  },
  "appearance": {
    "theme": "SYSTEM",
    "accentColor": "BLUE",
    "fontSize": "MEDIUM",
    "compactMode": false
  },
  "productivity": {
    "dailyGoal": 5,
    "showCompleted": true,
    "showWeekends": true,
    "smartScheduling": false,
    "streakTracking": true
  },
  "privacy": {
    "analyticsEnabled": false,
    "crashReportingEnabled": true
  }
}
```

---

# Export Flow

## Entry Point

Settings → Data → **Export local database**

## Flow

```
Tap Export

↓

Show Progress: "Creating backup…"

↓

1. Query all entities from local DB (transaction)
2. Serialize to JSON (streaming writer)
3. Compute SHA-256 checksum of data object
4. Write to temporary file in app cache
5. Compress to .json.gz (optional, default on)
6. Launch SAF ACTION_CREATE_DOCUMENT
   - MIME: application/gzip (or application/json)
   - Title: "mydo-backup-2026-01-15.json.gz"
   - Initial URI: Downloads/MyDo/
7. User chooses location
8. Copy temp file to chosen URI
9. Delete temp file
10. Show success snackbar: "Backup saved to [location] [View]"
```

## Progress & Cancellation

- Export runs in `WorkManager` (background, expedited)
- Progress notification: "Exporting… 45% (tasks)"
- User can cancel: deletes temp file; no partial backup left
- If app backgrounded: export continues; notification shows completion

## Error Handling

| Error | User Message | Data State |
|-------|--------------|------------|
| DB query fails | "Could not read local data" | Unchanged |
| Serialization fails | "Could not create backup" | Unchanged |
| Checksum mismatch (internal) | "Backup integrity check failed" | Unchanged |
| SAF picker cancelled | (silent, no message) | Unchanged |
| SAF write fails (no space, permission) | "Could not save backup: [reason]" | Unchanged |
| Compression fails | "Could not compress backup" | Unchanged |

---

# Import Flow

## Entry Point

Settings → Data → **Import local database**

## Flow

```
Tap Import

↓

Launch SAF ACTION_OPEN_DOCUMENT
  - MIME: application/gzip, application/json
  - Title: "Select MyDo backup"

↓

User selects file

↓

Read file via ContentResolver (streaming)

↓

If .gz: decompress streaming

↓

Parse JSON (streaming, validate structure)

↓

Validate:
  1. Version supported (≤ current app version)
  2. Required fields present (version, exportedAt, checksum, data)
  3. Checksum matches computed SHA-256 of data
  4. Entity counts match actual array lengths
  5. All UUIDs valid; all dates valid ISO-8601
  6. Referential integrity (project_id refs exist, etc.)

↓

If INVALID: Show error; ABORT (local DB unchanged)

↓

Show Import Preview Dialog:
  ┌─────────────────────────────────────┐
  │ Import Backup                       │
  ├─────────────────────────────────────┤
  │ MyDo Backup v4                      │
  │ Created: Jan 15, 2026 2:30 PM       │
  │ Version: MyDo 1.3.0                 │
  ├─────────────────────────────────────┤
  │ 1,250 tasks  •  12 projects         │
  │ 28 labels  •  15 filters            │
  │ 890 reminders  •  56 attachments    │
  ├─────────────────────────────────────┤
  │ [Replace all local data]            │
  │ [Merge with local data]             │
  │ [Cancel]                            │
  └─────────────────────────────────────┘
```

## Replace Mode (Full Restore)

```
User selects "Replace all local data"

↓

Confirm Dialog:
"Replace ALL local data with this backup?
• 1,250 tasks will replace your current tasks
• All projects, labels, filters, reminders, preferences will be overwritten
• This cannot be undone (except by importing another backup)
• A precautionary backup of current data will be created first"

[Cancel]  [Replace]
```

### Replace Execution

```
1. Create precautionary backup of CURRENT local DB
   → Save to app cache as mydo-pre-import-backup-{timestamp}.json.gz
   → Keep for 7 days or until next successful import

2. Begin DB transaction

3. Delete all tables (in FK order: activities, notifications, reminders,
   attachments, subtasks, tasks, sections, projects, labels, filters, preferences)

4. Insert all entities from backup (in FK order)

5. Commit transaction

6. Reschedule all enabled reminders (via RescheduleRemindersWorker)

7. Refresh all UI (navigation, lists, widgets)

8. Show success: "Import complete. Previous data backed up. [View Backup]"
```

### Precautionary Backup

- Auto-saved to app-specific cache directory
- Not user-accessible via SAF (internal recovery only)
- Max 1 retained; older deleted on new import
- Used for "Undo Import" in Settings → Data → **Restore previous backup** (within 7 days)

## Merge Mode (Additive, Conflict-Aware)

```
User selects "Merge with local data"

↓

Confirm Dialog:
"Merge backup into current data?
• New tasks, projects, labels, filters will be added
• Existing records with matching IDs will be SKIPPED (not overwritten)
• Conflicts (same ID, different data) will be reported
• Your current reminders, preferences, and completed tasks are preserved"

[Cancel]  [Merge]
```

### Merge Execution

```
1. Begin DB transaction

2. For each entity type (in FK order):
   
   For each record in backup:
     IF record.id NOT EXISTS locally:
         INSERT
     ELSE:
         // Conflict: same ID, different data
         COMPARE all fields (except updated_at)
         IF identical: SKIP (count as "unchanged")
         ELSE: RECORD CONFLICT (do not overwrite)

3. Commit transaction

4. Build Conflict Report:
   {
     "skipped": 15,
     "conflicts": [
       { "entity": "task", "id": "uuid", "local": {...}, "backup": {...} },
       { "entity": "label", "id": "uuid", "local": {...}, "backup": {...} }
     ],
     "added": { "tasks": 42, "projects": 3, ... }
   }

5. Show result dialog:
   "Merge complete.
   • 42 tasks added
   • 3 projects added
   • 15 records unchanged (already exist)
   • 2 conflicts detected — review below"

   [View Conflicts]  [Done]
```

### Conflict Resolution (Post-Merge)

```
User taps "View Conflicts"

↓

Conflict Resolution Screen:
┌────────────────────────────────────────┐
│ Conflicts (2)                     Done │
├────────────────────────────────────────┤
│ Task: "Finish report"                  │
│   ID: 550e8400-e29b...                 │
│                                        │
│   Local:  P2, due Jan 20, #Work        │
│   Backup: P1, due Jan 18, #Work, #Urgent│
│                                        │
│   [Keep Local]  [Use Backup]  [Merge]  │
├────────────────────────────────────────┤
│ Label: "Urgent"                        │
│   ID: 880e8400-e29b...                 │
│                                        │
│   Local:  Red                          │
│   Backup: Orange                       │
│                                        │
│   [Keep Local]  [Use Backup]           │
└────────────────────────────────────────┘
```

- **Keep Local**: Discard backup version; local unchanged
- **Use Backup**: Overwrite local with backup version (update `updated_at = now`)
- **Merge** (tasks only): Open Task Detail with both versions side-by-side; user edits and saves

### Merge Rules by Entity

| Entity | Merge Behavior |
|--------|----------------|
| Projects | Add if new ID; conflict if same ID different data |
| Sections | Add if new ID; conflict if same ID |
| Tasks | Add if new ID; conflict if same ID (see resolution) |
| Subtasks | Add if new ID; conflict if same ID |
| Labels | Add if new ID; conflict if same ID different name/color |
| Filters | Add if new ID; conflict if same ID different query |
| Reminders | Add if new ID; conflict if same ID (compare all fields) |
| Attachments | Add if new ID; conflict if same ID (compare URI) |
| Activities | **Never merged** (local-only history) |
| Notifications | **Never merged** (local-only) |
| Preferences | **Never merged** (user's current preferences preserved) |

---

# Import Validation Details

## Version Compatibility

| Backup Version | Current App Version | Import Allowed? |
|----------------|---------------------|-----------------|
| v1 | v1.3 | Yes (migrate) |
| v2 | v1.3 | Yes (migrate) |
| v3 | v1.3 | Yes (migrate) |
| v4 | v1.3 | Yes (native) |
| v5 | v1.3 | **No** (future version) |

- App can import **older or equal** versions only
- Migration logic for v1→v4, v2→v4, v3→v4 built into importer
- Future versions: show "This backup is from a newer MyDo version. Please update the app."

## Referential Integrity Checks (Pre-Import)

```
For each task:
  - project_id → exists in backup.projects OR exists locally (merge)
  - section_id → exists in backup.sections for that project
  - parent_task_id → exists in backup.tasks
  - recurring_rule → valid RRULE syntax (if present)

For each section:
  - project_id → exists in backup.projects

For each reminder:
  - task_id → exists in backup.tasks

For each attachment:
  - task_id → exists in backup.tasks

For each label link (task_labels):
  - task_id + label_id → both exist
```

Failure on any check → **Reject import** with specific error.

---

# SAF Integration Details

## Export: ACTION_CREATE_DOCUMENT

```kotlin
val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/gzip"
    putExtra(Intent.EXTRA_TITLE, "mydo-backup-${DateTimeFormatter.ISO_DATE.format(Instant.now())}.json.gz")
    putExtra(Intent.EXTRA_INITIAL_URI, getDefaultBackupDirectory())
}
startActivityForResult(intent, REQUEST_EXPORT)
```

## Import: ACTION_OPEN_DOCUMENT

```kotlin
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/gzip"
    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/gzip", "application/json"))
}
startActivityForResult(intent, REQUEST_IMPORT)
```

## URI Permission Handling

```kotlin
// Export: grant persistable permission for the created URI
contentResolver.takePersistableUriPermission(
    uri,
    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
)

// Import: read permission auto-granted by SAF for returned URI
// No persist needed (one-time read)
```

## Default Backup Directory

```kotlin
fun getDefaultBackupDirectory(): Uri {
    // Try Downloads/MyDo first
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val myDoDir = File(downloads, "MyDo")
    if (!myDoDir.exists()) myDoDir.mkdirs()
    return Uri.fromFile(myDoDir) // For EXTRA_INITIAL_URI hint only
}
```

---

# Attachment Handling in Backup

## Export

- **Metadata only** (id, taskId, filename, mimeType, size, localUri)
- File content **NOT** included (too large; user manages files)
- `localUri` is the SAF `content://` URI at export time

## Import

- Metadata restored to DB
- Attempt to re-grant URI permission for each attachment:

```kotlin
attachments.forEach { attachment ->
    try {
        contentResolver.takePersistableUriPermission(
            Uri.parse(attachment.localUri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        attachment.status = "OK"
    } catch (e: SecurityException) {
        attachment.status = "BROKEN_PERMISSION"
    } catch (e: FileNotFoundException) {
        attachment.status = "FILE_MISSING"
    }
}
```

- Broken attachments shown with warning icon in Task Detail
- User can re-attach file to fix

---

# Preferences in Backup

## Exported

All preference categories (general, notifications, appearance, productivity, privacy)

## Import Behavior

| Mode | Behavior |
|------|----------|
| Replace | All preferences overwritten from backup |
| Merge | **Preferences never merged** — local preferences always preserved |

---

# Activity & Notification History in Backup

## Export

- All `ActivityEvent` records (full history)
- All `Notification` records (read/unread state)

## Import

| Mode | Behavior |
|------|----------|
| Replace | Full history replaced |
| Merge | **Never merged** — local history preserved (activities/notifications are device-specific) |

---

# Security & Privacy

- Backup file contains **all user data** — treat as sensitive
- No encryption by default (user controls file destination)
- Optional: "Encrypt backup" toggle in Export flow (AES-256, user-provided passphrase, PBKDF2)
- Import validates checksum before any decryption
- No data transmitted; SAF ensures user controls file location
- App cannot access files outside granted URIs

---

# Error States

## Export Errors

| Error | Message | Recovery |
|-------|---------|----------|
| DB locked | "Database busy. Try again." | Retry |
| No storage space | "Not enough space for backup" | Free space, retry |
| SAF permission denied | "Cannot access selected location" | Choose different location |
| Serialization error | "Failed to create backup" | Report bug |

## Import Errors

| Error | Message | Recovery |
|-------|---------|----------|
| Invalid JSON | "Backup file is corrupted" | Try another backup |
| Wrong version | "Backup from newer MyDo version" | Update app |
| Checksum mismatch | "Backup integrity check failed" | File may be damaged |
| Missing required fields | "Backup format not recognized" | Try another backup |
| Referential integrity fail | "Backup has invalid references" | Try another backup |
| SAF read denied | "Cannot read selected file" | Grant permission, retry |
| DB transaction fail | "Import failed; data unchanged" | Retry, or restore precautionary backup |

---

# Testing Requirements

## Export Tests

- Export with 0 tasks (empty DB) → valid minimal backup
- Export with 50k tasks, 10k attachments → completes < 30s, file size reasonable
- Export → verify checksum matches computed
- Export → import → data identical
- Export cancelled mid-way → no partial file left
- Export to various SAF locations (Downloads, Drive, USB OTG, network share)

## Import Tests

- Import valid v4 backup (replace) → data matches
- Import valid v4 backup (merge, no conflicts) → data added
- Import valid v4 backup (merge, with conflicts) → conflicts reported, local preserved
- Import v1/v2/v3 backup → migrated correctly
- Import v5 backup → rejected with version message
- Import corrupted JSON → rejected, DB unchanged
- Import checksum mismatch → rejected, DB unchanged
- Import with broken FK refs → rejected, DB unchanged
- Import cancelled mid-way → DB unchanged (transaction rollback)
- Import with broken attachment URIs → imported as BROKEN_PERMISSION/FILE_MISSING
- Import replace → precautionary backup created and restorable

## Edge Cases

- Reboot during export/import → no corruption
- App killed during import → transaction rolls back
- Import same backup twice (replace) → idempotent
- Import same backup twice (merge) → second time: all skipped/conflicts
- Merge with 500 conflicts → conflict screen usable
- Timezone change between export/import → dates preserved as UTC

---

# Performance Requirements

| Operation | Target |
|-----------|--------|
| Export 10k tasks | < 10s |
| Export 50k tasks | < 30s |
| Import 10k tasks (replace) | < 5s |
| Import 10k tasks (merge) | < 8s |
| Checksum compute (50k tasks) | < 2s |
| Streaming parse (no full JSON in memory) | < 100MB heap |

---

# Accessibility

- Export/Import buttons: descriptive labels ("Export backup", "Import backup")
- Progress announced: "Exporting backup, 45 percent complete"
- Confirm dialogs: focus trap, clear "Cancel" button
- Conflict resolution: screen reader reads both versions
- Error messages: specific, actionable
- File picker: system SAF (accessible by default)

---

# Navigation Summary

```
Settings → Data
├── Export local database
│   ├── Progress
│   ├── SAF Save Picker
│   └── Success/Error
│
├── Import local database
│   ├── SAF Open Picker
│   ├── Validation
│   ├── Preview Dialog
│   │   ├── Replace → Confirm → Execute → Success
│   │   └── Merge → Confirm → Execute → Conflict Report (optional)
│   └── Success/Error
│
├── Restore previous backup (precautionary)
│   └── Confirm → Replace → Success
│
└── Clear local data
    └── Confirm (with export offer) → Execute
```

---

# Success Criteria

The Backup/Export/Import feature succeeds when users can:

- Create a complete, verified backup of all MyDo data in < 30 seconds
- Save the backup to any location they choose (local, cloud, USB)
- Restore a backup on the same or different device
- Choose between full replace and safe merge
- Never lose data due to a failed or cancelled import
- See clear explanations of what will happen before confirming
- Recover from a mistaken import using the precautionary backup
- Trust that attachments, reminders, and preferences are included
- Perform all operations offline