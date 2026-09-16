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
import kotlinx.coroutines.launch

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
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
            // §3.3: offer to resume a session interrupted by process death.
            ResumeSessionPrompt(onResume = { routineId, exerciseIndex ->
                navController.navigate(Screen.RoutineCamera.createRoute(routineId, exerciseIndex))
            })
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
                onSeeAllRoutines = { navController.navigate(Screen.MyRoutines.route) },
                onStartPlannedRoutine = { routineId ->
                    navController.navigate(Screen.RoutineCamera.createRoute(routineId, 0))
                },
                onEditWeeklyPlan = { navController.navigate(Screen.WeeklyPlan.route) },
                onOpenBodyWeight = { navController.navigate(Screen.BodyWeight.route) },
                onOpenChat = { navController.navigate(Screen.Chat.route) }
            )
        }
        composable(Screen.WeeklyPlan.route) {
            com.example.gymformcoach.features.plan.WeeklyPlanScreen(
                onNavigateToTab = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.BodyWeight.route) {
            com.example.gymformcoach.features.profile.BodyWeightScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ExerciseLibrary.route) {
            ExerciseLibraryScreen(
                onNavigateToTab = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onExerciseSelected = { exercise ->
                    // Quick single-exercise session, same entry point WorkoutListScreen uses.
                    navController.navigate(Screen.Camera.createRoute(exercise.name, 0f, 10, 3))
                }
            )
        }
        composable(Screen.DataPortability.route) {
            com.example.gymformcoach.features.profile.DataPortabilityScreen(
                onBack = { navController.popBackStack() },
                onOpenPlanSharing = { navController.navigate(Screen.PlanSharing.route) },
                onOpenImport = { navController.navigate(Screen.ImportWorkouts.route) }
            )
        }
        composable(Screen.PlanSharing.route) {
            com.example.gymformcoach.features.profile.PlanSharingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ImportWorkouts.route) {
            com.example.gymformcoach.features.profile.ImportScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Coach.route) {
            com.example.gymformcoach.features.coach.CoachScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Chat.route) {
            com.example.gymformcoach.features.chat.ChatScreen(
                onBack = { navController.popBackStack() }
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
            StatsScreen(onNavigateToTab = { route ->
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
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenBodyWeight = { navController.navigate(Screen.BodyWeight.route) },
                onOpenWeeklyPlan = { navController.navigate(Screen.WeeklyPlan.route) },
                onOpenDataPortability = { navController.navigate(Screen.DataPortability.route) },
                onOpenCoach = { navController.navigate(Screen.Coach.route) }
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
                // §4.2: this is a STEP index (one per set, round-sequenced through
                // supersets), not an exercise index - a routine with 3-set exercises
                // has more steps than exercises.
                val stepIndex = backStackEntry.arguments?.getInt("exerciseIndex") ?: 0
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_detail_flow/$routineId") }
                val detailViewModel: RoutineDetailViewModel = viewModel(parentEntry)
                LaunchedEffect(routineId) { detailViewModel.load(routineId) }
                val steps by detailViewModel.sessionSteps.collectAsState()

                // §3.3: persist the in-progress session on entry and on every set
                // completion, so process death (likely with the camera running)
                // can be resumed instead of losing the workout.
                val activeSessionRepository = remember {
                    com.example.gymformcoach.core.data.ActiveSessionRepository(
                        com.example.gymformcoach.core.data.AppDatabase.getInstance(context)
                    )
                }
                LaunchedEffect(routineId, stepIndex) {
                    if (stepIndex == 0) activeSessionRepository.start(routineId)
                }

                if (stepIndex < steps.size) {
                    val step = steps[stepIndex]
                    val isPoseTracked = step.exercise.exerciseId.lowercase() in POSE_TRACKED_EXERCISES

                    // Standalone (non-superset) exercises with no camera tracking get the
                    // multi-set logging table (§3/§5) instead of one screen per set - it
                    // shows the progression reasoning once and lets every set be checked
                    // off in place. Superset members stay on the round-sequenced
                    // per-step camera flow since interleaving two exercises into one
                    // table is a larger redesign than this pass covers.
                    if (step.exercise.supersetGroupId == null && !isPoseTracked) {
                        val routine by detailViewModel.routine.collectAsState()
                        val exerciseStepCount = steps.drop(stepIndex)
                            .takeWhile { it.exercise.id == step.exercise.id }.size

                        SetLoggingScreen(
                            routineExercise = step.exercise,
                            routine = routine,
                            stepNumber = steps.take(stepIndex + 1).map { it.exercise.id }.distinct().size,
                            totalSteps = steps.map { it.exercise.id }.distinct().size,
                            canMakeSuperset = stepIndex + exerciseStepCount < steps.size,
                            onMakeSuperset = { /* superset grouping happens in the builder, §4.3 */ },
                            onBack = { navController.popBackStack() },
                            onFinishExercise = {
                                val nextIndex = stepIndex + exerciseStepCount
                                scope.launch { activeSessionRepository.recordSetCompleted(nextIndex, routineId) }
                                if (nextIndex < steps.size) {
                                    navController.navigate(Screen.RoutineCamera.createRoute(routineId, nextIndex)) {
                                        popUpTo(Screen.RoutineCamera.createRoute(routineId, stepIndex)) { inclusive = true }
                                    }
                                } else {
                                    scope.launch {
                                        com.example.gymformcoach.core.data.ActiveSessionRepository(
                                            com.example.gymformcoach.core.data.AppDatabase.getInstance(context)
                                        ).finish()
                                    }
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                        return@composable
                    }

                    CameraScreen(
                        workoutType = step.exercise.exerciseId,
                        weight = step.exercise.targetWeightKg,
                        targetReps = step.exercise.targetReps,
                        totalSets = step.exercise.targetSets,
                        currentSetNumber = step.setIndex + 1,
                        promptForBodyWeight = stepIndex == 0,
                        onBack = { navController.popBackStack() },
                        onFinishSet = { repCount, isPr, sessionId ->
                            scope.launch {
                                activeSessionRepository.recordSetCompleted(stepIndex, sessionId)
                            }
                            navController.navigate(Screen.RoutineResults.createRoute(routineId, stepIndex, repCount, isPr, sessionId))
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
                val stepIndex = backStackEntry.arguments?.getInt("exerciseIndex") ?: 0
                val reps = backStackEntry.arguments?.getInt("reps") ?: 0
                val isPr = backStackEntry.arguments?.getBoolean("isPr") ?: false
                val exerciseSessionId = backStackEntry.arguments?.getString("exerciseSessionId") ?: ""
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("routine_detail_flow/$routineId") }
                val detailViewModel: RoutineDetailViewModel = viewModel(parentEntry)
                val steps by detailViewModel.sessionSteps.collectAsState()
                val currentStep = steps.getOrNull(stepIndex)
                val exerciseName = currentStep?.exercise?.exerciseId ?: ""

                ResultsScreen(
                    workoutType = exerciseName,
                    reps = reps,
                    isPr = isPr,
                    exerciseSessionId = exerciseSessionId,
                    // §4.2: rest fires only after the last member of a superset round
                    // completes it - never between members mid-round.
                    showRestTimer = currentStep?.restAfter ?: true,
                    onDone = {
                        val nextIndex = stepIndex + 1
                        if (nextIndex < steps.size) {
                            navController.navigate(Screen.RoutineCamera.createRoute(routineId, nextIndex)) {
                                popUpTo(Screen.RoutineCamera.createRoute(routineId, stepIndex)) { inclusive = true }
                            }
                        } else {
                            // Routine finished — clear the resume record (§3.3) so the
                            // next launch doesn't offer to resume a completed session.
                            scope.launch {
                                com.example.gymformcoach.core.data.ActiveSessionRepository(
                                    com.example.gymformcoach.core.data.AppDatabase.getInstance(context)
                                ).finish()
                            }
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
