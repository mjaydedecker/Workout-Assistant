package com.mjaydedecker.workoutassistant.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mjaydedecker.workoutassistant.WorkoutAssistantApp
import com.mjaydedecker.workoutassistant.ui.exercise.ExerciseFormScreen
import com.mjaydedecker.workoutassistant.ui.exercise.ExerciseListScreen
import com.mjaydedecker.workoutassistant.ui.history.ExerciseWeightHistoryScreen
import com.mjaydedecker.workoutassistant.ui.history.SessionDetailScreen
import com.mjaydedecker.workoutassistant.ui.history.SessionHistoryScreen
import com.mjaydedecker.workoutassistant.ui.home.HomeScreen
import com.mjaydedecker.workoutassistant.ui.library.ExerciseDetailScreen
import com.mjaydedecker.workoutassistant.ui.library.ExerciseLibraryScreen
import com.mjaydedecker.workoutassistant.ui.session.ActiveSessionScreen
import com.mjaydedecker.workoutassistant.ui.settings.SettingsScreen
import com.mjaydedecker.workoutassistant.ui.workoutday.WorkoutDayDetailScreen
import com.mjaydedecker.workoutassistant.ui.workoutday.WorkoutDayListScreen

@Composable
fun NavGraph(app: WorkoutAssistantApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val hideBottomBar = currentDestination?.route?.startsWith("active_session") == true

    val bottomNavItems = listOf(
        Triple(Screen.Home, Icons.Default.Home, "Home"),
        Triple(Screen.ExerciseLibrary, Icons.AutoMirrored.Filled.LibraryBooks, "Library"),
        Triple(Screen.WorkoutDayList, Icons.Default.ViewDay, "Days"),
        Triple(Screen.SessionHistory, Icons.Default.History, "History"),
        Triple(Screen.Settings, Icons.Default.Settings, "Settings")
    )

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon, label) ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route ||
                            // Treat ExerciseDetail and ExerciseLibrary as part of the same tab
                            (screen == Screen.ExerciseLibrary &&
                                (it.route?.startsWith("exercise_library") == true ||
                                 it.route?.startsWith("exercise_detail") == true))
                        } == true
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                val route = if (screen == Screen.ExerciseLibrary)
                                    Screen.ExerciseLibrary.createRoute()
                                else
                                    screen.route
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    app = app,
                    onStartWorkout = { navController.navigate(Screen.WorkoutDayList.route) },
                    onResumeWorkout = { navController.navigate(Screen.ActiveSession.createRoute()) }
                )
            }

            composable(
                route = Screen.ExerciseLibrary.route,
                arguments = listOf(navArgument("workoutDayId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { backStackEntry ->
                val workoutDayId = backStackEntry.arguments?.getLong("workoutDayId") ?: -1L
                ExerciseLibraryScreen(
                    app = app,
                    onExerciseSelected = { exerciseId ->
                        navController.navigate(
                            Screen.ExerciseDetail.createRoute(exerciseId, workoutDayId)
                        )
                    },
                    onNavigateBack = if (workoutDayId != -1L) {
                        { navController.popBackStack() }
                    } else null,
                    onManageCustomExercises = { navController.navigate(Screen.ExerciseList.route) }
                )
            }

            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.LongType },
                    navArgument("workoutDayId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments!!.getLong("exerciseId")
                val workoutDayId = backStackEntry.arguments?.getLong("workoutDayId") ?: -1L
                ExerciseDetailScreen(
                    app = app,
                    exerciseId = exerciseId,
                    preselectedWorkoutDayId = workoutDayId.takeIf { it != -1L },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ExerciseList.route) {
                ExerciseListScreen(
                    app = app,
                    onAddExercise = { navController.navigate(Screen.ExerciseForm.createRoute()) },
                    onEditExercise = { id -> navController.navigate(Screen.ExerciseForm.createRoute(id)) }
                )
            }

            composable(
                route = Screen.ExerciseForm.route,
                arguments = listOf(navArgument("exerciseId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: -1L
                ExerciseFormScreen(
                    app = app,
                    exerciseId = exerciseId.takeIf { it != -1L },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WorkoutDayList.route) {
                WorkoutDayListScreen(
                    app = app,
                    onDaySelected = { id -> navController.navigate(Screen.WorkoutDayDetail.createRoute(id)) },
                    onStartSession = { id -> navController.navigate(Screen.ActiveSession.createRoute(id)) }
                )
            }

            composable(
                route = Screen.WorkoutDayDetail.route,
                arguments = listOf(navArgument("workoutDayId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutDayId = backStackEntry.arguments!!.getLong("workoutDayId")
                WorkoutDayDetailScreen(
                    app = app,
                    workoutDayId = workoutDayId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLibrary = { dayId ->
                        navController.navigate(Screen.ExerciseLibrary.createRoute(dayId))
                    }
                )
            }

            composable(
                route = Screen.ActiveSession.route,
                arguments = listOf(navArgument("workoutDayId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { backStackEntry ->
                val workoutDayId = backStackEntry.arguments?.getLong("workoutDayId") ?: -1L
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
            }

            composable(Screen.SessionHistory.route) {
                SessionHistoryScreen(
                    app = app,
                    onSessionSelected = { id -> navController.navigate(Screen.SessionDetail.createRoute(id)) }
                )
            }

            composable(
                route = Screen.SessionDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments!!.getLong("sessionId")
                SessionDetailScreen(
                    app = app,
                    sessionId = sessionId,
                    onNavigateBack = { navController.popBackStack() },
                    onExerciseSelected = { exerciseId ->
                        navController.navigate(Screen.ExerciseWeightHistory.createRoute(exerciseId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(app = app)
            }

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
        }
    }
}
