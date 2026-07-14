# /specs/18-drag-reorder.md

# Drag & Reorder Specification

## Purpose

Drag-and-drop reordering allows users to manually organize tasks, projects, and sections by priority, workflow order, or personal preference. Works with mouse, touch, and keyboard.

---

## Goals

- Reorder tasks within a list (Inbox, Today, Project, Section, Filter)
- Reorder sections within a project
- Reorder projects in the project list
- Move tasks between sections/projects via drag
- Reschedule tasks in Upcoming by dragging between dates
- Full keyboard and accessibility support

---

## Supported Contexts

| Context | Reorderable Items | Drag Targets |
|---------|-------------------|--------------|
| Inbox / Today / Filter / Search Results | Tasks | Task positions |
| Project (List view) | Tasks | Task positions |
| Project (Board view) | Tasks | Columns (sections) + positions |
| Project List | Projects | Project positions |
| Project Sections | Sections | Section positions |
| Upcoming | Tasks | Date groups (reschedule) |

---

## Visual Feedback

### Drag Handle

- All draggable items show a drag handle (⋮⋮) on long-press or focus
- Handle always visible on hover (desktop) or after long-press (touch)
- 48x48dp touch target for handle

### Drag Ghost

```
┌─────────────────────────────┐
│ ⋮⋮  Finish quarterly report │  ← Semi-transparent, follows finger
└─────────────────────────────┘
```

- Item lifts with elevation (8dp shadow)
- Slight scale (1.02x) and rotation (±2°)
- Original position shows drop placeholder

### Drop Placeholder

```
□ Task A
□ ─────────────────  ← Animated gap (height of item)
□ Task B
□ Task C
```

- Gap animates open as drag enters valid zone
- Color: Primary brand color (MyDo Red) at 20% opacity

### Drop Zones

| Zone | Indicator |
|------|-----------|
| Between items | Expanding gap |
| On section header (Board) | Header highlights |
| On date header (Upcoming) | Date header pulses |
| On project (Project List) | Project row highlights |

---

## Interaction Patterns

### Touch (Mobile/Tablet)

```
Long-press Item (500ms)

↓

Haptic Feedback (Light)

↓

Item Lifts (Drag Start)

↓

Drag to Position

↓

Release → Drop

↓

Haptic Feedback (Success)

↓

Animation to Final Position
```

- Scroll during drag: auto-scroll at list edges (30px threshold)
- Cancel: drag to screen edge or back button

### Mouse (Desktop/ChromeOS)

```
Hover Item → Show Handle

↓

Click + Drag Handle

↓

Drag to Position

↓

Release → Drop
```

- No long-press needed
- Right-click context menu also has "Move" option

### Keyboard

```
Focus Item (Tab / Arrow Keys)

↓

Ctrl/Cmd + Arrow Up/Down  → Move Up/Down

↓

Ctrl/Cmd + Shift + Arrow  → Move to Top/Bottom

↓

Enter/Space on Handle    → Enter Move Mode
  Arrow Keys             → Navigate
  Enter                  → Confirm Position
  Escape                 → Cancel
```

- Move mode announced: "Move mode. Use arrow keys to reposition. Enter to confirm."

---

## Reordering Tasks (List View)

### Within Same List

- Drag task vertically
- Drop between any two tasks
- Order saved as `Task.order` (integer, gap-based for O(1) inserts)
- Immediate DB update; other views react via Flow/LiveData

### Move to Section (Board View)

- Drag task onto section column header or into column
- Column header highlights on drag-enter
- Drop inside column: appends to end (or at position)
- Task's `sectionId` updated; `order` recalculated

### Move to Project

- Long-press → "Move to Project" in context menu (not drag)
- Drag between projects not supported directly (different lists)
- Use "Move" action from selection mode or Task Detail

---

## Reordering Sections (Project)

### List View

- Sections shown as headers with drag handles
- Drag section header vertically
- All tasks in section move with it
- `Section.order` updated

### Board View

- Sections are columns; reorder columns horizontally
- Drag column header
- Columns animate smoothly

---

## Reordering Projects (Project List)

- Long-press project row → drag vertically
- Favorites pinned at top; non-favorites below
- Cannot drag favorite into non-favorite zone or vice versa
- `Project.order` updated

---

