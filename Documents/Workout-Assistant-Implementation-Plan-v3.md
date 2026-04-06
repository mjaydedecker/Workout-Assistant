# Implementation Plan v3
# Workout-Assistant Android Application

**Version:** 3.0
**Date:** 2026-04-05
**Based on:** Workout-Assistant-FSD.md v1.5
**Supersedes:** Workout-Assistant-Implementation-Plan-v2.md

---

## Overview

The v2 implementation plan has been fully built. This plan covers only the six defects and new requirements surfaced by Manual Test Finding #1 and Manual Test Finding #2, now codified in FSD v1.5. No database schema changes are required for any of these items.

---

## What Is Being Fixed or Added

| # | Source | Type | Description |
|---|---|---|---|
| 1 | Finding #1 / FSD 5.8.2, 5.9.2 | Bug | Timer sounds do not play despite OS media volume being above zero |
| 2 | Finding #1 / FSD 5.3.3 | Bug | Workout Day Detail rename is broken — the edit field starts empty due to a loading-state race condition |
| 3 | Finding #2 / FSD 5.1.1 | New req | Exercise Library groups must start collapsed, not expanded |
| 4 | Finding #2 / FSD 5.1.1, 5.2, 6, 7 | New req | "My Exercises" must be a group inside the Exercise Library; the bottom nav item must be removed |
| 5 | Finding #2 / FSD 5.1.1 | New req | Home dashboard exercise count must show the total library count (prepopulated + custom) |
| 6 | Finding #2 / FSD 5.1.1 | New req | Scroll position and expand/collapse state must be preserved when navigating back from Exercise Detail to the Library |

All six items are independent and can be worked in any order. The milestones below are ordered by risk level — bugs first, then UI/navigation changes.

---

## Milestone 1 — Fix Timer Sound (Bug)

**Goal:** Timer alerts play audibly through `STREAM_MUSIC` whenever media volume is above zero, regardless of ringer mode.

**Root Cause:**

The static `MediaPlayer.create(context, resId, audioAttributes, sessionId)` factory method does not reliably apply custom `AudioAttributes` on all API levels (26–35). On some versions the system ignores the supplied attributes and routes through the ring or notification stream instead, which is silenced by the ringer mode switch — explaining why vibration works but sound does not.

**Fix:**

In `AudioVibrationManager.kt`, replace the static `MediaPlayer.create()` call with explicit instance construction:

```kotlin
fun playTimerAlert() {
    try {
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        val afd = context.resources.openRawResourceFd(R.raw.timer_alert) ?: return
        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
        mp.setOnCompletionListener { it.release() }
        mp.prepare()
        mp.start()
    } catch (_: Exception) {
    }
}
```

Remove the existing `mediaAudioAttributes` field and `AudioManager` session ID usage as they are no longer needed.

**Files changed:**
- `util/AudioVibrationManager.kt`

**Verification:** On a physical device with ringer set to silent and media volume at ~50%, trigger rest timer expiry. Sound must play. Repeat for inactivity timer. Confirm vibration still works.

**Risk:** Low. The change is self-contained; the exception guard prevents crashes.

---

## Milestone 2 — Fix Workout Day Detail Rename (Bug)

**Goal:** The rename action in `WorkoutDayDetailScreen` reliably pre-populates the edit field with the current name regardless of when the user opens it relative to the ViewModel's initial data load.

**Root Cause:**

```kotlin
var titleInput by remember(uiState.workoutDay?.name) {
    mutableStateOf(uiState.workoutDay?.name ?: "")
}
```

`uiState.workoutDay` is `null` for the brief window between first composition and the first Room Flow emission. If the user taps the edit icon before the name loads, `titleInput` is `""`, the checkmark is disabled (`titleInput.isNotBlank()` is false), and the field appears blank. The race condition is the root cause of the reported defect.

**Fix:**

In `WorkoutDayDetailScreen.kt`:

1. Decouple `titleInput` from the ViewModel state — initialise it once without a key:
   ```kotlin
   var titleInput by remember { mutableStateOf("") }
   ```

2. Prime `titleInput` with the current name whenever the user opens edit mode:
   ```kotlin
   LaunchedEffect(isEditingTitle) {
       if (isEditingTitle) {
           titleInput = uiState.workoutDay?.name ?: ""
       }
   }
   ```

