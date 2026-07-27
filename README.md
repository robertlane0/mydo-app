# MyDo

**Local-first task management for Android.**

MyDo is a personal task-management application that helps you capture work, organize it into projects, schedule execution, and track completion — all on your device. There are no accounts, no cloud services, and no automatic sync. Your data stays where it belongs.

## Features

- **Quick capture** — A global FAB opens a bottom sheet so you can record a task in seconds. Title-only tasks land in your Inbox; context is inherited when opened from a project or date view.
- **Projects & Sections** — Group related work into projects with optional sections. Projects can be favorited, archived, or deleted with configurable task disposition.
- **Today & Upcoming** — See what's due today (including overdue items) or browse a lazy chronological timeline. Drag to reschedule.
- **Recurring tasks** — Define daily, weekly, monthly, or custom recurrence rules. Completion auto-generates the next occurrence; undo restores the original.
- **Priorities** — Four levels (P1–P4) with semantic colors: red, orange, blue, grey.
- **Labels & Filters** — Apply reusable, cross-project labels and create saved filter queries for dynamic task lists.
- **Search** — Full-text local search across tasks, projects, sections, labels, and filters with incremental, debounced, case-insensitive matching, ranking, and recent-search history.
- **Reminders** — Schedule absolute or relative (before due date) reminders. Local notifications support open, complete, and snooze actions. Reminders reschedule after edits, recurrence, reboot, and app update.
- **Attachments** — Add files via Android's document picker. Stored in platform-protected local storage.
- **Bulk operations** — Select multiple tasks to set priority, due date, move, complete, delete, or apply labels.
- **Drag reorder** — Reorder tasks and sections with drag-and-drop.
- **Activity history** — Every change is recorded as an activity event for local audit.
- **Backup & Restore** — Manual export of a complete, versioned backup with integrity metadata. Import validates version and integrity before touching data; replace is transactional with a precautionary backup.
- **Themes** — Light, Dark, or System mode with optional Material You dynamic color.
- **Accessibility** — TalkBack labels, keyboard navigation, 48dp touch targets, 200% text scaling, and WCAG AA contrast.

## Architecture

