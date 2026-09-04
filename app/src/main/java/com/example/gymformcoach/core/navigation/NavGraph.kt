package com.example.gymformcoach.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymformcoach.features.auth.LaunchScreen
import com.example.gymformcoach.features.auth.LoginScreen
import com.example.gymformcoach.features.auth.SignUpScreen
import com.example.gymformcoach.features.onboarding.OnboardingScreen
import com.example.gymformcoach.features.setup.*
import com.example.gymformcoach.features.setup.fingerprint.SetFingerprintScreen
import com.example.gymformcoach.features.home.HomeScreen
import com.example.gymformcoach.features.profile.ProfileScreen
import com.example.gymformcoach.features.workout.*
import com.example.gymformcoach.features.routines.*
import com.example.gymformcoach.core.utils.PreferenceManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    val startDestination = when {
        !preferenceManager.isOnboardingComplete -> Screen.Onboarding.route
        !preferenceManager.isSetupComplete -> Screen.Launch.route
        else -> Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = { 
                    preferenceManager.isOnboardingComplete = true
                    navController.navigate(Screen.Launch.route) 
                }
            )
        }
        composable(Screen.Launch.route) {
            LaunchScreen(
                onLogin = { navController.navigate(Screen.Login.route) },
                onSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { 
                    if (preferenceManager.isSetupComplete) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Launch.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Gender.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = { navController.navigate(Screen.Gender.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Setup Flow
        composable(Screen.Gender.route) { GenderScreen(onNext = { navController.navigate(Screen.Age.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.Age.route) { AgeScreen(onNext = { navController.navigate(Screen.Weight.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.Weight.route) { WeightScreen(onNext = { navController.navigate(Screen.Height.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.Height.route) { HeightScreen(onNext = { navController.navigate(Screen.Goal.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.Goal.route) { GoalScreen(onNext = { navController.navigate(Screen.ActivityLevel.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.ActivityLevel.route) { ActivityLevelScreen(onNext = { navController.navigate(Screen.TrainingExperience.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.TrainingExperience.route) { TrainingExperienceScreen(onNext = { navController.navigate(Screen.Equipment.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.Equipment.route) { EquipmentScreen(onNext = { navController.navigate(Screen.TrainingSplit.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.TrainingSplit.route) { TrainingSplitScreen(onNext = { navController.navigate(Screen.FocusAreas.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.FocusAreas.route) { FocusAreasScreen(onNext = { navController.navigate(Screen.SessionLength.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.SessionLength.route) { SessionLengthScreen(onNext = { navController.navigate(Screen.TrainingLimitations.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.TrainingLimitations.route) { TrainingLimitationsScreen(onNext = { navController.navigate(Screen.Fingerprint.route) }, onBack = { navController.popBackStack() }) }
        composable(Screen.Fingerprint.route) { 
            SetFingerprintScreen(
                onContinue = { navController.navigate(Screen.Home.route) },
                onSkip = { navController.navigate(Screen.Home.route) }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onBodyPartSelected = { bodyPart -> navController.navigate(Screen.WorkoutList.createRoute(bodyPart)) },
                onNavigateToTab = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCreateRoutine = { navController.navigate(Screen.RoutineBuilder.route) },
                onRoutineSelected = { routineId -> navController.navigate(Screen.RoutineDetail.createRoute(routineId)) },
                onSeeAllRoutines = { navController.navigate(Screen.MyRoutines.route) }
            )
        }
        composable(
            route = Screen.WorkoutList.route,
            arguments = listOf(navArgument("bodyPart") { type = NavType.StringType })
        ) { backStackEntry ->
            val bodyPart = backStackEntry.arguments?.getString("bodyPart") ?: ""
            WorkoutListScreen(
                bodyPart = bodyPart,
                onBack = { navController.popBackStack() },
                onStartWorkout = { type, weight, reps, sets -> 
                    navController.navigate(Screen.Camera.createRoute(type, weight, reps, sets)) 
                }
            )
        }
        composable(Screen.Progress.route) {
            ProgressScreen(onNavigateToTab = { route ->
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToTab = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            com.example.gymformcoach.features.profile.SettingsScreen(onBack = { navController.popBackStack() })
        }
        
        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("workoutType") { type = NavType.StringType },
                navArgument("weight") { type = NavType.FloatType },
                navArgument("reps") { type = NavType.IntType },
                navArgument("sets") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val workoutType = backStackEntry.arguments?.getString("workoutType") ?: "Squat"
            val weight = backStackEntry.arguments?.getFloat("weight") ?: 0f
            val reps = backStackEntry.arguments?.getInt("reps") ?: 10
            val sets = backStackEntry.arguments?.getInt("sets") ?: 3
            
            CameraScreen(
                workoutType = workoutType,
                weight = weight,
                targetReps = reps,
                totalSets = sets,
                onBack = { navController.popBackStack() },
                onFinishSet = { repCount, isPr, sessionId ->
                    navController.navigate(Screen.Results.createRoute(workoutType, repCount, isPr, sessionId))
                }
            )
        }
        composable(
            route = Screen.Results.route,
            arguments = listOf(
                navArgument("workoutType") { type = NavType.StringType },
                navArgument("reps") { type = NavType.IntType },
                navArgument("isPr") { type = NavType.BoolType },
                navArgument("exerciseSessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workoutType = backStackEntry.arguments?.getString("workoutType") ?: "Squat"
            val reps = backStackEntry.arguments?.getInt("reps") ?: 0
            val isPr = backStackEntry.arguments?.getBoolean("isPr") ?: false
            val exerciseSessionId = backStackEntry.arguments?.getString("exerciseSessionId") ?: ""
            ResultsScreen(
                workoutType = workoutType,
                reps = reps,
                isPr = isPr,
                exerciseSessionId = exerciseSessionId,
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.MyRoutines.route) {
            RoutineListScreen(
                onBack = { navController.popBackStack() },
                onCreateRoutine = { navController.navigate(Screen.RoutineBuilder.route) },
                onRoutineSelected = { routineId -> navController.navigate(Screen.RoutineDetail.createRoute(routineId)) }
            )
        }

        navigation(startDestination = Screen.RoutineBuilder.route, route = "routine_builder_flow") {
            composable(Screen.RoutineBuilder.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_builder_flow") }
                val builderViewModel: RoutineBuilderViewModel = viewModel(parentEntry)
                RoutineBuilderScreen(
                    viewModel = builderViewModel,
                    onBack = { navController.popBackStack() },
                    onPickExercise = { navController.navigate(Screen.RoutinePickBodyPart.route) },
                    onSaved = { routineId ->
                        navController.navigate(Screen.RoutineDetail.createRoute(routineId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onStart = { routineId ->
                        navController.navigate(Screen.RoutineCamera.createRoute(routineId, 0)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
            composable(Screen.RoutinePickBodyPart.route) {
                RoutinePickBodyPartScreen(
                    onBack = { navController.popBackStack() },
                    onBodyPartSelected = { bodyPart -> navController.navigate(Screen.RoutinePickExercise.createRoute(bodyPart)) }
                )
            }
            composable(
                route = Screen.RoutinePickExercise.route,
                arguments = listOf(navArgument("bodyPart") { type = NavType.StringType })
            ) { backStackEntry ->
                val bodyPart = backStackEntry.arguments?.getString("bodyPart") ?: ""
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_builder_flow") }
                val builderViewModel: RoutineBuilderViewModel = viewModel(parentEntry)
                WorkoutListScreen(
                    bodyPart = bodyPart,
                    onBack = { navController.popBackStack() },
                    onStartWorkout = { _, _, _, _ -> },
                    selectionMode = true,
                    onExerciseSelected = { workout ->
                        builderViewModel.addExercise(workout)
                        navController.popBackStack(Screen.RoutineBuilder.route, inclusive = false)
                    }
                )
            }
        }

        navigation(
            startDestination = Screen.RoutineDetail.route,
            route = "routine_detail_flow/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.StringType })
        ) {
            composable(
                route = Screen.RoutineDetail.route,
                arguments = listOf(navArgument("routineId") { type = NavType.StringType })
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_detail_flow/$routineId") }
                val detailViewModel: RoutineDetailViewModel = viewModel(parentEntry)
                RoutineDetailScreen(
                    viewModel = detailViewModel,
                    routineId = routineId,
                    onBack = { navController.popBackStack() },
                    onStartRoutine = { navController.navigate(Screen.RoutineCamera.createRoute(routineId, 0)) }
                )
            }
            composable(
                route = Screen.RoutineCamera.route,
                arguments = listOf(
                    navArgument("routineId") { type = NavType.StringType },
                    navArgument("exerciseIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
                val exerciseIndex = backStackEntry.arguments?.getInt("exerciseIndex") ?: 0
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_detail_flow/$routineId") }
                val detailViewModel: RoutineDetailViewModel = viewModel(parentEntry)
                LaunchedEffect(routineId) { detailViewModel.load(routineId) }
                val exercises by detailViewModel.exercises.collectAsState()

                if (exerciseIndex < exercises.size) {
                    val current = exercises[exerciseIndex]
                    CameraScreen(
                        workoutType = current.exerciseId,
                        weight = current.targetWeightKg,
                        targetReps = current.targetReps,
                        totalSets = current.targetSets,
                        onBack = { navController.popBackStack() },
                        onFinishSet = { repCount, isPr, sessionId ->
                            navController.navigate(Screen.RoutineResults.createRoute(routineId, exerciseIndex, repCount, isPr, sessionId))
                        }
                    )
                }
            }
            composable(
                route = Screen.RoutineResults.route,
                arguments = listOf(
                    navArgument("routineId") { type = NavType.StringType },
                    navArgument("exerciseIndex") { type = NavType.IntType },
                    navArgument("reps") { type = NavType.IntType },
                    navArgument("isPr") { type = NavType.BoolType },
                    navArgument("exerciseSessionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
                val exerciseIndex = backStackEntry.arguments?.getInt("exerciseIndex") ?: 0
                val reps = backStackEntry.arguments?.getInt("reps") ?: 0
                val isPr = backStackEntry.arguments?.getBoolean("isPr") ?: false
                val exerciseSessionId = backStackEntry.arguments?.getString("exerciseSessionId") ?: ""
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_detail_flow/$routineId") }
                val detailViewModel: RoutineDetailViewModel = viewModel(parentEntry)
                val exercises by detailViewModel.exercises.collectAsState()
                val exerciseName = exercises.getOrNull(exerciseIndex)?.exerciseId ?: ""

                ResultsScreen(
                    workoutType = exerciseName,
                    reps = reps,
                    isPr = isPr,
                    exerciseSessionId = exerciseSessionId,
                    onDone = {
                        val nextIndex = exerciseIndex + 1
                        if (nextIndex < exercises.size) {
                            navController.navigate(Screen.RoutineCamera.createRoute(routineId, nextIndex)) {
                                popUpTo(Screen.RoutineCamera.createRoute(routineId, exerciseIndex)) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
