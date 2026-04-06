# Functional Specification Document
# Workout-Assistant Android Application

**Version:** 1.6  
**Date:** 2026-04-06  
**Platform:** Android (Native)  
**Language:** Kotlin  
**Repository:** https://github.com/mjaydedecker/Workout-Assistant

---

## Revision History

| Version | Date | Summary |
|---|---|---|
| 1.0 | 2026-03-15 | Initial release — core workout tracking, session management, timers, history, settings |
| 1.1 | 2026-03-22 | Added pause/resume session, keep-screen-on setting, workout day rename, weight unit setting |
| 1.2 | 2026-04-02 | Added calendar history view, CSV export, inactivity timer, session detail screen |
| 1.3 | 2026-04-05 | Exercise Library enhancement — prepopulated exercises from CSV, library browse/detail/video, sets moved to WorkoutDayExercise |
| 1.4 | 2026-04-05 | Confirmed requirements from Manual Test Finding #1: timer sound (5.8.2, 5.9.2), workout day name editing (5.3.3), fixed timer panel (5.4.2), history list/calendar views (5.6), data retention across updates (8.8). All requirements were already specified; findings confirmed these as implementation defects against the existing spec. |
| 1.5 | 2026-04-05 | Manual Test Finding #2: muscle groups default to collapsed (5.1.1); My Exercises merged into Exercise Library as a group, removed from navigation bar (5.1.1, 5.2, 6, 7); Home exercise count shows library total (5.1.1); scroll position and expand/collapse state preserved on back navigation from exercise detail (5.1.1). |
| 1.6 | 2026-04-06 | Manual Test Finding #3: added ability to view exercise detail from active session (5.4.9); confirmed edit-sets-on-workout-day (5.3.5) as implementation defect; confirmed timer sound (5.8.2, 5.9.2) as still-open defect; added exercise weight history graph requirement (5.6.5, 6, 7). |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Scope](#2-scope)
3. [User Roles](#3-user-roles)
4. [Data Models](#4-data-models)
5. [Feature Specifications](#5-feature-specifications)
   - 5.1 [Exercise Library](#51-exercise-library)
   - 5.2 [Exercise Management](#52-exercise-management)
   - 5.3 [Workout Day Management](#53-workout-day-management)
   - 5.4 [Workout Session](#54-workout-session)
     - 5.4.9 [View Exercise Detail from Session](#549-view-exercise-detail-from-session)
   - 5.5 [Weight Tracking](#55-weight-tracking)
   - 5.6 [Session History](#56-session-history) (List, Calendar, and Weight History views)
     - 5.6.5 [Exercise Weight History Graph](#565-exercise-weight-history-graph)
   - 5.7 [CSV Export](#57-csv-export)
   - 5.8 [Rest Timer](#58-rest-timer)
   - 5.9 [Inactivity Timer](#59-inactivity-timer)
   - 5.10 [Application Settings](#510-application-settings)
6. [Screen Inventory](#6-screen-inventory)
7. [Navigation Flow](#7-navigation-flow)
8. [Non-Functional Requirements](#8-non-functional-requirements)
9. [Data Migration](#9-data-migration)
10. [Implementation Phases](#10-implementation-phases)

---

## 1. Overview

Workout-Assistant is an Android application designed for tracking strength training workouts. The application allows users to define exercises, organize them into workout days, conduct workout sessions with real-time set tracking, and review historical performance over time.

---

## 2. Scope

This document covers all functionality to be delivered in the single implementation phase of the Workout-Assistant project. The application is a standalone Android app with local on-device data storage (no cloud sync or backend services).

---

## 3. User Roles

There is a single user role: the **Athlete**. The athlete is the sole user of the application and has full access to all features.

---

## 4. Data Models

### 4.1 Exercise

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| name | String | Name of the exercise (e.g., "Bench Press") |
| isCustom | Boolean | `true` for user-created exercises; `false` for prepopulated library exercises |
| force | String? | Movement type (e.g., "Push", "Pull"); null for custom exercises |
| equipment | String? | Equipment required (e.g., "Barbell", "Dumbbell", "Body Only"); null for custom exercises |
| primaryMuscles | String? | Comma-separated list of primary muscles targeted; null for custom exercises |
| secondaryMuscles | String? | Comma-separated list of secondary muscles targeted; null for custom exercises |
| description | String? | Step-by-step instructions for performing the exercise; null for custom exercises |
| videoUrls | String? | JSON array of YouTube video URLs for the exercise; null if unavailable |
| imageRefs | String? | JSON array of image asset paths for the exercise; null if unavailable |

### 4.2 WorkoutDay

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| name | String | Name of the workout day (e.g., "Push Day", "Monday") |

### 4.3 WorkoutDayExercise

Join table associating exercises to workout days, with ordering and set count.

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| workoutDayId | Long (FK) | Reference to WorkoutDay |
| exerciseId | Long (FK) | Reference to Exercise |
| sets | Int | Number of sets to be performed for this exercise on this workout day |
| orderIndex | Int | Display order within the workout day |

### 4.4 WorkoutSession

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| workoutDayId | Long (FK) | Reference to WorkoutDay |
| startTime | Instant | Timestamp when session was started |
| endTime | Instant? | Timestamp when session was ended (null if in progress) |
| durationSeconds | Long? | Total active session duration in seconds (excludes paused time) |
| totalPausedSeconds | Long | 0 | Total time spent paused across all pause/resume cycles |
| notes | String? | Optional notes added at session end |

### 4.5 SessionExercise

Represents an exercise within a specific session. An exercise may be deleted from the session mid-workout.

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| sessionId | Long (FK) | Reference to WorkoutSession |
| exerciseId | Long (FK) | Reference to Exercise |
| exerciseName | String | Snapshot of the exercise name at session time |
| targetSets | Int | Number of sets planned for this exercise |
| completedSets | Int | Number of sets marked as completed |
| weightKg | Double? | Weight used for this exercise in this session |
| orderIndex | Int | Display order within the session |

### 4.6 ExerciseDefaultWeight

Persists the last-used weight per exercise to pre-populate future sessions.

| Field | Type | Description |
|---|---|---|
| exerciseId | Long (PK, FK) | Reference to Exercise |
| weightKg | Double | Most recently recorded weight for this exercise |

### 4.7 AppSettings

Single-row settings table.

| Field | Type | Default | Description |
|---|---|---|---|
| restTimerSeconds | Int | 90 | Duration for the rest timer |
| inactivityTimerSeconds | Int | 300 | Duration for the inactivity timer |
| restTimerSound | Boolean | true | Play sound when rest timer expires |
| restTimerVibrate | Boolean | true | Vibrate when rest timer expires |
| inactivityTimerSound | Boolean | true | Play sound when inactivity timer expires |
| inactivityTimerVibrate | Boolean | true | Vibrate when inactivity timer expires |
| keepScreenOn | Boolean | true | Keep screen on during active workout session |
| darkMode | Boolean | (system) | Dark or light theme override |
| weightUnit | Enum (KG, LB) | KG | Unit used for displaying and entering weights |

---

## 5. Feature Specifications

### 5.1 Exercise Library

The Exercise Library provides a prepopulated catalog of exercises sourced from `exercises.csv`, bundled with the application. Users browse this catalog to add exercises to a workout day. Custom user-created exercises are also surfaced within the library as a dedicated group.

#### 5.1.1 Browse Library by Muscle Group
- The library screen presents exercises grouped by primary muscle (e.g., Chest, Back, Shoulders, Legs, Arms, Core).
- If an exercise has multiple primary muscles, it appears under each relevant muscle group.
- User-created custom exercises are displayed as a dedicated **"My Exercises"** group within the library list. This group appears alongside the muscle groups and is subject to the same expand/collapse behaviour.
- All muscle groups — including "My Exercises" — **start collapsed** by default. The user must tap a group header to expand it and view the exercises within. This reduces the need to scroll through the entire list to reach a desired group.
- The user can expand or collapse any group by tapping its header.
- A search field allows the user to filter exercises by name across all groups. When a search is active, all matching groups are shown expanded.
- **Scroll position and expand/collapse state are preserved** when the user navigates from an exercise detail screen back to the library. On returning, the list must restore the exact scroll position and the same set of expanded/collapsed groups that were in place when the user left. This was identified in Manual Test Finding #2.
- The exercise count displayed on the Home / Dashboard screen reflects the total number of exercises in the library (prepopulated + custom combined).

#### 5.1.2 View Exercise Detail
- The user can tap any exercise in the library to open an Exercise Detail screen.
- The detail screen displays:
  - Exercise name
  - Force (e.g., Push, Pull)
  - Equipment required
  - Primary muscles
  - Secondary muscles
  - Step-by-step description/instructions
  - Thumbnail images (if available)
  - A list of available video tutorials (if available)

#### 5.1.3 Play Exercise Video
- From the Exercise Detail screen, the user can tap a video thumbnail or link to play a tutorial video.
- Videos are played via the YouTube app or an in-app web view using the associated YouTube URL.

#### 5.1.4 Add Library Exercise to a Workout Day
- From the Exercise Detail screen (or directly from the library list), the user can tap "Add to Workout Day".
- The user is prompted to select the target workout day and enter the number of sets to be performed.
- The sets count is stored on the `WorkoutDayExercise` record, not on the exercise itself.
- An exercise may be added to multiple workout days with different set counts.

---

### 5.2 Exercise Management

Custom exercises allow the user to define exercises not present in the prepopulated library. Custom exercises are managed from within the Exercise Library — there is no separate navigation bar entry for custom exercises.

#### 5.2.1 View Custom Exercise List
- Custom exercises are displayed within the Exercise Library (5.1.1) under the **"My Exercises"** group.
- The user can access the standalone custom exercise management screen (create, edit, delete) via an action within the "My Exercises" group header or a dedicated button on the library screen.
- Each custom exercise entry displays the exercise name.
- Custom exercises are listed alphabetically within their group.

#### 5.2.2 Create Custom Exercise
- The user can tap an "Add Exercise" button to open a form.
- **Required fields:**
  - Name (text, non-empty, unique across all exercises)
- On save, the exercise is created with `isCustom = true` and added to the list.
- Validation errors are shown inline.
- No set count is entered here; sets are defined when the exercise is assigned to a workout day (see 5.3.5).

#### 5.2.3 Edit Custom Exercise
- The user can tap a custom exercise to open an edit form pre-populated with the current name.
- The same validation rules apply as creation.
- Saving updates the exercise record.
- Editing an exercise name does not retroactively alter historical session records (session records store a name snapshot).
- Prepopulated library exercises cannot be edited by the user.

#### 5.2.4 Delete Custom Exercise
- The user can delete a custom exercise from the list (e.g., via swipe-to-delete or a delete button).
- If the exercise is assigned to any workout day, the user is warned and must confirm deletion.
- Deleting an exercise removes it from all workout day assignments. Historical session records retain the exercise name snapshot and are not affected.
- Prepopulated library exercises cannot be deleted by the user.

---

### 5.3 Workout Day Management

#### 5.3.1 View Workout Day List
- The user can navigate to a workout day list screen.
- Each item displays the workout day name and the number of exercises assigned to it.

#### 5.3.2 Create Workout Day
- The user can create a new workout day by entering a name (non-empty, unique).

#### 5.3.3 Edit Workout Day Name
- The user can rename an existing workout day.
- Renaming is accessible from two entry points:
  - **Workout Day List** — via an edit action on the list item (e.g., long-press menu or edit icon).
  - **Workout Day Detail** — via an edit icon in the screen's top bar that switches the title to an editable text field. Changes are saved when the user taps a checkmark or presses Done on the keyboard. This entry point was confirmed as a defect in Manual Test Finding #1 — the rename action was not functioning from the Workout Day Detail screen.
- The new name must be non-empty and unique among workout days; validation errors are shown inline.

#### 5.3.4 Delete Workout Day
- The user can delete a workout day.
- The user is warned if sessions exist for the day, and must confirm deletion.
- Deleting a workout day does not delete historical session records.

#### 5.3.5 Assign Exercises to a Workout Day
- The user can open a workout day to view and manage its exercise list.
- The user can add exercises from either the Exercise Library (prepopulated) or the custom exercise list.
- When adding an exercise, the user enters the number of sets to be performed for that exercise on that workout day. This value is stored on the `WorkoutDayExercise` record.
- The sets count for an exercise assignment can be edited from the Workout Day Detail screen. This requirement was confirmed as an implementation defect in Manual Test Finding #3 — the edit action was not functioning as expected.
- An exercise may appear in multiple workout days, each with its own set count.
- The user can reorder exercises within a workout day (e.g., via drag-and-drop).
- The user can remove an exercise from a workout day without deleting the exercise from the library or master list.

---

### 5.4 Workout Session

#### 5.4.1 Start a Session
- The user selects a workout day from the workout day list and taps "Start Workout".
- A new WorkoutSession record is created with the current timestamp as `startTime`.
- The session screen loads all exercises assigned to the selected workout day, each with their target sets and the most recently recorded weight (if any) pre-populated.
- Only one session may be active at a time. If a session is already in progress, the user is taken directly to that session screen.

#### 5.4.2 Session Exercise List
- The timer panel (rest timer and inactivity timer) is displayed in a **fixed, non-scrolling area** at the top of the session screen, always visible regardless of how far the user has scrolled through the exercise list. The timer panel must remain on screen at all times during an active session — it must never scroll off screen. This requirement was confirmed as a defect in Manual Test Finding #1.
- Each exercise in the session is displayed as a card in a scrollable list below the fixed timer panel, showing:
  - Exercise name
  - Set progress indicator (e.g., "2 / 4 sets")
  - Weight field (editable)
  - Set action buttons (mark set complete / decrement)
  - A visual indicator when all sets are completed

#### 5.4.3 Mark a Set as Performed
- The user taps a "Complete Set" button (or "+" button) to increment `completedSets` for a session exercise.
- `completedSets` cannot exceed `targetSets`.
- When a set is marked:
  - Both the rest timer and inactivity timer reset and begin counting.
  - If this is the first set completed for this exercise and no weight has been entered, the user is prompted to enter the weight used.

#### 5.4.4 Decrement a Performed Set
- The user can tap a "-" (decrement) button to reduce `completedSets` by 1 for a session exercise.
- `completedSets` cannot go below 0.
- When decremented, the rest and inactivity timers are not affected.

#### 5.4.5 Mark Exercise as Complete
- When `completedSets` equals `targetSets`, the exercise card displays a "Completed" visual state (e.g., checkmark, green highlight).

#### 5.4.6 Delete an Exercise from a Session
- The user can remove an exercise from the active session (e.g., swipe-to-delete or a remove button on the card).
- The user must confirm before removal.
- This does not affect the exercise library or the workout day assignment; it only removes the exercise from the current session.

#### 5.4.7 End a Session
- The user taps an "End Workout" button.
- The user is presented with a dialog to optionally enter session notes.
- On confirming end, the session's `endTime` is recorded, `durationSeconds` is calculated as total elapsed time minus `totalPausedSeconds`, and the session is saved.
- The screen transitions to the Session Summary screen (or History screen).
- The keep-screen-on flag is released once the session ends.

#### 5.4.8 Pause and Resume a Session
- The user can tap a "Pause" button on the active session screen to pause the workout.
- While paused, the session screen shows a "Resume" button in place of the pause button.
- The rest and inactivity timers are suspended while the session is paused.
- When the user taps "Resume", the timers resume from where they left off and pause time accumulates into `totalPausedSeconds`.
- Multiple pause/resume cycles are supported; all pause durations are accumulated.
- `durationSeconds` at session end reflects only active (non-paused) time.
- The keep-screen-on flag remains active during a paused session, as the session has not ended.

#### 5.4.9 View Exercise Detail from Session
- From the active session screen, the user can tap an info icon (or the exercise name) on any session exercise card to view the Exercise Detail screen for that exercise.
- The Exercise Detail screen opened from the session displays the full exercise information: muscles, equipment, instructions, images, and video links (see 5.1.2).
- Viewing the exercise detail does not interrupt the session; timers continue running in the background.
- Back navigation from the Exercise Detail screen returns the user to the active session screen with no loss of session state.

---

### 5.5 Weight Tracking

#### 5.5.1 Enter Weight for an Exercise
- When the user completes the first set of an exercise in a session, if no weight is recorded yet, a weight input prompt appears.
- The weight input label and placeholder reflect the active weight unit (kg or lb) from settings.
- Weights are stored internally in kilograms regardless of display unit; conversion is applied at the UI layer.
- The entered weight is saved to the `SessionExercise` record.
- The weight is also saved to `ExerciseDefaultWeight` so it becomes the pre-populated default for future sessions.

#### 5.5.2 Edit Weight for a Session Exercise
- The user can tap the weight field on a session exercise card to edit the weight at any point during the session.
- Editing the weight updates the `SessionExercise` record and also updates `ExerciseDefaultWeight`.

#### 5.5.3 Default Weight Pre-population
- When a new session starts, each session exercise is pre-populated with the weight from `ExerciseDefaultWeight` if one exists for that exercise.

---

### 5.6 Session History

#### 5.6.1 View Toggle: List and Calendar
- The history screen provides two views, switchable via a toggle (e.g., tab bar or segmented button):
  - **List view** (default)
  - **Calendar view**
- The selected view is remembered for the duration of the app session.

#### 5.6.2 List View
- All completed workout sessions are listed in reverse chronological order (most recent first).
- Each list item shows:
  - Workout day name
  - Date and time of session
  - Total duration (formatted as HH:MM:SS or MM:SS)
  - Number of exercises performed
- Tapping a session navigates to the Session Detail screen.

#### 5.6.3 Calendar View
- A monthly calendar is displayed with days that have one or more completed sessions visually indicated (e.g., a dot or highlight on the date).
- The user can navigate between months using previous/next controls.
- Tapping a date that has sessions shows a brief summary list of that day's sessions below the calendar (or as a bottom sheet).
- Tapping a session from the day summary navigates to the Session Detail screen.
- Days with no sessions are tappable but produce no action.

#### 5.6.4 View Session Detail
- The user can tap a session (from either the list view or the calendar view) to view full details:
  - Workout day name
  - Date, start time, end time
  - Total duration
  - Session notes (if any)
  - For each exercise:
    - Exercise name
    - Sets completed / target sets
    - Weight used

#### 5.6.5 Exercise Weight History Graph
- From the Session Detail screen, the user can tap an exercise entry to view a weight history graph for that exercise.
- The graph plots the exercise's recorded weight over time:
  - **X-axis:** Session date, ordered chronologically.
  - **Y-axis:** Weight used, displayed in the active weight unit (kg or lb) from settings.
  - Each data point represents one completed session in which the exercise was performed and a weight was recorded.
- If fewer than two data points exist for the exercise, an appropriate message is shown (e.g., "Not enough data to display a graph yet.") rather than an empty chart.
- The graph is displayed on a dedicated Exercise Weight History screen, navigated to by tapping the exercise entry in the Session Detail screen.
- The screen title shows the exercise name.
- Back navigation returns the user to the Session Detail screen.

---

### 5.7 CSV Export

#### 5.7.1 Export History to CSV
- The user can initiate an export from the History screen via an "Export to CSV" action (e.g., menu or button).
- The app generates a CSV file with the following columns:

| Column | Description |
|---|---|
| Session Date | Date of the session (YYYY-MM-DD) |
| Session Start Time | Start time (HH:MM:SS) |
| Session End Time | End time (HH:MM:SS) |
| Duration | Total duration (HH:MM:SS) |
| Workout Day | Name of the workout day |
| Exercise | Exercise name |
| Sets Completed | Number of sets completed |
| Target Sets | Target number of sets |
| Weight (kg) | Weight used, always exported in kg regardless of display unit setting |
| Notes | Session notes |

- One row per exercise per session.
- The file is named `workout_history_YYYYMMDD_HHMMSS.csv`.
- The Android share sheet is invoked so the user can save or share the file via their preferred app (Files, Drive, email, etc.).

---

### 5.8 Rest Timer

#### 5.8.1 Timer Behavior
- The rest timer counts down from the configured `restTimerSeconds` value.
- It starts (or resets) each time the user marks a set as performed.
- It is displayed in the fixed timer panel at the top of the active session screen (see 5.4.2), always visible regardless of scroll position, showing remaining time as a progress indicator (e.g., circular arc or progress bar) and a MM:SS countdown.

#### 5.8.2 Timer Expiry
- When the rest timer reaches zero:
  - If `restTimerSound` is enabled, an audible alert **must** play using the **media audio stream** (`AudioManager.STREAM_MUSIC`). This ensures the alert is audible whenever the media volume is raised, regardless of the device ringer or silent mode switch. The sound must be audible at any media volume level above zero. This requirement was confirmed as a defect in Manual Test Finding #1 and re-confirmed as still open in Manual Test Finding #3 — the sound was not playing even with OS volume sliders at medium level or higher.
  - If `restTimerVibrate` is enabled, the device vibrates.
  - The progress indicator reflects the expired state.
- The timer does not loop after expiry.

#### 5.8.3 Timer Reset
- The rest timer resets to its configured duration whenever a set is marked as performed.

---

### 5.9 Inactivity Timer

#### 5.9.1 Timer Behavior
- The inactivity timer counts down from the configured `inactivityTimerSeconds` value.
- It starts (or resets) each time the user marks a set as performed.
- It runs concurrently with the rest timer.
- It is displayed in the same fixed timer panel as the rest timer (see 5.4.2 and 5.8.1), always visible regardless of scroll position.

#### 5.9.2 Timer Expiry
- When the inactivity timer reaches zero:
  - If `inactivityTimerSound` is enabled, an audible alert **must** play using the **media audio stream** (`AudioManager.STREAM_MUSIC`), consistent with the rest timer alert. The same requirement applies as 5.8.2 — the sound must be audible at any media volume level above zero. Re-confirmed as still open in Manual Test Finding #3.
  - If `inactivityTimerVibrate` is enabled, the device vibrates.
- The timer does not loop after expiry.

#### 5.9.3 Timer Reset
- The inactivity timer resets to its configured duration whenever a set is marked as performed.

#### 5.9.4 Distinction from Rest Timer
- The rest timer signals when the user *should* begin the next set (rest period over).
- The inactivity timer signals when the user has been inactive for too long (reminder to continue exercising).
- Both timers reset on the same event (set marked performed) but serve different motivational purposes.

---

### 5.10 Application Settings

The user can access a Settings screen from the main navigation.

#### 5.10.1 Rest Timer Duration
- Integer input (seconds), minimum 1.
- Label: "Rest Timer Duration (seconds)"
- Default: 90

#### 5.10.2 Inactivity Timer Duration
- Integer input (seconds), minimum 1.
- Label: "Inactivity Timer Duration (seconds)"
- Default: 300

#### 5.10.3 Rest Timer Notifications
- Toggle: "Rest Timer Sound" (default: on)
- Toggle: "Rest Timer Vibration" (default: on)

#### 5.10.4 Inactivity Timer Notifications
- Toggle: "Inactivity Timer Sound" (default: on)
- Toggle: "Inactivity Timer Vibration" (default: on)

#### 5.10.5 Keep Screen On
- Toggle: "Keep Screen On During Workout" (default: on)
- When enabled, the `FLAG_KEEP_SCREEN_ON` window flag is set on the session screen while a workout is active and cleared when the session ends.

#### 5.10.6 Theme
- Toggle or selector: "Dark Mode" / "Light Mode" / "System Default"
- Applies the selected theme across the entire application.

#### 5.10.7 Weight Unit
- Selector: "kg" / "lb" (default: kg)
- All weight values throughout the application (entry fields, session cards, history, CSV export) are displayed in the selected unit.
- Weights are stored internally in kilograms; conversion is applied at the UI layer.

---

## 6. Screen Inventory

| Screen | Description |
|---|---|
| Home / Dashboard | Entry point; quick-start workout, navigation to other sections |
| Exercise Library | Browse all exercises (prepopulated + custom "My Exercises" group) grouped by primary muscle; starts collapsed; preserves scroll position and expand/collapse state on back navigation |
| Exercise Detail | View full detail for any exercise: muscles, equipment, instructions, images, and video links |
| Custom Exercise Form | Create or edit a single custom exercise; accessed from within the Exercise Library "My Exercises" group |
| Workout Day List | View, add, edit, delete workout days |
| Workout Day Detail | Assign, reorder, remove exercises, and manage set counts for a workout day |
| Active Session | Current workout session with exercise cards, timers, and set controls; exercise name/info icon navigates to Exercise Detail without ending the session |
| End Session Dialog | Notes input and confirm to end session |
| Session History | List and calendar views of completed sessions; toggle between views via tab/segmented button |
| Session Detail | Full detail view of a single past session; tapping an exercise navigates to its weight history graph |
| Exercise Weight History | Line graph of an exercise's recorded weights over time; accessed from Session Detail by tapping an exercise |
| Settings | Application settings |

---

## 7. Navigation Flow

```
Home / Dashboard
├── [Start Workout] → Workout Day List
│   └── [Select Day] → Active Session
│       ├── [Tap exercise info] → Exercise Detail (session continues in background)
│       └── [End Workout] → End Session Dialog → Session History
├── Exercise Library → Exercise Library (grouped by muscle, all groups collapsed by default)
│   ├── [Expand group] → exercise rows visible; scroll position/state preserved on back
│   ├── [Tap Exercise] → Exercise Detail
│   │   ├── [Play Video] → YouTube / in-app video player
│   │   └── [Add to Workout Day] → Workout Day picker + set count entry → back to Library
│   └── [My Exercises group]
│       ├── [Add Custom Exercise] → Custom Exercise Form
│       └── [Tap Custom Exercise] → Custom Exercise Form (edit)
├── Workout Days → Workout Day List
│   ├── [Add] → Workout Day Form
│   └── [Tap Day] → Workout Day Detail
│       └── [Add Exercise] → Exercise Library (with workout day context) + set count entry
├── History → Session History
│   ├── [List view] → [Tap Session] → Session Detail
│   │   └── [Tap Exercise] → Exercise Weight History graph
│   └── [Calendar view] → [Tap Date] → Day summary → [Tap Session] → Session Detail
│       └── [Tap Exercise] → Exercise Weight History graph
└── Settings → Settings Screen
```

---

## 8. Non-Functional Requirements

### 8.1 Platform
- Android native application.
- Minimum SDK: Android 8.0 (API level 26).
- Target SDK: Latest stable Android release.

### 8.2 Language & Architecture
- Language: Kotlin.
- Architecture: MVVM (Model-View-ViewModel) with Jetpack Compose for UI.
- Recommended Jetpack libraries: Room (database), ViewModel, LiveData/StateFlow, Navigation Component.

### 8.3 Data Storage
- All data stored locally on-device using Room (SQLite).
- No network connectivity required.
- No cloud sync or account required.
- The `exercises.csv` file is bundled as an application asset and parsed at first launch to seed the Exercise table with prepopulated library exercises.

### 8.4 Permissions
- `VIBRATE` — for timer vibration alerts.
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` or `MediaStore` API — for CSV export (scoped storage, API 29+).

### 8.5 Performance
- Session screen must remain responsive during timer countdown.
- Timers must use a background-safe mechanism (e.g., `CountDownTimer` or coroutine-based timer) that survives screen rotations.

### 8.6 Accessibility
- All interactive elements must have content descriptions for screen reader support.
- Adequate color contrast in both light and dark themes.

### 8.7 App Icon
- The application launcher icon shall be a dumbbell icon.

### 8.8 Data Retention Across App Updates
- All user data — exercises, workout days, exercise sessions, history, and settings — must be preserved when the application is updated to a new version.
- When a new version of the app introduces database schema changes, a Room migration must be written for every version increment. Destructive migration (dropping and recreating the database) is not permitted in production releases.
- Migration logic must include default values for any newly added columns so that existing rows remain valid after the upgrade.

---

## 9. Data Migration

The Exercise Listing Enhancement introduces structural changes to the data model that require a migration for any existing installation.

### 9.1 Schema Changes
- The `defaultSets` column is removed from the `Exercise` table.
- A `sets` column is added to the `WorkoutDayExercise` table.
- New columns are added to the `Exercise` table: `isCustom`, `force`, `equipment`, `primaryMuscles`, `secondaryMuscles`, `description`, `videoUrls`, `imageRefs`.

### 9.2 Migration Requirements
- A Room database migration must be written to handle the schema changes described in 9.1.
- Existing exercises created before this enhancement are treated as custom exercises (`isCustom = true`); their library-specific fields (`force`, `equipment`, etc.) default to `null`.
- For existing `WorkoutDayExercise` rows, the `sets` value must be populated from the corresponding `Exercise.defaultSets` value prior to the column being dropped.
- Destructive migration is not permitted; all existing workout day assignments, session history, and user data must be preserved.

### 9.3 Scope
- This migration is required for the initial implementation of this enhancement.
- Future schema changes will each require their own incremental migration.

---

## 10. Implementation Phases

There is a single implementation phase for this project. All features described in this document are to be delivered together as version 1.0.