MyDo follows a clean-layered architecture with a local-first data model:

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose + ViewModels)        │
│  Inbox · Today · Upcoming · Projects    │
│  Search · Labels · Filters · Settings   │
│  Task Detail · Notifications            │
├─────────────────────────────────────────┤
│  Domain Layer (Use Cases + Models)      │
│  Task/Project CRUD · Recurrence ·       │
│  Search · Filter Evaluation · Backup    │
├─────────────────────────────────────────┤
│  Data Layer (Room Repositories + DAOs)  │
│  Task · Project · Section · Label ·     │
│  Filter · Reminder · Attachment · ...   │
├─────────────────────────────────────────┤
│  Platform Layer (Android Adapters)      │
│  Notifications · File Picker · Sharing  │
└─────────────────────────────────────────┘
```

- **UI** — Jetpack Compose with Material 3, `Navigation Compose`, and `ViewModel` per screen. Shared components live in `ui/components/`; theme tokens in `ui/theme/`.
- **Domain** — Plain Kotlin use cases and models with no Android dependencies. Repository interfaces define the contract; domain logic (recurrence calculation, filter query evaluation) is fully unit-testable.
- **Data** — Room database (`mydo.db`) with 12 entities, DAO abstractions, and repository implementations. Foreign keys enforce referential integrity; indexes cover active tasks, due dates, project/section membership, and reminder triggers. Schema version 3 with migrations.
- **Platform** — Android-specific adapters for notifications (`NotificationChannels`, `ReminderAlarmReceiver`, `BootCompletedReceiver`), attachments (`AttachmentGateway`), document picking, and sharing.
- **DI** — Hand-rolled dependency graph in `AppContainer` (no DI framework). Wired from a custom `Application` class to outlive any single Activity.

The database is the single source of truth. All reads use reactive `Flow` from Room; writes go through use cases that commit locally and update dependent views immediately.

## Repository Structure

```
├── app/
│   ├── build.gradle.kts          # App module build config
│   ├── schemas/                  # Room schema exports (versioned)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mydo/app/
│       │   │   ├── MainActivity.kt          # Single Activity entry point
│       │   │   ├── MydoApplication.kt       # Application class
│       │   │   ├── di/AppContainer.kt       # Dependency graph
│       │   │   ├── core/                    # Time provider, error types
│       │   │   ├── data/                    # Room DB, entities, DAOs, repos
│       │   │   ├── domain/                  # Models, use cases, repositories
│       │   │   ├── platform/                # Android adapters
│       │   │   └── ui/                      # Compose screens, components, theme
│       │   └── res/                         # Android resources
│       ├── test/                            # Unit tests (JUnit 4, Robolectric)
│       └── androidTest/                     # Instrumented tests
├── specs/                          # Product specifications (Markdown)
│   ├── specs00-overview.md
│   ├── specs01-navigation.md
│   ├── specs02-authentication.md
│   ├── specs03-home-screen.md
│   ├── specs04-inbox.md
│   ├── specs05-task-detail.md
│   ├── specs06-projects.md
│   ├── specs07-upcoming.md
│   ├── specs08-search.md
│   ├── specs09-notifications.md
│   ├── specs10-settings.md
│   ├── specs11-data-model.md
│   ├── specs11-design-system.md
│   ├── specs12-user-flows.md
│   ├── specs13-labels.md
│   ├── specs14-filters.md
│   ├── specs15-attachments.md
│   ├── specs16-recurring-tasks.md
│   ├── specs17-bulk-operations.md
│   ├── specs18-drag-reorder.md
│   └── specs19-implementation-audit.md
├── build.gradle.kts               # Root build config (AGP 8.3.2, Kotlin 1.9.23)
├── settings.gradle.kts            # Project settings
├── gradle.properties              # JVM args, AndroidX, Kotlin style
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat         # Gradle wrapper
├── AGENTS.md                      # Implementation plan & guidelines
├── SPECIFICATION.md               # Spec index
├── generate_ui.py                 # Stub generator (legacy)
└── LICENSE                        # MIT
```

## Prerequisites

- **Android Studio** (Ladybug or later recommended)
- **JDK 17** (embedded in Android Studio or via SDKMAN)
- **Android SDK** 34 (platform + build-tools)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/your-org/mydo-app.git
cd mydo-app

# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Install on connected device / emulator
./gradlew installDebug
```

Open the project in Android Studio via **File > Open** and select the root directory. The IDE will sync the Gradle configuration and set up the SDK automatically.

## Development

### Available Gradle tasks

```bash
# Full build (assemble + lint + test)
./gradlew build

# Lint
./gradlew lint

# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Code quality report (lint results in app/build/reports/)
./gradlew lintDebug

# Clean
./gradlew clean
```

### Code style

