# /specs/02-local-storage.md

# Local Storage and Data Portability Specification

## Purpose

MyDo has no accounts, authentication, subscriptions, remote services, or automatic synchronization. It opens directly into the app and stores all application data in a local database on the device.

---

# Goals

- Open without sign-up, sign-in, profile creation, or network access.
- Persist tasks, projects, labels, filters, reminders, and preferences locally.
- Let the user manually export a complete database backup to a chosen location.
- Let the user manually import a validated MyDo backup from a chosen location.

---

# Launch Flow

```
Launch App
↓
Open Local Database
↓
Restore Previous Destination
```

On first launch, MyDo creates an empty local database and opens the Inbox. No login, registration, password-recovery, or session screen is shown.

---

# Local Database

The database is the source of truth for the current installation. Task and preference changes commit locally when saved. The core experience must work with network connectivity disabled.

The app should use platform-protected storage and may encrypt the database where platform support is available. Deleting the app or clearing its storage can remove the database, so Settings must make manual export easy to discover.

---

# Export Flow

Entry point: **Settings → Data → Export local database**.

```
Choose Export
↓
Create Complete Backup
↓
Choose Save/Share Location
↓
Show Success or Error
```

An export contains the complete local database needed to restore MyDo: task hierarchy, projects, sections, labels, filters, reminders, completion history, and preferences. The format includes a version and integrity metadata. Export never transmits data to a MyDo service.

---

# Import Flow

Entry point: **Settings → Data → Import local database**.

```
Choose Backup File
↓
Validate Format and Integrity
↓
Choose Replace or Merge
↓
Confirm
↓
Import into Local Database
↓
Refresh Local Views
```

MyDo validates the selected backup before changing data and shows its format version and creation time when available. Invalid, corrupt, inaccessible, or unsupported files leave the current database unchanged.

**Replace local data** creates a fresh backup first when possible, then replaces all local data. **Merge with local data**, if offered, preserves existing data and adds imported records without silently overwriting an edit; unresolved ID conflicts are reported. The confirmation explains that import is local-only and can only be reversed by importing another backup.

---

# Data States and Errors

## Empty Database

Show the normal empty Inbox with actions to add a task or import a backup.

## Database Unavailable

Explain that local data could not be opened and offer retry, recovery export when possible, or reset after explicit confirmation.

## Import Error

Explain the failure—unsupported format, integrity failure, or read error—and leave the existing local data unchanged.

## Export Error

Explain the failure—insufficient storage, permission denied, or write error—and leave the existing local data unchanged.

---

# Accessibility

Import and export controls, confirmations, progress, and errors must work with screen readers and keyboard navigation. Replacement confirmations must identify affected local data and expose a clearly labelled cancel action.

---

# Success Criteria

- MyDo is usable immediately without identity or credentials.
- Ordinary task actions work with connectivity disabled.
- A complete local database can be manually exported and restored through import.
- Import failures do not alter the current local database.
