# Implementation Plan v4
# Workout-Assistant Android Application

**Version:** 4.0
**Date:** 2026-04-06
**Based on:** Workout-Assistant-FSD.md v1.6
**Supersedes:** Workout-Assistant-Implementation-Plan-v3.md

---

## Overview

This plan addresses the three issues and one enhancement surfaced by Manual Test Finding #3, now codified in FSD v1.6. No database schema changes are required.

---

## What Is Being Fixed or Added

| # | Source | Type | Description |
|---|---|---|---|
| 1 | Finding #3 / FSD 5.8.2, 5.9.2 | Verification | Timer sound — verify Plan v3 M1 fix resolves the issue on device |
| 2 | Finding #3 / FSD 5.3.5 | Bug | Edit sets on workout day — the edit icon is unreachable due to missing `Row` wrapper in `trailingContent` |
| 3 | Finding #3 / FSD 5.4.9 | New req | View exercise detail from active session via an info icon on each exercise card |
| 4 | Finding #3 / FSD 5.6.5 | New req | Exercise weight history graph — accessible from Session Detail by tapping an exercise |

All items are independent and can be worked in any order. Milestones are ordered by risk: verification first, then the bug fix, then new features by complexity.

---

## Milestone 1 — Verify Timer Sound Fix (Verification)

**Goal:** Confirm that the Plan v3 M1 fix resolves the timer sound defect reported in Manual Test Finding #3.

**Background:**

Manual Test Finding #3 was recorded against a build that predated Plan v3. Plan v3 M1 replaced the static `MediaPlayer.create()` call in `AudioVibrationManager.playTimerAlert()` with an instance-based construction that explicitly sets `USAGE_MEDIA` audio attributes before `setDataSource()`. The call site in `ActiveSessionViewModel` was already correct — both timer expiry callbacks invoke `audioVibrationManager.playTimerAlert()` directly:

```kotlin
timerManager.onRestExpired = {
    if (settings.restTimerSound) audioVibrationManager.playTimerAlert()
    if (settings.restTimerVibrate) audioVibrationManager.vibrate()
}
```

**Verification steps:**
1. Install the APK built at the end of Plan v3.
2. In Settings, enable both Rest Timer Sound and Inactivity Timer Sound.
3. Set media volume to ~50%. Set ringer to silent.
4. Trigger rest timer expiry (mark a set and wait). Sound must play. Vibration must also play.
5. Repeat for inactivity timer.

**If sound still does not play after the Plan v3 APK is installed:**