Kotlin code follows the [official Kotlin code style](https://kotlinlang.org/docs/coding-conventions.html) (`kotlin.code.style=official` in `gradle.properties`). The project uses AndroidX with non-transitive R classes.

### Key practices

- All state management uses `kotlinx.coroutines.flow` — Room DAOs expose `Flow` for reactive reads.
- Error handling goes through sealed `AppResult` types (`Success` / `Failure` with typed errors).
- ViewModels are created via custom `ViewModelProvider.Factory` classes wired through `AppContainer`.
- Destructive operations (completion, delete) offer an undo window via snackbar + `Undo*UseCase` classes.
- Room schema exports are checked into `app/schemas/` for migration testing.

### Specification-driven development

The `specs/` directory contains the complete product specification split across topic files (`specsNN-topic.md`). Each file describes one area of the app in terms of user-facing behavior, states, and edge cases — not implementation details. Changes to the app should be reflected in the relevant spec file first.

## Testing

| Test type | Framework | Location | Command |
|-----------|-----------|----------|---------|
| Unit | JUnit 4 + kotlinx-coroutines-test | `app/src/test/` | `./gradlew test` |
| Room migration | Room MigrationTestHelper | `app/src/test/` | `./gradlew test` |
| Robolectric | Robolectric 4.11 | `app/src/test/` | `./gradlew test` |
| Instrumented | Espresso + Compose UI Test | `app/src/androidTest/` | `./gradlew connectedAndroidTest` |

Test areas include: DAO queries, repository logic, domain use cases, recurrence calculation, filter query parsing/evaluation, search ranking, database migrations, and application-level behavior.

### Skipped tests

`MigrationTest.migrate1To2` is annotated `@Ignore` due to a Robolectric asset-loading limitation. Room migration tests can be run on-device via `connectedAndroidTest`.

## Build

```bash
# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (requires signing config)
./gradlew assembleRelease

# App bundle
./gradlew bundleRelease
```

The app uses **Gradle 8.6** with the **Android Gradle Plugin 8.3.2**. Kotlin compiler extension version is `1.5.12` (Compose compiler compatible with Kotlin 1.9.23).

## Configuration

### Environment / Build

Configuration is handled entirely through Gradle and Android resources:

- **`gradle.properties`** — JVM args (`-Xmx2048m`), AndroidX, Kotlin style, Gradle caching.
- **`app/build.gradle.kts`** — SDK versions, Compose options, dependencies.
- **`app/src/main/res/values/`** — App name, colors, styles.

No environment variables, API keys, or server endpoints are needed — the app has no network dependencies.

### Database

Room database at `mydo.db` (stored in the app's internal storage). Schema version 3 with:
- `MIGRATION_1_2` — Creates the full entity set (projects, sections, tasks, labels, filters, reminders, attachments, activity events, notifications).
- `MIGRATION_2_3` — Adds recurrence tracking fields and recent_searches table.

## Navigation

Bottom navigation provides five primary destinations:

| Tab | Route | Description |
|-----|-------|-------------|
| Inbox | `inbox` | Tasks without a project |
| Today | `today` | Tasks due today + overdue |
| Upcoming | `upcoming` | Chronological timeline |
| Projects | `projects` | Project & section listing |
| Search | `search` | Full-text search |

Secondary screens (Labels, Filters, Settings, Notifications) are accessed from the top-bar overflow menu. Task detail and project detail are contextual.

## Data Model

12 entities managed by Room:

| Entity | Description |
|--------|-------------|
| `preferences` | Key-value app settings (theme, dynamic color, etc.) |
| `projects` | Task containers with name, color, icon, archive/favorite flags |
| `sections` | Ordered groupings within a project |
| `tasks` | The core entity — title, description, priority, due date, recurrence, completion state, project/section/parent membership |
| `labels` | Reusable cross-project tags with name and color |
| `task_labels` | Many-to-many join between tasks and labels |
| `filters` | Saved query strings for dynamic task lists |
| `reminders` | Absolute or relative (before due) alarms per task |
| `attachments` | File references per task (filename, MIME, URI) |
| `activity_events` | Audit log of object mutations |
| `notifications` | In-app notification history |
| `recent_searches` | Recent search queries with deduplication |

All IDs are `UUID` strings. Foreign keys with `ON DELETE CASCADE` or `ON DELETE SET NULL` enforce referential integrity.

## Project Status

This is an active development project built incrementally:

- **Step 1** — Android foundation (Compose, M3, DI, theme, components)
- **Step 2** — Local data layer (Room entities, DAOs, repositories, migrations)
- **Step 3** — Task-management MVP (Inbox, Today, task composer, task detail, projects)
- **Step 4** — Planning & discovery (Upcoming, search, labels, filters, recurring tasks)
- **Step 5** — Platform integration (reminders, backups, attachments, settings)
- **Step 6** — Hardening (tests, accessibility, performance)

## License

MIT — see [LICENSE](LICENSE).

Copyright (c) 2026 Robert Lane.
