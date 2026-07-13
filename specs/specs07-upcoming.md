# /specs/07-upcoming.md

# Upcoming Specification

## Purpose

The Upcoming view provides a chronological planning interface for all scheduled work.

Unlike the Today view, which emphasizes immediate execution, Upcoming helps users understand future workload, identify scheduling conflicts, and plan tasks across days, weeks, and months.

It is the application's primary planning experience.

---

# Goals

The Upcoming view allows users to:

- View scheduled tasks chronologically
- Plan future work
- Identify overloaded days
- Reschedule tasks
- Complete upcoming tasks early
- Create tasks for future dates
- Navigate through time efficiently

---

# Navigation

```
Application

↓

Upcoming

├── Calendar Timeline
├── Daily Groups
├── Task List
└── Task Detail
```

Upcoming is a primary navigation destination.

---

# Screen Layout

```
┌────────────────────────────────────┐
│ Upcoming                     🔍    │
├────────────────────────────────────┤
│ July 2026                         │
│ ◀──────── Calendar ───────▶        │
├────────────────────────────────────┤
│ Today                             │
│ □ Submit proposal                 │
│ □ Team meeting                    │
├────────────────────────────────────┤
│ Tomorrow                          │
│ □ Review contract                 │
├────────────────────────────────────┤
│ Thursday                          │
│ □ Dentist                         │
│ □ Grocery shopping                │
├────────────────────────────────────┤
│ Friday                            │
│ □ Sprint Planning                 │
└────────────────────────────────────┘
```

---

# Timeline

The primary organizational element is chronological order.

Tasks are grouped beneath date headers.

Example:

```
Today

↓

Tomorrow

↓

Wednesday

↓

Thursday

↓

Friday
```

Days without tasks may still appear depending on the selected view.

---

# Date Header

Each group displays:

- Relative date
- Calendar date
- Task count
- Optional completion summary

Example:

```
Tuesday

July 14

5 Tasks
```

---

# Task Group

Each date contains zero or more tasks.

Tasks inherit the application's configured sorting method.

Typical order:

- Priority
- Time
- Manual order

---

# Calendar Navigation

Users may navigate through time by:

- Vertical scrolling
- Horizontal calendar selection
- Jump to Today
- Month picker (where supported)

Navigation should preserve the selected date.

---

# Task Item

Each task may display:

- Completion checkbox
- Title
- Due time
- Priority
- Project
- Labels
- Recurring indicator

Selecting a task opens Task Detail.

---

# Rescheduling

Tasks may be rescheduled directly.

Flow:

```
Task

↓

Change Due Date

↓

Select New Date

↓

Save

↓

Task Moves Automatically
```

The list updates immediately.

---

# Drag and Drop

Where supported:

Users may drag tasks between dates.

```
Tuesday

↓

Drag

↓

Thursday
```

The due date updates automatically.

---

# Recurring Tasks

Recurring tasks appear on their next scheduled occurrence.

Example:

```
Every Monday

↓

Upcoming Monday
```

Completing the task schedules the next recurrence.

---

# Overdue Tasks

Overdue tasks may appear before Today.

Example:

```
Overdue

□ Pay utility bill

□ Call insurance
```

These tasks remain until completed or rescheduled.

---

# Empty Days

Days without scheduled work may display:

```
No tasks scheduled.
```

Users should still be able to create a task directly from that date.

---

# Add Task

Creating a task from Upcoming automatically assigns the selected date.

Flow:

```
Select Day

↓

Add Task

↓

Task Created

↓

Appears Under Selected Date
```

---

# Search

Search filters scheduled tasks.

Supported queries include:

- Task title
- Date
- Label
- Project
- Priority

Selecting a result opens Task Detail.

---

# Filters

Optional filters include:

- Priority
- Project
- Labels
- Recurring
- Completed Status

Filters affect presentation only.

---

# Loading State

Displayed while scheduled tasks are loading from the local database.

Characteristics:

- Skeleton date headers
- Placeholder tasks
- Loading indicator

Cached content should remain visible whenever possible.

---

# Empty State

If no scheduled tasks exist:

```
Nothing is scheduled.

Plan your future work by creating a task.
```

Primary action:

```
Add Task
```

---

# Offline State

Users may:

- View cached schedules
- Create scheduled tasks
- Edit due dates
- Complete tasks
- Move tasks

Changes are saved to the local database immediately and require no connectivity.

---

# Error State

Possible causes:

- Local database error
- Calendar data unavailable

Recovery actions:

- Retry
- Continue using locally available data
- Return to previous screen

---

# User Interactions

| Action | Result |
|----------|--------|
| Tap task | Open Task Detail |
| Tap checkbox | Complete task |
| Tap date | Jump to date |
| Drag task | Reschedule |
| Pull to refresh | Reload local schedule |
| Tap + | Create scheduled task |
| Scroll | Browse timeline |

---

# Accessibility

Upcoming should:

- Announce date headers
- Expose chronological ordering
- Identify overdue tasks programmatically
- Support keyboard navigation
- Preserve focus after task movement
- Respect system accessibility settings

---

# Performance Requirements

Upcoming should:

- Efficiently render long timelines
- Lazily load distant dates
- Preserve scroll position
- Apply scheduling changes immediately
- Update changed local tasks without interrupting scrolling

---

# Business Rules

- Every scheduled task belongs to exactly one calendar date.
- Unscheduled tasks do not appear in Upcoming.
- Completing a recurring task generates its next scheduled occurrence.
- Rescheduling immediately updates all affected views.
- Overdue tasks remain visible until completed or reassigned.

---

# Navigation Summary

```
Upcoming

├── Calendar
├── Date Group
│     ├── Task Detail
│     └── Add Task
│
├── Search
└── Filters
```

---

# Success Criteria

The Upcoming feature succeeds when users can:

- Understand future workload at a glance
- Plan work across multiple days or weeks
- Quickly reschedule tasks when priorities change
- Identify overdue work before it is forgotten
- Add future tasks with minimal effort
- Maintain confidence that scheduled work is accurately stored locally
