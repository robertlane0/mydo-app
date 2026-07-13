# /specs/12-user-flows.md

# User Flows Specification

## Purpose

This document outlines the end-to-end sequences for critical user actions. The flows prioritize speed of data entry and satisfying completion mechanics, ensuring users spend minimal time managing tasks and maximum time executing them.

---

# Flow Principles

1. **Frictionless Capture:** Adding a task should take no more than one tap and typing.
2. **Forgiving Interactions:** Destructive or state-changing actions (like completing a task) must offer a brief "Undo" window.
3. **Contextual Awareness:** Actions taken within a specific view (e.g., adding a task while inside the "Groceries" project) should automatically inherit that view's context.
4. **Local-First:** Core flows must function entirely without connectivity and persist changes to the local database immediately.

---

# Core Flow: Quick Add Task

The central input loop of the application, optimized for speed.

## Preconditions

* User is on any primary navigation screen.

## Steps

1. **Trigger:** User taps the global Floating Action Button (FAB) or a Quick Settings widget.
2. **UI Transition:** - A modal bottom sheet expands.
* The system keyboard automatically deploys.
* The text input field immediately receives focus.


3. **Input:** User types the task ("Submit report tomorrow at 5pm #Work").
4. **Parsing (NLP):** As the user types, the system instantly highlights and extracts contextual data:
* "tomorrow at 5pm" becomes a Due Date pill.
* "#Work" is assigned to the Work project.


5. **Manual Override (Optional):** User taps the action row above the keyboard to manually assign labels or priority flags.
6. **Submission:** User taps the "Submit" arrow button.
7. **Resolution:**
* Bottom sheet collapses.
* A Snackbar confirms: "Task Added to Work [Undo]".
* The task is committed to the local database and injected into the local UI.



## Edge Cases

* **No connection:** Task is saved locally; no sync or later network action is required.
* **Empty Input:** The "Submit" button remains disabled (greyed out) until at least one character is typed.

---

# Core Flow: Task Completion

The primary reward loop and state-change mechanism.

## Preconditions

* User is viewing a populated task list.

## Steps

1. **Trigger:** User taps the circular checkbox adjacent to a task.
2. **Micro-Interaction:**
* Device triggers a light haptic vibration.
* The checkbox instantly fills with its priority color.
* A checkmark appears inside the circle.
* The task text turns grey and receives a strikethrough.


3. **Delay:** The system holds this visual state for **3 seconds**.
4. **Resolution:** The task animates upwards (collapsing its height to 0dp) and is removed from the active list, transferring to the completed log.

## Edge Cases

* **Accidental Tap (Undo):** If the user taps the completed task again during the 3-second delay, the animation reverses, the strikethrough is removed, and the task remains active.

---

# Core Flow: Project Creation

## Preconditions

* User is in the "Browse" tab or sidebar navigation.

## Steps

1. **Trigger:** User taps the "+" icon adjacent to the "Projects" header.
2. **UI Transition:** A full-screen or large modal dialog opens.
3. **Input Configuration:**
* **Name:** User types a required project name.
* **Color:** User selects from a grid of predefined semantic colors (defaults to Charcoal).
* **View Type:** User selects "List" or "Board" layout via a toggle switch.


4. **Submission:** User taps "Done" in the top app bar.
5. **Resolution:**
* The modal dismisses.
* The application immediately navigates the user into the newly created, empty project screen.



## Empty State Handling

Upon landing in the new project, the user is presented with the **Empty State**:

* **Illustration:** A contextual illustration (e.g., a blank canvas or checklist).
* **Copy:** "Your peace of mind is priceless. Get things done by adding tasks."
* **Call to Action:** A prominent expressive button reading "Add a task".

---

# Error Handling & States

Across all flows, the system handles errors predictably:

* **Validation Errors:** Handled inline. If a user attempts to save an invalid project name, the text field border turns red, and helper text appears below the field.
* **Local Database Errors (Fetch):** If the app cannot load a view from the local database, an **Error State** is displayed featuring:
* A local-data illustration.
* Text: "Unable to open local data."
* A primary "Retry" button.


* **Local Database Errors (Mutation):** If a local save fails, MyDo preserves the unsaved draft when possible, explains the error, and offers retry. It never queues a network mutation.
