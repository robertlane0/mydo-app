# /specs/05-task-detail.md

# Task Detail Specification

## Purpose

The Task Detail screen is the primary interface for viewing and editing an individual task.

Every task in the application, regardless of where it is accessed (Inbox, Today, Upcoming, Project, Search, Notification, etc.), is represented by the same Task Detail experience.

The screen should expose all task metadata while keeping the primary editing experience lightweight.

---

# Goals

The Task Detail screen allows users to:

- View complete task information
- Edit task properties
- Mark tasks complete
- Organize tasks
- Create subtasks
- Collaborate through comments
- Attach files
- Configure reminders
- Review task activity

---

# Navigation

```
Task List

↓

Task Detail

├── Description
├── Due Date
├── Project
├── Labels
├── Priority
├── Reminder
├── Comments
├── Activity
└── Subtasks
```

---

# Screen Layout

```
┌─────────────────────────────────────┐
│ ← Task Detail                  ⋮    │
├─────────────────────────────────────┤
│ ☑ Finish quarterly report           │
│                                     │
│ Finalize financial summary...       │
│                                     │
│ 📅 Tomorrow 2:00 PM                 │
│ 📁 Work                            │
│ 🏷 Finance                          │
│ 🔴 Priority 1                       │
│                                     │
├─────────────────────────────────────┤
│ Subtasks                            │
│ □ Verify numbers                    │
│ □ Export PDF                        │
├─────────────────────────────────────┤
│ Comments                            │
│                                     │
├─────────────────────────────────────┤
│ Activity                            │
└─────────────────────────────────────┘
```

---

# Header

Contains:

- Back navigation
- Overflow menu

Optional actions:

- Share
- Favorite
- More Actions

---

# Primary Task Section

Displays the task itself.

Components:

- Completion checkbox
- Task title
- Editable title
- Description

Selecting either field enters edit mode.

---

# Description

Purpose:

Store long-form task notes.

Supports:

- Plain text
- Multiple paragraphs
- Links
- Rich formatting (where supported)

Empty descriptions display a placeholder encouraging users to add more information.

---

# Due Date

Displays scheduling information.

Possible values:

```
Today
```

```
Tomorrow
```

```
Next Monday
```

```
No Due Date
```

Selecting the field opens the Due Date picker.

---

# Priority

Priority levels organize work.

Supported levels:

| Level | Meaning |
|--------|----------|
| P1 | Highest |
| P2 | High |
| P3 | Normal |
| P4 | Low |

Priority is represented using consistent visual indicators throughout the application.

---

# Project

Displays the current project assignment.

Possible states:

```
Inbox
```

```
Marketing
```

```
Personal
```

Selecting the field opens the Project Picker.

Changing the project immediately updates task organization.

---

# Section

If the selected project contains sections:

```
Marketing

↓

Planning
```

The user may move the task between sections.

---

# Labels

Displays applied labels.

Examples:

```
@home
```

```
@urgent
```

```
@phone
```

Selecting Labels opens the Label Picker.

Multiple labels may be assigned.

---

# Reminders

Displays configured reminders.

Examples:

```
30 minutes before
```

```
Tomorrow 9 AM
```

```
Every Friday
```

Selecting the field opens Reminder configuration.

---

# Recurrence

Recurring tasks display scheduling rules.

Examples:

```
Every day
```

```
Every Monday
```

```
Every 3 weeks
```

Completing the task schedules the next occurrence automatically.

---

# Subtasks

Subtasks divide work into smaller items.

Example:

```
Finish Report

├── □ Review Draft
├── □ Export PDF
└── □ Send Email
```

Users may:

- Create subtasks
- Complete subtasks
- Reorder subtasks
- Delete subtasks

---

# Comments

Comments support collaboration.

Each comment contains:

- Author
- Timestamp
- Message
- Attachments (optional)

Users may:

- Add comments
- Edit their own comments
- Delete permitted comments
- Mention collaborators

---

# Attachments

Tasks may contain attached files.

Supported actions:

- Upload
- Download
- Preview
- Remove

Attachment availability depends on storage providers and permissions.

---

# Activity

Displays chronological history.

Examples:

```
Task created

↓

Priority changed

↓

Due date updated

↓

Completed
```

Activity entries are read-only.

---

# Overflow Menu

Typical actions include:

- Duplicate
- Move
- Archive
- Delete
- Share
- Copy Link
- Print (where supported)

Available options depend on task state and permissions.

---

# Editing

Every editable field supports:

```
Select Field

↓

Edit

↓

Validate

↓

Save

↓

Synchronize
```

Updates should appear immediately in the UI.

---

# Completion

Selecting the completion checkbox:

```
Open Task

↓

Complete

↓

Completion Animation

↓

Task Removed

↓

Sync
```

Recurring tasks instead schedule the next occurrence.

---

# Loading State

Displayed while task data loads.

Characteristics:

- Skeleton fields
- Placeholder metadata
- Disabled editing controls

Cached content should be shown whenever possible.

---

# Offline State

Users may:

- Edit title
- Edit description
- Change priority
- Change due date
- Complete task
- Create subtasks
- Add comments (queued where supported)

Changes synchronize automatically when connectivity returns.

---

# Error State

Possible causes:

- Synchronization failure
- Permission denied
- Network unavailable

Recovery options:

- Retry
- Continue editing offline
- Cancel

---

# User Interactions

| Action | Result |
|----------|--------|
| Tap title | Edit title |
| Tap description | Edit description |
| Tap due date | Open date picker |
| Tap project | Open project picker |
| Tap labels | Open label picker |
| Tap reminder | Configure reminder |
| Tap checkbox | Complete task |
| Tap subtask | Open or edit subtask |
| Add comment | Append comment |
| Back | Return to previous screen |

---

# Accessibility

Task Detail should:

- Announce completion status
- Expose editable fields with clear labels
- Support screen readers for comments and activity
- Maintain logical focus after edits
- Provide accessible controls for all metadata editors
- Respect system font scaling and contrast settings

---

# Performance Requirements

The screen should:

- Open instantly from cached task data
- Save edits optimistically with background synchronization
- Update dependent task lists immediately after changes
- Lazily load comments, attachments, and activity history when appropriate

---

# Business Rules

- Every task has a single parent project.
- Tasks may contain zero or more subtasks.
- Tasks may have zero or more labels.
- A task can have at most one active due date.
- Completing a recurring task creates the next scheduled occurrence.
- Users may only modify fields permitted by their access level in shared projects.

---

# Navigation Summary

```
Task Detail

├── Edit Title
├── Edit Description
├── Due Date Picker
├── Project Picker
├── Section Picker
├── Label Picker
├── Reminder Editor
├── Subtask
├── Comments
├── Activity
└── Overflow Menu
```

---

# Success Criteria

The Task Detail screen succeeds when users can:

- Understand all information associated with a task at a glance
- Modify any task attribute without unnecessary navigation
- Collaborate effectively through comments and attachments
- Organize work using projects, labels, priorities, and schedules
- Complete tasks with immediate visual feedback and reliable synchronization