The `MediaPlayer.prepare()` call may be throwing silently on the test device. Switch to the async preparation pattern:

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
        mp.setOnPreparedListener { it.start() }
        mp.prepareAsync()
    } catch (_: Exception) {
    }
}
```

**Files changed (only if the fallback fix is required):**
- `util/AudioVibrationManager.kt`

**Risk:** Low. The fix from Plan v3 is already in place; this milestone is verification only.

---

## Milestone 2 — Fix Edit Sets in Workout Day Detail (Bug)

**Goal:** Tapping the edit (pencil) icon on a workout day exercise opens the edit-sets dialog as intended.

**Root Cause:**

In `WorkoutDayDetailScreen.kt`, the `ReorderableItem`'s `ListItem` has two `IconButton` composables in its `trailingContent` lambda without a layout container:

```kotlin
trailingContent = {
    IconButton(onClick = { /* edit sets */ }) {
        Icon(Icons.Default.Edit, ...)
    }
    IconButton(onClick = { pendingRemove = item }) {
        Icon(Icons.Default.Delete, ...)
    }
}
```

Material 3's `ListItem` places `trailingContent` in a `Box`. Without an explicit `Row`, both buttons are drawn at the same coordinates. The Delete button (rendered last) sits on top of the Edit button, making the Edit icon unreachable to touch.

**Fix:**

Wrap both icon buttons in a `Row`:

```kotlin
trailingContent = {
    Row {
        IconButton(onClick = {
            editSetsItem = item
            editSetsInput = item.sets.toString()
            editSetsError = null
        }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit sets for ${item.exercise.name}")
        }
        IconButton(onClick = { pendingRemove = item }) {
            Icon(Icons.Default.Delete, contentDescription = "Remove ${item.exercise.name}")
        }
    }
}
```

Add `import androidx.compose.foundation.layout.Row` if not already present (it is already imported in this file).

**Files changed:**
- `ui/workoutday/WorkoutDayDetailScreen.kt`

**Verification:** Open a Workout Day Detail screen. Tap the pencil icon on any exercise — the edit-sets dialog must open, pre-populated with the current set count. Enter a new number and save — confirm the sets label updates. Tap the trash icon — confirm the remove dialog opens.

**Risk:** Low. Single layout change confined to one block.

---

## Milestone 3 — View Exercise Detail from Active Session (New Requirement)

**Goal:** During a workout session, an info icon appears on each exercise card. Tapping it opens the Exercise Detail screen for that exercise. Timers continue running. Back navigation returns to the session with all state intact.

This milestone has two sub-tasks.

### 3a — ExerciseCard: Add Info Icon

`ExerciseCard` in `ui/session/components/ExerciseCard.kt` renders the exercise name in a `Row` with an optional completed checkmark. Add an `onInfoClicked: (() -> Unit)? = null` parameter to `ExerciseCard`. When not null, render an `IconButton` with `Icons.Default.Info` before the completed icon:

```kotlin
@Composable
fun ExerciseCard(
    exercise: SessionExercise,
    weightUnit: WeightUnit = WeightUnit.KG,
    onMarkComplete: () -> Unit,
    onDecrement: () -> Unit,
    onWeightChanged: (Double) -> Unit,
    onRemove: () -> Unit,
    onInfoClicked: (() -> Unit)? = null,   // new parameter
    modifier: Modifier = Modifier
) {
```

In the top `Row` of the card (exercise name row), add the info button after the exercise name `Text`:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = exercise.exerciseName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f)
    )
    if (onInfoClicked != null) {
        IconButton(onClick = onInfoClicked) {
            Icon(Icons.Default.Info, contentDescription = "View info for ${exercise.exerciseName}")
        }
    }
    if (exercise.isCompleted) {
        Icon(Icons.Default.CheckCircle, contentDescription = "Exercise complete", tint = CompletedColor)
    }
}
```

Add `import androidx.compose.material.icons.filled.Info`.

`exercise.exerciseId` is already available on `SessionExercise` — the caller in `ActiveSessionScreen` passes it through the lambda.

**Files changed:**
- `ui/session/components/ExerciseCard.kt`

### 3b — ActiveSessionScreen and NavGraph: Wire Navigation

Add an `onExerciseInfoSelected: (exerciseId: Long) -> Unit = {}` parameter to `ActiveSessionScreen`. Pass the callback into each `ExerciseCard`:

```kotlin
ExerciseCard(
    exercise = exercise,
    weightUnit = uiState.settings.weightUnit,
    onMarkComplete = { viewModel.markSetComplete(exercise.id) },
    onDecrement = { viewModel.decrementSet(exercise.id) },
    onWeightChanged = { weight -> viewModel.updateWeight(exercise.id, weight) },
    onRemove = { viewModel.requestRemoveExercise(exercise.id) },
    onInfoClicked = { onExerciseInfoSelected(exercise.exerciseId) }   // new
)
```

In `NavGraph.kt`, in the `composable(Screen.ActiveSession.route)` block, pass the callback:

```kotlin
ActiveSessionScreen(
    app = app,
    workoutDayId = workoutDayId.takeIf { it != -1L },
    onSessionEnded = {
        navController.navigate(Screen.SessionHistory.route) {
            popUpTo(Screen.Home.route)
        }
    },
    onExerciseInfoSelected = { exerciseId ->
        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId, -1L))
    }
)
```

This reuses the existing `ExerciseDetail` route with `workoutDayId = -1` (no "add to workout day" context). The session ViewModel is scoped to the `Screen.ActiveSession.route` back-stack entry and is not destroyed when navigating forward to `ExerciseDetail` — it remains alive and all timer state is preserved.

**Files changed:**
- `ui/session/ActiveSessionScreen.kt`
- `ui/navigation/NavGraph.kt`

**Verification:** Start a workout session. Tap the info (ⓘ) icon on an exercise card. Confirm the Exercise Detail screen opens showing correct exercise information. Confirm the rest and inactivity timers continue running (check by timing the countdown before and after opening the detail). Press Back — confirm the session screen is restored with correct set counts, weights, and timer values.

**Risk:** Low–Medium. The `ActiveSessionViewModel` is scoped to the session back-stack entry and survives forward navigation. The `ExerciseDetail` route already exists and handles `workoutDayId = -1` gracefully.

---

## Milestone 4 — Exercise Weight History Graph (New Feature)

**Goal:** From the Session Detail screen, the user taps an exercise entry to open a screen displaying a line graph of that exercise's recorded weight over time.

This milestone has four sub-tasks.

### 4a — Data Layer: Weight History Query

Add a data class to hold a single history point. Place it alongside other model files:

```kotlin
// data/model/ExerciseWeightPoint.kt
package com.mjaydedecker.workoutassistant.data.model

import java.time.Instant

data class ExerciseWeightPoint(
    val exerciseName: String,
    val weightKg: Double,
    val startTime: Instant
)
```

Add the query to `SessionExerciseDao`. The existing `Instant` TypeConverter in `AppDatabase` handles the `startTime` column (stored as Long):

```kotlin
@Query("""
    SELECT se.exerciseName, se.weightKg, ws.startTime
    FROM session_exercises se
    JOIN workout_sessions ws ON se.sessionId = ws.id
    WHERE se.exerciseId = :exerciseId
      AND se.weightKg IS NOT NULL
      AND ws.endTime IS NOT NULL
    ORDER BY ws.startTime ASC
""")
fun getWeightHistory(exerciseId: Long): Flow<List<ExerciseWeightPoint>>
```

Expose via `SessionRepository`:

```kotlin
fun getWeightHistory(exerciseId: Long): Flow<List<ExerciseWeightPoint>> =
    sessionExerciseDao.getWeightHistory(exerciseId)
```

**Files changed:**
- `data/model/ExerciseWeightPoint.kt` (new)
- `data/db/dao/SessionExerciseDao.kt`
- `data/repository/SessionRepository.kt`

### 4b — ExerciseWeightHistoryViewModel

```kotlin
// ui/history/ExerciseWeightHistoryViewModel.kt

data class WeightPoint(val date: LocalDate, val weight: Double)

data class ExerciseWeightHistoryUiState(
    val exerciseName: String = "",
    val points: List<WeightPoint> = emptyList(),
    val weightLabel: String = "kg",
    val isLoading: Boolean = true
)

class ExerciseWeightHistoryViewModel(
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    exerciseId: Long
) : ViewModel() {

    val uiState: StateFlow<ExerciseWeightHistoryUiState> = combine(
        sessionRepository.getWeightHistory(exerciseId),
        settingsRepository.getSettings()
    ) { history, settings ->
        val unit = settings.weightUnit
        ExerciseWeightHistoryUiState(
            exerciseName = history.firstOrNull()?.exerciseName ?: "",
            points = history.map { point ->
                WeightPoint(
                    date = point.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                    weight = if (unit == WeightUnit.LB) point.weightKg * 2.20462 else point.weightKg
                )
            },
            weightLabel = WeightFormatter.label(unit),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseWeightHistoryUiState())
}

class ExerciseWeightHistoryViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val exerciseId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExerciseWeightHistoryViewModel(sessionRepository, settingsRepository, exerciseId) as T
    }
}
```

**Files changed:**
- `ui/history/ExerciseWeightHistoryViewModel.kt` (new)

### 4c — ExerciseWeightHistoryScreen

A Scaffold with a TopAppBar (exercise name as title, back navigation) and a Canvas-based line chart drawn using Compose's `Canvas` composable. No new library dependency required.

**Chart implementation approach:**

```kotlin
@Composable
fun WeightLineChart(points: List<WeightPoint>, weightLabel: String, modifier: Modifier = Modifier) {
    val minWeight = points.minOf { it.weight }
    val maxWeight = points.maxOf { it.weight }
    val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)
    val firstDate = points.first().date
    val lastDate = points.last().date
    val dayRange = ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        val padLeft = 80f; val padRight = 24f; val padTop = 24f; val padBottom = 48f
        val chartWidth = size.width - padLeft - padRight
        val chartHeight = size.height - padTop - padBottom

        // Map a data point to canvas coordinates
        fun pointX(date: LocalDate) =
            padLeft + (ChronoUnit.DAYS.between(firstDate, date).toFloat() / dayRange) * chartWidth
        fun pointY(weight: Double) =
            padTop + ((maxWeight - weight) / weightRange * chartHeight).toFloat()

        // Draw axes
        drawLine(Color.Gray, Offset(padLeft, padTop), Offset(padLeft, padTop + chartHeight))
        drawLine(Color.Gray, Offset(padLeft, padTop + chartHeight), Offset(padLeft + chartWidth, padTop + chartHeight))

        // Draw connecting lines
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFF6650A4),  // Material primary
                start = Offset(pointX(points[i].date), pointY(points[i].weight)),
                end   = Offset(pointX(points[i + 1].date), pointY(points[i + 1].weight)),
                strokeWidth = 3f
            )
        }

        // Draw dots
        points.forEach { p ->
            drawCircle(Color(0xFF6650A4), radius = 6f, center = Offset(pointX(p.date), pointY(p.weight)))
        }
    }
}
```

Y-axis and X-axis labels are rendered as `Text` composables overlaid on the chart using a `Box` layout, positioned relative to the chart area.

The screen structure:
- If `uiState.isLoading` → show `CircularProgressIndicator`
- If `uiState.points.size < 2` → show centered message: "Not enough data to display a graph yet."
- Otherwise → show the chart with axis labels

**Files changed:**
- `ui/history/ExerciseWeightHistoryScreen.kt` (new)

### 4d — Navigation

Add to `Screen.kt`:

```kotlin
object ExerciseWeightHistory : Screen("exercise_weight_history/{exerciseId}") {
    fun createRoute(exerciseId: Long) = "exercise_weight_history/$exerciseId"
}
```

Add the composable to `NavGraph.kt`:

```kotlin
composable(
    route = Screen.ExerciseWeightHistory.route,
    arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
) { backStackEntry ->
    val exerciseId = backStackEntry.arguments!!.getLong("exerciseId")
    ExerciseWeightHistoryScreen(
        app = app,
        exerciseId = exerciseId,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

Update `SessionDetailScreen` to accept `onExerciseSelected: (exerciseId: Long) -> Unit = {}` and make each exercise `Card` clickable. The `SessionExercise` model already exposes `exerciseId`. Add `import androidx.compose.foundation.clickable` and modify the Card:

```kotlin
uiState.exercises.forEach { exercise ->
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onExerciseSelected(exercise.exerciseId) },   // new
        elevation = CardDefaults.cardElevation(1.dp)
    ) { ... }
}
```

In the `composable(Screen.SessionDetail.route)` block in `NavGraph.kt`, add:

```kotlin
SessionDetailScreen(
    app = app,
    sessionId = sessionId,
    onNavigateBack = { navController.popBackStack() },
    onExerciseSelected = { exerciseId ->          // new
        navController.navigate(Screen.ExerciseWeightHistory.createRoute(exerciseId))
    }
)
```

**Files changed:**
- `ui/navigation/Screen.kt`
- `ui/navigation/NavGraph.kt`
- `ui/history/SessionDetailScreen.kt`

**Verification:**
- Navigate to History → tap a completed session → Session Detail opens.
- Tap an exercise card — navigates to Exercise Weight History screen with the exercise name as the title.
- If the exercise was performed in multiple sessions with weight recorded: a line graph is displayed connecting the data points in chronological order.
- If fewer than two weight data points exist: "Not enough data to display a graph yet." is shown.
- Verify weight values display in the correct unit (kg vs lb) per the current Settings.
- Change weight unit in Settings — return to the graph — confirm values convert.
- Back navigation returns to Session Detail.

**Risk:** Medium. The Canvas chart coordinate math requires care; test with both single-day data (all points on same date — `dayRange` coerced to 1) and multi-week data. The `ExerciseWeightPoint` Room projection must match column names exactly; verify with a test or by inspecting the generated Room code if the build reports a mapping error.

---

## Summary of All File Changes

| File | Milestones | Type of Change |
|---|---|---|
| `util/AudioVibrationManager.kt` | M1 (if needed) | Switch to `prepareAsync()` pattern |
| `ui/workoutday/WorkoutDayDetailScreen.kt` | M2 | Wrap trailing `IconButton`s in `Row` |
| `ui/session/components/ExerciseCard.kt` | M3a | Add optional `onInfoClicked` param; render info `IconButton` |
| `ui/session/ActiveSessionScreen.kt` | M3b | Add `onExerciseInfoSelected` param; pass to `ExerciseCard` |
| `ui/navigation/NavGraph.kt` | M3b, M4d | Wire exercise-info and weight-history navigation |
| `data/model/ExerciseWeightPoint.kt` | M4a | New data class |
| `data/db/dao/SessionExerciseDao.kt` | M4a | Add `getWeightHistory()` Flow query |
| `data/repository/SessionRepository.kt` | M4a | Expose `getWeightHistory()` Flow |
| `ui/history/ExerciseWeightHistoryViewModel.kt` | M4b | New ViewModel |
| `ui/history/ExerciseWeightHistoryScreen.kt` | M4c | New Screen with Canvas line chart |
| `ui/navigation/Screen.kt` | M4d | Add `ExerciseWeightHistory` sealed object |
| `ui/history/SessionDetailScreen.kt` | M4d | Add `onExerciseSelected` param; make exercise cards clickable |

**No database schema changes. Two new files. No new dependencies.**

---

## Risks and Gotchas

| Risk | Severity | Mitigation |
|---|---|---|
| Timer sound may already be resolved by Plan v3 M1 — install and test before any code change | Low | Verification-first approach; only apply the `prepareAsync()` fallback if needed |
| `ExerciseWeightPoint` Room projection: column names in the SQL query must exactly match field names in the data class | Low | Use `@ColumnInfo(name = "...")` annotations on the data class fields if there is any mismatch |
| Canvas chart: all points on the same date causes `dayRange = 0` | Low | `coerceAtLeast(1L)` on `dayRange` prevents division by zero; all points render at the same X position |
| Canvas chart: single data point (exactly 1) should show "not enough data" — the `< 2` guard handles this | Low | Already covered by the `points.size < 2` check |
| `ActiveSessionViewModel` must not be destroyed when navigating to `ExerciseDetail` | Low | ViewModel is scoped to `Screen.ActiveSession.route` back-stack entry via `viewModel()` — forward navigation does not pop the entry |
| `onExerciseInfoSelected` default parameter `= {}` means no crash if NavGraph is not updated before screen | Low | Default is a no-op; safe during incremental development |
