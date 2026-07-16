# Platform Integration Specification

## Purpose

MyDo integrates with Android platform features to provide a native, local-first experience. This specification covers app shortcuts, widgets, boot/update handling, Storage Access Framework (SAF) for attachments and backups, notification channels, and offline verification.

---

# Goals

- Provide quick task capture via App Shortcuts and home screen widget
- Survive device reboot and app updates with reminders intact
- Use SAF for all file operations (attachments, backups) — no `MANAGE_EXTERNAL_STORAGE`
- Verify all workflows function with networking disabled
- Respect Android lifecycle, permissions, and scoped storage

---

# App Shortcuts (Android 7.1+)

## Static Shortcuts (res/xml/shortcuts.xml)

```xml
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="new_task"
        android:enabled="true"
        android:icon="@drawable/ic_add"
        android:shortcutShortLabel="@string/shortcut_new_task"
        android:shortcutLongLabel="@string/shortcut_new_task_long">
        <intent
            android:action="com.mydo.action.NEW_TASK"
            android:targetPackage="com.mydo"
            android:targetClass="com.mydo.ui.taskcomposer.TaskComposerActivity" />
        <categories android:name="android.shortcut.conversation" />
    </shortcut>

    <shortcut
        android:shortcutId="new_task_inbox"
        android:enabled="true"
        android:icon="@drawable/ic_inbox"
        android:shortcutShortLabel="@string/shortcut_new_inbox"
        android:shortcutLongLabel="@string/shortcut_new_inbox_long">
        <intent
            android:action="com.mydo.action.NEW_TASK"
            android:targetPackage="com.mydo"
            android:targetClass="com.mydo.ui.taskcomposer.TaskComposerActivity">
            <extra android:name="default_project" android:value="inbox" />
        </intent>
    </shortcut>

    <shortcut
        android:shortcutId="search"
        android:enabled="true"
        android:icon="@drawable/ic_search"
        android:shortcutShortLabel="@string/shortcut_search"
        android:shortcutLongLabel="@string/shortcut_search_long">
        <intent
            android:action="com.mydo.action.SEARCH"
            android:targetPackage="com.mydo"
            android:targetClass="com.mydo.ui.search.SearchActivity" />
    </shortcut>

    <shortcut
        android:shortcutId="today"
        android:enabled="true"
        android:icon="@drawable/ic_today"
        android:shortcutShortLabel="@string/shortcut_today"
        android:shortcutLongLabel="@string/shortcut_today_long">
        <intent
            android:action="com.mydo.action.OPEN_TODAY"
            android:targetPackage="com.mydo"
            android:targetClass="com.mydo.ui.main.MainActivity">
            <extra android:name="destination" android:value="today" />
        </intent>
    </shortcut>
</shortcuts>
```

## Dynamic Shortcuts

- **Recent projects** (up to 4): Updated when user opens a project
- **Recent labels** (up to 2): Updated when user filters by label
- **Pinned shortcuts**: User can pin "New Task" to launcher

```kotlin
// Update dynamic shortcuts on project open
fun updateProjectShortcuts(projects: List<Project>) {
    val shortcuts = projects.take(4).map { project ->
        ShortcutInfoCompat.Builder(context, "project_${project.id}")
            .setShortLabel(project.name)
            .setLongLabel("Open ${project.name}")
            .setIcon(IconCompat.createWithResource(context, project.iconRes))
            .setIntent(Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_PROJECT)
                .putExtra(EXTRA_PROJECT_ID, project.id.toString()))
            .setRank(projects.indexOf(project))
            .build()
    }
    ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
}
```

---

# Home Screen Widget (Android 12+, Glance)

## Widget Types

| Widget | Size | Content |
|--------|------|---------|
| **Quick Add** | 2×1 | Large "+" button; tap opens Task Composer |
| **Today List** | 4×2 | Up to 5 tasks due today; tap opens task |
| **Upcoming** | 4×3 | Next 7 days, 3 tasks/day; tap date opens Upcoming |
| **Project** | 4×2 | Tasks from pinned project; configurable per instance |

## Quick Add Widget (Glance)

```kotlin
@Composable
fun QuickAddWidget() {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(actionStartActivity<TaskComposerActivity>(
                    Intent(context, TaskComposerActivity::class.java)
                ))
        ) {
            Text(
                text = "+",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
        }
        Text("Add Task", fontSize = 14.sp, color = MaterialTheme.colors.onSurface)
    }
}
```

