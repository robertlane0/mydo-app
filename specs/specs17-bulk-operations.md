# /specs/17-bulk-operations.md

# Bulk Operations Specification

## Purpose

Bulk operations allow users to apply the same action to multiple tasks simultaneously, accelerating organization and cleanup workflows.

---

## Goals

- Select multiple tasks across all list views
- Apply common actions: move, label, priority, due date, complete, delete
- Provide clear feedback and undo for destructive actions
- Work consistently in Inbox, Today, Upcoming, Projects, Search, Filters, Labels

---

## Navigation

```
Any Task List

↓

Long-press / Select Mode

↓

Multi-Select Toolbar

├── Move to Project
├── Add Labels
├── Set Priority
├── Set Due Date
├── Complete
├── Delete
└── Clear Selection
```

---

## Entry Points

### Long-Press Selection

```
Long-press Task

↓

Selection Mode Activated

↓

Task Highlighted (checked)

↓

Toolbar Appears (bottom)
```

- First long-press enters selection mode
- Checkboxes appear on all visible tasks
- Selected count shown in toolbar

### Explicit Select Button

Overflow menu → "Select tasks" (alternative entry)

### Select All

In selection mode: "Select all" in toolbar (selects all loaded tasks; virtualized lists load more as needed)

---

## Selection Mode UI

### Toolbar (Bottom App Bar)

```
┌────────────────────────────────────────────┐
│ 12 selected                    ✕ Clear     │
├────────────────────────────────────────────┤
│ Move   Labels   Priority   Due   Complete  │
│                                             │
│         Delete          More ▼             │
└────────────────────────────────────────────┘
```

- Left: Selected count, Clear button
- Right: Action chips (primary) + Delete + More menu
- Toolbar floats above FAB; FAB hidden in selection mode

### Task Row in Selection Mode

```
☑  □ Finish Report          P1  📅 Tomorrow
   └─ Checkbox replaces completion circle
```

- Leading checkbox (replaces completion checkbox)
- Row background tinted (selection color)
- Completion checkbox hidden; use "Complete" action instead

---

## Bulk Actions

### 1. Move to Project

```
Tap Move

↓

Project Picker (searchable list)

↓

Select Project (+ optional Section)

↓

Confirm: "Move 12 tasks to 'Work'?"

✓ Confirm

↓

Tasks Moved; Toast: "12 tasks moved to Work [Undo]"
```

- Tasks removed from Inbox if applicable
- Section optional; defaults to project root
- Recurring tasks: moves entire series (current + future)
- Undo: restores original project/section

### 2. Add Labels

```
Tap Labels

↓

Label Picker (multi-select, same as Task Detail)

✓ Done

↓

Confirm: "Add 3 labels to 12 tasks?"

✓ Confirm

↓

Labels Added; Toast: "Labels added [Undo]"
```

- Adds labels (does not replace existing)
- Create new label inline supported
- Undo removes added labels only

### 3. Set Priority

```
Tap Priority

↓

Priority Picker (P1/P2/P3/P4/None)

✓ Select

↓

Confirm: "Set priority P1 for 12 tasks?"

✓ Confirm

↓

Priority Updated; Toast: "Priority set to P1 [Undo]"
```

- "None" clears priority
- Undo restores previous priorities per task

### 4. Set Due Date

```
Tap Due

↓

Date Picker (Today, Tomorrow, Next Week, Custom, None)

✓ Select

↓

If Custom: Time Picker (optional)

↓

Confirm: "Set due date to Tomorrow for 12 tasks?"

✓ Confirm

↓

Due Dates Set; Toast: "Due date set [Undo]"
```

- "None" clears due date/time
- Time optional; if not set, uses default reminder time (Settings)
- Recurring tasks: sets due date on current occurrence only (series unchanged)

### 5. Complete

```
Tap Complete

↓

Confirm: "Complete 12 tasks?"

✓ Confirm

↓

All 12 Completed

↓

Toast: "12 tasks completed [Undo]"
```

- Recurring tasks: completes current occurrence, generates next
- Subtasks: not auto-completed (user must complete parent or subtasks individually)
- Undo: un-completes all; for recurring, removes generated next occurrence

### 6. Delete

```
Tap Delete

↓

Confirm Dialog:
"Delete 12 tasks permanently?
This cannot be undone from the trash.
Tasks in projects will be removed from those projects."

☐ Also delete subtasks
[Cancel]  [Delete]
```

