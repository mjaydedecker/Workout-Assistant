package com.mjaydedecker.workoutassistant.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ExerciseList : Screen("exercise_list")
    object ExerciseForm : Screen("exercise_form?exerciseId={exerciseId}") {
        fun createRoute(exerciseId: Long = -1L) = "exercise_form?exerciseId=$exerciseId"
    }
    object ExerciseLibrary : Screen("exercise_library?workoutDayId={workoutDayId}") {
        fun createRoute(workoutDayId: Long = -1L) = "exercise_library?workoutDayId=$workoutDayId"
    }
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}?workoutDayId={workoutDayId}") {
        fun createRoute(exerciseId: Long, workoutDayId: Long = -1L) =
            "exercise_detail/$exerciseId?workoutDayId=$workoutDayId"
    }
    object WorkoutDayList : Screen("workout_day_list")
    object WorkoutDayDetail : Screen("workout_day_detail/{workoutDayId}") {
        fun createRoute(workoutDayId: Long) = "workout_day_detail/$workoutDayId"
    }
    object ActiveSession : Screen("active_session?workoutDayId={workoutDayId}") {
        fun createRoute(workoutDayId: Long = -1L) = "active_session?workoutDayId=$workoutDayId"
    }
    object SessionHistory : Screen("session_history")
    object SessionDetail : Screen("session_detail/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_detail/$sessionId"
    }
    object Settings : Screen("settings")
    object ExerciseWeightHistory : Screen("exercise_weight_history/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_weight_history/$exerciseId"
    }
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.ExerciseLibrary,
    Screen.WorkoutDayList,
    Screen.SessionHistory,
    Screen.Settings
)