## Today List Widget

- Shows incomplete tasks with `dueDate == today` OR `dueDate < today` (overdue)
- Max 5 tasks; "Show more" opens Today screen
- Updates every 30 min via `WorkManager` + `GlanceAppWidget.update()`
- Tap task → open Task Detail
- Empty state: "Nothing due today. + Add task"

## Widget Configuration

- Long-press widget → "Configure" → select project for Project widget
- Settings → Widgets → "Default widget project", "Show completed in widget"

---

# Boot & Update Handling (Detailed)

## Broadcast Receivers

| Receiver | Actions | Purpose |
|----------|---------|---------|
| `BootReceiver` | `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `TIMEZONE_CHANGED`, `LOCALE_CHANGED` | Reschedule reminders, sync widget |
| `PackageUpdateReceiver` | `MY_PACKAGE_REPLACED` | Reschedule reminders, migrate DB if needed |
| `TimeZoneReceiver` | `TIMEZONE_CHANGED` | Reschedule reminders, refresh widget |
| `LocaleReceiver` | `LOCALE_CHANGED` | Update date formatting, refresh widget |

## WorkManager for Deferred Work

```kotlin
// Reschedule reminders after boot/update
@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val reminders = reminderRepository.getAllEnabledForIncompleteTasks()
        val permission = NotificationPermissionHelper.isGranted(applicationContext)

        if (!permission) {
            alarmScheduler.cancelAll()
            return Result.success()
        }

        reminders.forEach { reminder ->
            val task = taskRepository.getById(reminder.taskId)
            if (task != null && !task.completed) {
                val trigger = calculateTrigger(reminder, task)
                if (trigger > Instant.now()) alarmScheduler.schedule(reminder.id, trigger)
                else alarmScheduler.schedule(reminder.id, Instant.now().plusSeconds(60))
            }
        }
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

// Widget refresh after boot
@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        GlanceAppWidget.updateAll(context, TodayWidget::class.java)
        GlanceAppWidget.updateAll(context, UpcomingWidget::class.java)
        Result.success()
    }
}
```

## Boot Sequence

```
Device Boots
    ↓
BOOT_COMPLETED received
    ↓
Enqueue RescheduleRemindersWorker (30s delay)
Enqueue WidgetRefreshWorker (immediate)
    ↓
Workers run when system ready
    ↓
Reminders scheduled, widgets updated
```

---

# Storage Access Framework (SAF) Integration

## Attachments (from specs15-attachments.md)

### Document Picker (Add Attachment)

```kotlin
// Intent
Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "*/*"
    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    putExtra(Intent.EXTRA_TITLE, "Select files to attach")
    flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
}

// Result handling
override fun onActivityResult(requestCode, resultCode, data) {
    if (resultCode == RESULT_OK) {
        val uris = if (data.clipData != null) {
            (0 until data.clipData.itemCount).map { data.clipData.getItemAt(it).uri }
        } else {
            listOf(data.data!!)
        }
        uris.forEach { uri ->
            // Persist permission
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Query metadata
            val metadata = queryDocumentMetadata(uri)
            // Save to DB
            attachmentRepository.insert(Attachment(
                taskId = currentTaskId,
                filename = metadata.displayName,
                mimeType = metadata.mimeType,
                size = metadata.size,
                localUri = uri.toString()
            ))
        }
    }
}
```

### Open Attachment

```kotlin
fun openAttachment(attachment: Attachment) {
    val uri = Uri.parse(attachment.localUri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        showSnackbar("No app can open this file")
    } catch (e: SecurityException) {
        // Permission lost - re-request
        requestPermissionAndOpen(uri)
    }
}
```

### URI Permission Persistence

- Call `takePersistableUriPermission()` immediately after picker result
- Survives app restart, device reboot
- If lost (file moved/deleted): show "File not found" → offer to remove attachment
- Release on attachment removal: `releasePersistableUriPermission()`

## Backups (from specs20-backup-export-import.md)

### Export: ACTION_CREATE_DOCUMENT

```kotlin
val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/gzip"
    putExtra(Intent.EXTRA_TITLE, "mydo-backup-${date}.json.gz")
    putExtra(Intent.EXTRA_INITIAL_URI, getDefaultBackupDir())
}
startActivityForResult(intent, REQUEST_EXPORT)
```

### Import: ACTION_OPEN_DOCUMENT

```kotlin
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/gzip"
    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/gzip", "application/json"))
}
startActivityForResult(intent, REQUEST_IMPORT)
```

### Default Backup Directory

```kotlin
fun getDefaultBackupDir(): Uri {
    // Try to get Downloads/MyDo
    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val myDoDir = File(downloads, "MyDo").apply { mkdirs() }
    return Uri.parse(myDoDir.toString())
}
```

---

# Notification Channels (Android 8.0+)

## Channel Definitions

```kotlin
object NotificationChannels {
    const val REMINDERS = "reminders"
    const val SYSTEM = "system"
    const val DAILY_SUMMARY = "daily_summary"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // Reminders - high importance, heads-up
        val reminders = NotificationChannel(
            REMINDERS,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for tasks with due dates"
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
        }