- Destructive; no automated undo (see Undo section below)
- Subtasks: checkbox to include (default off)
- Deleted tasks not recoverable via app (only via backup import)
- Confirmation required; no "Delete" in More menu without confirmation

### 7. More Menu (Overflow)

Less common actions:
- Duplicate
- Archive (if in project)
- Set Reminder
- Remove Labels
- Clear Due Date
- Clear Priority

---

## Undo System

### Undo Snackbar

Shown after every bulk action (except Delete):

```
┌─────────────────────────────────────┐
│ 12 tasks moved to Work    [Undo]    │
└─────────────────────────────────────┘
```

- Duration: 5 seconds
- Swipe to dismiss
- Tap Undo: reverses entire bulk operation atomically
- After undo: selection mode exits, list restored

### Undo Scope

| Action | Undo Reverses |
|--------|---------------|
| Move | Project/section assignment |
| Add Labels | Only added labels (keeps pre-existing) |
| Set Priority | Previous priority per task |
| Set Due Date | Previous due date/time per task |
| Complete | Un-completes; removes generated recurring next |
| Delete | **No undo** (destructive confirmation) |

---

## Selection Behavior Details

### Cross-View Selection

- Selection is **per-list**; does not persist across navigation
- Leaving list (back, tab switch) clears selection
- Search/Filter within list: selection preserved on visible items

### Virtualized Lists

- "Select All" selects currently loaded items
- Scroll to load more → new items unselected
- "Select All Loaded" vs "Select All (X total)" distinction in toolbar

### Mixed States

- Some tasks may not support an action (e.g., completed tasks can't be completed again)
- Toolbar actions disabled if **no selected task** supports them
- If **some** support: action applies to supported subset; toast reports count

---

## Business Rules

- Maximum selection: 500 tasks (configurable; prevents accidental mass ops)
- Bulk operations run in single database transaction
- All changes persist locally immediately
- Activity log: one entry per task per action (e.g., 12 "Priority changed" events)
- Recurring tasks: bulk complete generates next for each; bulk move moves series
- Subtasks: not auto-selected when parent selected; must select explicitly

---

## Error States

| Cause | Behavior |
|-------|----------|
| DB error mid-operation | Rollback; toast "Operation failed; no changes made" |
| Partial failure (e.g., permission) | Process supported tasks; report "X of Y succeeded" |
| Selection limit exceeded | Disable "Select All"; show limit in toolbar |
| App backgrounded during op | Operation completes; result shown on return |

---

## User Interactions

| Action | Result |
|--------|--------|
| Long-press task | Enter selection mode; select task |
| Tap checkbox (selection mode) | Toggle task selection |
| Tap task (selection mode) | Toggle selection (configurable: tap = select, not open) |
| Tap toolbar action | Execute bulk action |
| Tap Clear / ✕ | Exit selection mode |
| Back gesture | Exit selection mode |
| Pull-to-refresh | Exit selection mode; refresh list |

---

## Accessibility

Bulk operations should:
- Announce "Selection mode, X tasks selected"
- Expose checkbox state to screen readers
- Toolbar actions announced with selected count
- Undo snackbar announced; focus moves to Undo
- Confirm dialogs trap focus; clear labels
- Support keyboard: Space to toggle, Enter to act, Escape to clear

---

## Performance Requirements

- Selection mode toggle < 50ms
- Bulk action execution < 200ms for 100 tasks (transaction)
- "Select All" on 10k tasks: loads in batches; UI responsive
- Undo reversal < 200ms
- No main-thread DB writes

---

## Navigation Summary

```
Task List
└── Selection Mode
    ├── Toolbar (Count, Clear, Actions)
    ├── Task Rows (Checkboxes)
    ├── Action Flows
    │   ├── Move → Project Picker → Confirm → Toast+Undo
    │   ├── Labels → Label Picker → Confirm → Toast+Undo
    │   ├── Priority → Picker → Confirm → Toast+Undo
    │   ├── Due Date → Picker → Confirm → Toast+Undo
    │   ├── Complete → Confirm → Toast+Undo
    │   ├── Delete → Confirm Dialog → Execute (no undo)
    │   └── More → [Duplicate, Archive, Reminder, Clear...]
    └── Exit (Back, Clear, Navigate Away)
```

---

## Success Criteria

The Bulk Operations feature succeeds when users can:
- Select and act on dozens of tasks in seconds
- Recover from mistakes via consistent 5-second undo
- Apply any task property change in bulk
- Trust that destructive actions require explicit confirmation
- Use bulk operations identically across all task lists
- Complete the workflow without leaving the current view