`LaunchedEffect(isEditingTitle)` fires only when `isEditingTitle` changes, not on every recomposition. By the time the user taps the edit icon, the ViewModel has loaded and `uiState.workoutDay?.name` is populated.

**Files changed:**
- `ui/workoutday/WorkoutDayDetailScreen.kt`

**Verification:** Open a workout day. Immediately tap the edit icon (before name renders). Confirm the field is pre-populated. Also test after the name loads — change it, confirm the top bar and list update correctly.

**Risk:** Low. Behaviour during normal non-editing use is unchanged.

---

## Milestone 3 — Exercise Library: Groups Start Collapsed (New Requirement)

**Goal:** All group headers in the Exercise Library are collapsed by default. The user taps a header to expand it. When a search is active, all matching groups are shown expanded automatically.

**Fix:**

In `ExerciseLibraryScreen.kt`, flip the state variable from tracking "collapsed groups" to "expanded groups":

```kotlin
// Before
var collapsedGroups by remember { mutableStateOf(emptySet<String>()) }

// After
var expandedGroups by remember { mutableStateOf(emptySet<String>()) }
```

Update all references accordingly:
- `val isCollapsed = muscle in collapsedGroups` → `val isExpanded = muscle in expandedGroups || uiState.searchQuery.isNotBlank()`
- Toggle: `collapsedGroups = if (isCollapsed) collapsedGroups - muscle else collapsedGroups + muscle` → `expandedGroups = if (isExpanded && uiState.searchQuery.isBlank()) expandedGroups - muscle else expandedGroups + muscle`
- Icon and content description: update to reflect `isExpanded`

Note: In Milestone 6 this local `remember` state will be lifted into the ViewModel for back-navigation preservation. It is acceptable to implement Milestone 3 first with `remember` and then lift it in Milestone 6.

**Files changed:**
- `ui/library/ExerciseLibraryScreen.kt`

**Verification:** Open Library — all groups collapsed. Tap a header — expands. Tap again — collapses. Type in search — matching groups expand. Clear search — groups return to their stored collapsed/expanded state.

**Risk:** Low. Self-contained UI state change.

---

## Milestone 4 — Merge "My Exercises" into Exercise Library; Remove Nav Item (New Requirement)

**Goal:** Custom exercises appear as a "My Exercises" group inside the Exercise Library. The "My Exercises" bottom nav item is removed. The custom exercise management screen (create/edit/delete) is still accessible via an edit icon in the "My Exercises" group header.

This milestone has three sub-tasks.

### 4a — ExerciseLibraryViewModel: Include Custom Exercises

Update the `combine` in `ExerciseLibraryViewModel` to combine three flows:

```kotlin
combine(
    exerciseRepository.getAllLibrary(),
    exerciseRepository.getAllCustom(),
    _searchQuery
) { libraryExercises, customExercises, query ->
    // ... existing filter and grouping logic for library exercises ...

    // Append "My Exercises" group at the end
    val filteredCustom = if (query.isBlank()) customExercises
        else customExercises.filter { it.name.contains(query, ignoreCase = true) }

    val finalGrouped = if (filteredCustom.isNotEmpty())
        sortedGrouped + (MY_EXERCISES_GROUP to filteredCustom.sortedBy { it.name })
    else
        sortedGrouped

    ExerciseLibraryUiState(groupedExercises = finalGrouped, searchQuery = query, isLoading = false)
}
```

Add a shared constant to avoid hardcoding the group name string in multiple places:
```kotlin
const val MY_EXERCISES_GROUP = "My Exercises"
```

**Files changed:**
- `ui/library/ExerciseLibraryViewModel.kt`

### 4b — ExerciseLibraryScreen: Add Edit Icon to "My Exercises" Header

Add an `onManageCustomExercises: () -> Unit = {}` parameter to `ExerciseLibraryScreen`.

In the `LazyColumn` group header `item` block, render an additional icon button when the group is "My Exercises":

```kotlin
if (muscle == MY_EXERCISES_GROUP) {
    IconButton(onClick = onManageCustomExercises) {
        Icon(Icons.Default.Edit, contentDescription = "Manage My Exercises")
    }
}
```

**Files changed:**
- `ui/library/ExerciseLibraryScreen.kt`

