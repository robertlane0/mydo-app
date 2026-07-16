# /specs/15-attachments.md

# Attachments Specification

## Purpose

Attachments allow users to associate local files with tasks. Files are accessed via Android's Storage Access Framework (SAF) / DocumentProvider APIs; MyDo never uploads, syncs, or moves files.

---

## Goals

- Attach any local file (documents, images, PDFs, etc.) to a task
- Open/preview attachments using system handlers
- Remove attachments without deleting source files
- Respect Android scoped storage and permissions
- Include attachment metadata in backups (not file content)

---

## Navigation

```
Task Detail

↓

Attachments Section

├── Attachment List
├── Add Attachment (SAF Picker)
├── Open Attachment
├── Preview (where supported)
└── Remove Attachment
```

---

## Data Model (from specs11-data-model.md)

| Property | Type | Description |
|----------|------|-------------|
| id | UUID | Unique identifier |
| taskId | UUID | Parent task |
| filename | String | Original file name |
| mimeType | String | MIME type (e.g., `application/pdf`) |
| size | Integer | File size in bytes |
| localUri | URI | `content://` URI from SAF; persists across reboots |

---

## Attachment Section (Task Detail)

### Layout

```
┌─────────────────────────────────────┐
│ Attachments                  +      │
├─────────────────────────────────────┤
│ 📄  Q3_Report.pdf        2.4 MB     │
│ 🖼  whiteboard.jpg       1.1 MB     │
│ 📊  budget.xlsx          512 KB     │
└─────────────────────────────────────┘
```

Each row shows:
- File type icon (based on MIME type)
- Filename (truncated if long)
- File size (human-readable)
- Tap to open; long-press for menu

### Empty State

```
No Attachments

Attach files from your device to this task.

+ Add Attachment
```

---

## Add Attachment Flow

```
Tap +

↓

System Document Picker (ACTION_OPEN_DOCUMENT)

↓

User Selects File(s)

↓

MyDo Reads Metadata (name, type, size)

↓

Persist URI + Metadata to Local DB

↓

Attachment Appears in List
```

### Document Picker Configuration

- **Intent**: `ACTION_OPEN_DOCUMENT`
- **MIME Types**: `*/*` (all) or configurable filter
- **Flags**: `FLAG_GRANT_PERSIST_URI_PERMISSION` | `FLAG_GRANT_READ_URI_PERMISSION`
- **Multi-select**: Supported (`EXTRA_ALLOW_MULTIPLE`)

### Persisted URI Permission

- MyDo calls `ContentResolver.takePersistableUriPermission()` on result
- Permission survives app restart and device reboot
- If permission lost (file moved/deleted), show error on open

### Metadata Extraction

On pick, MyDo queries `DocumentsContract` for:
- `DOCUMENT_ID`
- `DISPLAY_NAME`
- `MIME_TYPE`
- `SIZE` (bytes)

Stored locally; file content never copied.

---

## Open Attachment Flow

```
Tap Attachment

↓

Check Persisted URI Permission

↓

If Granted: Intent.ACTION_VIEW with URI
    ↓
    System Opens with Default Handler

↓

If Denied/Lost: Request Permission Again
    ↓
    If Granted: Open
    If Denied: Show Error Snackbar
```

### Error Handling

| Scenario | Behavior |
|----------|----------|
| File deleted externally | "File not found" snackbar; offer to remove attachment |
| Permission revoked | Re-request via `DocumentsContract.openDocumentTree` or picker |
| No handler for MIME type | "No app can open this file" |
| URI stale (Android version upgrade) | Attempt re-grant; fallback to picker |

---

## Remove Attachment

```
Long-press Attachment

↓

Remove Attachment

↓

Confirm: "Remove from task? Source file unchanged."

↓

Delete Local DB Record

↓

Release Persisted URI Permission (optional)

↓

List Updates
```

- Source file on device **never deleted**
- Only MyDo's reference removed
- URI permission released via `releasePersistableUriPermission()`

---

## Preview (Where Supported)

For images (`image/*`) and PDFs (`application/pdf`):

