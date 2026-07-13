# /specs/01-navigation.md

# Navigation Specification

## Purpose

The navigation system provides fast access to the user's work while keeping task creation available from nearly every part of the application.

The application is organized around a small number of permanent destinations supplemented by contextual detail screens and modal editors.

---

# Navigation Principles

The navigation model follows five principles:

1. Work is always one tap away.
2. Task creation is globally accessible.
3. Navigation state is preserved.
4. Context should never be lost.
5. Detail screens should be shallow whenever possible.

---

# Application Structure

```
Application

├── Authentication Flow
│
└── Main Application
    │
    ├── Inbox
    ├── Today
    ├── Upcoming
    ├── Projects
    ├── Search
    │
    ├── Notifications
    ├── Activity
    ├── Productivity
    └── Settings
```

---

# Navigation Types

## Primary Navigation

Persistent destinations representing major application areas.

Characteristics:

- Always available
- Preserve navigation state
- Top-level destinations
- No back navigation required

---

## Secondary Navigation

Used for organizational hierarchy.

Examples:

- Project
- Section
- Label
- Filter
- Team

---

## Context Navigation

Displays information related to the current object.

Examples:

- Task Details
- Project Members
- Comments
- Attachments
- Reminder Editor

---

## Modal Navigation

Temporary screens used for focused tasks.

Examples:

- Add Task
- Edit Task
- Date Picker
- Project Picker
- Label Picker
- Share Dialog

Dismissal returns the user to the previous context without altering navigation history.

---

# Launch Flow

```
App Launch

↓

Splash Screen

↓

Authentication Check

↓

Authenticated?
    │
 ┌──Yes─────────────┐
 │                  │
 │                  ▼
 │            Restore Session
 │                  │
 │                  ▼
 │           Previous Destination
 │
 └──No──────────────┐
                    ▼
              Login Screen
                    │
                    ▼
               Main Application
```

---

# Root Destinations

## Inbox

Purpose:

Default collection point for uncategorized tasks.

Primary actions:

- View inbox tasks
- Add task
- Organize tasks
- Move tasks

---

## Today

Purpose:

Display all tasks due today or overdue.

Primary actions:

- Complete tasks
- Reschedule
- Reprioritize
- Quick add

---

## Upcoming

Purpose:

Display future scheduled work.

Primary actions:

- Browse upcoming days
- Move tasks
- Reschedule
- Plan workload

---

## Projects

Purpose:

Browse work grouped by project.

Primary actions:

- Open project
- Create project
- Edit project
- Archive project

---

## Search

Purpose:

Locate information quickly.

Searchable entities include:

- Tasks
- Projects
- Labels
- Filters
- Comments (where supported)

---

# Project Navigation

```
Projects

↓

Project List

↓

Project

↓

Section

↓

Task

↓

Task Details
```

The user may return one level at a time using system back navigation.

---

# Task Navigation

Tasks can be opened from many locations:

- Inbox
- Today
- Upcoming
- Project
- Search
- Filter
- Label
- Notification

Regardless of origin, selecting a task opens the same Task Detail screen.

---

# Task Detail Hierarchy

```
Task

├── Description
├── Due Date
├── Priority
├── Labels
├── Project
├── Section
├── Subtasks
├── Comments
├── Attachments
├── Activity
└── Reminders
```

Each child editor is presented as a subordinate screen or modal.

---

# Project Hierarchy

```
Workspace

↓

Project

↓

Section

↓

Task

↓

Subtask
```

Tasks may be nested to create multiple levels of subtasks.

---

# Back Navigation

Android system back behavior should:

- Close dialogs first
- Close sheets second
- Return to previous screen
- Exit only from root destinations

The back action must never discard user changes without confirmation.

---

# Deep Linking

The application should support direct navigation into specific resources.

Examples:

```
Notification
    ↓
Task

Email Link
    ↓
Project

Shared Link
    ↓
Task

Widget
    ↓
Today

Search Result
    ↓
Task
```

---

# State Restoration

When the application resumes, it should restore:

- Current destination
- Scroll position
- Expanded sections
- Active filters
- Search query (where appropriate)
- Draft task (if supported)

---

# Global Actions

These actions are available from multiple destinations.

## Add Task

Accessible from:

- Inbox
- Today
- Upcoming
- Projects
- Search

Result:

Opens the Task Composer.

---

## Search

Accessible globally.

Result:

Navigates to the Search screen without affecting underlying navigation history.

---

## Notifications

Accessible globally.

Result:

Displays recent user notifications.

---

## Settings

Accessible from the account/profile entry point.

---

# Navigation States

Each destination may exist in one of the following states.

## Loading

Displayed while data is being fetched.

Characteristics:

- Skeleton UI
- Progress indicator
- Placeholder content

---

## Empty

Displayed when no data exists.

Examples:

- No tasks today
- Empty project
- No search results

The UI should provide a primary action to create relevant content.

---

## Populated

Displays the standard interactive interface.

---

## Error

Displayed when content cannot be loaded.

The interface should provide:

- Error explanation
- Retry action
- Offline indication (if applicable)

---

# Navigation Consistency Rules

- The same task always opens the same Task Detail screen.
- Projects retain their internal navigation state when revisited.
- Modal editors do not replace the current navigation stack.
- Global destinations preserve scroll position when possible.
- Completing a task returns the user to the originating list unless explicitly navigating elsewhere.

---

# Accessibility

Navigation components should:

- Be fully operable via keyboard and assistive technologies.
- Expose semantic labels for all icons.
- Maintain visible focus indicators.
- Support Android back gestures.
- Preserve logical focus order after navigation transitions.

---

# Future Specifications

This document defines the navigation framework only.

Subsequent specifications describe each destination individually:

- 02-authentication.md
- 03-home-screen.md
- 04-inbox.md
- 05-task-detail.md
- 06-projects.md
- 07-upcoming.md
- 08-search.md
- 09-notifications.md
- 10-settings.md
```
