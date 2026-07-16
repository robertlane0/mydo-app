# /specs/16-recurring-tasks.md

# Recurring Tasks Specification

## Purpose

Recurring tasks automatically generate the next occurrence when the current one is completed, enabling habits, routines, and repeating work without manual rescheduling.

---

## Goals

- Define flexible recurrence rules (daily, weekly, monthly, yearly, custom)
- Auto-generate next occurrence on completion
- Display recurrence clearly in all views
- Edit/delete recurrence with clear consequences
- Persist recurrence rules locally; include in backups

---

## Data Model (from specs11-data-model.md)

Task property:
| Property | Type | Description |
|----------|------|-------------|
| recurringRule | String? | RRULE-compatible string (e.g., `FREQ=WEEKLY;BYDAY=MO,WE,FR`) |

Only tasks with `recurringRule` set are recurring.

---

## Recurrence Rule Format

Uses a simplified **RRULE subset** (RFC 5545 compatible):

```
FREQ=DAILY|WEEKLY|MONTHLY|YEARLY
[;INTERVAL=N]           (default 1)
[;BYDAY=MO,TU,WE...]    (for WEEKLY)
[;BYMONTHDAY=1,15,-1]   (for MONTHLY)
[;BYMONTH=1,6,12]       (for YEARLY)
[;COUNT=N]              (max occurrences; optional)
[;UNTIL=YYYYMMDD]       (end date; optional)
```

### Examples

| Rule | Meaning |
|------|---------|
| `FREQ=DAILY` | Every day |
| `FREQ=DAILY;INTERVAL=2` | Every 2 days |
| `FREQ=WEEKLY;BYDAY=MO,WE,FR` | Mon, Wed, Fri |
| `FREQ=WEEKLY;INTERVAL=2;BYDAY=TU` | Every other Tuesday |
| `FREQ=MONTHLY;BYMONTHDAY=1` | 1st of every month |
| `FREQ=MONTHLY;BYMONTHDAY=-1` | Last day of every month |
| `FREQ=YEARLY;BYMONTH=1;BYMONTHDAY=1` | Every Jan 1 |
| `FREQ=WEEKLY;BYDAY=MO;COUNT=10` | 10 Mondays |

---

## Recurrence Editor (Task Detail → Recurrence)

### Layout

```
┌─────────────────────────────────────┐
│ Recurrence                    ✕     │
├─────────────────────────────────────┤
│ Repeat: [Every week          ▼]     │
├─────────────────────────────────────┤
│ Frequency: Weekly                   │
│ Every: [1] week(s)                  │
│ On: ☑ Mon  ☐ Tue  ☑ Wed  ☐ Thu      │
│       ☐ Fri  ☐ Sat  ☐ Sun           │
├─────────────────────────────────────┤
│ Ends: [Never              ▼]        │
│   After [10] occurrences            │
│   On [Date Picker]                  │
├─────────────────────────────────────┤
│ [Save]                    [Remove]  │
└─────────────────────────────────────┘
```

### Frequency Options

| Frequency | Config Fields |
|-----------|---------------|
| Daily | Interval (days) |
| Weekly | Interval (weeks), Days of week (multi-select) |
| Monthly | Interval (months), Day of month (1-31, Last) |
| Yearly | Interval (years), Month, Day of month |

### End Conditions

- **Never** (default): No COUNT/UNTIL
- **After N occurrences**: `COUNT=N`
- **On date**: `UNTIL=YYYYMMDD`

---

## Completion Behavior

### Completing a Recurring Task

```
User Taps Checkbox on Recurring Task

↓

Task Marked Complete (3s animation)

↓

Generate Next Occurrence:
  - New Task Created
  - Same title, description, project, section, labels, priority
  - Due date = next rule match after current due date
  - Due time = same as completed task
  - recurringRule = copied from parent
  - parentTaskId = completed task's ID (for history)
  - createdAt = now

↓

Original Task Moves to Completed
New Task Appears in Active List

↓

Toast: "Task completed. Next due [Date] [Undo]"
```

### Undo Completion

```
Tap Undo (within 3s)

↓

Delete Generated Next Occurrence

↓

Restore Original Task to Active

↓

Toast: "Restored"
```

---

## Recurrence Display

### Task Row Indicator

```
□ Weekly Report          🔁 Every Mon, Wed
```

- Circular arrow icon (🔁) + human-readable summary
- Summary generated from RRULE (e.g., "Every weekday", "Monthly on 1st", "Every 2 weeks")

### Task Detail

```
🔁 Every Monday
   (Recurring)
```

- Full rule summary
- Tap to open Recurrence Editor

### Incomplete vs Complete

- Active recurring task: shows next due date
- Completed recurring task (in history): shows "Completed [date] · Next was [date]"

---

## Editing Recurrence

### Edit Rule on Active Task

```
Open Recurrence Editor

↓

Modify Rule

↓

Save

↓

Confirm: "Update recurrence for this task and all future occurrences?"

[This task only]  [This and future]  [Cancel]
```

