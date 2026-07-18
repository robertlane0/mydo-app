# /specs/09-notifications.md

# Notifications Specification

## Purpose

MyDo uses local device notifications to remind the user about their tasks and to report important local-database events. It has no push service, activity feed, assignment, invitation, comment, project-sharing, or account notifications.

---

# Goals

- Deliver task reminders at their scheduled local time.
- Open the related local task when a reminder is selected.
- Let the user control local reminder and summary preferences.
- Keep notification history and read state in the local database when supported.

---

# Notification Types

| Type | Trigger |
|---|---|
| Reminder | A task's time-, date-, or location-based reminder becomes due. |
| System | A local event needs attention, such as an import failure or database recovery warning. |

---

# Navigation and Layout

The Notifications screen displays locally stored notices, newest first. Each item shows title, related task or local event, timestamp, and read state. Selecting a reminder opens its Task Detail; selecting a system notice opens the relevant local recovery or data screen.

---

# Reminder Notifications

When a reminder is due, MyDo asks the operating system to display a local notification. The notification includes task title, relevant due information, and actions such as complete, snooze, or open. Availability depends on the user's device-level notification permission.

---

# Preferences

Settings can enable or disable task reminders and daily summaries. There are no email, push, assignment, comment, mention, invitation, or shared-project preferences.

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

# Success Criteria

Users reliably receive and act on task reminders without an account, connection, synchronization service, or data sent to MyDo.