- Tap shows full-screen preview (system `ACTION_VIEW` or custom `DocumentViewActivity`)
- Swipe to dismiss
- Pinch-to-zoom for images
- Page navigation for PDFs

Preview uses same URI; no separate copy.

---

## Attachments in Task Composer

Quick Add bottom sheet includes attachment button:

```
┌─────────────────────────────────────┐
│ New Task                            │
├─────────────────────────────────────┤
│ Title: [______________________]     │
├─────────────────────────────────────┤
│ 📎 Attachments: 2 files      +      │
└─────────────────────────────────────┘
```

- Tap attachment chip to open/remove before save
- Attached files linked to task on submit

---

## Attachments in Bulk Operations

Bulk operations (specs17-bulk-operations.md) support:
- **Remove attachments** from selected tasks
- (Not supported: add same attachment to multiple tasks — each task needs its own URI permission)

---

## Attachments in Search / Filters

- Search: `hasattachment:true` / `hasattachment:false`
- Filters: same syntax
- Search results show attachment indicator (📎)
- No full-text search inside file content

---

## Attachments in Backup / Export

Export (specs20-backup-export-import.md) includes attachment **metadata only**:
- id, taskId, filename, mimeType, size, localUri

**File content not included** (too large; user manages files separately).

Import:
- Validates metadata
- Attempts to re-grant URI permission for each `localUri`
- If file missing/permission denied: attachment imported as "broken" (shows warning icon)
- User can re-attach from broken state

**SAF integration details: specs21-platform-integration.md**

---

## Android Version Considerations

| API Level | Behavior |
|-----------|----------|
| 19-28 (KitKat-Pie) | `ACTION_OPEN_DOCUMENT`; persist permissions via `takePersistableUriPermission` |
| 29 (Q) | Scoped storage; `requestLegacyExternalStorage` not needed for SAF |
| 30+ (R+) | `MediaStore` access optional; SAF preferred; `MANAGE_EXTERNAL_STORAGE` not required |
| All | `content://` URIs only; no `file://` paths |

---

## Business Rules

- One task may have multiple attachments
- One attachment belongs to exactly one task
- Attachment metadata stored locally; file content never read/copied by MyDo
- Maximum attachment size: none enforced by MyDo (limited by storage/Intent)
- Supported MIME types: all (system handler determines openability)
- Deleting task deletes its attachments (cascade)
- Archiving project preserves attachments
- Export includes metadata; import attempts permission re-grant

---

## Error States

| Cause | UI Response |
|-------|-------------|
| Picker cancelled | No change |
| Picker returns no URI | No change |
| URI permission grant failed | "Could not access file" snackbar; attachment not added |
| Open fails (no handler) | "No app can open this file" |
| Open fails (permission lost) | Re-request permission; if denied, mark broken |
| DB error on add | "Could not save attachment" snackbar; retry |

---

## User Interactions

| Action | Result |
|--------|--------|
| Tap attachment | Open with system handler |
| Long-press attachment | Context menu: Open, Preview (if supported), Remove |
| Tap + in Attachments | Open system document picker |
| Tap + in Task Composer | Open system document picker (multi-select) |
| Swipe attachment (if enabled) | Remove (with undo snackbar) |

---

## Accessibility

Attachments should:
- Announce filename, type, size
- Label "Remove" action clearly
- Support keyboard navigation in list
- Preview honors system font scaling
- File type icons have content descriptions

---

## Performance Requirements

- Attachment list loads in < 50ms (metadata only)
- Picker launch < 200ms
- Open intent fire < 100ms
- No main-thread I/O (metadata queries on IO dispatcher)
- Large file counts: virtualized list

---

## Navigation Summary

```
Task Detail
└── Attachments
    ├── Add (SAF Picker)
    ├── Attachment List
    │   ├── Open (System Handler)
    │   ├── Preview (Image/PDF)
    │   └── Remove
    └── Task Composer (inline add)
```

---

## Success Criteria

The Attachments feature succeeds when users can:
- Attach any local file to a task in < 3 taps
- Open attachments reliably with system apps
- Remove attachments without losing source files
- Survive app updates, reboots, and Android version upgrades
- See attachment metadata in backups (for manual re-linking)
- Use attachments fully offline