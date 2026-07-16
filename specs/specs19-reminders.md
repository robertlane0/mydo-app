# Reminders Specification

## Purpose

MyDo schedules local Android reminders for tasks with due dates/times and recurring rules. Reminders are local device notifications only; MyDo has no push service, server scheduling, or cloud delivery. Reminders are scheduled only after the user grants the required notification permission.

---

# Goals

- Schedule local reminders at the correct local time for each task due date/time and recurrence
- Request and respect Android notification permission (`POST_NOTIFICATIONS` on Android 13+)
- Support notification actions: **Open**, **Complete**, **Snooze**
- Reschedule reminders after task edits, recurrence completion, device reboot, and app update
- Persist reminder configuration locally; include in manual backup/import
- Work fully offline; no network required for scheduling or delivery

---

# Data Model (from specs11-data-model.md)

| Property | Type | Description |
|----------|------|-------------|
| id | UUID | Unique identifier |
| taskId | UUID | Parent task |
| triggerTime | DateTime | When the reminder should fire (UTC) |
| type | Enum | `AT_TIME`, `BEFORE_DUE` (e.g., 30 min before), `RECURRING` |
| enabled | Boolean | User can disable individual reminders |
| offsetMinutes | Integer? | For `BEFORE_DUE`: minutes before due time |

---

# Navigation

```
Settings → Notifications → Reminder Defaults
Task Detail → Reminder Row → Reminder Editor
Notification Shade → Actions (Open, Complete, Snooze)
```

---

# Permission Model

## Android 13+ (API 33+)

- **POST_NOTIFICATIONS** runtime permission required before any reminder can be shown
- MyDo requests permission:
  - On first launch (if targeting API 33+)
  - When user enables reminders in Settings → Notifications
  - When user adds first reminder to a task
- If permission denied: reminders are saved locally but not scheduled; UI shows inline notice with "Open Settings" action

## Android 12 and below

- No runtime permission needed; notifications work by default
- User can still disable via system app notification settings

---

# Reminder Editor (Task Detail → Reminder Row)

## Layout

```
┌─────────────────────────────────────┐
│ Reminder                      ✕     │
├─────────────────────────────────────┤
│ ✓ Enabled                           │
├─────────────────────────────────────┤
│ Type: [At due time           ▼]     │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ 🔔 At time of task              │ │  ← when "At due time"
│ │ 🔔 30 minutes before            │ │
│ │ 🔔 1 hour before                │ │
│ │ 🔔 1 day before                 │ │
│ │ 🔔 Custom…                      │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ For recurring tasks:                │
│ ☐ Apply to all future occurrences   │
│ [Save]                      [Remove]│
└─────────────────────────────────────┘
```

## Type Options

| Type | Trigger Time | Use Case |
|------|--------------|----------|
| `AT_TIME` | Task's `dueDate` + `dueTime` | "Remind me at 2 PM" |
| `BEFORE_DUE` | `dueDateTime - offset` | "30 min before", "1 day before" |
| `CUSTOM` | User-specified date/time | "Custom reminder" |

---

# Notification Content

## Content Fields

| Field | Value |
|-------|-------|
| Title | Task title |
| Text | Due date/time, project name (if any), priority indicator |
| Large Icon | App icon |
| Small Icon | Bell icon (reminder) or check icon (if overdue) |
| Color | Priority color (P1=Red, P2=Orange, P3=Blue, P4=Grey) |
| Category | `CATEGORY_REMINDER` |
| Priority | `PRIORITY_HIGH` (heads-up) for due now; `PRIORITY_DEFAULT` for before-due |
| Visibility | `VISIBILITY_PUBLIC` (shows on lock screen per user settings) |

## Actions (Android 7.0+)

| Action | Intent Action | Behavior |
|--------|---------------|----------|
| **Open** | `MYDO_ACTION_OPEN_TASK` | Opens Task Detail for `taskId` |
| **Complete** | `MYDO_ACTION_COMPLETE_TASK` | Marks task complete locally; cancels reminder; generates next recurrence if applicable |
| **Snooze** | `MYDO_ACTION_SNOOZE` | Reschedules reminder +10 min (configurable); shows confirmation toast |

