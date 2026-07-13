# /specs/06-projects.md

# Projects Specification

## Purpose

A MyDo project is a local container for related tasks. Projects are personal to the device's local database; MyDo has no project sharing, members, invitations, roles, or access permissions.

---

# Goals

- Organize local tasks by initiative, area, client, or personal objective.
- Create, edit, archive, and delete projects quickly.
- Use sections to group tasks within a project.
- Keep project changes immediately available in the local database.

---

# Navigation and Layout

```
Projects
├── Project List
│   └── Project
│       ├── Sections
│       └── Tasks
└── Create Project
```

The project list supports favorites, archived projects, search, sorting, and an empty-state action to create a project.

---

# Project Properties

| Property | Description |
|---|---|
| Name | Required local project name |
| Description | Optional context |
| Color | Visual identifier |
| Icon | Optional visual identifier |
| Favorite | Pins the project in navigation |
| Archived | Hides the project from active navigation |

Projects contain sections and tasks only. They do not have visibility modes, collaborators, or member lists.

---

# Creating and Editing a Project

Creating requires a name and may include color, icon, and description. Saving writes the project locally and makes it visible immediately. Editing supports the same fields, favorite state, and archive state.

---

# Sections and Tasks

Sections are local, ordered subdivisions of one project. Users can create, rename, reorder, or delete them. Deleting a section asks whether to move contained tasks to another section or leave them unsectioned in the project.

Tasks can be created, completed, reordered, moved between sections, and moved to another project. A task inherits no people, permissions, or membership data.

---

# Project Menu

The menu includes edit, favorite/unfavorite, archive/unarchive, export local database (via Settings), and delete. Deletion requires confirmation and explains what happens to contained tasks; it must not silently discard data.

---

# States and Errors

An empty project offers task creation. Loading and pull-to-refresh read the local database. If a local database error prevents loading or saving, show the error and offer retry; project data is never fetched from a server.

---

# Interactions, Accessibility, and Performance

Support tapping to open, long-pressing to multi-select, and dragging to reorder tasks or sections. Controls expose descriptive accessibility labels and remain usable with screen readers, keyboard navigation, and scaled text. Virtualize large task lists and update changed local rows incrementally.

---

# Business Rules

- Sections belong to exactly one project.
- A project may be archived but remains in the local database and in manual backups.
- Every project is personal and editable on this installation.
- Project changes persist locally immediately and are included in the next manual export.

---

# Success Criteria

Users can organize thousands of local tasks into projects and sections, retrieve them quickly, and preserve them through manual export and import.
