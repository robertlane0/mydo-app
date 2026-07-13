# /specs/00-overview.md

# Todoist Mobile App
## Product Specification Overview

**Platform:** Android  
**Source:** Decompiled APK analysis  
**Version:** Derived from APK build 12206  
**Document Version:** 1.0

---

# Purpose

Todoist is a personal and collaborative task management application centered around quickly capturing work, organizing it into projects, scheduling execution, and tracking completion across devices.

The mobile application is optimized around three primary workflows:

1. Capture something immediately.
2. Organize work into projects.
3. Complete work throughout the day.

The application minimizes friction between these three actions while synchronizing state continuously with cloud services.

---

# Product Goals

The application is designed to help users:

- Remember tasks
- Organize projects
- Plan upcoming work
- Prioritize tasks
- Track recurring work
- Collaborate with others
- Reduce cognitive overhead

---

# Primary User Types

## Individual

Uses Todoist as a personal planner.

Typical activities:

- Grocery lists
- Daily planning
- Personal goals
- Habit tracking
- Bills
- Appointments

---

## Professional

Uses Todoist to organize work.

Typical activities:

- Client work
- Meetings
- Deadlines
- Sprint planning
- Documentation
- Personal productivity

---

## Team Member

Works inside shared projects.

Typical activities:

- Assigned tasks
- Comments
- Shared deadlines
- Notifications
- Collaboration

---

## Power User

Uses advanced planning features.

Examples include:

- Labels
- Filters
- Priority levels
- Templates
- Recurring schedules
- Sections
- Integrations

---

# Core Product Principles

## Capture First

Users should never lose an idea.

Creating a task is always one tap away.

---

## Organize Later

Tasks may initially contain minimal information.

Additional metadata can be added later.

---

## Focus on Today

The application continually surfaces work that requires immediate attention.

Examples:

- Today
- Upcoming
- Overdue

---

## Everything Syncs

Changes should synchronize automatically between:

- Mobile
- Desktop
- Web
- Wearables
- Integrations

---

## Fast Interaction

Nearly every common action should require:

- One tap
- One swipe
- Minimal typing

---

# High-Level Information Architecture

```
Application

├── Authentication
│
├── Inbox
│
├── Today
│
├── Upcoming
│
├── Projects
│   ├── Sections
│   └── Tasks
│
├── Labels
│
├── Filters
│
├── Notifications
│
├── Search
│
├── Activity
│
├── Productivity
│
└── Settings
```

---

# Core Domain Objects

## Task

Represents actionable work.

Typical properties include:

- Title
- Description
- Due date
- Due time
- Priority
- Labels
- Project
- Section
- Parent task
- Assignee
- Attachments
- Comments
- Completion status

---

## Project

Container for related tasks.

Projects may contain:

- Sections
- Tasks
- Collaborators
- Shared permissions
- Color
- Icon

---

## Section

Organizational grouping inside a project.

---

## Label

Reusable categorization.

Labels are independent of projects.

Examples:

- Work
- Home
- Phone
- Waiting

---

## Filter

Saved query that dynamically displays tasks matching specific conditions.

Examples:

- Today & P1
- Overdue
- Assigned to Me

---

## Comment

Conversation attached to a task.

May include:

- Text
- Mentions
- Attachments

---

## Reminder

Notification associated with a task.

Can be:

- Time-based
- Location-based (if supported)
- Relative

---

# Major Application Modules

## Authentication

Responsible for:

- Login
- Registration
- Password recovery
- Session management

---

## Task Management

Responsible for:

- Create
- Edit
- Complete
- Delete
- Schedule
- Prioritize
- Duplicate

---

## Project Management

Responsible for:

- Create project
- Edit project
- Archive
- Share
- Invite members

---

## Planning

Responsible for:

- Today
- Upcoming
- Calendar
- Scheduling

---

## Collaboration

Responsible for:

- Comments
- Mentions
- Assignments
- Shared projects

---

## Notifications

Responsible for:

- Reminders
- Assignment notifications
- Comments
- Activity updates

---

## Search

Supports searching across:

- Tasks
- Projects
- Labels
- Comments

---

## Productivity

Provides:

- Daily streaks
- Completed tasks
- Weekly summaries
- Karma system (where available)

---

# Navigation Model

The application is organized around persistent navigation to primary destinations.

Primary destinations include:

- Inbox
- Today
- Upcoming
- Projects
- Search

Secondary destinations include:

- Notifications
- Activity
- Settings
- Productivity

Contextual screens are presented modally or via push navigation.

---

# Primary User Journey

```
Launch App

↓

Authenticate

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

Sync Changes
```

---

# Offline Behavior

The application is expected to support:

- Viewing cached tasks
- Creating tasks offline
- Editing tasks offline
- Completing tasks offline

Changes are synchronized automatically when connectivity returns.

---

# Synchronization Principles

Synchronization should preserve:

- Task edits
- Completion state
- Ordering
- Comments
- Project changes

Conflict resolution should prioritize preserving user data.

---

# Security

The application stores user-specific task information.

Expected protections include:

- Authenticated sessions
- Secure network communication
- Local encrypted storage where supported
- Cloud synchronization over HTTPS

---

# Accessibility Goals

The application should support:

- Screen readers
- Dynamic text sizing
- High contrast themes
- Touch targets meeting Android accessibility guidance
- Keyboard navigation where applicable

---

# Design Philosophy

The interface emphasizes:

- Low visual clutter
- Fast task capture
- Predictable navigation
- Progressive disclosure
- Consistent interaction patterns

Content takes precedence over decoration.

---

# Specification Roadmap

Subsequent documents describe the application in detail:

- 01-navigation.md
- 02-authentication.md
- 03-home-screen.md
- 04-inbox.md
- 05-tasks.md
- 06-projects.md
- 07-upcoming.md
- 08-search.md
- 09-notifications.md
- 10-settings.md
- 11-data-model.md
- 12-design-system.md
- 13-user-flows.md
- 14-sync.md
- 15-api-observations.md