### 4c — NavGraph and Screen: Remove Bottom Nav Item; Wire Callback

In `NavGraph.kt`:
1. Remove `Triple(Screen.ExerciseList, Icons.Default.Person, "My Exercises")` from `bottomNavItems`. The list returns to 5 items.
2. In the `composable(Screen.ExerciseLibrary.route)` block, pass the new callback:
   ```kotlin
   onManageCustomExercises = { navController.navigate(Screen.ExerciseList.route) }
   ```

In `Screen.kt`:
- Remove `Screen.ExerciseList` from the `bottomNavScreens` list.
- Keep `object ExerciseList` in the sealed class — the route must remain registered in `NavHost`.

**Files changed:**
- `ui/navigation/NavGraph.kt`
- `ui/navigation/Screen.kt`

**Verification:**
- Confirm 5 bottom nav items (no "My Exercises" tab).
- Open Library — confirm "My Exercises" group appears.
- Tap the edit icon in the "My Exercises" header — confirm navigation to the custom exercise list screen.
- Create a custom exercise — confirm it appears in the "My Exercises" group on return.
- Search for the custom exercise by name — confirm it appears filtered under "My Exercises".

**Risk:** Medium. Three files change and interact. Use `MY_EXERCISES_GROUP` constant to prevent a silent string mismatch.

---

## Milestone 5 — Home Dashboard: Show Total Exercise Count (New Requirement)

**Goal:** The "Exercises" summary card on the Home screen shows the total count of all exercises (prepopulated library + custom), not just custom exercises.

**Root Cause:**

```kotlin
private val customExerciseCountFlow = exerciseRepository.getAllCustom().map { it.size }
```

This counts only custom exercises. The dashboard was showing 16 because only the user's custom exercises were counted — not the ~1,145 seeded library exercises.

**Fix:**

In `HomeViewModel.kt`, replace `customExerciseCountFlow` with a combined count:

```kotlin
private val totalExerciseCountFlow = combine(
    exerciseRepository.getAllCustom(),
    exerciseRepository.getAllLibrary()
) { custom, library -> custom.size + library.size }
```

Then replace `customExerciseCountFlow` with `totalExerciseCountFlow` in the `combine` that builds `uiState`.

**Files changed:**
- `ui/home/HomeViewModel.kt`

**Verification:** After seeding, the Exercises count on the dashboard should equal the total library count (~1,145 + any custom exercises). Create a custom exercise — count increments by 1.

**Risk:** Very low. Single ViewModel change.

---

## Milestone 6 — Preserve Scroll Position and Expand/Collapse State on Back Navigation (New Requirement)

**Goal:** When the user navigates from the Library to Exercise Detail and presses Back, the Library returns to the same scroll position and the same expand/collapse state that was in place when they left.

**Root Cause:**

Both the `expandedGroups` set (from Milestone 3) and the `LazyListState` are stored in local Compose `remember` state inside `ExerciseLibraryScreen`. When Compose Navigation re-enters the Library destination after a back press, the composable is recomposed from scratch and all `remember` state is lost.

**Fix:** Lift both pieces of state into `ExerciseLibraryViewModel`, which is scoped to the back-stack entry and survives forward navigation.

### 6a — ExerciseLibraryViewModel: Add Scroll and Expand State

```kotlin
// Expanded groups — replaces the local remember in the screen
val expandedGroups = MutableStateFlow<Set<String>>(emptySet())

fun toggleGroup(muscle: String) {
    expandedGroups.update { if (muscle in it) it - muscle else it + muscle }
}

// Scroll position
private val _scrollIndex = MutableStateFlow(0)
private val _scrollOffset = MutableStateFlow(0)
val scrollIndex: StateFlow<Int> = _scrollIndex
val scrollOffset: StateFlow<Int> = _scrollOffset

fun saveScrollPosition(index: Int, offset: Int) {
    _scrollIndex.value = index
    _scrollOffset.value = offset
}
```

**Files changed:**
- `ui/library/ExerciseLibraryViewModel.kt`

### 6b — ExerciseLibraryScreen: Read from ViewModel, Persist on Scroll

1. Remove the local `remember { mutableStateOf(emptySet<String>()) }` for expanded groups. Instead:
   ```kotlin
   val expandedGroups by viewModel.expandedGroups.collectAsState()
   // toggle via: viewModel.toggleGroup(muscle)
   ```

