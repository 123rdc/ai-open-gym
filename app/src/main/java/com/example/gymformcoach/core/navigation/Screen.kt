package com.example.gymformcoach.core.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Launch : Screen("launch")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Fingerprint : Screen("fingerprint")
    object Home : Screen("home")
    object Camera : Screen("camera/{workoutType}/{weight}/{reps}/{sets}") {
        fun createRoute(workoutType: String, weight: Float, reps: Int, sets: Int) = 
            "camera/$workoutType/$weight/$reps/$sets"
    }
    object Results : Screen("results/{workoutType}/{reps}/{isPr}/{exerciseSessionId}") {
        fun createRoute(workoutType: String, reps: Int, isPr: Boolean, exerciseSessionId: String) =
            "results/$workoutType/$reps/$isPr/$exerciseSessionId"
    }
    object History : Screen("history")

    // Setup Flow
    object Gender : Screen("setup/gender")
    object Age : Screen("setup/age")
    object Weight : Screen("setup/weight")
    object Height : Screen("setup/height")
    object Goal : Screen("setup/goal")
    object ActivityLevel : Screen("setup/activity_level")
    object TrainingExperience : Screen("setup/training_experience")
    object Equipment : Screen("setup/equipment")
    object TrainingSplit : Screen("setup/training_split")
    object FocusAreas : Screen("setup/focus_areas")
    object SessionLength : Screen("setup/session_length")
    object TrainingLimitations : Screen("setup/training_limitations")

    // Bottom Nav
    object Progress : Screen("progress")
    object Profile : Screen("profile")
    object Settings : Screen("settings")

    object WorkoutList : Screen("workout_list/{bodyPart}") {
        fun createRoute(bodyPart: String) = "workout_list/$bodyPart"
    }

    // Routines
    object MyRoutines : Screen("my_routines")
    object RoutineBuilder : Screen("routine_builder")
    object RoutinePickBodyPart : Screen("routine_pick_body_part")
    object RoutinePickExercise : Screen("routine_pick_exercise/{bodyPart}") {
        fun createRoute(bodyPart: String) = "routine_pick_exercise/$bodyPart"
    }
    object RoutineDetail : Screen("routine_detail/{routineId}") {
        fun createRoute(routineId: String) = "routine_detail/$routineId"
    }
    object RoutineCamera : Screen("routine_camera/{routineId}/{exerciseIndex}") {
        fun createRoute(routineId: String, exerciseIndex: Int) = "routine_camera/$routineId/$exerciseIndex"
    }
    object RoutineResults : Screen("routine_results/{routineId}/{exerciseIndex}/{reps}/{isPr}/{exerciseSessionId}") {
        fun createRoute(routineId: String, exerciseIndex: Int, reps: Int, isPr: Boolean, exerciseSessionId: String) =
            "routine_results/$routineId/$exerciseIndex/$reps/$isPr/$exerciseSessionId"
    }
}