### Snooze Behavior

```
User taps Snooze

↓

Calculate new trigger = now + 10 minutes (configurable in Settings)

↓

Cancel current notification

↓

Schedule new reminder with same taskId, type=AT_TIME, triggerTime=new time

↓

Show toast: "Snoozed 10 minutes [Undo]"
```

- Snooze creates a **one-time** reminder; does not affect original reminder config
- Multiple snoozes stack (each adds 10 min from snooze time)
- Undo within 5 seconds cancels snooze and restores original

---

# Scheduling Logic

## Initial Schedule

```
Task Created/Edited with Reminder

↓

If notification permission GRANTED:
    Calculate triggerTime (UTC) from task dueDate/dueTime + reminder type
    Schedule via AlarmManager.setExactAndAllowWhileIdle() (API 23+)
    or AlarmManager.setExact() (API 19-22)
    Store alarmId in Reminder record for cancellation

Else:
    Save reminder locally with enabled=true
    Show inline notice: "Enable notifications to receive reminders"
```

## Trigger Time Calculation

| Reminder Type | Calculation |
|---------------|-------------|
| `AT_TIME` | `dueDate` at `dueTime` (user's local timezone → UTC) |
| `BEFORE_DUE` | `dueDateTime - offsetMinutes` |
| `CUSTOM` | User-specified date/time → UTC |

- All times stored in UTC; calculation uses user's current timezone
- Recurring tasks: reminder scheduled for **current occurrence only**
- Overdue tasks (`dueDateTime < now`): if reminder enabled, schedule for **now + 1 minute** (immediate heads-up)

---

# Rescheduling Triggers

| Trigger | Action |
|---------|--------|
| **Task edited** (due date/time changed) | Cancel old alarm; schedule new if reminder enabled and permission granted |
| **Task completed** (recurring) | Cancel current reminder; schedule for next occurrence due date/time |
| **Task completed** (non-recurring) | Cancel reminder; mark reminder `enabled=false` |
| **Reminder toggled off** | Cancel alarm; `enabled=false` |
| **Reminder toggled on** | Schedule new alarm if permission granted |
| **Notification permission granted** | Reschedule all enabled reminders for incomplete tasks |
| **Notification permission revoked** | Cancel all scheduled alarms; keep reminders `enabled=true` locally |
| **Device reboot** (`BOOT_COMPLETED`) | On boot: reschedule all enabled reminders for incomplete tasks |
| **App update** (`MY_PACKAGE_REPLACED`) | On update: reschedule all enabled reminders for incomplete tasks |
| **Timezone change** (`TIMEZONE_CHANGED`) | Reschedule all enabled reminders using new timezone |
| **Locale change** (`LOCALE_CHANGED`) | Reschedule all enabled reminders (timezone may have changed) |
| **Recurrence completed** (in Task Detail) | Generate next occurrence; schedule its reminder per original reminder config |

---

# Boot & Update Receivers

## BOOT_COMPLETED Receiver

```xml
<!-- AndroidManifest.xml -->
<receiver
    android:name=".platform.BootReceiver"
    android:exported="true"
    android:enabled="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.QUICKBOOT_POWERON" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
        <action android:name="android.intent.action.LOCALE_CHANGED" />
    </intent-filter>
</receiver>
```

```kotlin
// BootReceiver.kt
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_QUICKBOOT_POWERON -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<RescheduleRemindersWorker>()
                        .setInitialDelay(30, TimeUnit.SECONDS)
                        .build()
                )
            }
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
                )
            }
        }
    }
}
```

## MY_PACKAGE_REPLACED Receiver

```xml
<receiver
    android:name=".platform.PackageUpdateReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
</receiver>
```

```kotlin
class PackageUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
        )
    }
}
```

---

# RescheduleRemindersWorker

```kotlin
class RescheduleRemindersWorker(
    context: Context,
    params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminders = reminderRepository.getAllEnabledForIncompleteTasks()
        val granted = NotificationPermissionHelper.isGranted(applicationContext)

        if (!granted) {
            alarmScheduler.cancelAll()
            return Result.success()
        }

        reminders.forEach { reminder ->
            val task = taskRepository.getById(reminder.taskId)
            if (task != null && !task.completed) {
                val triggerTime = calculateTriggerTime(reminder, task)
                if (triggerTime > Instant.now()) {
                    alarmScheduler.schedule(reminder.id, triggerTime)
                } else {
                    // Overdue: schedule immediate
                    alarmScheduler.schedule(reminder.id, Instant.now().plusSeconds(60))
                }
            }
        }
        return Result.success()
    }
}
```

---

# AlarmScheduler

```kotlin
interface AlarmScheduler {
    fun schedule(reminderId: UUID, triggerTime: Instant)
    fun cancel(reminderId: UUID)
    fun cancelAll()
}

class AlarmSchedulerImpl @Inject constructor(
    @Suppress("UNUSED_PARAMETER") context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val pendingIntentFactory = PendingIntentFactory(context)

    override fun schedule(reminderId: UUID, triggerTime: Instant) {
        val triggerMillis = triggerTime.toEpochMilli()
        val pendingIntent = pendingIntentFactory.createReminderIntent(reminderId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(reminderId: UUID) {
        val pendingIntent = pendingIntentFactory.createReminderIntent(reminderId)
        alarmManager.cancel(pendingIntent)
    }

    override fun cancelAll() {
        val allIntents = pendingIntentFactory.createAllReminderIntents()
        allIntents.forEach { alarmManager.cancel(it) }
    }
}
```

---

# Notification Receiver & Actions

## ReminderReceiver (BroadcastReceiver for alarm)

```kotlin
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = UUID.fromString(intent.getStringExtra(EXTRA_REMINDER_ID))
        val taskId = UUID.fromString(intent.getStringExtra(EXTRA_TASK_ID))

        // Check permission again (user may have revoked since scheduling)
        if (!NotificationPermissionHelper.isGranted(context)) {
            MissedReminderRepository.recordMissed(reminderId, Instant.now())
            return
        }

        // Build and show notification
        NotificationBuilder.buildAndShow(context, reminderId, taskId)
    }
}
```

## NotificationActionReceiver (handles Open/Complete/Snooze)

```kotlin
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = UUID.fromString(intent.getStringExtra(EXTRA_TASK_ID))
        val reminderId = UUID.fromString(intent.getStringExtra(EXTRA_REMINDER_ID))

        when (action) {
            MYDO_ACTION_OPEN_TASK -> {
                NotificationManagerCompat.from(context).cancel(reminderId.hashCode())
                val intent = TaskDetailActivity.newIntent(context, taskId)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            MYDO_ACTION_COMPLETE_TASK -> {
                CompleteTaskUseCase(taskRepository).execute(taskId)
                AlarmSchedulerImpl.cancel(reminderId)
                Toast.makeText(context, "Task completed [Undo]", Toast.LENGTH_LONG).show()
            }
            MYDO_ACTION_SNOOZE -> {
                val snoozeMinutes = Preferences.getSnoozeMinutes(context) // default 10
                val newTrigger = Instant.now().plus(snoozeMinutes, ChronoUnit.MINUTES)
                AlarmSchedulerImpl.schedule(reminderId, newTrigger)
                NotificationBuilder.updateForSnooze(context, reminderId, newTrigger)
                Toast.makeText(context, "Snoozed $snoozeMinutes min [Undo]", Toast.LENGTH_LONG).show()
            }
        }
    }
}
```

---

# Settings Integration (from specs10-settings.md)

## Notifications Screen

```
Notifications
├── Task Reminders          [ON/OFF]  → Master toggle; requests permission if off→on
├── Default Reminder        [At due time ▼]  → Default for new tasks
├── Snooze Duration         [10 min ▼]  → 5/10/15/30/60 min
├── Daily Summary           [OFF]  → Optional daily digest (local only)
└── Notification Permission  [Granted/Denied]  → Opens system settings if denied
```

---

# Missed Reminders

- If reminder fires but permission denied or app killed: record in `MissedReminder` table
- On next app launch or permission grant: show "Missed reminders" snackbar with "View" action
- View opens filtered task list: `reminderMissed:true`

---

# Recurring Task Reminder Handling

From specs16-recurring-tasks.md:

```
Completing Recurring Task

↓

Generate Next Occurrence

↓

Copy Reminder Config from Completed Task

↓

Schedule Reminder for New Occurrence

↓

If permission denied: save enabled=true locally; schedule on grant
```

- Reminder config (type, offset, enabled) copied to next occurrence
- Snoozed reminders **not** copied (one-time only)

---

# Error States

| Scenario | Behavior |
|----------|----------|
| `AlarmManager` throws `SecurityException` | Log error; show "Could not schedule reminder" snackbar; keep reminder enabled locally |
| `PendingIntent` creation fails | Log error; retry on next app start |
| `NotificationManager` throws (channel deleted) | Recreate channel; retry |
| Database error saving reminder | Show "Could not save reminder" snackbar; do not schedule |
| Timezone data unavailable | Fall back to UTC; log warning |

---

# Backup/Export (from specs20-backup-export-import.md)

Reminders are included in manual backup:

```json
{
  "reminders": [
    {
      "id": "uuid",
      "taskId": "uuid",
      "triggerTime": "2026-01-15T14:30:00Z",
      "type": "BEFORE_DUE",
      "offsetMinutes": 30,
      "enabled": true
    }
  ]
}
```

On import:
- Reminders imported with `enabled=true`
- If permission granted: schedule immediately
- If permission denied: save enabled; schedule on grant

---

# Testing Requirements

| Scenario | Expected |
|----------|----------|
| Set reminder, grant permission, wait for trigger | Notification appears with Open/Complete/Snooze |
| Set reminder, deny permission, then grant in settings | Reminder scheduled immediately |
| Complete recurring task | Next occurrence reminder scheduled |
| Reboot device | All enabled reminders rescheduled |
| Update app | All enabled reminders rescheduled |
| Change timezone | Reminders fire at correct local time |
| Snooze 3 times | Each snooze adds 10 min; original reminder config unchanged |
| Complete task via notification | Task marked complete; notification dismissed; undo works |
| Permission revoked after scheduling | Alarms cancelled; reminders stay enabled locally |

---

# Performance Requirements

| Operation | Target |
|-----------|--------|
| Schedule single reminder | < 50ms |
| Reschedule all (boot/update) | < 2s for 500 reminders |
| Notification build/show | < 100ms |
| Complete via notification | < 200ms |

---

# Accessibility

- Notification content read by TalkBack: task title, due time, actions
- Actions announced: "Open task", "Complete task", "Snooze 10 minutes"
- Snooze duration configurable; announced in action
- In-app reminder editor: labels, hints, focus order
- Permission rationale screen: clear explanation, "Open Settings" button

---

# Navigation Summary

```
Task Detail
└── Reminder Editor
    ├── Type Picker
    ├── Offset Picker (for BEFORE_DUE)
    ├── Custom Date/Time Picker
    ├── Apply to Future Toggle (recurring)
    └── Save / Remove

Settings → Notifications
├── Master Toggle
├── Default Reminder
├── Snooze Duration
├── Daily Summary
└── Permission Status

Notification Shade
└── Actions: Open | Complete | Snooze
```

---

# Success Criteria

The Reminders feature succeeds when users can:

- Set a reminder for any task in < 10 seconds
- Trust that reminders fire at the correct local time
- Act on reminders directly from the notification shade
- Snooze and complete tasks without opening the app
- Survive reboots, updates, and timezone changes
- Include reminders in manual backups
- Use reminders fully offline