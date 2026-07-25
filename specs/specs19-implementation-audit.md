# Implementation audit and change log

This document records a full audit of the Mydo Android app against `SPECIFICATION.md` and
`specs/`, the bugs and spec gaps found, what was fixed or built in this pass, and what is
knowingly deferred. It follows on from `AGENTS.md`'s "Definition of done" checklist and is
meant to be updated the same way the numbered spec files are: in place, as requirements or
implementation status change.

No build tooling (Gradle, the Android Gradle Plugin, Google's Maven repository) was reachable
from the environment this audit was performed in, so nothing here was verified by compiling
or running the app. Every change was checked by hand against the surrounding code, cross-file
signatures, and the specs. Before merging, run a full `./gradlew build lint test` locally.

## How the audit was done

1. Read every file in `specs/` (`specs00`–`specs18`) and `SPECIFICATION.md` end to end.
2. Read the domain layer (models, repositories, use cases) and the Room schema.
3. Read every screen and ViewModel, comparing behavior against the spec for that screen.
4. Fixed defects and gaps in priority order: broken core interactions first, then missing
   primary-navigation screens, then bulk/organization features, then polish.
5. Downloaded a standalone `ktlint` binary (via `github.com`/`release-assets.githubusercontent.com`,
   which this sandbox's network allowlist permits) and ran it across `app/src/`, since the
   sandbox has no access to Google's/Maven's repositories to run a Gradle-based linter.

## Bugs found and fixed

### Core interactions that silently did nothing

- **Task completion was a no-op in four screens.** `onCompletionToggle = { }` in Search,
  Label Detail, Filter Results, and Upcoming meant tapping a task's checkbox anywhere except
  Inbox had no effect — a direct violation of the per-screen "Tap checkbox → Complete task"
  requirement in specs04, specs07, specs08, specs13, and specs14. All four now call
  `CompleteTaskUseCase` with an Undo snackbar, matching Inbox's existing behavior.
  - Search and Filter Results/Label Detail run one-shot queries rather than reactive Flows,
    so completing a task now explicitly re-runs the query (`refreshTick` in `SearchViewModel`,
    `refresh()` in `LabelDetailViewModel`/`FilterResultsViewModel`) so the completed task
    disappears immediately instead of only after the next full reload.
- **The app-wide snackbar host was structurally disconnected.** `MydoSnackbarHost()` created
  its own private `SnackbarHostState` with nothing able to push messages into it, so it never
  showed anything. Added `MydoSnackbarController`, a small process-wide event bus
  (`ui/components/MydoFeedback.kt`), that the Task Composer and other screen-agnostic UI now
  publish to.
- **"Add a task" empty-state buttons did nothing** in Inbox and Upcoming (`onAction = { }`).
  Wired to open the Quick Add sheet with appropriate context.
- **Upcoming's reschedule always reset the task's time to noon**, discarding whatever time it
  already had — specs18-drag-reorder.md's rescheduling section explicitly requires "Time
  preserved." `UpcomingViewModel.reschedule()` now derives the new due timestamp from the
  task's existing time-of-day (defaulting to noon only when it had no prior due time).
- **The Quick Add composer accepted a blank title without feedback.** Tapping Submit with an
  empty field silently closed the sheet with nothing created. Submit is now disabled until
  there's a non-blank title (specs12-user-flows.md, "Empty Input"), and the field autofocuses
  on open, per the same spec.
- **`CreateTaskUseCase` returned `AppResult<Unit>`**, so nothing downstream could know which
  task was created — meaning Quick Add could never implement its required Undo action or
  report which project a task landed in. It now returns `AppResult<Task>`.

### Bulk operations wired to nothing

- **Bulk "Add Labels" had a use case (`BulkAddLabelsUseCase`) but no UI**, and the call site
  in `InboxScreen` was a comment explaining it had been left out. Added
  `LabelMultiSelectDialog` (`ui/components/Pickers.kt`) and wired it into every list screen's
  bulk action bar.

### Search result navigation

- Selecting a **project or section** search result navigated to the generic Projects list
  rather than the specific project (there was no Project Detail screen to navigate to — see
  below). Now routes to `Screen.ProjectDetail.createRoute(...)` using the project's own id
  (`Section.projectId` for section results).

## Missing primary features (built this pass)

`AGENTS.md` step 3 requires "primary navigation for Inbox, Today, Upcoming, Projects, and
Search." Two of those five were unimplemented placeholders:

- **`TodayScreen` was `Box { Text("Today") }`** — no data, no interaction — despite Today
  being the app's default landing destination (`startDestination = Screen.Today.route` in
  `MydoApp.kt` already pointed at it). The repository-level query (`observeTodayTasks`) and
  DAO already existed and were simply unused. Built:
  - `ObserveTodayTasksUseCase` (`domain/usecase/`)
  - `TodayViewModel`/`TodayUiState` — mirrors `HomeViewModel`'s feature set (completion+undo,
    multi-select bulk actions, sort modes, manual drag reorder) but splits results into
    Overdue and Today sections (specs00-overview.md: "Today: tasks due today or overdue").
    Manual drag reorder is available within the Today section (not Overdue, which is better
    served by completing or rescheduling than reordering).
  - `TodayScreen` — full UI matching Inbox's interaction patterns.

