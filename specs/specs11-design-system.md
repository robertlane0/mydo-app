# /specs/11-design-system.md

# Design System Specification

## Purpose

The design system establishes the foundational visual language, interactive components, and usability standards for the application. It ensures a cohesive, predictable, and branded user experience across all platforms while maintaining high performance and accessibility.

The application utilizes a heavily customized adaptation of Google's Material Design 3 (M3) system, blending M3 expressive components with distinct brand identity and semantic productivity coding.

---

# Design Principles

The design system follows five principles:

1. **Clarity over Decoration:** Visual elements must serve a functional purpose; priority is given to user content (tasks).
2. **Semantic Consistency:** Colors and shapes carry specific, unvarying meanings (e.g., Red always means Priority 1).
3. **Frictionless Interaction:** Components must respond instantly with clear micro-feedback.
4. **Platform Native:** Leverage native Android UI rendering for optimal performance and battery life.
5. **Accessible by Default:** All components must exceed minimum WCAG AA contrast ratios and touch target standards.

---

# System Structure

```
Design System
│
├── Tokens
│   ├── Color Palette
│   ├── Typography Scale
│   ├── Spacing & Grid
│   └── Elevation
│
└── Components
    │
    ├── Foundational
    │   ├── Buttons & FABs
    │   ├── Checkboxes
    │   └── Chips
    │
    ├── Structural
    │   ├── App Bars (Top & Bottom)
    │   ├── Navigation Drawers
    │   └── Bottom Sheets
    │
    └── Informational
        ├── Cards (Task Items)
        ├── Dialogs
        └── Snackbars / Toasts

```

---

# Design Tokens

## Color System

### Brand Colors

* **Primary Brand (Todoist Red):** Used for main Floating Action Button (FAB), active tab indicators, and primary marketing touchpoints.
* **Accent Theme Colors:** Dynamic based on user settings (e.g., Tangerine, Sunflower, Clover, Amethyst). Alters secondary interactive elements.

### Semantic Priority Colors

Strictly enforced across checkboxes, flags, and text highlights:

* **Priority 1 (P1):** Red (`#D1453B`)
* **Priority 2 (P2):** Orange (`#EB8909`)
* **Priority 3 (P3):** Blue (`#246FE0`)
* **Priority 4 (P4):** Grey (System Default)

### Surface & Background Colors

* **Light Mode:** Pure White (`#FFFFFF`) backgrounds. Elevated surfaces use slight gray tints (M3 tonal elevation).
* **Dark Mode:** Deep Dark Grey (`#1E1E1E`) or True Black (`#000000`) for OLED optimization. Text inverts to high-emphasis white (`#FFFFFF`, 87% opacity).

## Typography Scale

Relies on the system default font (Roboto) for maximum legibility and localization support.

* **Headline Large:** 22sp, Medium Weight. Used for main screen titles (e.g., "Today").
* **Title Medium:** 16sp, Medium Weight. Used for Project titles and Dialog headers.
* **Body Large:** 16sp, Regular Weight. Core interactive text (Task names).
* **Body Medium:** 14sp, Regular Weight. Secondary information (Descriptions, standard list items).
* **Label Small:** 12sp, Medium Weight. Metadata (Due dates, project paths, tags).

## Spacing & Grid

* Base unit is **8dp**.
* Standard screen margins: **16dp**.
* Spacing between related local elements: **4dp** or **8dp**.
* Spacing between distinct component groups: **16dp** or **24dp**.

---

# Core Components

## Action Components

### Expressive Buttons

Used for primary call-to-action moments (e.g., "Add Task" in empty states).

* **Shape:** Pill-shaped (fully rounded corners).
* **States:** Default, Hover/Focus (light overlay), Pressed (ripple effect), Disabled (greyed out, 38% opacity).

### Checkboxes

The most interacted component in the app.

* **Shape:** Circular.
* **Border:** 2dp stroke matching the task's Priority Color.
* **Interaction:** On tap, fills entirely with priority color, displays a checkmark icon, and triggers haptic feedback.

## Structural Components

### Bottom Sheets

Used heavily for context-preserving data entry (Quick Add).

* **Behavior:** Slides up over the current view. Dims the background (scrim at 32% opacity black).
* **Corner Radius:** 16dp top-left and top-right.

### Chips (Collaborators & Labels)

* **Visuals:** Highly rounded rectangles.
* **Content:** May contain an avatar (collaborators), an icon (labels), and a text string.
* **State:** Outlined by default; filled with a tonal background when selected.

---

# Accessibility

Design components must enforce:

* **Touch Targets:** Minimum 48x48dp for all interactive elements (even if the visual icon is smaller).
* **Contrast:** Minimum 4.5:1 ratio for text against its background.
* **Scalability:** UI layouts must not break when the user increases system text size by up to 200%.
