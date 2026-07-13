# Repository Guidelines

## Project Structure & Module Organization

This repository defines the Mydo app through Markdown specifications. `SPECIFICATION.md` is the entry point; use it to orient readers, then keep the detailed requirements in `specs/`. Files follow the `specsNN-topic.md` pattern, for example `specs/specs03-home-screen.md` and `specs/specs11-data-model.md`. Keep related requirements in the existing topic file rather than creating overlapping documents. There is no application source, asset directory, or generated output committed here.

## Implementation Plan

Implement MyDo as a native Android, local-first application. Treat the local database as the single source of truth: do not add accounts, authentication, servers, automatic sync, collaboration, or network-dependent task behavior unless the specifications are explicitly expanded.

### 1. Establish the Android foundation

- Create a Kotlin Android project using Jetpack Compose and Material 3, with a minimum supported Android version chosen from current product requirements.
- Define a layered structure: Compose UI and view models, domain use cases, repository interfaces, Room-backed local storage, and Android platform adapters for notifications, file selection, sharing, and attachments.
- Add dependency injection, coroutine/Flow-based reactive state, structured error reporting, and a test configuration before implementing screens.
- Encode the specified visual tokens and reusable components first: typography, theme modes, priority colors, 8dp spacing, 48dp touch targets, task rows, circular completion controls, bottom sheets, dialogs, snackbars, and loading/empty/error states.

### 2. Build and validate the local data layer

- Model the entities and relationships in `specs/specs11-data-model.md`: projects, sections, tasks and subtasks, labels and task-label links, filters, reminders, attachments, activity events, notifications, preferences, and local search/recent-history data where needed.
- Use immutable UUIDs, foreign keys, transactions, UTC timestamps, migrations, and indexes for active tasks, due dates, project/section membership, reminder triggers, and search. Enforce that a task section belongs to its assigned project and that deleting a parent cannot orphan children.
- Implement repositories and use cases for task/project CRUD, reordering, completion and undo, recurrence, filtering, and atomic updates that refresh all dependent views immediately.
- Add database migration, repository, and referential-integrity tests. Test with a large local dataset before optimizing UI screens.

### 3. Deliver the task-management MVP

- Implement application launch, database recovery states, state restoration, Android back behavior, and primary navigation for Inbox, Today, Upcoming, Projects, and Search.
- Build the task composer as the globally available quick-add bottom sheet. It must save a title-only task to Inbox, inherit project/date context when launched from a project or Upcoming day, preserve a failed draft, and offer Undo after creation.
- Implement the shared Task Detail editor and task-list behavior: title, description, completion with a three-second undo window, due date/time, priority, project/section, labels, subtasks, delete/duplicate/move actions, and local activity history.
- Ship Inbox, Today (due today plus overdue), project and section lists, and project create/edit/archive/delete flows. Confirm destructive operations and define task disposition before deleting a project or section.

### 4. Add planning, organization, and discovery

- Implement recurring-task generation, reminder data, attachments through Android's document APIs, task ordering, bulk operations, and drag/reorder behavior where supported.
- Build Upcoming as a lazy chronological timeline with overdue grouping, selected-date task creation, direct rescheduling, and date/calendar navigation. Keep scheduled and recurrence changes consistent across every list.
- Build local search across tasks, notes, projects, sections, labels, and filters. Include incremental, debounced, case-insensitive partial matching; ranking; result-type navigation; recent searches; filters; and large-dataset performance tests.
- Add labels, saved filters, the Notifications history screen, and Settings for general, appearance, productivity, notification, privacy, help, and about preferences. Persist each setting locally and apply it immediately when applicable.

### 5. Integrate platform features and data portability

- Schedule local Android reminders only after required notification permission is granted. Support open, complete, and snooze notification actions; reschedule reminders after task edits, recurrence completion, reboot, and app update as required by Android.
- Implement manual export as a complete, versioned backup with integrity metadata, using the system save/share flow. Include every specified entity and preference.
- Implement import using the system file picker. Validate version and integrity before any mutation; make replace transactional and create a precautionary backup when possible. If merge is shipped, define deterministic ID-conflict reporting and never silently overwrite an existing record. Invalid or inaccessible backups must leave the database unchanged.
- Use platform-protected storage and Android document permissions for attachments and backups. Verify all normal task workflows with networking disabled.

### 6. Harden, verify, and release incrementally

- For each feature, cover success, empty, loading, validation, database-error, permission-denied, and recovery states; preserve drafts and navigation context where the specifications require it.
- Add unit tests for domain rules and backup validation, Room integration tests for transactions/migrations, Compose UI tests for primary flows, and instrumented tests for reminders, import/export, rotation/process restoration, and Android back navigation.
- Audit TalkBack labels and announcements, keyboard navigation, focus restoration, 200% text scaling, contrast, and touch target sizes. Profile scrolling, search, and Upcoming with tens of thousands of tasks.
- Deliver in small vertical slices: foundation/data layer; task capture and lists; task detail/projects; planning/search; reminders/settings/backup; accessibility and release hardening. At the end of each slice, update the affected specifications if an approved product decision changes a requirement, then run the checks below and manually review the rendered Markdown and Android flows.

### Definition of done

A release candidate is ready only when a user can create, organize, schedule, complete, search, and restore local tasks without connectivity; all changes persist immediately; reminders and backups behave safely; destructive actions are confirmed and undoable where specified; and the primary flows meet the documented accessibility, performance, and state-handling requirements.

## Development and Validation

There are currently no build, development-server, formatter, or automated test commands. Before submitting a documentation change, use lightweight checks:

```bash
git diff --check          # detect whitespace errors
rg "term to verify" specs # check terminology and cross-references
git diff -- specs/         # review the proposed requirement changes
```

Preview edited Markdown in a renderer when possible. Verify heading levels, links, lists, and code blocks render clearly.

## Writing Style & Naming Conventions

Use concise Markdown with sentence-case headings and direct, product-focused language. Preserve the established file naming scheme: lowercase, hyphenated topic names, prefixed by a two-digit section number (`specs08-search.md`). Prefer one requirement per bullet or short paragraph. Use backticks for UI labels, commands, data fields, and paths. Maintain consistent terminology across files; update the data model and user flows when a feature change affects them.

## Specification Review Guidelines

Treat review as the test suite. Check a change against the overview, navigation, design system, data model, and user-flow documents where relevant. Requirements should describe expected behavior, states, edge cases, and user-visible outcomes—not implementation guesses. Search `specs/` for renamed screens, fields, or concepts and update every affected reference.

## Commit & Pull Request Guidelines

The available history uses concise, scoped summaries such as `Codex: edited specifications`. Follow that style with an imperative description, for example `Specs: clarify task completion behavior`. Keep commits focused on one coherent feature or correction.

Pull requests should explain the user-facing change, list the specification files touched, and note cross-document updates. Link the relevant issue when one exists. Include screenshots only when the change includes rendered designs or UI artifacts.
