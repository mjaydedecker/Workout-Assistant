# Implementation Plan v2
# Workout-Assistant Android Application

**Version:** 2.0
**Date:** 2026-04-05
**Based on:** Workout-Assistant-FSD.md v1.3
**Supersedes:** Workout-Assistant-Implementation-Plan.md (v1.2)

---

## Table of Contents

1. [What Is Already Built](#1-what-is-already-built)
2. [What the FSD v1.3 Requires That Is New or Changed](#2-what-the-fsd-v13-requires-that-is-new-or-changed)
3. [Updated Project Structure](#3-updated-project-structure)
4. [Milestones](#4-milestones)
   - M1: Room Database Migration (version 2 → 3)
   - M2: CSV Asset Parsing and Library Seeding
   - M3: Data Layer Updates
   - M4: Exercise Library UI — Browse and Search
   - M5: Exercise Detail Screen
   - M6: Video Playback
   - M7: Workout Day Detail — Sets Per Assignment
   - M8: Custom Exercise Management Cleanup
   - M9: Session Start — Sets Source Change
   - M10: Navigation and Screen Registration
   - M11: Integration Testing and Regression
5. [Architectural Decisions and Risks](#5-architectural-decisions-and-risks)

---

## 1. What Is Already Built

The following milestones from the v1.2 plan are fully implemented and in a state that only requires targeted edits (not rewrites) to accommodate the v1.3 changes.

### Fully Complete (No New Work Required)

| Area | Files | Status |
|---|---|---|
| Project scaffolding and build config | `build.gradle.kts`, `AndroidManifest.xml`, `file_paths.xml` | Complete |
| Room database version 2 | `AppDatabase.kt`, all entity files, all DAO files | Complete — requires schema bump to v3 |
| Migrations 1→2 | `Migrations.kt` | Complete — v1→v2 migration object exists |
| Theme and navigation skeleton | `Theme.kt`, `Color.kt`, `Type.kt`, `NavGraph.kt`, `Screen.kt` | Complete |
| Settings feature | `SettingsScreen.kt`, `SettingsViewModel.kt`, `SettingsRepository.kt`, `AppSettingsDao.kt`, `AppSettingsEntity.kt` | Complete |
| Workout Day management (CRUD, rename, reorder) | `WorkoutDayListScreen.kt`, `WorkoutDayListViewModel.kt`, `WorkoutDayDetailScreen.kt`, `WorkoutDayDetailViewModel.kt`, `WorkoutDayRepository.kt` | Complete — requires sets-per-assignment edits |
| Timer infrastructure | `TimerManager.kt`, `AudioVibrationManager.kt` | Complete |
| Active Session feature | `ActiveSessionScreen.kt`, `ActiveSessionViewModel.kt`, `EndSessionDialog.kt`, `ExerciseCard.kt`, `RestTimerWidget.kt` | Complete — requires sets-source fix |
| Session History and CSV Export | `SessionHistoryScreen.kt`, `SessionHistoryViewModel.kt`, `SessionDetailScreen.kt`, `SessionDetailViewModel.kt`, `WorkoutCalendar.kt`, `CsvExporter.kt` | Complete |
| Home / Dashboard | `HomeScreen.kt`, `HomeViewModel.kt` | Complete |
| Utilities | `TimerManager.kt`, `WeightFormatter.kt`, `DurationFormatter.kt`, `CsvExporter.kt`, `AudioVibrationManager.kt` | Complete |
| Unit tests | `SessionDurationTest.kt`, `TimerManagerTest.kt`, `WeightFormatterTest.kt` | Complete |

### Complete but Requiring Targeted Edits

| Area | Reason for Edit |
|---|---|
| `ExerciseEntity.kt` | Must add 8 new columns; `defaultSets` column must be dropped |
| `WorkoutDayExerciseEntity.kt` | Must add `sets: Int` column |
| `Exercise.kt` (domain model) | Must add new fields; remove `defaultSets` |
| `WorkoutDayExerciseItem.kt` (domain model) | Must add `sets: Int` field |
| `ExerciseRepository.kt` | Must support library vs. custom filtering; CSV seeding |
| `WorkoutDayRepository.kt` | `addExerciseToDay` signature must accept `sets` parameter |
| `SessionRepository.startSession()` | Must read sets from `WorkoutDayExercise.sets`, not `Exercise.defaultSets` |
| `ExerciseListScreen.kt` | Must be repurposed as Custom Exercise List; remove `defaultSets` display |
| `ExerciseFormScreen.kt` | Must remove the Default Sets field |
| `ExerciseFormViewModel.kt` | Must remove `defaultSets` validation and saving |
| `ExerciseListViewModel.kt` | Must filter to `isCustom = true` exercises only |
| `WorkoutDayDetailScreen.kt` | Add Exercise picker must show sets entry dialog; list items must display sets from assignment |
| `WorkoutDayDetailViewModel.kt` | `addExercise` must accept `sets: Int`; UI state must expose per-assignment sets |
| `NavGraph.kt` | Must register two new routes: Exercise Library and Exercise Detail |
| `Screen.kt` | Must add `ExerciseLibrary` and `ExerciseDetail` sealed objects |
| `WorkoutAssistantApp.kt` | Must handle library seeding trigger on init |

---

## 2. What the FSD v1.3 Requires That Is New or Changed

### New Files to Create

| File | Purpose |
|---|---|
| `app/src/main/assets/exercises.csv` | Bundled exercise data asset (copy from repo root) |
| `util/CsvParser.kt` | Parses `exercises.csv` from assets into `ExerciseEntity` objects |
| `util/LibrarySeeder.kt` | Runs once on first launch (guarded by a flag) to insert all library rows |
| `ui/library/ExerciseLibraryScreen.kt` | Browse screen: exercises grouped by primary muscle, expandable sections, search |
| `ui/library/ExerciseLibraryViewModel.kt` | Loads and groups library exercises; manages search query state |
| `ui/library/ExerciseDetailScreen.kt` | Detail screen: all fields, images, video links |
| `ui/library/ExerciseDetailViewModel.kt` | Loads a single exercise by ID; exposes parsed video URL list |

### Schema Changes (Room Database Migration v2 → v3)

| Table | Change |
|---|---|
| `exercises` | DROP `defaultSets` (via table-rebuild — SQLite does not support DROP COLUMN before API 35) |
| `exercises` | ADD `isCustom INTEGER NOT NULL DEFAULT 1` |
| `exercises` | ADD `force TEXT` |
| `exercises` | ADD `equipment TEXT` |
| `exercises` | ADD `primaryMuscles TEXT` |
| `exercises` | ADD `secondaryMuscles TEXT` |
| `exercises` | ADD `description TEXT` |
| `exercises` | ADD `videoUrls TEXT` |
| `exercises` | ADD `imageRefs TEXT` |
| `workout_day_exercises` | ADD `sets INTEGER NOT NULL DEFAULT 3` (populated from `Exercise.defaultSets` before that column is dropped) |

### Behavioral Changes

| Feature | Old Behavior | New Behavior |
|---|---|---|
| Exercise creation | Name + default sets required | Name only; `isCustom = true` set automatically; no sets |
| Exercise list screen | Shows all exercises | Shows only `isCustom = true` exercises |
| Add exercise to workout day | Bottom sheet of all exercises, no sets entry | Library or custom picker; sets entry dialog appears at add time |
| Session start | `targetSets` copied from `Exercise.defaultSets` | `targetSets` copied from `WorkoutDayExercise.sets` |
| Workout Day Detail items | Show `exercise.defaultSets` sets | Show `assignmentItem.sets` sets |
| Navigation | No Exercise Library destination | Two new destinations: `ExerciseLibrary` and `ExerciseDetail` |

---

## 3. Updated Project Structure

Only the additions and changes are shown. The full tree from the v1.2 plan remains intact except where noted.

```
app/src/main/
├── assets/
│   └── exercises.csv                            ← NEW (copied from repo root)
│
├── java/com/mjaydedecker/workoutassistant/
│   ├── data/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt                   ← EDIT: version 2→3; add new entity fields
│   │   │   ├── migrations/
│   │   │   │   └── Migrations.kt                ← EDIT: add MIGRATION_2_3
│   │   │   ├── dao/
│   │   │   │   ├── ExerciseDao.kt               ← EDIT: add library-specific queries
│   │   │   │   └── WorkoutDayExerciseDao.kt     ← EDIT: include sets in insert/query
│   │   │   └── entity/
│   │   │       ├── ExerciseEntity.kt            ← EDIT: new columns, remove defaultSets
│   │   │       └── WorkoutDayExerciseEntity.kt  ← EDIT: add sets field
│   │   ├── model/
│   │   │   ├── Exercise.kt                      ← EDIT: new fields, remove defaultSets
│   │   │   └── WorkoutDay.kt                    ← EDIT: WorkoutDayExerciseItem gains sets
│   │   └── repository/
│   │       ├── ExerciseRepository.kt            ← EDIT: filter by isCustom; seed support
│   │       ├── WorkoutDayRepository.kt          ← EDIT: addExerciseToDay takes sets param
│   │       └── SessionRepository.kt            ← EDIT: startSession reads sets from WDE
│   │
│   ├── ui/
│   │   ├── exercise/
│   │   │   ├── ExerciseListScreen.kt            ← EDIT: custom-only; remove sets display
│   │   │   ├── ExerciseListViewModel.kt         ← EDIT: filter isCustom = true
│   │   │   ├── ExerciseFormScreen.kt            ← EDIT: remove Default Sets field
│   │   │   └── ExerciseFormViewModel.kt         ← EDIT: remove sets validation
│   │   ├── library/                             ← NEW PACKAGE
│   │   │   ├── ExerciseLibraryScreen.kt         ← NEW
│   │   │   ├── ExerciseLibraryViewModel.kt      ← NEW
│   │   │   ├── ExerciseDetailScreen.kt          ← NEW
│   │   │   └── ExerciseDetailViewModel.kt       ← NEW
│   │   ├── workoutday/
│   │   │   ├── WorkoutDayDetailScreen.kt        ← EDIT: sets in list items; sets dialog
│   │   │   └── WorkoutDayDetailViewModel.kt     ← EDIT: addExercise takes sets; expose
│   │   └── navigation/
│   │       ├── Screen.kt                        ← EDIT: add ExerciseLibrary, ExerciseDetail
│   │       └── NavGraph.kt                      ← EDIT: register two new composable routes
│   │
│   ├── util/
│   │   ├── CsvParser.kt                         ← NEW
│   │   └── LibrarySeeder.kt                     ← NEW
│   │
│   └── WorkoutAssistantApp.kt                   ← EDIT: trigger library seeding on init
```

---

## 4. Milestones

Milestones are ordered so that each one compiles and runs before the next begins. The database migration is the foundation for everything else and goes first.

---

### Milestone 1 — Room Database Migration (version 2 → 3)

**Goal:** The database schema reflects the v1.3 data model. Existing user data (exercises, workout days, sessions) is preserved without destructive migration.

**Background on SQLite DROP COLUMN:** SQLite added `DROP COLUMN` support in version 3.35 (Android API 35 / Android 15). Since this app targets `minSdk = 26`, the `defaultSets` column cannot be dropped with a plain `ALTER TABLE ... DROP COLUMN` on all supported devices. The correct approach is the SQLite table-rebuild pattern: create a new table with the desired schema, copy data, drop the old table, rename the new one.

**Tasks:**

1. **Edit `ExerciseEntity.kt`**
   - Remove `val defaultSets: Int`
   - Add the following fields:
     ```kotlin
     val isCustom: Boolean = true
     val force: String? = null
     val equipment: String? = null
     val primaryMuscles: String? = null
     val secondaryMuscles: String? = null
     val description: String? = null
     val videoUrls: String? = null
     val imageRefs: String? = null
     ```

2. **Edit `WorkoutDayExerciseEntity.kt`**
   - Add `val sets: Int = 3`

3. **Edit `Migrations.kt`** — add `MIGRATION_2_3`:
   - **Step 1** — Copy `defaultSets` into `workout_day_exercises` before the column is dropped (correlated subquery):
     ```sql
     ALTER TABLE workout_day_exercises ADD COLUMN sets INTEGER NOT NULL DEFAULT 3
     UPDATE workout_day_exercises
     SET sets = (
         SELECT defaultSets FROM exercises
         WHERE exercises.id = workout_day_exercises.exerciseId
     )
     ```
   - **Step 2** — Add 8 new nullable columns to `exercises` via `ALTER TABLE ... ADD COLUMN` (safe on all supported API levels):
     ```sql
     ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 1
     ALTER TABLE exercises ADD COLUMN force TEXT
     ALTER TABLE exercises ADD COLUMN equipment TEXT
     ALTER TABLE exercises ADD COLUMN primaryMuscles TEXT
     ALTER TABLE exercises ADD COLUMN secondaryMuscles TEXT
     ALTER TABLE exercises ADD COLUMN description TEXT
     ALTER TABLE exercises ADD COLUMN videoUrls TEXT
     ALTER TABLE exercises ADD COLUMN imageRefs TEXT
     ```
   - **Step 3** — Rebuild `exercises` table to remove `defaultSets`:
     ```sql
     CREATE TABLE exercises_new (
         id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
         name TEXT NOT NULL,
         isCustom INTEGER NOT NULL DEFAULT 1,
         force TEXT,
         equipment TEXT,
         primaryMuscles TEXT,
         secondaryMuscles TEXT,
         description TEXT,
         videoUrls TEXT,
         imageRefs TEXT
     )
     INSERT INTO exercises_new
         SELECT id, name, isCustom, force, equipment,
                primaryMuscles, secondaryMuscles, description, videoUrls, imageRefs
         FROM exercises
     DROP TABLE exercises
     ALTER TABLE exercises_new RENAME TO exercises
     ```
   - Add `MIGRATION_2_3` to `ALL_MIGRATIONS`.

4. **Edit `AppDatabase.kt`** — bump `version = 2` to `version = 3`.

5. **Write a Room `MigrationTestHelper` instrumented test** that:
   - Opens a v2 database with a known exercise row (e.g., `defaultSets = 4`) assigned to a workout day.
   - Runs `MIGRATION_2_3`.
   - Verifies the exercise row is present with `isCustom = 1` and that no `defaultSets` column exists.
   - Verifies the `workout_day_exercises` row has `sets = 4`.

**Risk:** The SQL steps in `migrate()` are sequential, so the `sets` copy must come before the table rebuild. This ordering is enforced by the test.

---

### Milestone 2 — CSV Asset Parsing and Library Seeding

**Goal:** On first launch, all ~1,145 library exercises from `exercises.csv` are parsed and inserted into the `exercises` table. Subsequent launches do nothing.

**Tasks:**

1. **Copy `exercises.csv`** from the repo root to `app/src/main/assets/exercises.csv`.

2. **Create `util/CsvParser.kt`**:
   - `fun parseExercises(inputStream: InputStream): List<ExerciseEntity>`
   - The CSV header row: `exercise, url, primary_muscle, secondary_muscles, equipment, type, force, video, instructions`
   - The `type` column (index 5) contains a Schema.org `ExerciseAction` JSON blob. Extract from it:
     - `description` → `ExerciseEntity.description`
     - `video[].contentUrl` → serialized as a JSON array string in `ExerciseEntity.videoUrls`
     - `image[]` → serialized as a JSON array string in `ExerciseEntity.imageRefs`
     - `exerciseRelatedEquipment.name` → `ExerciseEntity.equipment`
     - `muscleAction[].name` → `ExerciseEntity.primaryMuscles` (comma-separated)
   - Top-level CSV columns provide `exercise` (index 0) as the name and `force` (index 6).
   - Set `isCustom = false` for all parsed rows.
   - **Use Apache Commons CSV** for RFC 4180 parsing (handles quoted fields with embedded commas and escaped double-quotes). Add `implementation("org.apache.commons:commons-csv:1.10.0")` to `app/build.gradle.kts`.
   - Use `org.json.JSONObject` / `JSONArray` (bundled in Android SDK) to parse the embedded JSON — no additional dependency needed.

3. **Create `util/LibrarySeeder.kt`**:
   - `class LibrarySeeder(private val context: Context, private val exerciseDao: ExerciseDao)`
   - `suspend fun seedIfNeeded()`: reads a `SharedPreferences` flag `"library_seeded_v1"`. If false: opens `assets/exercises.csv`, calls `CsvParser.parseExercises`, calls `exerciseDao.insertAllLibrary(exercises)`. Sets flag to true.
   - The versioned key suffix (`_v1`) allows future CSV updates to force a re-seed by changing the key.

4. **Edit `ExerciseDao.kt`**:
   - Add `@Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAllLibrary(exercises: List<ExerciseEntity>)` — `IGNORE` makes the seeder idempotent.
   - Add `@Query("SELECT * FROM exercises WHERE isCustom = 0 ORDER BY name ASC") fun getAllLibrary(): Flow<List<ExerciseEntity>>`
   - Add `@Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC") fun getAllCustom(): Flow<List<ExerciseEntity>>`
   - Add `@Query("SELECT * FROM exercises WHERE id = :id") suspend fun getByIdOnce(id: Long): ExerciseEntity?`

5. **Edit `WorkoutAssistantApp.kt`**:
   - Add `private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`
   - Add `val librarySeeder by lazy { LibrarySeeder(this, database.exerciseDao()) }`
   - In `onCreate()`, launch `applicationScope.launch { librarySeeder.seedIfNeeded() }`. This runs asynchronously and does not block the first frame.

**Risk:** The CSV rows contain large embedded JSON blobs (~1 KB each). Parsing 1,145 rows on the main thread would cause an ANR. The seeder must run on `Dispatchers.IO` via `applicationScope`.

**Risk:** The embedded JSON uses double-double-quote escaping inside CSV quoted fields. Do not use a naive `split(",")` approach — use Apache Commons CSV.

---

### Milestone 3 — Data Layer Updates

**Goal:** All repositories and domain models reflect the new schema. No UI changes yet — this milestone makes the data layer compile and all existing tests pass.

**Tasks:**

1. **Edit `Exercise.kt`** (domain model):
   - Remove `val defaultSets: Int`
   - Add: `val isCustom: Boolean`, `val force: String?`, `val equipment: String?`, `val primaryMuscles: String?`, `val secondaryMuscles: String?`, `val description: String?`, `val videoUrls: String?`, `val imageRefs: String?`

2. **Edit `WorkoutDay.kt`** (`WorkoutDayExerciseItem`):
   - Add `val sets: Int`

3. **Edit `ExerciseRepository.kt`**:
   - Rename `getAll()` to `getAllCustom()` — backed by `exerciseDao.getAllCustom()`
   - Add `getAllLibrary(): Flow<List<Exercise>>` — backed by `exerciseDao.getAllLibrary()`
   - Add or update `getById(id: Long): Exercise?` — backed by `exerciseDao.getByIdOnce(id)`
   - Update `toDomain()` and `toEntity()` mapping functions to include all new fields; remove `defaultSets`

4. **Edit `WorkoutDayRepository.kt`**:
   - `addExerciseToDay(workoutDayId: Long, exerciseId: Long, sets: Int)` — add `sets` parameter; pass to entity constructor
   - In the `combine` lambda that builds `WorkoutDayExerciseItem`, populate `sets` from `assignment.sets`
   - Update all `WorkoutDayExerciseEntity(...)` constructor calls to include `sets`

5. **Edit `SessionRepository.startSession()`**:
   - Replace `targetSets = exercise.defaultSets` with `targetSets = assignment.sets`

6. **Fix all remaining compile errors** from `defaultSets` removal across `ExerciseFormViewModel.kt`, `ExerciseListViewModel.kt`, and any other files that construct `Exercise(...)` or read `.defaultSets`.

---

### Milestone 4 — Exercise Library UI — Browse and Search

**Goal:** The Exercise Library screen is accessible from the bottom nav and the Workout Day "Add Exercise" flow. Exercises are grouped by primary muscle with a search field.

**Tasks:**

1. **Create `ui/library/ExerciseLibraryViewModel.kt`**:
   - Receives `ExerciseRepository` and optional `workoutDayId: Long?` (non-null when accessed from "Add Exercise" flow)
   - Exposes `uiState: StateFlow<ExerciseLibraryUiState>` containing:
     - `groupedExercises: Map<String, List<Exercise>>` — keyed by muscle name, filtered by `searchQuery`, sorted alphabetically
     - `searchQuery: String`
     - `workoutDayId: Long?`
   - `fun onSearchChange(query: String)`
   - Grouping logic: parse `exercise.primaryMuscles` (comma-separated) into a list; an exercise with multiple primary muscles appears in multiple groups.

2. **Create `ui/library/ExerciseLibraryScreen.kt`**:
   - `Scaffold` with top bar ("Exercise Library") and a persistent search `TextField` below the top bar
   - Body: `LazyColumn` with grouped sections. Each group:
     - A tappable header row (muscle name) that toggles expanded/collapsed state (use `remember { mutableStateOf(setOf<String>()) }` for expanded group keys; all groups start expanded)
     - When expanded: exercise rows showing name, equipment, and force
     - Tapping a row navigates to `ExerciseDetail`
   - Empty state: "No exercises match your search."
   - **Do not use nested `LazyColumn`** — use a flat `LazyColumn` with conditional `item { Header }` and `items(list) { Row }` blocks per group

3. **Create `ExerciseLibraryViewModelFactory`** following the existing factory pattern.

---

### Milestone 5 — Exercise Detail Screen

**Goal:** Tapping an exercise in the library opens a detail screen with all metadata, images, and an "Add to Workout Day" action.

**Tasks:**

1. **Create `ui/library/ExerciseDetailViewModel.kt`**:
   - Receives `ExerciseRepository`, `WorkoutDayRepository`, `exerciseId: Long`, and `workoutDayId: Long?`
   - Loads exercise by ID; parses `videoUrls` and `imageRefs` JSON strings into `List<String>`
   - `fun addToWorkoutDay(workoutDayId: Long, sets: Int)` — calls `workoutDayRepository.addExerciseToDay(...)`; emits `navigateBack: SharedFlow<Unit>` on success
   - Exposes all workout days (for the workout day picker when `workoutDayId == null`)

2. **Create `ui/library/ExerciseDetailScreen.kt`**:
   - `Scaffold` with top bar (exercise name, back button)
   - Scrollable `Column`:
     - **Metadata section**: Force, Equipment, Primary Muscles, Secondary Muscles (null fields omitted)
     - **Instructions section**: `Text` with `description` (omitted if null)
     - **Images section**: horizontal `LazyRow` of thumbnails loaded from `context.assets.open(path)` wrapped in a `produceState` composable; `IOException` is caught silently (images may not be bundled)
     - **Videos section**: list of tappable video link rows (see Milestone 6)
     - **"Add to Workout Day" button** at the bottom:
       - If `workoutDayId != null`: show a sets-count dialog directly
       - If `workoutDayId == null`: show a workout day picker bottom sheet first, then a sets-count dialog

3. **Create `ExerciseDetailViewModelFactory`**.

**Risk:** Image asset files referenced in `imageRefs` (e.g., `"Bench_Press/0.jpg"`) are not present in the repository. `IOException` must be caught and the images section must be silently omitted when assets are absent.

---

### Milestone 6 — Video Playback

**Goal:** Tapping a video row on the Exercise Detail screen plays it via the YouTube app or browser.

**Tasks:**

1. In `ExerciseDetailScreen.kt`, each video row's `onClick`:
   ```kotlin
   val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
   context.startActivity(intent)
   ```
   This opens the YouTube app if installed, or a browser fallback — no additional dependency required.

2. Both `watch?v=` and `shorts/` YouTube URL formats are handled natively by this intent; no special-casing needed.

3. No `WebView` or `INTERNET` permission is required — the YouTube app handles its own network access.

---

### Milestone 7 — Workout Day Detail: Sets Per Assignment

**Goal:** The Workout Day Detail screen displays `sets` from the `WorkoutDayExercise` assignment. Adding and editing exercises includes a set count entry.

**Tasks:**

1. **Edit `WorkoutDayDetailViewModel.kt`**:
   - `addExercise(exerciseId: Long, sets: Int)` — pass `sets` to `workoutDayRepository.addExerciseToDay`
   - Add `fun updateSets(assignmentId: Long, sets: Int)` — calls new DAO method

2. **Edit `WorkoutDayExerciseDao.kt`**:
   - Add `@Query("UPDATE workout_day_exercises SET sets = :sets WHERE id = :id") suspend fun updateSets(id: Long, sets: Int)`

3. **Edit `WorkoutDayDetailScreen.kt`**:
   - Replace `Text("${item.exercise.defaultSets} sets")` with `Text("${item.sets} sets")` in list item `supportingContent`
   - FAB "Add Exercise" taps navigate to `ExerciseLibraryScreen` with `workoutDayId` as a route argument (Option A — reuses the library browse + detail flow). Alternatively, also show a custom exercise picker to allow adding custom exercises that don't appear in the library.
   - Add a long-press or edit icon on each list item showing an "Edit Sets" `AlertDialog` with a numeric `TextField` pre-populated with the current sets value; on confirm calls `viewModel.updateSets(assignmentId, newSets)`.

**Recommended "Add Exercise" flow (Option A):** Navigate to `ExerciseLibraryScreen(workoutDayId)` → user browses and taps an exercise → `ExerciseDetailScreen(exerciseId, workoutDayId)` → user taps "Add to Workout Day" → sets dialog → confirms → pops back to Workout Day Detail. For custom exercises, provide a separate "Add Custom Exercise" button in the Workout Day Detail that shows a filtered picker of custom exercises plus a sets dialog.

---

### Milestone 8 — Custom Exercise Management Cleanup

**Goal:** The custom exercise list and form are updated to remove all `defaultSets` references and show only custom exercises.

**Tasks:**

1. **Edit `ExerciseListViewModel.kt`**:
   - Change `repository.getAll()` to `repository.getAllCustom()`
   - Remove any reference to `exercise.defaultSets`

2. **Edit `ExerciseListScreen.kt`**:
   - Change screen title to "My Exercises"
   - Remove `Text("${exercise.defaultSets} sets")` from list item `supportingContent`

3. **Edit `ExerciseFormScreen.kt`**:
   - Remove the "Default Sets" `OutlinedTextField` entirely
   - Change the name field's `ImeAction` from `Next` to `Done`

4. **Edit `ExerciseFormViewModel.kt`**:
   - Remove `defaultSets: String` from `ExerciseFormState`
   - Remove `onSetsChange(value: String)`
   - Remove sets validation from `save()`
   - Set `isCustom = true` explicitly in the `Exercise(...)` constructor call; all library fields remain `null`

5. **Update the bottom nav label** for `Screen.ExerciseList` from "Exercises" to "My Exercises" in `NavGraph.kt`.

---

### Milestone 9 — Session Start: Sets Source Fix

**Goal:** When a session starts, `targetSets` per `SessionExercise` is populated from `WorkoutDayExercise.sets`.

**Tasks:**

1. **Edit `SessionRepository.startSession()`**:
   - Replace `targetSets = exercise.defaultSets` with `targetSets = assignment.sets`
   - This is a one-line change but is the most behaviorally critical change in the entire enhancement.

2. **Manual verification:** Start a session from a workout day where exercises have different set counts (e.g., 3 and 5). Confirm the session screen shows the correct `targetSets` per exercise.

---

### Milestone 10 — Navigation and Screen Registration

**Goal:** The two new screens are wired into the navigation graph and reachable from both the bottom nav and the Workout Day flow.

**Tasks:**

1. **Edit `Screen.kt`**:
   ```kotlin
   object ExerciseLibrary : Screen("exercise_library?workoutDayId={workoutDayId}") {
       fun createRoute(workoutDayId: Long = -1L) = "exercise_library?workoutDayId=$workoutDayId"
   }
   object ExerciseDetail : Screen("exercise_detail/{exerciseId}?workoutDayId={workoutDayId}") {
       fun createRoute(exerciseId: Long, workoutDayId: Long = -1L) =
           "exercise_detail/$exerciseId?workoutDayId=$workoutDayId"
   }
   ```

2. **Edit `NavGraph.kt`**:
   - Add `composable` for `Screen.ExerciseLibrary.route` with `navArgument("workoutDayId") { type = NavType.LongType; defaultValue = -1L }`.
   - Add `composable` for `Screen.ExerciseDetail.route` with arguments for both `exerciseId` and `workoutDayId`.
   - Pass `workoutDayId.takeIf { it != -1L }` as the optional parameter to both screens.
   - In `WorkoutDayDetailScreen`'s composable entry, add `onAddFromLibrary = { navController.navigate(Screen.ExerciseLibrary.createRoute(workoutDayId)) }`.

3. **Update bottom nav items**:
   - The current 5-item bottom nav (Home, Exercises, Days, History, Settings) gains a Library entry, which would make 6. To stay within the Material 3 recommended maximum of 5:
   - **Recommended approach:** Replace the "Exercises" bottom nav item with "Library" (`Screen.ExerciseLibrary`). Access to "My Exercises" (custom exercises) is available via a top-bar action icon or a tab within the Library screen. This keeps 5 bottom nav items and prioritizes the primary use case (browsing the library).

---

### Milestone 11 — Integration Testing and Regression

**Goal:** All existing features continue to work correctly. New features have been end-to-end tested.

**Tasks:**

1. **Migration instrumented test**:
   - Create exercise with `defaultSets = 5`, assign to a workout day, migrate v2→v3.
   - Verify `workout_day_exercises.sets = 5`; verify `exercises` table has no `defaultSets` column.

2. **Library seeder test**:
   - Clear app data. Verify exercises table contains library rows (`isCustom = 0`) after first launch.
   - Verify second launch does not create duplicates.

3. **Session sets regression**:
   - Create workout day with two exercises: 3 sets and 5 sets.
   - Start session. Verify exercise A shows "0 / 3 sets" and exercise B shows "0 / 5 sets".

4. **Custom exercise regression**:
   - Create a custom exercise. Confirm it appears in "My Exercises" but not in the Exercise Library.
   - Add it to a workout day with 4 sets. Confirm the Workout Day Detail shows "4 sets".
   - Start a session. Confirm "0 / 4 sets" appears in the session.

5. **Library browse regression**:
   - Navigate to Exercise Library. Confirm exercises are grouped by primary muscle.
   - Search "Bench Press". Confirm filtered results.
   - Tap an exercise. Confirm detail screen shows name, force, equipment, muscles, and instructions.
   - Tap a video link. Confirm YouTube app or browser opens.

6. **Existing feature regression**:
   - Workout session: mark sets, pause/resume, end — confirm timer and duration behavior is unchanged.
   - Session history: confirm CSV export contains correct `targetSets` values.
   - Settings: confirm all settings still apply correctly.

---

## 5. Architectural Decisions and Risks

### Architectural Decisions

| Decision | Rationale |
|---|---|
| `isCustom` boolean on `ExerciseEntity` rather than a separate library table | Avoids joins on every query; the distinction is a simple filter predicate; both types share all nullable library fields |
| Library exercises seeded from assets CSV at first launch | Assets are smaller than a pre-populated SQLite file; the seeder pattern is idiomatic for Room; a versioned flag guards repeat runs |
| `IGNORE` conflict strategy on library insert | Makes the seeder idempotent; safe to call more than once |
| Video playback via implicit `ACTION_VIEW` intent | No YouTube API dependency; works on all API levels; respects user's preferred YouTube app |
| Image assets loaded via `context.assets.open()` with silent `IOException` fallback | Consistent with the offline-first NFR; gracefully handles missing image files |
| `workoutDayId` passed as optional route argument to ExerciseLibrary and ExerciseDetail | Single set of screens serves both standalone browse mode and the "add to day" flow |
| Sets dialog shown at add-time, not stored on the exercise | Matches FSD 5.1.4 and 5.3.5; sets are a property of the workout day assignment, not the exercise |
| Table-rebuild pattern for removing `defaultSets` | Required for `minSdk = 26` compatibility; `ALTER TABLE ... DROP COLUMN` only works on API 35+ |
| "Add Exercise" from Workout Day Detail navigates to ExerciseLibraryScreen with workoutDayId (Option A) | Reuses the library browsing UI; avoids a duplicate heavy list in a bottom sheet |

### Risks

| Risk | Severity | Mitigation |
|---|---|---|
| CSV parsing complexity — embedded JSON with escaped quotes inside CSV quoted fields | High | Use Apache Commons CSV (`commons-csv:1.10.0`); add a unit test covering edge-case rows |
| Image files referenced in `imageRefs` are not bundled in the repository | Medium | Catch `IOException` in the image-loading composable and render nothing; no crash |
| Library seeding performance — ~1,145 rows with large JSON blobs | Medium | Run on `Dispatchers.IO` in `applicationScope`; library screen shows empty state until the `Flow` emits |
| Migration data integrity — `defaultSets` must be copied before the column is dropped | High | SQL step order in `MIGRATION_2_3.migrate()` is sequential and verified by the `MigrationTestHelper` instrumented test |
| Bottom nav item count — adding Library brings total to 6 | Low | Replace "Exercises" nav item with "Library"; move custom exercise access to a top-bar icon or tab within the Library screen |
| `ExerciseRepository.getAll()` removal breaks compilation at all call sites | High | Rename to `getAllCustom()` / `getAllLibrary()` in Milestone 3 before any UI work; compiler enforces all call site fixes |
| `SessionRepository.startSession()` reads `exercise.defaultSets` until Milestone 9 | High | Removing `defaultSets` from the domain model in Milestone 3 causes a compile error that forces the Milestone 9 fix before the app can build |

### Critical Files for Implementation

- `app/src/main/java/com/mjaydedecker/workoutassistant/data/db/migrations/Migrations.kt`
- `app/src/main/java/com/mjaydedecker/workoutassistant/data/db/entity/ExerciseEntity.kt`
- `app/src/main/java/com/mjaydedecker/workoutassistant/data/db/entity/WorkoutDayExerciseEntity.kt`
- `app/src/main/java/com/mjaydedecker/workoutassistant/data/repository/SessionRepository.kt`
- `app/src/main/java/com/mjaydedecker/workoutassistant/data/repository/WorkoutDayRepository.kt`
- `app/src/main/java/com/mjaydedecker/workoutassistant/ui/workoutday/WorkoutDayDetailScreen.kt`
- `app/src/main/java/com/mjaydedecker/workoutassistant/util/CsvParser.kt` (new)
- `app/src/main/java/com/mjaydedecker/workoutassistant/util/LibrarySeeder.kt` (new)
