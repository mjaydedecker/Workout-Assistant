# Functional Specification Document
# Workout-Assistant Android Application

**Version:** 1.2  
**Date:** 2026-04-02  
**Platform:** Android (Native)  
**Language:** Kotlin  
**Repository:** https://github.com/mjaydedecker/Workout-Assistant

---

## Table of Contents

1. [Overview](#1-overview)
2. [Scope](#2-scope)
3. [User Roles](#3-user-roles)
4. [Data Models](#4-data-models)
5. [Feature Specifications](#5-feature-specifications)
   - 5.1 [Exercise Management](#51-exercise-management)
   - 5.2 [Workout Day Management](#52-workout-day-management)
   - 5.3 [Workout Session](#53-workout-session)
   - 5.4 [Weight Tracking](#54-weight-tracking)
   - 5.5 [Session History](#55-session-history) (List and Calendar views)
   - 5.6 [CSV Export](#56-csv-export)
   - 5.7 [Rest Timer](#57-rest-timer)
   - 5.8 [Inactivity Timer](#58-inactivity-timer)
   - 5.9 [Application Settings](#59-application-settings)
6. [Screen Inventory](#6-screen-inventory)
7. [Navigation Flow](#7-navigation-flow)
8. [Non-Functional Requirements](#8-non-functional-requirements)
9. [Implementation Phases](#9-implementation-phases)

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
| defaultSets | Int | Default number of sets normally performed |

### 4.2 WorkoutDay

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| name | String | Name of the workout day (e.g., "Push Day", "Monday") |

### 4.3 WorkoutDayExercise

Join table associating exercises to workout days, with ordering.

| Field | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-generated unique identifier |
| workoutDayId | Long (FK) | Reference to WorkoutDay |
| exerciseId | Long (FK) | Reference to Exercise |
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

### 5.1 Exercise Management

#### 5.1.1 View Exercise List
- The user can navigate to an exercise list screen showing all defined exercises.
- Each list item displays the exercise name and default number of sets.
- Exercises are listed alphabetically.

#### 5.1.2 Create Exercise
- The user can tap an "Add Exercise" button to open a form.
- **Required fields:**
  - Name (text, non-empty, unique)
  - Default Sets (integer, minimum 1)
- On save, the exercise is added to the list.
- Validation errors are shown inline.

#### 5.1.3 Edit Exercise
- The user can tap an exercise to open an edit form pre-populated with current values.
- The same validation rules apply as creation.
- Saving updates the exercise record.
- Editing an exercise name or set count does not retroactively alter historical session records (session records store snapshots).

#### 5.1.4 Delete Exercise
- The user can delete an exercise from the exercise list (e.g., via swipe-to-delete or a delete button).
- If the exercise is assigned to any workout day, the user is warned and must confirm deletion.
- Deleting an exercise removes it from all workout day assignments. Historical session records retain the exercise name snapshot and are not affected.

---

### 5.2 Workout Day Management

#### 5.2.1 View Workout Day List
- The user can navigate to a workout day list screen.
- Each item displays the workout day name and the number of exercises assigned to it.

#### 5.2.2 Create Workout Day
- The user can create a new workout day by entering a name (non-empty, unique).

#### 5.2.3 Edit Workout Day Name
- The user can rename an existing workout day.
- Renaming is accessible from two entry points:
  - **Workout Day List** — via an edit action on the list item (e.g., long-press menu or edit icon).
  - **Workout Day Detail** — via an editable title field in the screen's top bar; changes are saved when the user confirms (e.g., taps a checkmark or presses Done on the keyboard).
- The new name must be non-empty and unique among workout days; validation errors are shown inline.

#### 5.2.4 Delete Workout Day
- The user can delete a workout day.
- The user is warned if sessions exist for the day, and must confirm deletion.
- Deleting a workout day does not delete historical session records.

#### 5.2.5 Assign Exercises to a Workout Day
- The user can open a workout day to view and manage its exercise list.
- The user can add exercises from the master exercise list to the workout day.
- An exercise may appear in multiple workout days.
- The user can reorder exercises within a workout day (e.g., via drag-and-drop).
- The user can remove an exercise from a workout day without deleting the exercise from the master list.

---

### 5.3 Workout Session

#### 5.3.1 Start a Session
- The user selects a workout day from the workout day list and taps "Start Workout".
- A new WorkoutSession record is created with the current timestamp as `startTime`.
- The session screen loads all exercises assigned to the selected workout day, each with their target sets and the most recently recorded weight (if any) pre-populated.
- Only one session may be active at a time. If a session is already in progress, the user is taken directly to that session screen.

#### 5.3.2 Session Exercise List
- The timer panel (rest timer and inactivity timer) is displayed in a **fixed, non-scrolling area** at the top of the session screen, always visible regardless of how far the user has scrolled through the exercise list.
- Each exercise in the session is displayed as a card in a scrollable list below the fixed timer panel, showing:
  - Exercise name
  - Set progress indicator (e.g., "2 / 4 sets")
  - Weight field (editable)
  - Set action buttons (mark set complete / decrement)
  - A visual indicator when all sets are completed

#### 5.3.3 Mark a Set as Performed
- The user taps a "Complete Set" button (or "+" button) to increment `completedSets` for a session exercise.
- `completedSets` cannot exceed `targetSets`.
- When a set is marked:
  - Both the rest timer and inactivity timer reset and begin counting.
  - If this is the first set completed for this exercise and no weight has been entered, the user is prompted to enter the weight used.

#### 5.3.4 Decrement a Performed Set
- The user can tap a "-" (decrement) button to reduce `completedSets` by 1 for a session exercise.
- `completedSets` cannot go below 0.
- When decremented, the rest and inactivity timers are not affected.

#### 5.3.5 Mark Exercise as Complete
- When `completedSets` equals `targetSets`, the exercise card displays a "Completed" visual state (e.g., checkmark, green highlight).

#### 5.3.6 Delete an Exercise from a Session
- The user can remove an exercise from the active session (e.g., swipe-to-delete or a remove button on the card).
- The user must confirm before removal.
- This does not affect the exercise master list or the workout day assignment; it only removes the exercise from the current session.

#### 5.3.7 End a Session
- The user taps an "End Workout" button.
- The user is presented with a dialog to optionally enter session notes.
- On confirming end, the session's `endTime` is recorded, `durationSeconds` is calculated as total elapsed time minus `totalPausedSeconds`, and the session is saved.
- The screen transitions to the Session Summary screen (or History screen).
- The keep-screen-on flag is released once the session ends.

#### 5.3.8 Pause and Resume a Session
- The user can tap a "Pause" button on the active session screen to pause the workout.
- While paused, the session screen shows a "Resume" button in place of the pause button.
- The rest and inactivity timers are suspended while the session is paused.
- When the user taps "Resume", the timers resume from where they left off and pause time accumulates into `totalPausedSeconds`.
- Multiple pause/resume cycles are supported; all pause durations are accumulated.
- `durationSeconds` at session end reflects only active (non-paused) time.
- The keep-screen-on flag remains active during a paused session, as the session has not ended.

---

### 5.4 Weight Tracking

#### 5.4.1 Enter Weight for an Exercise
- When the user completes the first set of an exercise in a session, if no weight is recorded yet, a weight input prompt appears.
- The weight input label and placeholder reflect the active weight unit (kg or lb) from settings.
- Weights are stored internally in kilograms regardless of display unit; conversion is applied at the UI layer.
- The entered weight is saved to the `SessionExercise` record.
- The weight is also saved to `ExerciseDefaultWeight` so it becomes the pre-populated default for future sessions.

#### 5.4.2 Edit Weight for a Session Exercise
- The user can tap the weight field on a session exercise card to edit the weight at any point during the session.
- Editing the weight updates the `SessionExercise` record and also updates `ExerciseDefaultWeight`.

#### 5.4.3 Default Weight Pre-population
- When a new session starts, each session exercise is pre-populated with the weight from `ExerciseDefaultWeight` if one exists for that exercise.

---

### 5.5 Session History

#### 5.5.1 View Toggle: List and Calendar
- The history screen provides two views, switchable via a toggle (e.g., tab bar or segmented button):
  - **List view** (default)
  - **Calendar view**
- The selected view is remembered for the duration of the app session.

#### 5.5.2 List View
- All completed workout sessions are listed in reverse chronological order (most recent first).
- Each list item shows:
  - Workout day name
  - Date and time of session
  - Total duration (formatted as HH:MM:SS or MM:SS)
  - Number of exercises performed
- Tapping a session navigates to the Session Detail screen.

#### 5.5.3 Calendar View
- A monthly calendar is displayed with days that have one or more completed sessions visually indicated (e.g., a dot or highlight on the date).
- The user can navigate between months using previous/next controls.
- Tapping a date that has sessions shows a brief summary list of that day's sessions below the calendar (or as a bottom sheet).
- Tapping a session from the day summary navigates to the Session Detail screen.
- Days with no sessions are tappable but produce no action.

#### 5.5.4 View Session Detail
- The user can tap a session (from either the list view or the calendar view) to view full details:
  - Workout day name
  - Date, start time, end time
  - Total duration
  - Session notes (if any)
  - For each exercise:
    - Exercise name
    - Sets completed / target sets
    - Weight used

---

### 5.6 CSV Export

#### 5.6.1 Export History to CSV
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

### 5.7 Rest Timer

#### 5.7.1 Timer Behavior
- The rest timer counts down from the configured `restTimerSeconds` value.
- It starts (or resets) each time the user marks a set as performed.
- It is displayed in the fixed timer panel at the top of the active session screen (see 5.3.2), always visible regardless of scroll position, showing remaining time as a progress indicator (e.g., circular arc or progress bar) and a MM:SS countdown.

#### 5.7.2 Timer Expiry
- When the rest timer reaches zero:
  - If `restTimerSound` is enabled, an audible alert is played using the **media audio stream** (`AudioManager.STREAM_MUSIC`). This ensures the alert is audible whenever the media volume is raised, regardless of the device ringer/silent mode.
  - If `restTimerVibrate` is enabled, the device vibrates.
  - The progress indicator reflects the expired state.
- The timer does not loop after expiry.

#### 5.7.3 Timer Reset
- The rest timer resets to its configured duration whenever a set is marked as performed.

---

### 5.8 Inactivity Timer

#### 5.8.1 Timer Behavior
- The inactivity timer counts down from the configured `inactivityTimerSeconds` value.
- It starts (or resets) each time the user marks a set as performed.
- It runs concurrently with the rest timer.
- It is displayed in the same fixed timer panel as the rest timer (see 5.3.2 and 5.7.1), always visible regardless of scroll position.

#### 5.8.2 Timer Expiry
- When the inactivity timer reaches zero:
  - If `inactivityTimerSound` is enabled, an audible alert is played using the **media audio stream** (`AudioManager.STREAM_MUSIC`), consistent with the rest timer alert.
  - If `inactivityTimerVibrate` is enabled, the device vibrates.
- The timer does not loop after expiry.

#### 5.8.3 Timer Reset
- The inactivity timer resets to its configured duration whenever a set is marked as performed.

#### 5.8.4 Distinction from Rest Timer
- The rest timer signals when the user *should* begin the next set (rest period over).
- The inactivity timer signals when the user has been inactive for too long (reminder to continue exercising).
- Both timers reset on the same event (set marked performed) but serve different motivational purposes.

---

### 5.9 Application Settings

The user can access a Settings screen from the main navigation.

#### 5.9.1 Rest Timer Duration
- Integer input (seconds), minimum 1.
- Label: "Rest Timer Duration (seconds)"
- Default: 90

#### 5.9.2 Inactivity Timer Duration
- Integer input (seconds), minimum 1.
- Label: "Inactivity Timer Duration (seconds)"
- Default: 300

#### 5.9.3 Rest Timer Notifications
- Toggle: "Rest Timer Sound" (default: on)
- Toggle: "Rest Timer Vibration" (default: on)

#### 5.9.4 Inactivity Timer Notifications
- Toggle: "Inactivity Timer Sound" (default: on)
- Toggle: "Inactivity Timer Vibration" (default: on)

#### 5.9.5 Keep Screen On
- Toggle: "Keep Screen On During Workout" (default: on)
- When enabled, the `FLAG_KEEP_SCREEN_ON` window flag is set on the session screen while a workout is active and cleared when the session ends.

#### 5.9.6 Theme
- Toggle or selector: "Dark Mode" / "Light Mode" / "System Default"
- Applies the selected theme across the entire application.

#### 5.9.7 Weight Unit
- Selector: "kg" / "lb" (default: kg)
- All weight values throughout the application (entry fields, session cards, history, CSV export) are displayed in the selected unit.
- Weights are stored internally in kilograms; conversion is applied at the UI layer.

---

## 6. Screen Inventory

| Screen | Description |
|---|---|
| Home / Dashboard | Entry point; quick-start workout, navigation to other sections |
| Exercise List | View, add, edit, delete exercises |
| Exercise Form | Create or edit a single exercise |
| Workout Day List | View, add, edit, delete workout days |
| Workout Day Detail | Assign, reorder, and remove exercises for a workout day |
| Active Session | Current workout session with exercise cards, timers, and set controls |
| End Session Dialog | Notes input and confirm to end session |
| Session History | List and calendar views of completed sessions; toggle between views via tab/segmented button |
| Session Detail | Full detail view of a single past session |
| Settings | Application settings |

---

## 7. Navigation Flow

```
Home / Dashboard
├── [Start Workout] → Workout Day List
│   └── [Select Day] → Active Session
│       └── [End Workout] → End Session Dialog → Session History
├── Exercises → Exercise List
│   ├── [Add] → Exercise Form
│   └── [Tap Exercise] → Exercise Form (edit)
├── Workout Days → Workout Day List
│   ├── [Add] → Workout Day Form
│   └── [Tap Day] → Workout Day Detail
│       └── [Add Exercise] → Exercise picker
├── History → Session History
│   ├── [List view] → [Tap Session] → Session Detail
│   └── [Calendar view] → [Tap Date] → Day summary → [Tap Session] → Session Detail
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

## 9. Implementation Phases

There is a single implementation phase for this project. All features described in this document are to be delivered together as version 1.0.
