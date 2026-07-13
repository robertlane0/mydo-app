# /specs/00-overview.md

# MyDo Mobile App
## Product Specification Overview

**Platform:** Android  
**Product model:** Free, local-only, account-free

---

# Purpose

MyDo is a personal task-management application for capturing work, organizing it into projects, scheduling execution, and tracking completion. It operates entirely on the device: all data is stored in a local database, with no account, cloud service, automatic sync, or collaboration features.

The primary workflows are:

1. Capture something immediately.
2. Organize work into projects.
3. Complete work throughout the day.
4. Manually export a backup or import a backup when needed.

---

# Product Goals

- Remember tasks and organize projects.
- Plan upcoming and recurring work.
- Work fully without network connectivity.
- Keep data private and under the device owner's control.
- Provide reliable, user-initiated local-database import and export.

---

# Primary User Types

## Individual

Uses MyDo as a personal planner for groceries, appointments, habits, bills, goals, and daily planning.

## Professional

Uses MyDo to organize personal work: meetings, deadlines, documentation, and client work.

## Power User

Uses labels, filters, priority levels, templates, recurring schedules, sections, and manual backups.

---

# Core Product Principles

## Capture First

Creating a task is always one tap away.

## Organize Later

Tasks may start with only a title; metadata can be added later.

## Focus on Today

Today, Upcoming, and overdue views surface work requiring attention.

## Local Data Ownership

The local database is the source of truth. MyDo never uploads data or synchronizes it automatically. The user may manually export a complete backup and manually import a MyDo backup.

## Fast Interaction

Common actions should require minimal taps and typing.

---

# High-Level Information Architecture

```
Application
├── Inbox
├── Today
├── Upcoming
├── Projects
│   ├── Sections
│   └── Tasks
├── Labels
├── Filters
├── Notifications
├── Search
├── Productivity
└── Settings
    └── Data: Import / Export
```

---

# Core Domain Objects

## Task

Actionable work with a title, description, due date/time, priority, labels, project, section, parent task, attachments, notes, reminders, and completion status.

## Project

A local container for related tasks, with sections, color, and icon.

## Section

An organizational grouping inside one project.

## Label

A reusable local categorization independent of projects.

## Filter

A saved local query that dynamically displays matching tasks.

## Reminder

A locally scheduled notification associated with a task.

---

# Major Application Modules

## Local Storage and Data Portability

Creates and maintains the on-device database and supports manual, validated import and export of complete backups.

## Task Management

Creates, edits, completes, deletes, schedules, prioritizes, and duplicates tasks.

## Project Management

Creates, edits, archives, and deletes local projects and sections.

## Planning

Provides Today, Upcoming, calendar, and scheduling views.

## Notifications

Provides local reminder notifications and preferences.

## Search

Searches the local database across tasks, projects, labels, filters, and notes.

---

# Primary User Journey

```
Launch App
↓
Open Local Database
↓
View Today
↓
Create Task
↓
Assign Project
↓
Add Due Date
↓
Complete Task
↓
Persist Changes Locally
```

---

# Local Database, Import, and Export

All ordinary task actions read and write the local database immediately, including when the device has no connectivity.

- **Export:** From Settings, the user chooses a destination for a complete, portable MyDo database backup. The backup contains tasks, projects, sections, labels, filters, reminders, completion history, and preferences.
- **Import:** From Settings, the user selects a MyDo backup. MyDo validates it before changing data, then offers a confirmed replacement of local data or a non-destructive merge when supported.
- Import and export are always manual; no data is sent to a MyDo server.

---

# Security

MyDo protects personal task data through platform-protected local storage, optional local encryption where supported, and secure platform file-sharing flows for backups. The app has no sessions, credentials, accounts, or remote data store.

---

# Accessibility Goals

The application supports screen readers, dynamic text sizing, high-contrast themes, Android-sized touch targets, and keyboard navigation where applicable.

---

# Design Philosophy

The interface emphasizes low visual clutter, fast task capture, predictable navigation, progressive disclosure, and consistent interaction patterns. Content takes precedence over decoration.