2. Create the `LazyListState` with the saved position from the ViewModel:
   ```kotlin
   val lazyListState = rememberLazyListState(
       initialFirstVisibleItemIndex = viewModel.scrollIndex.value,
       initialFirstVisibleItemScrollOffset = viewModel.scrollOffset.value
   )
   ```
   Note: read `.value` directly (not `collectAsState()`) to initialise the list state before the first composition.

3. Persist the scroll position continuously via `LaunchedEffect`:
   ```kotlin
   LaunchedEffect(lazyListState) {
       snapshotFlow {
           lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
       }.collect { (index, offset) ->
           viewModel.saveScrollPosition(index, offset)
       }
   }
   ```

This replaces the local `remember` for `expandedGroups` that was added in Milestone 3. If Milestone 3 is implemented first with local `remember`, Milestone 6 replaces it with the ViewModel-backed version.

**Files changed:**
- `ui/library/ExerciseLibraryScreen.kt`

**Verification:**
- Open Library. Expand "Chest" and "Back". Scroll down 6 rows.
- Tap an exercise → Exercise Detail opens.
- Press Back → Library returns to the same scroll position with "Chest" and "Back" still expanded; all other groups still collapsed.
- Navigate away via bottom nav and return → state resets (expected — the ViewModel is cleared when its back-stack entry is fully popped).

**Risk:** Medium. The `rememberLazyListState` initialisation with saved values only works correctly if the values are read from `StateFlow.value` (not `collectAsState()`) on the first call. If the ViewModel hasn't loaded data yet on re-entry, the offset refers to a valid position because the ViewModel is not destroyed on forward navigation — the list data remains loaded.

---

## Summary of All File Changes

| File | Milestones | Type of Change |
|---|---|---|
| `util/AudioVibrationManager.kt` | M1 | Rewrite `playTimerAlert()` to use instance-based `MediaPlayer` construction |
| `ui/workoutday/WorkoutDayDetailScreen.kt` | M2 | Fix `titleInput` initialisation; add `LaunchedEffect(isEditingTitle)` |
| `ui/library/ExerciseLibraryScreen.kt` | M3, M4b, M6b | Flip to expanded-groups logic; add "My Exercises" header action; lift scroll+expand state to ViewModel |
| `ui/library/ExerciseLibraryViewModel.kt` | M4a, M6a | Combine custom exercises into grouped map with `MY_EXERCISES_GROUP`; add `expandedGroups`, `saveScrollPosition` |
| `ui/home/HomeViewModel.kt` | M5 | Replace `customExerciseCountFlow` with combined total count |
| `ui/navigation/NavGraph.kt` | M4c | Remove ExerciseList from bottom nav; wire `onManageCustomExercises` callback |
| `ui/navigation/Screen.kt` | M4c | Remove `Screen.ExerciseList` from `bottomNavScreens` |

**No database schema changes. No new files. No new dependencies.**

---

## Risks and Gotchas

| Risk | Severity | Mitigation |
|---|---|---|
| `MediaPlayer.prepare()` is synchronous — may block briefly on slow devices | Low | `timer_alert` is a short clip; in-memory fd prepare is near-instant. If needed, switch to `prepareAsync()` + `OnPreparedListener` |
| `LaunchedEffect(isEditingTitle)` fires once on initial composition with `false` — harmless but worth noting | Low | The `if (isEditingTitle)` guard inside the effect body handles this |
| "My Exercises" group key is a string — silent mismatch if changed in one place | Low | Extract to `const val MY_EXERCISES_GROUP` shared between ViewModel and Screen |
| `ExerciseLibraryViewModel` now combines three flows — increases `combine` arity | Low | Use the 3-argument `combine()` overload from `kotlinx.coroutines.flow` |
| Removing `Screen.ExerciseList` from `bottomNavScreens` — must verify no other usage | Low | Search codebase for `bottomNavScreens`; also verify bottom-nav selected-tab highlight logic still works correctly |
| ViewModel scroll state read via `.value` on `rememberLazyListState` initialisation | Medium | This is intentional and correct — reading the latest value before first composition. If ViewModel is somehow null (it won't be via `viewModel()`), the default of 0 is safe |
