# /specs/11-data-model.md

# Data Model Specification

## Purpose

This document defines MyDo's local-only logical data model. All entities are stored in one local database; there are no users, accounts, workspaces, memberships, servers, synchronization records, or remote API representations.

---

# Design Principles

- Every persisted entity has a stable unique identifier.
- Relationships are explicit and protected by local referential-integrity rules.
- The local database is the source of truth.
- Changes commit locally and are immediately available to local views.
- Complete data can be manually exported and imported in a versioned backup format.

---

# Entity Relationship Diagram

```text
Local Database
├── Projects
│   ├── Sections
│   │   └── Tasks
│   │       ├── Subtasks
│   │       ├── Attachments
│   │       ├── Reminders
│   │       └── Activity Events
├── Labels
├── Filters
├── Notifications
└── Preferences
```

---

# Project

| Property | Type |
|---|---|
| id | UUID |
| name | String |
| description | String |
| color | String |
| icon | String |
| archived | Boolean |
| favorite | Boolean |
| createdAt | DateTime |
| updatedAt | DateTime |

Each project is local to the database and contains sections and tasks.

# Section

| Property | Type |
|---|---|
| id | UUID |
| projectId | UUID |
| name | String |
| order | Integer |

# Task

| Property | Type |
|---|---|
| id | UUID |
| projectId | UUID? |
| sectionId | UUID? |
| parentTaskId | UUID? |
| title | String |
| description | Text |
| completed | Boolean |
| priority | Enum |
| dueDate | Date? |
| dueTime | Time? |
| recurringRule | String? |
| createdAt | DateTime |
| updatedAt | DateTime |
| completedAt | DateTime? |

A task without a project is in the Inbox. A task may have child tasks, labels, attachments, reminders, and local activity history.

# Label

| Property | Type |
|---|---|
| id | UUID |
| name | String |
| color | String |
| createdAt | DateTime |

Tasks and labels have a many-to-many relationship.

# Filter

| Property | Type |
|---|---|
| id | UUID |
| name | String |
| query | String |
| favorite | Boolean |

# Reminder

| Property | Type |
|---|---|
| id | UUID |
| taskId | UUID |
| triggerTime | DateTime |
| type | Enum |
| enabled | Boolean |

Reminders schedule local device notifications.

# Attachment

| Property | Type |
|---|---|
| id | UUID |
| taskId | UUID |
| filename | String |
| mimeType | String |
| size | Integer |
| localUri | URI |

Attachments refer to locally accessible files; they are never uploaded by MyDo.

# Activity Event

| Property | Type |
|---|---|
| id | UUID |
| objectId | UUID |
| objectType | Enum |
| eventType | Enum |
| timestamp | DateTime |

Activity records local actions such as created, updated, completed, and deleted.

# Notification

| Property | Type |
|---|---|
| id | UUID |
| type | Enum |
| taskId | UUID? |
| read | Boolean |
| createdAt | DateTime |

Notification types are **Reminder** and **System** (for example, local database or import errors).

---

# Local Database and Backup Format

The database stores all listed entities plus preferences. A manual export creates a complete, versioned MyDo backup with integrity metadata. Import validates the format before creating, replacing, or merging local records. Imports neither contact a server nor create an account.

---

# Referential Integrity Rules

- A section belongs to exactly one project.
- A task may have zero or one project and section; an assigned section must belong to its project.
- A reminder and attachment belong to exactly one task.
- Labels may belong to many tasks.
- Deleting a parent object must not leave orphaned child records.

---

# Data Validation and Performance

IDs are immutable; required fields cannot be null; foreign keys reference existing records; dates use UTC internally; local presentation respects locale and time zone. The local database supports efficient indexes, partial updates, and datasets containing tens of thousands of tasks.

---

# Success Criteria

The model represents all local MyDo entities, maintains referential integrity, supports fast local queries and updates, and can be fully backed up and restored through manual export and import.