- **`ProjectsScreen` was also `Box { Text("Projects") }`.** The repository/DAO layer already
  fully supported project and section CRUD, archiving, favoriting, and reordering — only the
  domain-layer use cases and every screen were missing. Built:
  - `ProjectUseCases.kt`, `SectionUseCases.kt`, `ObserveProjectTasksUseCase.kt`
  - `ProjectsViewModel`/`ProjectsScreen` — favorites pinned above the rest of the active
    projects, an archived-projects toggle, create/edit/delete with an accurate confirmation
    (see below).
  - `ProjectDetailViewModel`/`ProjectDetailScreen` (new route `projects/{projectId}`) —
    unsectioned tasks plus each section in order, section create/rename/delete, task
    completion, and the same bulk-action toolkit as Inbox/Today.
  - Added `sectionId` to `TaskSummary` (previously absent) so Project Detail can group a
    project's tasks by section from a single query instead of one query per section.

  **Project/section deletion and task disposition.** specs06-projects.md requires that
  deleting a project or section "must not silently discard data" and explain what happens to
  contained tasks. The schema already enforces this correctly: `tasks.projectId` and
  `tasks.sectionId` are `ON DELETE SET NULL` (see `TaskEntity.kt`), so deleting a project
  moves its tasks to the Inbox and deleting a section leaves its tasks unsectioned in the same
  project — nothing is ever cascade-deleted. The new confirmation dialogs say this explicitly
  and (for project deletion) state the exact number of active tasks that will move.

  **Scope decision — task reordering in Project Detail.** Inbox, Today, and a project's
  Unsectioned bucket support full drag-to-reorder, reusing the existing
  `DragDropListState`/`DragHandle` (`ui/components/DragReorder.kt`), which tracks drag
  position by the dragged item's index within a single `LazyColumn`. A project detail screen
  has multiple independently-orderable groups (Unsectioned, then each section) sharing one
  scrollable list; making per-group drag math correct requires computing each group's index
  offset within the shared list, and getting that arithmetic wrong is exactly the kind of bug
  that's easy to introduce and hard to catch without a compiler or a device to test on. Rather
  than ship a half-verified generalization of the drag code, tasks within a *named section*
  use explicit up/down controls instead (still fully functional reordering, just not a drag
  gesture). This is called out as a candidate for a follow-up pass with real device testing.

## Fit, finish, and accessibility

- **Replaced every raw-text navigation glyph with a real icon.** The bottom nav previously
  rendered `Text(screen.route.first().uppercase())` — literal single letters ("I", "T", "U",
  "P", "S") — as its icons, and the top bar's notification bell and overflow menu were emoji
  characters (`"\uD83D\uDD14"`, `"\u22EE"`) with no `contentDescription`. specs11-design-system.md
  calls for "descriptive labels for icons." Added `androidx.compose.material:material-icons-core`
  and `-extended` to `app/build.gradle.kts` and replaced these with `Icons.Filled.*` plus
  accessible content descriptions throughout `MydoApp.kt`, `ProjectsScreen.kt`, and
  `ProjectDetailScreen.kt`.
- **Quick Add now recognizes a handful of inline tokens** — `#Project`, `p1`–`p4`,
  `today`/`tomorrow`/a weekday name — stripping them from the title and previewing the
  resulting due date/project/priority as chips before the task is created
  (`parseQuickAdd` in `ui/components/TaskComposerViewModel.kt`). This is intentionally a small,
  literal token scanner rather than full natural-language date parsing: specs12-user-flows.md's
  "Parsing (NLP)" section only asks for a few concrete examples, and a token is only ever
  consumed when it's an unambiguous match, so this can't silently misinterpret a title.
- **Undo affordances made consistent.** Every completion action across every screen (Inbox,
  Today, Upcoming, Search, Label Detail, Filter Results, Project Detail) now shows the same
  "Task completed" / Undo pattern.

## Linting

Ran `ktlint` (standalone binary, v1.3.1) against all of `app/src/`. The project had no
`.editorconfig`; added one at the repo root. ktlint's default `ktlint_official` code style is
tuned for greenfield code and its more opinionated wrapping rules (function-signature
wrapping, multiline-expression wrapping, forced trailing commas, one-statement-per-line for
lambda bodies) would have rewritten large parts of this ~11,500-line, mostly pre-existing
codebase purely for style, with no compiler available in this environment to confirm the
rewrite was safe. Those specific rules are disabled with a comment explaining why; correctness
and hygiene rules (unused imports, import ordering, indentation, trailing whitespace, blank
lines, file naming) are left enabled and were run to a clean pass:

