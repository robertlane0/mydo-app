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
│ Activity                            │
└─────────────────────────────────────┘
```

---

# Header

Contains:

- Back navigation
- Overflow menu

Optional actions:

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

# Attachments

Tasks may contain attached files.

Supported actions:

- Add local file
- Open local file
- Preview
- Remove

Attachments remain local to the device or reference a user-selected local file. MyDo never uploads them to a service.

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
- Print (where supported)

Available options depend on task state and platform support.

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

Save Locally
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

Persist Locally
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
Changes are saved to the local database immediately and require no connectivity.

---

# Error State

Possible causes:

- Local database error

Recovery options:

- Retry
- Continue editing locally
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
| Back | Return to previous screen |

---

# Accessibility

Task Detail should:

- Announce completion status
- Expose editable fields with clear labels
- Support screen readers for activity
- Maintain logical focus after edits
- Provide accessible controls for all metadata editors
- Respect system font scaling and contrast settings

---

# Performance Requirements

The screen should:

- Open instantly from cached task data
- Save edits locally as soon as they are confirmed
- Update dependent task lists immediately after changes
- Lazily load attachments and activity history when appropriate

---

# Business Rules

- A task may be in the Inbox or in a single project.
- Tasks may contain zero or more subtasks.
- Tasks may have zero or more labels.
- A task can have at most one active due date.
- Completing a recurring task creates the next scheduled occurrence.
- All local task fields are editable on this installation.

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
├── Activity
└── Overflow Menu
```

---

# Success Criteria

The Task Detail screen succeeds when users can:

- Understand all information associated with a task at a glance
- Modify any task attribute without unnecessary navigation
- Organize work using projects, labels, priorities, and schedules
- Complete tasks with immediate visual feedback and local persistence
