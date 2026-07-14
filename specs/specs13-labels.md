# /specs/13-labels.md

# Labels Specification

## Purpose

Labels provide a flexible, project-independent way to categorize and filter tasks. Unlike projects, labels are reusable across all tasks and support many-to-many relationships.

---

## Goals

- Create, edit, and delete local labels
- Assign and remove labels from tasks quickly
- Filter tasks by label
- Maintain label colors for visual identification
- Persist all label data locally

---

## Navigation

```
Application

↓

Labels

├── Label List
├── Create Label
├── Edit Label
└── Label Tasks
```

Labels are accessible from the secondary navigation (Projects menu or dedicated Labels destination).

---

## Screen Layout

```
┌────────────────────────────────────┐
│ Labels                      +      │
├────────────────────────────────────┤
│  ● Work                    12      │
│  ● Personal                 5      │
│  ● Urgent                   3      │
│  ● Phone                    8      │
│  ● Errands                  2      │
│                                  │
│  + Create Label                  │
└────────────────────────────────────┘
```

---

## Label List

Displays all local labels with:
- Color indicator (●)
- Label name
- Active task count (optional)

Labels are sorted alphabetically by default; user may reorder manually.

---

## Create Label

Flow:

```
Tap +

↓

Enter Name
Select Color

↓

Save

↓

Label Appears in List
```

Fields:
- **Name** (required, unique, max 50 characters)
- **Color** (required, select from predefined palette)

Predefined color palette:
- Red, Orange, Yellow, Green, Teal, Blue, Indigo, Purple, Pink, Gray

---

## Edit Label

Flow:

```
Long-press Label

↓

Edit Label

↓

Modify Name / Color

↓

Save

↓

List Updates
```

Changing a label name or color updates all associated tasks immediately.

---

## Delete Label

Flow:

```
Long-press Label

↓

Delete Label

↓

Confirm

↓

Label Removed from All Tasks
```

Deleting a label removes it from all tasks but does not delete the tasks themselves. Confirmation dialog required.

---

## Label Picker (Task Detail / Composer)

Opened from Task Detail or Task Composer when assigning labels.

Layout:

```
┌────────────────────────────────────┐
│ Labels                      Done   │
├────────────────────────────────────┤
│ 🔍 Search labels...                │
├────────────────────────────────────┤
│ ● Work                    ✓        │
│ ● Personal                       │
│ ● Urgent                  ✓        │
├────────────────────────────────────┤
│ + Create Label                     │
└────────────────────────────────────┘
```

Features:
- Search/filter existing labels
- Multi-select with checkboxes
- Create new label inline
- Selected labels show checkmark
- Tap "Done" to apply

---

## Label Tasks View

Selecting a label from the Labels list or Search opens a filtered task list.

Layout mirrors the Inbox/Project task list:
- Shows only active tasks with the selected label
- Supports same sorting, completion, and bulk operations
- Title bar shows label color and name

---

## Empty State

If no labels exist:

```
No Labels Yet

Labels help you categorize tasks across projects.

Create Label
```

---

## Loading State

Characteristics:
- Skeleton label rows
- Disabled interactions
- Loading indicator

---

## Error State

Possible causes:
- Local database error

Recovery actions:
- Retry
- Continue using available labels

---

## User Interactions

| Action | Result |
|--------|--------|
| Tap label | Open Label Tasks view |
| Long-press label | Open context menu (Edit, Delete) |
| Tap + | Create Label |
| Tap label in picker | Toggle selection |
| Tap Create Label in picker | Create Label inline |
| Pull to refresh | Reload local labels |

---

## Accessibility

Labels should:
- Announce color and name
- Expose task count to screen readers
- Support keyboard navigation
- Preserve focus after edit/delete
- Respect system font scaling
- Maintain sufficient contrast for color indicators

---

## Performance Requirements

Labels should:
- Load instantly from local database
- Update task associations immediately
- Support thousands of labels without degradation
- Debounce search in picker (300ms)

---

## Business Rules

- Label names are unique (case-insensitive)
- A task may have zero or more labels
- Labels exist independently of projects
- Deleting a label never deletes tasks
- Label changes persist locally immediately
- Labels included in manual backup/export

---

## Navigation Summary

```
Labels
├── Label List
│   ├── Create Label
│   ├── Edit Label
│   └── Delete Label
├── Label Picker (from Task Detail/Composer)
│   ├── Search Labels
│   ├── Select/Deselect
│   └── Create Label
└── Label Tasks View
    ├── Task List
    └── Task Detail
```

---

## Success Criteria

The Labels feature succeeds when users can:
- Create and manage labels without leaving the current task flow
- Visually distinguish labels by color across all views
- Filter tasks by label from multiple entry points
- Trust that label changes persist locally and immediately
- Include labels in manual backups and restores