- Fixed unused imports in `LabelsViewModel.kt`, `MainActivity.kt`, `domain/model/SearchResult.kt`.
- Fixed import ordering in half a dozen files (`Pickers.kt`, `MydoBottomSheet.kt`,
  `SettingsScreen.kt`, `MainActivity.kt`, `AppContainer.kt`).
- Renamed `ui/navigation/Screens.kt` → `Screen.kt` and `ui/theme/Spacing.kt` → `MydoSpacing.kt`
  to match their single top-level declaration (filename only; package and class names, and
  therefore every import, are unchanged, so this is a zero-risk rename).
- Wrapped a handful of overlong lines in code touched this pass; left two long Room `@Query`
  SQL strings in `TaskDao.kt` as single-line string concatenation rather than restructuring
  working query text.

`ktlint app/src/**/*.kt` now exits 0 (no findings) under the new `.editorconfig`.

## Known gaps and deferred work

Being transparent about what this pass did *not* attempt, so it isn't mistaken for "done":

- **Drag-to-reorder sections, and dragging a task between sections**, per specs18's mention of
  reordering "tasks or sections" — Project Detail supports section reordering and cross-section
  moves via explicit controls (see above), not drag gestures.
- **Board (kanban) view** — specs06/specs18 mention it as a display mode; only List view is
  implemented, consistent with the rest of the pre-existing app.
- **Full natural-language Quick Add parsing** (arbitrary date phrases, `@label` tags) — only a
  literal, unambiguous token subset is recognized (see above).
- **The double toolbar on pushed detail screens.** `TaskDetailScreen` (pre-existing) nests its
  own `Scaffold`/`TopAppBar` with a back button inside `MydoApp`'s outer `Scaffold`/`TopAppBar`,
  so both are visible at once. `ProjectDetailScreen` follows the same existing pattern for
  consistency rather than introducing a new navigation shell (hiding the outer bar per-route)
  that touches every screen's layout with no way to verify the change here.
- **Label Detail and Filter Results are not fully reactive.** `ObserveTasksForLabelUseCase`
  and `RunFilterUseCase` are one-shot queries; this pass added an explicit `refresh()` after
  completing a task so the two screens stay correct for the interactions they support, but
  changes made from *other* screens (e.g. relabeling a task from Task Detail) won't be
  reflected until the screen is revisited. A proper fix is a reactive Flow-based query, which
  is a larger, riskier change to the query layer than this pass's scope.
- **No automated tests were added** for the new Today/Projects/Project Detail code. The
  existing test suite (`app/src/test/`) doesn't reference any of the changed or added public
  APIs, so nothing there should be broken, but the new ViewModels have no unit test coverage
  of their own yet.

## Post-audit: first real build results

The caveat above — "nothing here was verified by compiling" — was not theoretical. A local
`./gradlew build` surfaced two real compile errors on the first attempt, both now fixed:

- **`MainActivity.kt`: `TaskComposerViewModel.Factory` missing `deleteTaskUseCase`.**
  `TaskComposerViewModel`'s constructor and `Factory` were changed to take a
  `deleteTaskUseCase` (needed for Quick Add's Undo action), but the direct `Factory(...)`
  call site in `MainActivity.kt` was missed and still passed only one argument.
- **`MydoApp.kt`: `Unresolved reference: padding`.** `Modifier.padding(paddingValues)` was
  used on the `NavHost` without importing `androidx.compose.foundation.layout.padding`.

Both are now fixed. The verification method used before delivery (grepping for capitalized,
i.e. type-shaped, identifiers against imports) didn't catch either — the first is a
call-site/constructor mismatch, not an import problem, and the second is a lowercase
extension-function import the earlier heuristic never checked. The check has since been
redone properly (every dotted call, not just capitalized ones) across all files touched in
this pass; it surfaced a couple of additional candidates (`weight`, `align`) that traced back
to correct `RowScope`/`BoxScope` member-extension usage inside the right lexical scope, not
real bugs.

This doesn't change the recommendation above: run a full local build before relying on this,
since this environment still can't compile the project directly.

## Files touched

Use `git diff` / `git log` against this repository for the authoritative list; at a summary
level, this pass touched: the Task Composer and its ViewModel; the global snackbar
infrastructure; `CreateTaskUseCase`; `Upcoming`'s screen and ViewModel; `Search`, `Label
Detail`, and `Filter Results`' ViewModels and screens; `Inbox`'s screen; `TaskSummary` and its
Room mapper; `MydoApp.kt` and `AppContainer.kt`; `app/build.gradle.kts`; and added the Today
and Projects/Project Detail features in full (new files under `ui/today/`, `ui/projects/`,
and `domain/usecase/ProjectUseCases.kt` / `SectionUseCases.kt` / `ObserveTodayTasksUseCase.kt`
/ `ObserveProjectTasksUseCase.kt`).
