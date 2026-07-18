# /specs/10-settings.md

# Settings Specification

## Purpose

Settings manages local MyDo preferences, reminder behavior, appearance, privacy, and manual data portability. MyDo has no account, remote configuration, sync service, subscription, or connected session.

---

# Goals

- Configure local application preferences.
- Control locally scheduled notifications.
- Customize appearance and productivity features.
- Import and export the local database manually.
- Make destructive local-data actions clear and confirmable.

---

# Navigation and Layout

Settings is accessed from the main overflow menu or a settings icon.

```
Settings
├── General
├── Notifications
├── Appearance
├── Productivity
├── Data
├── Privacy
├── Help
└── About
```

---

# General

Options include default start screen, default task priority, default reminder, date and time formats, week start day, language, and time zone. Changes take effect locally and immediately unless otherwise specified.

---

# Notifications

Controls local task reminders and daily summaries. MyDo schedules notifications on the device; it provides no push, email, assignment, comment, or mention notifications.

---

# Appearance

Options include light, dark, and system themes; accent or dynamic color; font size; and compact mode. Theme changes apply immediately.

---

# Productivity

Options may include daily goal, completed-task visibility, weekend visibility, smart scheduling, and streak tracking. These settings affect only the local experience.

---

# Data

Data settings make the local database portable and recoverable.

| Action | Result |
|---|---|
| Export local database | Creates a complete MyDo backup and opens the system save/share chooser. |
| Import local database | Opens a file chooser, validates a MyDo backup, then asks whether to replace or merge local data. |
| Clear local data | Requires explicit confirmation and offers export first. |

Exports include tasks, projects, sections, labels, filters, reminders, completion history, and preferences. They use a versioned format with integrity metadata. Export is manual and never uploads data.

Before importing, MyDo validates the selected file. An invalid or unreadable file leaves the current local database unchanged. Replacing data requires a warning and, when possible, a fresh backup. A merge must not silently overwrite an existing edit; unresolved conflicts are reported.

---

# Privacy

Privacy options include analytics participation, crash reporting, local-data export, and local-data deletion. MyDo does not collect account information because it has no accounts.

---

# Help and About

Help includes documentation, FAQ, bug reporting, and feature requests. About displays version, build number, license information, open-source acknowledgements, and privacy information.

---

# Loading and Error States

Settings loads from local storage. If the database cannot be opened, show a clear explanation and offer retry or recovery options. Import and export failures explain the cause—such as unsupported format, insufficient storage, permission denial, or write error—without changing the existing local data.

---

# Accessibility

All controls expose descriptive labels, announce switch states, support keyboard navigation, preserve focus after returning from detail screens, respect font scaling, and maintain sufficient contrast. Import, export, and destructive-data confirmations provide a clearly labelled cancel action.

---

# Business Rules

- Preferences persist in the local database and affect only this installation.
- Sensitive local-data operations require confirmation.
- Import and export are explicit file operations; neither happens automatically.
- MyDo does not sign in, sign out, synchronize, upload, or connect to a MyDo server.

---

# Success Criteria

Users can customize MyDo, manage local reminders and appearance, manually export a complete backup, safely import a backup, and understand any local-data risk before confirming a destructive action.
