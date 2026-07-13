# /specs/04-inbox.md

# Inbox Specification

## Purpose

The Inbox is the application's default capture destination.

It acts as an unprocessed queue for newly created tasks that have not yet been organized into projects or fully planned.

The Inbox supports the "capture first, organize later" philosophy by allowing users to quickly record ideas without requiring immediate categorization.

---

# Goals

The Inbox should allow users to:

- Capture tasks with minimal effort
- Review uncategorized work
- Organize tasks into projects
- Schedule tasks
- Prioritize tasks
- Complete tasks directly
- Bulk organize captured items

---

# Navigation

```
Application

↓

Inbox

↓

Task List

↓

Task Detail
```

The Inbox is a top-level navigation destination.

---

# Screen Layout

```
┌─────────────────────────────────┐
│ Inbox                           │
├─────────────────────────────────┤
│                                 │
│ □ Email accountant              │
│ □ Buy coffee                    │
│ □ Renew passport                │
│ □ Call contractor               │
│                                 │
│ □ Read article                  │
│                                 │
├─────────────────────────────────┤
│                ＋                │
└─────────────────────────────────┘
```

---

# Primary Components

## Top App Bar

Contains:

- Inbox title
- Search
- Overflow menu

Optional indicators:

- Remaining task count
- Sync status

---

## Task List

Displays all active Inbox tasks.

Tasks are ordered according to the user's configured sort method.

Possible sorting methods include:

- Manual
- Creation date
- Due date
- Priority
- Alphabetical

---

## Floating Action Button

Purpose:

Quickly capture a new task.

Selecting the button opens the Task Composer.

---

# Task Item

Each Inbox task may display:

- Completion checkbox
- Task title
- Due date
- Due time
- Priority
- Labels
- Recurring indicator
- Attachment indicator
- Comment indicator

Because Inbox tasks are uncategorized, project information is typically omitted.

---

# Task Creation

The Inbox is the default destination for tasks created without a project.

Example flow:

```
Tap +

↓

Enter Title

↓

Save

↓

Task Appears In Inbox
```

Additional metadata may be added immediately or later.

---

# Organizing Tasks

Tasks may be organized by assigning:

- Project
- Section
- Due date
- Priority
- Labels
- Reminder

Moving a task into a project removes it from the Inbox.

---

# Task Completion

Completing a task:

```
Checkbox

↓

Completed

↓

Animated Removal

↓

Completion Sync
```

Completed tasks are removed from the active Inbox view.

---

# Bulk Operations

Where supported, multiple tasks may be selected.

Available actions include:

- Move to project
- Assign labels
- Set priority
- Set due date
- Complete
- Delete

Bulk operations should provide progress feedback.

---

# Empty State

Displayed when no Inbox tasks exist.

Example:

```
Inbox Zero

Your Inbox is empty.

Capture new ideas whenever they come to mind.
```

Primary action:

```
Add Task
```

---

# Loading State

Displayed while Inbox data is loading.

Characteristics:

- Skeleton task rows
- Disabled interactions where appropriate
- Loading indicator

Previously cached content should remain visible when possible.

---

# Offline State

Users may:

- View cached Inbox tasks
- Create new tasks
- Edit existing tasks
- Complete tasks
- Organize tasks

Changes are queued until synchronization resumes.

---

# Error State

Possible causes:

- Synchronization failure
- Server unavailable
- Local database error

The interface should display:

- Error message
- Retry action
- Offline indicator when applicable

---

# Overflow Menu

Typical actions include:

- Sort
- View completed tasks
- Select multiple
- Refresh
- Help

The available actions may vary by platform and user permissions.

---

# Search

Selecting Search filters Inbox content.

Search should support:

- Partial task names
- Labels
- Dates
- Priorities

Selecting a result opens the corresponding Task Detail screen.

---

# Sorting

Supported sorting options may include:

| Sort | Behavior |
|--------|----------|
| Manual | User-defined order |
| Due Date | Earliest first |
| Priority | Highest priority first |
| Name | Alphabetical |
| Date Added | Most recent or oldest first |

Changing the sort order updates the visible list without affecting task data.

---

# Swipe Actions

Where enabled, swipe gestures may perform actions such as:

Left swipe:

- Complete
- Delete

Right swipe:

- Schedule
- Move
- Edit

Available gestures should respect user preferences and platform conventions.

---

# User Interactions

| Action | Result |
|----------|--------|
| Tap task | Open Task Detail |
| Tap checkbox | Complete task |
| Tap + | Create task |
| Long press | Enter multi-select mode |
| Swipe | Execute configured quick action |
| Pull to refresh | Synchronize Inbox |
| Search | Filter tasks |

---

# Accessibility

The Inbox should:

- Announce task completion state
- Expose list position information
- Provide descriptive labels for action buttons
- Support keyboard navigation
- Maintain logical focus after task removal
- Respect system font scaling

---

# Performance Requirements

The Inbox should:

- Open immediately using cached data
- Efficiently render large task lists
- Support smooth scrolling
- Apply task updates incrementally
- Synchronize changes in the background without disrupting interaction

---

# Navigation Summary

```
Inbox

├── Task Detail
│
├── Search
│
├── Sort
│
├── Multi-Select
│
└── Task Composer
```

---

# Business Rules

- Every new task without an assigned project is created in the Inbox.
- Assigning a project removes the task from the Inbox.
- Completing a task removes it from the active Inbox list.
- Deleted tasks cannot be recovered from the active Inbox.
- Offline changes are synchronized automatically when connectivity is restored.

---

# Success Criteria

The Inbox succeeds when users can:

- Capture ideas in seconds
- Process uncategorized work efficiently
- Organize tasks into meaningful projects
- Maintain an empty Inbox as part of their productivity workflow
- Reliably access and update captured tasks regardless of network connectivity
