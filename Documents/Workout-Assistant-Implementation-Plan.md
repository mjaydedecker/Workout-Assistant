# Implementation Plan
# Workout-Assistant Android Application

**Version:** 1.0  
**Date:** 2026-04-02  
**Based on:** Workout-Assistant-FSD.md v1.0

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Dependency Graph Between Layers](#2-dependency-graph-between-layers)
3. [Milestones](#3-milestones)
   - M1: Project Scaffolding and Build Configuration
   - M2: Data Layer
   - M3: Navigation Skeleton and Theme Wiring
   - M4: Settings Feature
   - M5: Exercise Management Feature
   - M6: Workout Day Management Feature
   - M7: Timer Infrastructure
   - M8: Active Session Feature
   - M9: Session History and CSV Export
   - M10: Home / Dashboard
   - M11: Polish, Accessibility, and Permissions
4. [Key Architectural Decisions](#4-key-architectural-decisions)
5. [Critical Files](#5-critical-files)

---

## 1. Project Structure

```
app/src/main/java/com/example/workoutassistant/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── converters/
│   │   │   └── InstantConverter.kt
│   │   ├── dao/
│   │   │   ├── ExerciseDao.kt
│   │   │   ├── WorkoutDayDao.kt
│   │   │   ├── WorkoutDayExerciseDao.kt
│   │   │   ├── WorkoutSessionDao.kt
│   │   │   ├── SessionExerciseDao.kt
│   │   │   ├── ExerciseDefaultWeightDao.kt
│   │   │   └── AppSettingsDao.kt
│   │   └── entity/
│   │       ├── ExerciseEntity.kt
│   │       ├── WorkoutDayEntity.kt
│   │       ├── WorkoutDayExerciseEntity.kt
│   │       ├── WorkoutSessionEntity.kt
│   │       ├── SessionExerciseEntity.kt
│   │       ├── ExerciseDefaultWeightEntity.kt
│   │       └── AppSettingsEntity.kt
│   ├── repository/
│   │   ├── ExerciseRepository.kt
│   │   ├── WorkoutDayRepository.kt
│   │   ├── SessionRepository.kt
│   │   └── SettingsRepository.kt
│   └── model/                          ← domain/UI models (non-entity)
│       ├── Exercise.kt
│       ├── WorkoutDay.kt
│       ├── WorkoutDayWithExercises.kt
│       ├── WorkoutSession.kt
│       ├── SessionExercise.kt
│       └── AppSettings.kt
├── di/
│   └── AppModule.kt                    ← Hilt module (optional; manual DI is fine)
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt                   ← sealed class of routes
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── exercise/
│   │   ├── ExerciseListScreen.kt
│   │   ├── ExerciseListViewModel.kt
│   │   ├── ExerciseFormScreen.kt
│   │   └── ExerciseFormViewModel.kt
│   ├── workoutday/
│   │   ├── WorkoutDayListScreen.kt
│   │   ├── WorkoutDayListViewModel.kt
│   │   ├── WorkoutDayDetailScreen.kt
│   │   └── WorkoutDayDetailViewModel.kt
│   ├── session/
│   │   ├── ActiveSessionScreen.kt
│   │   ├── ActiveSessionViewModel.kt
│   │   ├── EndSessionDialog.kt
│   │   └── components/
│   │       ├── ExerciseCard.kt
│   │       ├── RestTimerWidget.kt
│   │       └── InactivityTimerWidget.kt
│   ├── history/
│   │   ├── SessionHistoryScreen.kt
│   │   ├── SessionHistoryViewModel.kt
│   │   ├── SessionDetailScreen.kt
│   │   └── SessionDetailViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
├── util/
│   ├── TimerManager.kt
│   ├── CsvExporter.kt
│   ├── AudioVibrationManager.kt
│   └── DurationFormatter.kt
└── MainActivity.kt

app/src/main/res/
├── raw/
│   └── timer_alert.mp3
└── xml/
    └── file_paths.xml

app/src/main/AndroidManifest.xml
build.gradle (app)
```

---

## 2. Dependency Graph Between Layers

```
Entities → DAOs → AppDatabase
                      ↓
                Repositories
                      ↓
              ViewModels  ←── TimerManager / CsvExporter / AudioVibrationManager
                      ↓
            Composable Screens
                      ↓
               NavGraph (wired in MainActivity)
```

Each layer only depends on the layer below it. ViewModels never import Composable classes; Screens never import Repositories or DAOs directly.

---

## 3. Milestones

---

### Milestone 1 — Project Scaffolding and Build Configuration

**Goal:** A compiling, runnable skeleton with no features yet.

**Tasks:**

1. Create a new Android project in Android Studio (Empty Compose Activity template, package name `com.example.workoutassistant`, min SDK 26, target SDK latest).
2. `build.gradle (app)` — add all library dependencies:
   - `room-runtime`, `room-ktx`, `room-compiler` (KSP)
   - `lifecycle-viewmodel-compose`, `lifecycle-runtime-ktx`
   - `navigation-compose`
   - `kotlinx-coroutines-android`
   - `hilt-android` + `hilt-navigation-compose` (optional; manual DI via `AppContainer` is also acceptable)
   - `sh.calvin.reorderable:reorderable` for drag-and-drop in Compose `LazyColumn`
3. `AndroidManifest.xml` — declare `VIBRATE` permission; add `FileProvider` entry pointing to `res/xml/file_paths.xml`.
4. `res/xml/file_paths.xml` — define scoped path for CSV output directory.
5. `ui/theme/` — create `Color.kt`, `Type.kt`, `Theme.kt` with both light and dark `ColorScheme` definitions. Theme accepts a `darkTheme: Boolean` parameter driven by settings.
6. `MainActivity.kt` — minimal `setContent { WorkoutAssistantTheme { NavGraph() } }`.
7. `Screen.kt` — define all route constants as a sealed class with string routes and typed argument definitions.

**Key decision:** Whether to use Hilt or a custom `AppContainer` on the `Application` subclass. For a small single-user app, manual DI is simpler and avoids annotation processing overhead.

---

### Milestone 2 — Data Layer

**Goal:** Full persistence layer, fully testable in isolation, no UI yet.

#### 2a — Entities

| File | Key Details |
|---|---|
| `ExerciseEntity.kt` | `@Entity(tableName = "exercises")` — `id`, `name`, `defaultSets` |
| `WorkoutDayEntity.kt` | `@Entity(tableName = "workout_days")` |
| `WorkoutDayExerciseEntity.kt` | `@Entity` with foreign keys to exercises and workout_days; `indices` on both FKs; fields: `id`, `workoutDayId`, `exerciseId`, `orderIndex` |
| `WorkoutSessionEntity.kt` | `startTime` and `endTime` stored as `Long` epoch millis via `InstantConverter` |
| `SessionExerciseEntity.kt` | Snapshot field `exerciseName`; nullable `weightKg: Double?` |
| `ExerciseDefaultWeightEntity.kt` | Primary key is `exerciseId` (not auto-generated) |
| `AppSettingsEntity.kt` | Single-row table; fixed PK = 1 |
| `InstantConverter.kt` | `@TypeConverter` pair: `Long? ↔ Instant?` |

#### 2b — DAOs

| File | Key Operations |
|---|---|
| `ExerciseDao` | `upsert`, `delete`, `getAll(): Flow<List<ExerciseEntity>>`, `getById`, join query for exercises in a day |
| `WorkoutDayDao` | `upsert`, `delete`, `getAll(): Flow`, `getById`, relation query for day with exercises |
| `WorkoutDayExerciseDao` | `insert`, `delete`, `deleteAllForDay`, `updateOrderIndex`, `getExercisesForDay` ordered by `orderIndex` |
| `WorkoutSessionDao` | `insert`, `update`, `getActiveSession` (where `endTime IS NULL`), `getAllCompleted(): Flow` (desc by `startTime`), `getById` |
| `SessionExerciseDao` | `insert`, `update`, `delete`, `getForSession(): Flow` ordered by `orderIndex` |
| `ExerciseDefaultWeightDao` | `upsert`, `getByExerciseId` |
| `AppSettingsDao` | `upsert`, `get(): Flow<AppSettingsEntity?>` |

**Key decision for `AppSettings`:** Repository coalesces a `null` emission to a hardcoded default object so the rest of the app never deals with null settings.

#### 2c — Database

- `AppDatabase.kt` — `@Database(entities = [...], version = 1)`, `@TypeConverters(InstantConverter::class)`, abstract DAO accessors, singleton `getInstance` with `fallbackToDestructiveMigration` for v1.

#### 2d — Domain Models

Non-Room data classes in `data/model/` — these are what ViewModels and UI consume; entities never leave the repository layer:
- `Exercise`, `WorkoutDay`, `WorkoutDayWithExercises`, `WorkoutSession`, `SessionExercise`, `AppSettings`

#### 2e — Repositories

| Repository | Responsibilities |
|---|---|
| `ExerciseRepository` | Wraps `ExerciseDao` + `WorkoutDayExerciseDao`; maps entities to domain models; `deleteExercise` surfaces whether the exercise is assigned to a day |
| `WorkoutDayRepository` | `deleteWorkoutDay` checks for existing sessions; `reorderExercises` updates all `orderIndex` values in a single `withTransaction` |
| `SessionRepository` | `startSession` creates the session and copies exercises from `WorkoutDayExercise` into `SessionExercise` rows with default weights pre-populated; `updateWeight` saves to both `SessionExercise` and `ExerciseDefaultWeight` atomically |
| `SettingsRepository` | Exposes `Flow<AppSettings>`; handles null coalescion from `AppSettingsDao` |

---

### Milestone 3 — Navigation Skeleton and Theme Wiring

**Goal:** All screens exist as empty stubs; navigation between them works; theme toggles correctly.

**Tasks:**

1. `NavGraph.kt` — `NavHost` with composable routes for all 10 screens. Typed arguments use `navArgument(...)` with `NavType.LongType` and `defaultValue = -1L` to distinguish create vs. edit.
2. Each screen file gets a minimal stub composable rendering the screen name.
3. `HomeScreen.kt` — `BottomNavigationBar` linking to Exercises, Workout Days, History, Settings; prominent "Start Workout" button.
4. `MainActivity.kt` — observes `SettingsViewModel.darkMode` via `collectAsState` and passes the boolean to `WorkoutAssistantTheme`.

**Key decision:** Use `BottomNavigationBar` for top-level destinations. The Active Session screen is a full-screen destination that hides the bar (controlled by checking the current route in `NavGraph`).

---

### Milestone 4 — Settings Feature

**Goal:** Settings screen fully functional before other features — it drives timer durations and theme across the entire app.

**Tasks:**

1. `SettingsViewModel.kt` — collects `Flow<AppSettings>` from `SettingsRepository`; exposes `uiState: StateFlow<AppSettings>`; individual `suspend fun updateXxx(...)` functions call `SettingsRepository.save(...)`.
2. `SettingsScreen.kt`:
   - Number inputs for rest/inactivity timer durations (min 1, enforced in ViewModel)
   - Toggles for sound and vibration (rest and inactivity)
   - Toggle for keep-screen-on
   - Three-option theme selector: Light / Dark / System Default
   - All changes applied immediately (no Save button)
3. Verify theme switching end-to-end.

---

### Milestone 5 — Exercise Management Feature

**Goal:** Full CRUD for exercises.

**Tasks:**

1. `ExerciseListViewModel.kt` — collects `Flow<List<Exercise>>`; `deleteExercise` emits a confirmation event when the exercise is assigned to a day.
2. `ExerciseListScreen.kt` — alphabetical `LazyColumn`; swipe-to-delete with `SwipeToDismiss`; FAB for add; tap to edit.
3. `ExerciseFormViewModel.kt` — accepts optional `exerciseId`; validates name (non-empty, unique) and sets (>= 1); emits `NavigateBack` side-effect on success.
4. `ExerciseFormScreen.kt` — `TextField` for name with error state; numeric `TextField` for default sets; save in top bar.
5. "Assigned to day" confirmation dialog on delete, driven by `StateFlow<ExercisePendingDelete?>` in the ViewModel.

---

### Milestone 6 — Workout Day Management Feature

**Goal:** Full CRUD for workout days including exercise assignment with drag-and-drop reorder.

**Tasks:**

1. `WorkoutDayListViewModel.kt` — collects `Flow<List<WorkoutDay>>`; `deleteWorkoutDay` checks for existing sessions and emits a confirmation event.
2. `WorkoutDayListScreen.kt` — `LazyColumn` showing name and exercise count; swipe-to-delete; FAB to create; tap to open detail.
3. `WorkoutDayDetailViewModel.kt` — loads `WorkoutDayWithExercises`; `addExercise`, `removeExercise`, `onReorder` (updates all `orderIndex` in a single repository transaction).
4. `WorkoutDayDetailScreen.kt`:
   - Editable title in top bar
   - `LazyColumn` with `reorderable` modifier (drag handle on each item)
   - "Add Exercise" opens a bottom sheet listing exercises not yet in the day
   - Drag release triggers `onReorder` in ViewModel

**Key decision on drag-and-drop:** Use `sh.calvin.reorderable:reorderable`. The `onReorder` callback fires on drag release; the ViewModel computes new dense 0-based `orderIndex` values and writes them in a `withTransaction` block.

---

### Milestone 7 — Timer Infrastructure

**Goal:** Rotation-safe, reusable timer logic ready before the session screen.

**Tasks:**

1. `TimerManager.kt` — owned by `ActiveSessionViewModel` (lives in `viewModelScope`, survives rotation):
   - `restTimeRemaining: StateFlow<Long>` (seconds, -1 = idle)
   - `inactivityTimeRemaining: StateFlow<Long>` (seconds, -1 = idle)
   - `fun startOrReset(restSeconds: Int, inactivitySeconds: Int)` — cancels existing jobs; launches two independent coroutine-based countdown flows using `flow { while(...) { emit(tick); delay(1000) } }`
   - `fun cancel()` — cancels both jobs

2. `AudioVibrationManager.kt` — accepts `Context`:
   - `fun playTimerAlert()` — `MediaPlayer` with `R.raw.timer_alert`; proper lifecycle (prepare → setOnCompletionListener → release)
   - `fun vibrate(durationMs: Long)` — `VibrationEffect.createOneShot` via `Vibrator` / `VibratorManager` (API 26+)

3. `res/raw/timer_alert.mp3` — add a short alert sound asset.

---

### Milestone 8 — Active Session Feature

**Goal:** Core workout tracking screen — exercise cards, set controls, weight tracking, timers, end session.

**Tasks:**

1. `ActiveSessionViewModel.kt`:
   - `init` checks `SessionRepository.getActiveSession()` — resumes if in progress, else creates via `startSession(workoutDayId)`
   - `uiState: StateFlow<ActiveSessionUiState>` — session + `List<SessionExercise>`
   - `markSetComplete(id)` — increment `completedSets` (capped at `targetSets`), persist, reset timers, trigger weight prompt if needed
   - `decrementSet(id)` — decrement `completedSets` (floor 0), persist; does not touch timers
   - `updateWeight(id, weightKg)` — updates `SessionExercise.weightKg` and calls `SessionRepository.saveDefaultWeight`
   - `deleteExercise(id)` — emits confirmation event; removes from session on confirm
   - `endSession(notes)` — sets `endTime`, calculates `durationSeconds`, saves, emits `ScreenFlagEvent` to release keep-screen-on
   - Observes `SettingsRepository.settings` to keep timer durations and alert prefs current
   - On timer expiry, checks sound/vibrate settings and calls `AudioVibrationManager`

2. `ActiveSessionScreen.kt`:
   - `Scaffold` with top bar showing workout day name and "End Workout" button
   - `RestTimerWidget` and `InactivityTimerWidget` as sticky header
   - `LazyColumn` of `ExerciseCard` composables
   - `DisposableEffect` on window to set/clear `FLAG_KEEP_SCREEN_ON` based on `keepScreenOn` setting

3. `ExerciseCard.kt`:
   - Exercise name, `"X / Y sets"` progress
   - `+` button (disabled when complete), `-` button (disabled at 0)
   - Decimal `TextField` for weight
   - Visual "completed" state (green tint / checkmark) when `completedSets == targetSets`
   - Swipe-to-delete with confirmation

4. `RestTimerWidget.kt` — `Canvas`-drawn circular arc progress; MM:SS countdown in center; `animateFloatAsState` for smooth arc; grayed-out when idle.

5. `InactivityTimerWidget.kt` — same pattern as `RestTimerWidget`, visually distinct color.

6. `EndSessionDialog.kt` — `AlertDialog` with multiline notes `TextField`; Confirm/Cancel.

7. Weight prompt dialog — triggered by `uiState.weightPromptForExerciseId != null`; decimal `TextField`; OK / Skip.

---

### Milestone 9 — Session History and CSV Export

**Goal:** Read-only history views and CSV export.

**Tasks:**

1. `SessionHistoryViewModel.kt` — collects `Flow<List<WorkoutSession>>`; maps to `SessionSummaryUiModel` (formatted date, time, duration); `exportCsv()` delegates to `CsvExporter`.
2. `SessionHistoryScreen.kt` — reverse-chronological `LazyColumn`; each item shows day name, date/time, duration, exercise count; tap to detail; top bar "Export CSV" action.
3. `SessionDetailViewModel.kt` — loads `WorkoutSession` + `List<SessionExercise>` by `sessionId`.
4. `SessionDetailScreen.kt` — day name, date, start/end times, duration, notes, `LazyColumn` of per-exercise rows (name, sets, weight).
5. `CsvExporter.kt`:
   - Columns: Session Date, Start Time, End Time, Duration, Workout Day, Exercise, Sets Completed, Target Sets, Weight (kg), Notes
   - One row per exercise per session
   - File name: `workout_history_YYYYMMDD_HHMMSS.csv`
   - Written to `context.cacheDir` (API 26–28) or `MediaStore.Downloads` (API 29+) and wrapped in a `FileProvider` URI
   - Caller emits `ShareCsvEvent(uri)`; screen starts `Intent.ACTION_SEND`
6. `DurationFormatter.kt` — `Long.toHhMmSs(): String` and `Long.toMmSs(): String` used across session display and timer widgets.

---

### Milestone 10 — Home / Dashboard

**Goal:** App entry point with quick-start and navigation.

**Tasks:**

1. `HomeViewModel.kt` — exposes `Flow<WorkoutSession?>` from `SessionRepository.getActiveSession()` so the UI can show "Resume Workout" vs "Start Workout".
2. `HomeScreen.kt`:
   - "Start Workout" (navigates to Workout Day List) or "Resume Workout" (navigates directly to Active Session) based on active session state
   - Summary counts: exercises defined, workout days, completed sessions
   - Navigation shortcuts to main sections
3. Enforce single-session constraint: `ActiveSessionViewModel.init` redirects to the existing session if one is already active, regardless of which workout day is tapped.

---

### Milestone 11 — Polish, Accessibility, and Permissions

**Goal:** Production-quality finish.

**Tasks:**

1. Add `contentDescription` to all `IconButton`, `Image`, and custom `Canvas` composables.
2. Verify WCAG AA color contrast (4.5:1 for normal text) in both themes; adjust `Color.kt` as needed.
3. Confirm `VIBRATE` permission and `FileProvider` config are correct for all API levels 26–latest.
4. Test screen rotation on the Active Session screen — timers must not reset (they live in `viewModelScope`).
5. Handle the edge case where an exercise is deleted while an active session is running — the session ViewModel reacts to the updated `Flow<List<SessionExercise>>` emission automatically; no crash.
6. Add empty-state UI (illustration + prompt) to `ExerciseListScreen`, `WorkoutDayListScreen`, and `SessionHistoryScreen`.
7. Set correct `keyboardOptions` and `keyboardActions` on all `TextField` inputs:
   - `KeyboardType.Decimal` for weight fields
   - `KeyboardType.Number` for set count and duration fields
   - `ImeAction.Done` on the last field in each form

---

## 4. Key Architectural Decisions

| Decision | Rationale |
|---|---|
| Room `Flow` on all list queries | Automatic UI updates on data changes with no manual refresh calls |
| `StateFlow` in ViewModels (not `LiveData`) | Compose works naturally with `collectAsStateWithLifecycle`; no `LiveData` observer boilerplate |
| `TimerManager` owned by ViewModel | `viewModelScope` survives rotation; no foreground `Service` complexity needed for a local-only timer |
| Repositories as the only gateway to DAOs | ViewModels never import DAOs; entity-to-domain mapping is centralized in the repository layer |
| `AppSettings` single-row with fixed PK = 1 | Simplest Room pattern; `upsert` always targets the same row |
| Snapshot `exerciseName` in `SessionExercise` | Historical records are immutable with respect to future exercise renames or deletes |
| `FileProvider` for CSV sharing | Required for sharing files via `Intent` on API 24+; avoids `file://` URI exposure |
| `ExerciseDefaultWeight` as a separate table | Clean separation; can be independently managed; PK = `exerciseId` makes upsert trivially correct |
| No Hilt (or optional) | Reduces annotation processing complexity; an `AppContainer` on a custom `Application` class is sufficient |
| `BottomNavigationBar` for top-level navigation | Standard Material 3 pattern; Active Session hides the bar (full-screen destination) |

---

## 5. Critical Files

These files are the highest-risk, highest-complexity files in the project. Get them right before building screens that depend on them.

| File | Why Critical |
|---|---|
| `data/db/AppDatabase.kt` | Root of the Room database; all entity and DAO wiring flows through here; must be correct before any feature can be tested |
| `ui/session/ActiveSessionViewModel.kt` | Most complex ViewModel; owns `TimerManager`, enforces single-session constraint, coordinates weight tracking, and orchestrates all session state transitions |
| `data/repository/SessionRepository.kt` | Wraps the most tables; contains transactional `startSession` and `updateWeight` logic that must be atomic |
| `util/TimerManager.kt` | The countdown engine; dual timers, reset-on-set, and expiry callbacks are all centralized here; must be correct before the session screen can be built |
| `ui/navigation/NavGraph.kt` | Single source of truth for all routes and argument definitions; any route change cascades across every navigation call site in the app |
