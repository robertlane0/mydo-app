# /specs/09-notifications.md

# Notifications Specification

## Purpose

MyDo uses local device notifications to remind the user about their tasks and to report important local-database events. It has no push service, activity feed, assignment, invitation, comment, project-sharing, or account notifications.

For detailed reminder scheduling, permission handling, actions (Open/Complete/Snooze), and rescheduling logic, see **specs19-reminders.md**.

---

# Goals

- Deliver task reminders at their scheduled local time.
- Open the related local task when a reminder is selected.
- Let the user control local reminder and summary preferences.
- Keep notification history and read state in the local database when supported.
- Respect Android notification permission (`POST_NOTIFICATIONS` on API 33+).
- Support notification actions: Open, Complete, Snooze.
- Reschedule reminders after task edits, recurrence completion, reboot, and app update.

---

# Notification Types

| Type | Trigger | Specification |
|---|---|---|
| Reminder | A task's time-, date-, or location-based reminder becomes due. | specs19-reminders.md |
| System | A local event needs attention, such as an import failure or database recovery warning. | This spec |

---

# Navigation and Layout

The Notifications screen displays locally stored notices, newest first. Each item shows title, related task or local event, timestamp, and read state. Selecting a reminder opens its Task Detail; selecting a system notice opens the relevant local recovery or data screen.

See specs19-reminders.md for reminder notification content, actions, and snooze behavior.

---

# Reminder Notifications

When a reminder is due, MyDo asks the operating system to display a local notification. The notification includes task title, relevant due information, and actions such as complete, snooze, or open. Availability depends on the user's device-level notification permission.

**Detailed behavior defined in specs19-reminders.md:**
- Permission model (POST_NOTIFICATIONS runtime request)
- Notification content, priority, color, actions
- Snooze behavior (configurable duration, stackable, undo)
- Scheduling via AlarmManager (exact alarms, WorkManager for reschedule)
- Boot/update/timezone receivers and RescheduleRemindersWorker

---

# System Notifications

| Event | Notification | Action |
|-------|--------------|--------|
| Import failed | "Import failed: [reason]" | Open Settings → Data |
| Export failed | "Export failed: [reason]" | Retry |
| Database recovery | "Database recovered from backup" | Open recovery screen |
| Backup import complete | "Import complete: X tasks added" | View changes |
| Permission needed | "Enable notifications for reminders" | Open system settings |

---

# Preferences

Settings can enable or disable task reminders and daily summaries. There are no email, push, assignment, comment, mention, invitation, or shared-project preferences.

**Reminder preferences (Settings → Notifications):**
- Task Reminders master toggle (requests permission if enabling)
- Default reminder type (At due time / 30 min before / etc.)
- Snooze duration (5/10/15/30/60 min)
- Daily Summary toggle

See specs10-settings.md for full Settings structure.

---

# Read State, Deletion, and Badge

Opening a notice marks it read unless settings say otherwise. Users may mark all read or remove a local notice; doing so does not change the related task. Badge count reflects unread local notices where supported.

---

# States and Errors

An empty state says there are no local notifications. Loading reads from the local database. If local notifications cannot be scheduled because device permission is denied, explain how to enable permission in system settings. Pull-to-refresh reloads the local list and does not contact a remote service.

---

# Accessibility and Rules

Notifications expose task title, state, and actions to assistive technology. Every notification references a task or local system event. Delivery respects local preferences and device permissions. Notification data is included in manual database exports when stored by the app.

---

# Cross-References

- **specs19-reminders.md** — Reminder scheduling, actions, permission, rescheduling
- **specs10-settings.md** — Notifications settings screen
- **specs11-data-model.md** — Notification and Reminder entities
- **specs20-backup-export-import.md** — Notifications included in backup
- **specs16-recurring-tasks.md** — Reminder copied to next recurrence

---

# Success Criteria

Users reliably receive and act on task reminders without an account, connection, synchronization service, or data sent to MyDo. Reminders survive reboots, updates, and timezone changes. Snooze and Complete actions work from the notification shade without opening the app.
