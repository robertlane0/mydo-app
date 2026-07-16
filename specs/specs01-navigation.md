# /specs/01-navigation.md

# Navigation Specification

## Purpose

MyDo provides fast access to locally stored work, with task creation available from nearly every screen. It has no authentication flow: launch opens the local database and restores the last safe destination.

---

# Application Structure

```
Application
├── Inbox
├── Today
├── Upcoming
├── Projects
├── Search
├── Notifications
├── Productivity
├── Settings
│   └── Data: Import / Export
├── App Shortcuts (launcher long-press)
└── Home Screen Widgets
```

Primary destinations are Inbox, Today, Upcoming, Projects, and Search. Notifications, Productivity, and Settings are secondary destinations. Detail screens and editors are contextual. App shortcuts and widgets provide direct access from the launcher and home screen.

**App shortcuts and widgets detailed in specs21-platform-integration.md.**

---

# Launch Flow

```
App Launch
↓
Open Local Database
↓
Restore Previous Destination
```

If the database is unavailable, show a recovery state with retry and, where possible, import or recovery-export actions.

---

# Navigation Types

**Primary navigation** is persistent, top-level, and preserves state. **Secondary navigation** covers projects, sections, labels, and filters. **Context navigation** covers task details, attachments, reminders, and local activity. **Modal navigation** covers adding or editing tasks, picking dates/projects/labels, import confirmation, and export destination selection.

---

# Root Destinations

- **Inbox:** Default collection for tasks without a project.
- **Today:** Tasks due today or overdue.
- **Upcoming:** Future scheduled work.
- **Projects:** Browse, create, edit, archive, and delete local projects.
- **Search:** Search local tasks, projects, labels, filters, and notes.

---

# Task and Project Navigation

```
Projects → Project → Section → Task → Task Details
```

A task may be opened from Inbox, Today, Upcoming, Projects, Search, a filter, a label, a local reminder notification, or a widget. Selecting it always opens the same Task Detail screen.

Task Detail contains description, due date, priority, labels, project, section, subtasks, attachments, local activity, and reminders.

---

# Back Navigation and State Restoration

System back closes dialogs, then sheets, then returns to the prior screen; it exits only from root destinations. It must not discard edits without confirmation.

When MyDo resumes, it restores the current destination, scroll position, expanded sections, active filters, search query where appropriate, and an in-progress task draft when supported.

---

# Global Actions

**Add Task** is accessible from Inbox, Today, Upcoming, Projects, and Search and opens the Task Composer. **Search** opens the Search screen without replacing the underlying navigation history. **Notifications** displays local reminder and local-system notices. **Settings** is available from the main overflow menu or settings icon.

---

# Navigation States

Loading states represent opening or querying the local database. Empty states offer relevant creation or import actions. Error states explain a local database problem and provide retry or recovery actions. Pull-to-refresh, where present, reloads local data rather than synchronizing with a service.

---

# Accessibility and Consistency

Navigation is usable by keyboard and assistive technology, provides semantic icon labels and visible focus, supports Android back gestures, and keeps focus order logical after transitions. Projects retain internal state when revisited, modal editors preserve the navigation stack, and completing a task returns to its originating list unless the user chooses otherwise.

---

# Cross-References

- **specs19-reminders.md** — Reminder notification actions (Open task)
- **specs20-backup-export-import.md** — Settings → Data navigation
- **specs21-platform-integration.md** — App shortcuts, widgets, boot/update handling
- **specs05-task-detail.md** — Task Detail navigation
- **specs04-inbox.md** — Inbox navigation
- **specs07-upcoming.md** — Upcoming navigation
