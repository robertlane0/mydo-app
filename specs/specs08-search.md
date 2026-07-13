# /specs/08-search.md

# Search Specification

## Purpose

The Search module provides a unified interface for locating locally stored tasks, projects, labels, notes, and other user-created content.

Search is designed to return relevant results instantly while allowing users to navigate directly to the desired object.

It serves as the primary discovery mechanism for large task collections.

---

# Goals

The Search module allows users to:

- Find tasks quickly
- Locate projects
- Search completed work
- Discover labels and filters
- Access recently viewed items
- Navigate directly to search results

---

# Navigation

```
Application

↓

Search

├── Search Field
├── Recent Searches
├── Suggestions
├── Results
└── Result Detail
```

Search is accessible globally from any major application screen.

---

# Screen Layout

```
┌────────────────────────────────────┐
│ ← Search                           │
├────────────────────────────────────┤
│ 🔍 Search tasks...                 │
├────────────────────────────────────┤
│ Recent Searches                    │
│ • Marketing                        │
│ • Invoice                          │
│ • Priority 1                       │
├────────────────────────────────────┤
│ Results                            │
│                                    │
│ □ Finish Invoice                   │
│ 📁 Marketing Website               │
│ 🏷 Urgent                          │
│                                    │
└────────────────────────────────────┘
```

---

# Entry Points

Search may be opened from:

- Home
- Inbox
- Projects
- Today
- Upcoming
- Notifications

Opening Search should preserve the previous navigation state.

---

# Search Field

The search field receives focus immediately.

Characteristics:

- Single-line input
- Placeholder text
- Clear button
- Voice input (where supported)

Example placeholder:

```
Search tasks, projects, or labels
```

---

# Search Behavior

```
User Types

↓

Query Updates

↓

Results Refresh

↓

Select Result

↓

Navigate
```

Results update incrementally as the query changes.

---

# Supported Content Types

Search may return:

- Tasks
- Projects
- Sections
- Labels
- Filters
- Task notes

Each result type should have a distinct visual representation.

---

# Task Results

Task results display:

- Title
- Project
- Due date
- Priority
- Completion state

Selecting a task opens Task Detail.

---

# Project Results

Project results display:

- Name
- Color
- Icon
- Task count (optional)

Selecting a project opens the Project screen.

---

# Label Results

Label results display:

- Label name
- Associated color
- Matching task count (optional)

Selecting a label opens the filtered task list.

---

# Filter Results

Saved filters appear alongside other search results.

Example:

```
Today & P1
```

Selecting a filter opens the corresponding filtered task view.

---

# Recent Searches

When no query is entered:

```
Recent Searches

↓

Recently Viewed

↓

Suggested Results
```

Users may:

- Reopen previous searches
- Remove individual entries
- Clear search history

---

# Suggestions

As the user types, suggestions may include:

- Matching task names
- Project names
- Labels
- Frequently accessed items

Suggestions update in real time.

---

# Result Ranking

Results should prioritize:

1. Exact matches
2. Prefix matches
3. Partial matches
4. Recently accessed items
5. Frequently used content

Task completion status may influence ranking.

---

# Search Filters

Optional filters include:

- Tasks
- Projects
- Labels
- Due Date
- Priority
- Completed Status

Filters narrow visible results without changing the search query.

---

# Empty State

If no matches exist:

```
No results found.

Try another keyword.
```

Optional suggestions:

- Remove filters
- Correct spelling
- Search shorter terms

---

# Loading State

Characteristics:

- Loading indicator
- Placeholder results
- Incremental updates

Existing results should remain visible until replacement data is available.

---

# Local-Only Behavior

Search queries the local database and returns all locally stored supported content. No remote content or connection is required.

---

# Error State

Possible causes:

- Local database error

Recovery actions:

- Retry
- Continue searching available local content

---

# Voice Search

Where supported:

```
Tap Microphone

↓

Speak Query

↓

Recognize Speech

↓

Display Results
```

Recognition failures should allow immediate text editing.

---

# User Interactions

| Action | Result |
|----------|--------|
| Type query | Update results |
| Select result | Open destination |
| Tap clear | Remove query |
| Tap recent search | Execute search |
| Remove recent search | Delete history entry |
| Apply filter | Narrow results |

---

# Accessibility

Search should:

- Automatically focus the search field
- Announce result counts
- Expose search suggestions to screen readers
- Support keyboard shortcuts
- Maintain logical focus when results update
- Respect system font scaling

---

# Performance Requirements

Search should:

- Begin returning results immediately as the user types
- Efficiently search large local datasets
- Debounce rapid input to avoid unnecessary processing
- Cache recent queries
- Open selected results with minimal latency

---

# Business Rules

- Search is case-insensitive.
- Partial word matching is supported.
- Search results reflect all locally stored content.
- Completed items may be hidden or shown according to active filters.
- Recent searches are stored locally and may be cleared by the user.

---

# Navigation Summary

```
Search

├── Recent Searches
├── Suggestions
├── Task Detail
├── Project
├── Label View
└── Filter View
```

---

# Success Criteria

The Search feature succeeds when users can:

- Locate any task or project within seconds
- Navigate directly to relevant content
- Refine results using filters and suggestions
- Reuse previous searches efficiently
- Reliably search all local content without a connection
- Scale to thousands of tasks without noticeable performance degradation