        // System - default importance
        val system = NotificationChannel(
            SYSTEM,
            "System Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Important app messages (import errors, etc.)"
            setShowBadge(true)
        }

        // Daily Summary - low importance, no heads-up
        val daily = NotificationChannel(
            DAILY_SUMMARY,
            "Daily Summary",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Optional daily task summary"
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(reminders, system, daily))
    }
}
```

## Channel Settings UI

```
Settings → Notifications
├── Task Reminders (channel: reminders)
│   ├── Enable/Disable → setChannelEnabled()
│   ├── Sound → setSound()
│   ├── Vibration → setVibrationPattern()
│   └── Lock screen visibility → setLockscreenVisibility()
├── Daily Summary (channel: daily_summary)
│   └── Enable/Disable
└── System (channel: system) - always enabled
```

---

# Offline Verification

## Requirements

All core workflows must function with **networking completely disabled** (Airplane mode, no Wi-Fi, no mobile data).

## Verified Workflows

| Workflow | Must Work Offline |
|----------|-------------------|
| Create/edit/complete task | ✓ |
| Create/edit/delete project, section | ✓ |
| Add/remove labels, filters | ✓ |
| Set/change due dates, reminders | ✓ |
| Add/remove attachments (local files) | ✓ |
| Search tasks, projects, labels | ✓ |
| Upcoming/Today/Inbox views | ✓ |
| Recurring task completion | ✓ |
| Bulk operations | ✓ |
| Drag/reorder | ✓ |
| Export backup (to local storage) | ✓ |
| Import backup (from local storage) | ✓ |
| Notification reminders | ✓ |
| Widget updates | ✓ |
| App shortcuts | ✓ |
| Settings changes | ✓ |

## Test Procedure

```bash
# Manual test checklist
1. Enable Airplane mode
2. Disable Wi-Fi and Mobile Data
3. Launch MyDo
4. Perform each workflow above
5. Verify no "network error" messages
6. Verify all changes persist after app restart
7. Verify reminders still fire (alarm-based, not push)
```

## Network-Independent Design

- No Firebase, no Analytics, no Crashlytics (or all optional + disabled by default)
- No license checks, no feature flags from server
- All images/icons bundled in APK
- NLP for task parsing runs on-device (ML Kit or custom regex)
- No remote config, no A/B testing

---

# Android Version Compatibility

| Feature | Min API | Notes |
|---------|---------|-------|
| Core app (Compose, Room, WorkManager) | 24 (Android 7.0) | |
| App Shortcuts (static) | 25 (7.1) | |
| App Shortcuts (dynamic) | 25 | |
| Notification Channels | 26 (8.0) | Required for reminders |
| SAF (ACTION_OPEN_DOCUMENT) | 19 (4.4) | Full support |
| Persistable URI permissions | 19 | |
| Scoped Storage | 29 (10) | SAF bypasses |
| Exact Alarms (setExactAndAllowWhileIdle) | 23 (6.0) | Fallback to setExact on 19-22 |
| POST_NOTIFICATIONS permission | 33 (13) | Runtime request |
| Glance Widgets | 26 (8.0) | Via Jetpack Glance |
| WorkManager | 23 | |
| ML Kit (on-device NLP) | 21 | Optional |

---

# Permissions Summary

| Permission | Protection | When Requested |
|------------|------------|----------------|
| `POST_NOTIFICATIONS` | Runtime (API 33+) | First launch, or when enabling reminders |
| `FOREGROUND_SERVICE` (dataSync) | Normal | WorkManager expedited work (export) |
| `RECEIVE_BOOT_COMPLETED` | Normal | Manifest (auto-granted) |
| `SCHEDULE_EXACT_ALARM` | Special (API 31+) | When scheduling first reminder |
| `USE_EXACT_ALARM` | Normal (API 31+) | Manifest for exact alarms |

### Special Permission: SCHEDULE_EXACT_ALARM (API 31+)

```kotlin
// Check before scheduling exact alarms
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    if (!alarmManager.canScheduleExactAlarms()) {
        // Request via Settings
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        startActivityForResult(intent, REQUEST_EXACT_ALARM)
    }
}
```

---

# Lifecycle & Process Death Handling

## State Restoration

| Scenario | Restored State |
|----------|----------------|
| Process death (low memory) | Navigation stack, scroll position, search query, task draft |
| Configuration change (rotation) | All Compose state (rememberSaveable) |
| App update | Reminders rescheduled, DB migrated if needed |
| Reboot | Reminders rescheduled, widgets refreshed |

## Task Composer Draft Persistence

```kotlin
@Composable
fun TaskComposerScreen(
    onSave: (TaskDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var projectId by rememberSaveable { mutableStateOf<UUID?>(null) }
    // ... all fields

    // Auto-save draft to DataStore on every change
    LaunchedEffect(title, description, dueDate, projectId, ...) {
        draftRepository.save(TaskDraft(...))
    }

    // On dismiss with content: show "Draft saved" snackbar with "Restore" action
}
```

---

# Accessibility Integration

- App shortcuts: content descriptions for each shortcut
- Widgets: Glance semantic annotations (`contentDescription`)
- Notification actions: `setSemanticAction()`
- SAF pickers: system-provided accessibility
- Boot/update: silent (no user-facing UI)

---

# Performance Requirements

| Operation | Target |
|-----------|--------|
| App cold start → interactive | < 800ms |
| App shortcut launch → Composer | < 300ms |
| Widget initial render | < 200ms |
| Widget periodic update | < 100ms |
| Boot reschedule (500 reminders) | < 2s |
| SAF picker launch | < 200ms |
| Attachment metadata query | < 100ms |

---

# Testing Requirements

## Platform Integration Tests

| Test | Expected |
|------|----------|
| App shortcut "New Task" → opens Composer | ✓ |
| App shortcut "Today" → opens Today screen | ✓ |
| Dynamic shortcuts update on project open | ✓ |
| Widget "Quick Add" tap → opens Composer | ✓ |
| Widget "Today" shows correct tasks | ✓ |
| Widget updates after task completion | ✓ |
| Reboot → reminders rescheduled | ✓ |
| App update → reminders rescheduled | ✓ |
| Timezone change → reminders fire at correct local time | ✓ |
| Attachment add via SAF → permission persists after reboot | ✓ |
| Attachment open → system handler launches | ✓ |
| Export to Downloads/MyDo → file readable | ✓ |
| Import from Downloads → data restored | ✓ |
| All workflows in Airplane mode | ✓ |
| POST_NOTIFICATIONS denied → reminders saved, not scheduled | ✓ |
| POST_NOTIFICATIONS granted later → pending reminders scheduled | ✓ |
| SCHEDULE_EXACT_ALARM denied → fallback to inexact (with warning) | ✓ |

---

# Navigation Summary

```
Launcher
├── App Icon (long-press) → Shortcuts: New Task, Today, Search, Inbox
├── Widget: Quick Add → Task Composer
├── Widget: Today List → Task Detail / Today Screen
├── Widget: Upcoming → Upcoming Screen
└── Widget: Project → Project Screen

Settings → Notifications
├── Notification Channels (system UI)
└── Permission Status (opens system settings)

Boot/Update (background)
├── RescheduleRemindersWorker
└── WidgetRefreshWorker
```

---

# Success Criteria

The Platform Integration feature succeeds when:

- Users can create a task from home screen in 1 tap (shortcut/widget)
- Reminders survive reboots, updates, timezone changes
- Attachments and backups use system file pickers; no permission dialogs beyond SAF
- App works identically in Airplane mode
- All Android version behaviors handled (API 24-34+)
- Accessibility works for shortcuts, widgets, notifications
- Performance targets met on low-end devices