| Option | Behavior |
|--------|----------|
| This task only | Creates new rule on this task; future occurrences keep old rule (effectively splits series) |
| This and future | Updates rule on this task; next generation uses new rule |

### Edit Rule on Completed Task (History)

- Not allowed directly
- User must un-complete (undo) or edit the active occurrence

---

## Deleting Recurrence

### Remove Recurrence (Keep Task)

```
Recurrence Editor → Remove

↓

Confirm: "Remove recurrence? Task becomes one-time."

✓ Remove

↓

recurringRule = null
Task Stays Active
```

### Delete Recurring Task

```
Task Detail → Delete

↓

Confirm: "Delete this recurring task?
• This occurrence only
• This and all future occurrences"

[This only]  [All future]  [Cancel]
```

| Option | Behavior |
|--------|----------|
| This occurrence | Deletes current task; next occurrence still generates on schedule (treated as "skipped") |
| All future | Deletes current task + clears recurringRule; no future generated |

---

## Skipping / Postponing

### Skip Next Occurrence

Long-press recurring task → "Skip next"

- Advances due date to following rule match
- No new task created; current task rescheduled
- Activity log: "Skipped occurrence"

### Reschedule Current (Drag/Drop or Date Picker)

- Changes due date of **current occurrence only**
- Next occurrence still calculated from **original rule schedule**, not new date
- Example: Weekly Monday task moved to Wednesday → next still Monday

---

## Recurrence in Other Views

### Today / Upcoming

- Shows only **current active occurrence**
- Next occurrence not visible until current completed
- Overdue recurring tasks show in Overdue group

### Search / Filters

- `recurring:true` / `recurring:false`
- Search matches current occurrence title

### Project / Section

- Recurring tasks appear in assigned project/section
- Moving recurring task to another project: moves series (current + future)

### Labels

- Labels apply to series (current + future)

---

## Recurrence Generation Details

### Next Due Date Calculation

Algorithm: Given current due date + RRULE, find next matching date **after** current due date.

Edge cases:
- `BYMONTHDAY=31` in 30-day month → last day of month
- `BYMONTHDAY=-1` → last day of month
- `FREQ=WEEKLY;BYDAY=MO` on Monday → next Monday (7 days)
- Time preserved from original task

### Timezone

- Due dates stored in UTC
- Recurrence calculated in user's local timezone
- DST transitions handled by platform calendar library

### Generation Timing

- Next occurrence generated **immediately on completion** (synchronous, local DB)
- No background job; no delay

---

## Business Rules

- Only active (incomplete) tasks generate next occurrence
- Completing a recurring task with `COUNT` reached: marks complete, **no next generated**
- Completing a recurring task past `UNTIL`: marks complete, **no next generated**
- Recurring tasks can have subtasks; subtasks **not** recurring by default
- Recurring tasks can have reminders; reminders copied to next occurrence. **See specs19-reminders.md for scheduling details.**
- Recurring tasks can have attachments; attachments **not** copied (user re-attaches)
- Parent task completion does not auto-complete subtasks
- Deleting project with recurring tasks: confirms disposition per task (move/delete series)

---

## Error States

| Cause | Behavior |
|-------|----------|
| Invalid RRULE on save | Inline error: "Invalid recurrence rule" |
| Next date calculation fails | Log error; task completes but no next generated; toast "Could not schedule next" |
| DB error on generation | Rollback; task not marked complete; toast "Failed to complete" |

---

## User Interactions

| Action | Result |
|--------|--------|
| Tap recurrence in Task Detail | Open Recurrence Editor |
| Complete recurring task | Generate next; show undo |
| Undo completion | Remove generated; restore current |
| Skip next | Advance due date to following match |
| Edit recurrence | Choose scope (this / this+future) |
| Remove recurrence | Task becomes one-time |
| Delete recurring task | Choose scope (this / all future) |
| Drag recurring task (Upcoming) | Reschedule current only |

---

## Accessibility

Recurring tasks should:
- Announce "Recurring, repeats [summary]"
- Expose recurrence editor to screen readers
- Announce "Next occurrence generated for [date]" on completion
- Undo announced with "Restored recurring task"

---

## Performance Requirements

- Next occurrence generation < 100ms
- RRULE parsing cached per rule string
- Human-readable summary computed on demand (cached)
- Upcoming view: only current occurrences queried

---

## Backup / Export

- `recurringRule` exported with task
- On import: rule preserved; next occurrence calculated from imported due date
- If imported task completed: no auto-generation (user must complete manually)

---

## Navigation Summary

```
Task Detail
└── Recurrence Editor
    ├── Frequency Picker
    ├── Interval Input
    ├── By-Day/Month Selectors
    ├── End Condition Picker
    ├── Save → Scope Confirm
    └── Remove Recurrence
```

---

## Success Criteria

The Recurring Tasks feature succeeds when users can:
- Create any common recurrence pattern in < 30 seconds
- Trust that completing a task schedules the next correctly
- See clear visual indicators of recurrence everywhere
- Modify or stop recurrence without losing history
- Handle edge cases (month ends, DST) correctly
- Include recurrence rules in manual backups