# /specs/11-data-model.md

# Data Model Specification

## Purpose

This document defines the logical data model used throughout the application.

The model describes the primary entities, their relationships, lifecycle, ownership, and synchronization behavior. It intentionally avoids implementation details such as database schema or API serialization.

---

# Design Principles

The data model follows several principles:

- Every entity has a unique identifier.
- Relationships are explicit.
- Objects synchronize independently.
- Most entities support offline modification.
- Changes are eventually consistent across devices.

---

# Entity Relationship Diagram

```text
Workspace
│
├── Projects
│     │
│     ├── Sections
│     │      │
│     │      └── Tasks
│     │              │
│     │              ├── Subtasks
│     │              ├── Comments
│     │              ├── Attachments
│     │              ├── Reminders
│     │              └── Activity
│     │
│     └── Members
│
├── Labels
│
├── Filters
│
├── Notifications
│
└── User
```

---

# User

Represents an authenticated account.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| name | String |
| email | String |
| avatar | URL |
| timezone | String |
| locale | String |
| subscription | Enum |
| createdAt | DateTime |
| updatedAt | DateTime |

---

# Workspace

Represents the highest organizational boundary.

A workspace contains:

- Projects
- Members
- Shared configuration

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| name | String |
| ownerId | UUID |
| createdAt | DateTime |

---

# Project

Primary organizational container.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| workspaceId | UUID |
| name | String |
| description | String |
| color | String |
| icon | String |
| archived | Boolean |
| favorite | Boolean |
| createdAt | DateTime |
| updatedAt | DateTime |

## Relationships

```
Project

↓

Sections

↓

Tasks
```

---

# Section

Logical subdivision within a project.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| projectId | UUID |
| name | String |
| order | Integer |

---

# Task

The central entity in the application.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| projectId | UUID |
| sectionId | UUID? |
| parentTaskId | UUID? |
| title | String |
| description | Text |
| completed | Boolean |
| priority | Enum |
| dueDate | Date |
| dueTime | Time |
| recurringRule | String |
| createdBy | UUID |
| assignedTo | UUID? |
| createdAt | DateTime |
| updatedAt | DateTime |
| completedAt | DateTime? |

---

# Task Relationships

```
Task

├── Labels
├── Comments
├── Attachments
├── Reminders
├── Activity
└── Subtasks
```

---

# Subtask

A task may contain child tasks.

Relationship:

```
Task

↓

Subtask

↓

Subtask
```

Nested subtasks are supported.

---

# Label

Reusable categorization.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| name | String |
| color | String |
| createdAt | DateTime |

Many-to-many relationship with Tasks.

---

# Filter

Saved search expression.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| name | String |
| query | String |
| favorite | Boolean |

---

# Reminder

Represents a scheduled notification.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| taskId | UUID |
| triggerTime | DateTime |
| type | Enum |
| enabled | Boolean |

One task may have multiple reminders.

---

# Comment

Discussion attached to a task.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| taskId | UUID |
| authorId | UUID |
| body | Text |
| createdAt | DateTime |
| updatedAt | DateTime |

---

# Attachment

Represents uploaded files.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| taskId | UUID |
| filename | String |
| mimeType | String |
| size | Integer |
| url | URL |
| uploadedBy | UUID |

---

# Activity Event

Immutable history record.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| objectId | UUID |
| objectType | Enum |
| actorId | UUID |
| eventType | Enum |
| timestamp | DateTime |

Example events:

- Created
- Updated
- Completed
- Deleted
- Assigned

---

# Notification

Represents user-visible activity.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| userId | UUID |
| type | Enum |
| objectId | UUID |
| read | Boolean |
| createdAt | DateTime |

---

# Membership

Defines project permissions.

## Properties

| Property | Type |
|-----------|------|
| id | UUID |
| projectId | UUID |
| userId | UUID |
| role | Enum |

Example roles:

- Owner
- Admin
- Editor
- Viewer

---

# Enumerations

## Priority

```
P1

P2

P3

P4
```

---

## Notification Type

```
Reminder

Comment

Mention

Assignment

Invitation

Project

System
```

---

## Sync Status

```
Local

Pending

Uploading

Synced

Conflict

Deleted
```

---

## Project State

```
Active

Archived

Deleted
```

---

# Relationships

## One-to-Many

```
Workspace → Projects

Project → Sections

Section → Tasks

Task → Comments

Task → Attachments

Task → Reminders

Task → Activity
```

---

## Many-to-Many

```
Tasks ↔ Labels

Projects ↔ Members
```

---

# Object Lifecycle

## Task

```
Created

↓

Edited

↓

Scheduled

↓

Completed

↓

Archived

↓

Deleted
```

---

## Project

```
Created

↓

Updated

↓

Shared

↓

Archived

↓

Deleted
```

---

# Synchronization Model

Every synchronized object maintains:

| Property | Purpose |
|-----------|----------|
| id | Stable identity |
| version | Conflict detection |
| updatedAt | Last modification |
| deleted | Soft deletion |
| syncState | Local synchronization status |

---

# Offline Persistence

Entities expected to exist locally include:

- User
- Projects
- Sections
- Tasks
- Labels
- Filters
- Notifications
- Comments
- Pending changes

Attachments may use lazy loading.

---

# Referential Integrity Rules

- Every Task belongs to exactly one Project.
- Every Section belongs to exactly one Project.
- Every Comment belongs to exactly one Task.
- Every Reminder belongs to exactly one Task.
- Every Attachment belongs to exactly one Task.
- Labels may belong to many Tasks.
- Projects may contain many Members.
- Deleted parent objects must be handled without creating orphaned records.

---

# Conflict Resolution

When concurrent edits occur:

1. Detect version mismatch.
2. Preserve local edits until reconciliation.
3. Merge non-conflicting fields where possible.
4. Surface conflicts requiring user intervention.
5. Update all dependent views after resolution.

---

# Data Validation

General validation rules:

- IDs are immutable.
- Required fields cannot be null.
- Foreign keys must reference existing objects.
- Dates use UTC internally.
- User-facing formatting respects locale and timezone.
- Soft-deleted objects are excluded from standard queries.

---

# Performance Considerations

The data model should support:

- Incremental synchronization
- Partial object updates
- Lazy loading of large collections
- Efficient local indexing
- Offline-first operation
- Scalable datasets containing tens of thousands of tasks

---

# Success Criteria

The data model succeeds when it:

- Accurately represents all application entities
- Maintains referential integrity
- Supports offline-first synchronization
- Enables efficient querying and updates
- Scales to large personal and collaborative workspaces
- Provides a stable foundation for future application features
