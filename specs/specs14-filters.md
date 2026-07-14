# /specs/14-filters.md

# Saved Filters Specification

## Purpose

Saved Filters allow users to define and reuse complex task queries. A filter stores a query string and optionally a favorite state for quick access from navigation.

---

## Goals

- Create, edit, and delete saved filters
- Execute filters to view matching tasks
- Pin favorite filters for quick access
- Support rich query syntax (projects, labels, dates, priorities, etc.)
- Persist filters locally; include in backups

---

## Navigation

```
Application

↓

Filters (secondary nav)

├── Filter List
├── Create Filter
├── Edit Filter
└── Filter Results
```

Filters accessible from Projects menu or dedicated Filters destination.

---

## Screen Layout

```
┌────────────────────────────────────┐
│ Filters                     +      │
├────────────────────────────────────┤
│ ★ Today & P1              (fav)    │
│ ★ Overdue                 (fav)    │
│   Work - High Priority            │
│   Personal - This Week            │
│   No Due Date                     │
│   @Waiting                        │
│                                  │
│ + Create Filter                   │
└────────────────────────────────────┘
```

- Star (★) indicates favorite (pinned to top)
- Tap filter to execute
- Long-press for context menu

---

## Filter List

Displays all saved filters with:
- Favorite star (toggle)
- Filter name
- Optional: result count badge (async, cached)

Sorting: Favorites first (manual order), then alphabetical.

---

## Create Filter

Flow:

```
Tap +

↓

Enter Name
Enter Query
Toggle Favorite

↓

Save

↓

Filter Appears in List
```

Fields:
- **Name** (required, max 50 chars, unique)
- **Query** (required, see Query Syntax below)
- **Favorite** (optional, default off)

Query validation on save: invalid syntax shows inline error.

---

## Edit Filter

Flow:

```
Long-press Filter

↓

Edit Filter

↓

Modify Name / Query / Favorite

↓

Save

↓

List Updates
```

---

## Delete Filter

Flow:

```
Long-press Filter

↓

Delete Filter

↓

Confirm

↓

Filter Removed
```

Deleting a filter does not affect tasks.

---

## Query Syntax

Filters use the same query language as Search (specs08-search.md).

### Supported Fields

| Field | Syntax | Example |
|-------|--------|---------|
| Text search | (bare words) | `report budget` |
| Project | `project:` | `project:Work` |
| Section | `section:` | `section:Planning` |
| Label | `label:` or `@` | `label:urgent` / `@urgent` |
| Priority | `priority:` or `p` | `priority:1` / `p1` |
| Due date | `due:` | `due:today`, `due:tomorrow`, `due:2026-07-15` |
| Due before | `duebefore:` | `duebefore:2026-07-20` |
| Due after | `dueafter:` | `dueafter:2026-07-01` |
| No due date | `nodue:` | `nodue:true` |
| Completed | `completed:` | `completed:false` |
| Created date | `created:` | `created:2026-07-01` |
| Recurring | `recurring:` | `recurring:true` |
| Has attachment | `hasattachment:` | `hasattachment:true` |
| Has subtasks | `hassubtasks:` | `hassubtasks:true` |
| In project (any) | `hasproject:` | `hasproject:true` |
| In Inbox | `inbox:` | `inbox:true` |

### Operators

- **Implicit AND**: Space-separated terms = AND
- **OR**: `,` (comma) within same field: `label:work,personal`
- **NOT**: `-` prefix: `-label:done` or `-project:Archive`
- **Grouping**: Parentheses: `(project:Work project:Personal) label:urgent`

### Date Values

- Relative: `today`, `tomorrow`, `yesterday`, `week`, `month`, `overdue`
- Absolute: `YYYY-MM-DD`
- Ranges: `due:2026-07-01..2026-07-31`

### Examples

| Query | Meaning |
|-------|---------|
| `p1 due:today` | Priority 1 tasks due today |
| `project:Work @urgent` | Work tasks with @urgent label |
| `-completed:true duebefore:tomorrow` | Incomplete tasks due before tomorrow |
| `nodue:true recurring:false` | One-time tasks with no due date |
| `created:2026-07-01..2026-07-31 label:@phone` | July tasks with @phone label |

---

## Filter Results View

Executing a filter opens a task list identical to Inbox/Project list:
- Same task row layout
- Same sorting options
- Same bulk operations
- Title bar shows filter name and favorite toggle
- Pull-to-refresh re-executes query

### Empty State

```
No tasks match "Work - High Priority"

Try adjusting your filter or creating a task.
```

---

## Favorite Filters

Favorite filters:
- Appear at top of Filter List
- May appear in Home screen quick-access (configurable in Settings)
- Show in Search results (specs08-search.md)
- Star icon toggles favorite state inline

---

## Filter in Task Composer / Quick Add

When creating a task from a filter results view:
- New task does NOT auto-apply filter criteria
- User must manually set properties
- (Future: "Inherit filter context" setting)

---

## Business Rules

- Filter names unique (case-insensitive)
- Query validated on save; invalid queries rejected with error
- Maximum 100 saved filters per database
- Filters included in manual export/import
- Favorite state persisted locally
- Filter execution queries local database only

---

## Error States

| Cause | Recovery |
|-------|----------|
| Invalid query syntax | Inline error on field; prevent save |
| Duplicate name | Inline error: "Name already exists" |
| Database error on load | Retry; show cached list if available |
| Query execution error | Show error in results view; offer retry |

---

## User Interactions

| Action | Result |
|--------|--------|
| Tap filter | Execute query; open results |
| Long-press filter | Context menu: Edit, Delete, Toggle Favorite, Duplicate |
| Tap star | Toggle favorite |
| Pull-to-refresh (results) | Re-execute query |
| Tap result task | Open Task Detail |
| Tap filter name in results bar | Return to Filter List |

---

## Accessibility

Filters should:
- Announce favorite state changes
- Expose query as description for screen readers
- Support keyboard navigation in list and editor
- Preserve focus after edit/delete
- Respect font scaling

---

## Performance Requirements

- Filter list loads in < 100ms
- Query execution < 200ms for 10k tasks
- Result count badge updates asynchronously (debounced 500ms)
- Favorites sorted without full re-query

---

## Navigation Summary

```
Filters
├── Filter List
│   ├── Create Filter
│   ├── Edit Filter
│   ├── Delete Filter
│   └── Toggle Favorite
└── Filter Results
    ├── Task List
    │   ├── Task Detail
    │   ├── Bulk Operations
    │   └── Sort
    └── Filter Bar (name, favorite, back)
```

---

## Success Criteria

The Saved Filters feature succeeds when users can:
- Define complex queries once and reuse them instantly
- Access favorite filters with one tap from multiple entry points
- Trust that filter results are always current with local data
- Share filter definitions via manual backup/restore
- Build filters using the same syntax as global search