## Rescheduling in Upcoming (Drag Between Dates)

### Behavior

- Drag task from one date group to another
- Target date group highlights
- Drop: task's `dueDate` updated to target date
- Time preserved; if target is "No Date", clears due date

### Visual

```
Today
□ Task A
□ ─────────────  ← Drop here → Updates to "Tomorrow"

Tomorrow
□ Task B
```

### Constraints

- Cannot drag to past dates (Overdue group) — shows error toast
- Cannot drag recurring task's current occurrence (creates detached instance instead)
- Completed tasks not draggable

---

## Sorting vs Manual Order

### Sort Modes

| Mode | Behavior |
|------|----------|
| Manual | Drag reorder enabled; `order` field authoritative |
| Due Date | Drag disabled; sorted by dueDate asc |
| Priority | Drag disabled; sorted by priority desc, dueDate asc |
| Name | Drag disabled; sorted alphabetically |
| Created | Drag disabled; sorted by createdAt desc |

### Switching Modes

- Changing from Manual → Due Date: preserves `order` values
- Changing to Manual: restores last manual order
- User can always return to Manual to customize

---

## Multi-Select Drag

- Enter selection mode (long-press or toolbar)
- Select multiple tasks
- Drag any selected task → entire selection moves as block
- Drop: all selected tasks inserted at drop position, relative order preserved
- Block ghost shows item count: "3 tasks"

---

## Accessibility

### Screen Readers

- Announce "Drag handle, [task name]. Double-tap and hold to move."
- In move mode: "Move mode active. Item [name] at position X of Y."
- Arrow keys: "Moved to position X"
- Drop: "Placed at position X"

### Keyboard

- All drag operations achievable via keyboard
- Focus indicator visible on drag handle
- Move mode trap focus; Escape exits

### Touch Targets

- Drag handle: 48x48dp minimum
- Drop zones: full row height + 16dp margins

### Reduced Motion

- `prefers-reduced-motion`: disable lift/scale animation; instant placeholder
- Auto-scroll speed reduced

---

## Technical Implementation

### Order Storage

- `Task.order`: Integer (0, 1000, 2000...) — gap-based for efficient inserts
- `Section.order`: Integer (same)
- `Project.order`: Integer (same)
- Rebalance on conflict (e.g., insert between 1000 and 1001 → renumber)

### Database Updates

- Single transaction per drop
- `UPDATE` with `CASE` for batch reorder
- Emit Flow update; UI reacts without full reload

### Conflict Resolution

- Concurrent edits (rare, local-only): last-write-wins on `updatedAt`
- If order collision: rebalance affected range

---

## Error States

| Scenario | Handling |
|----------|----------|
| Drop on invalid target | Item animates back; toast "Can't move here" |
| DB error on drop | Item animates back; toast "Move failed"; order unchanged |
| Drag during sync (N/A) | N/A — local only |
| App backgrounded mid-drag | Drag cancelled; item returns |

---

## User Interactions Summary

| Gesture | Action |
|---------|--------|
| Long-press task | Start drag (touch) |
| Click drag handle | Start drag (mouse) |
| Drag vertically | Reorder in list |
| Drag to section header | Move to section |
| Drag to date header (Upcoming) | Reschedule |
| Ctrl+Arrow (keyboard) | Move up/down |
| Escape | Cancel drag/move mode |
| Multi-select + drag | Move block |

---

## Performance Requirements

- Drag start latency < 50ms
- 60fps during drag (placeholder animation)
- Drop commit < 100ms (DB + UI)
- Auto-scroll at 60fps
- No frame drops with 1000+ items (virtualized list)

---

## Navigation Summary

```
Drag & Reorder
├── Task List (Manual sort)
│   ├── Vertical Reorder
│   ├── Section Move (Board)
│   └── Multi-select Block Move
├── Project Sections
│   └── Vertical Reorder
├── Project List
│   └── Vertical Reorder (favorites locked)
└── Upcoming
    └── Date-to-Date Reschedule
```

---

## Success Criteria

The Drag & Reorder feature succeeds when users can:
- Reorder any list intuitively with touch, mouse, or keyboard
- Move tasks between sections in Board view naturally
- Reschedule in Upcoming by dragging between days
- Trust that manual order persists across views and restarts
- Access full functionality without a mouse
- See clear visual feedback at every step