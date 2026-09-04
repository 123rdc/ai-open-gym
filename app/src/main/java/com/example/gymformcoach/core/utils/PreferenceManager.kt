package com.example.gymformcoach.core.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gym_form_coach_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        private const val KEY_GENDER = "profile_gender"
        private const val KEY_AGE = "profile_age"
        private const val KEY_WEIGHT_KG = "profile_weight_kg"
        private const val KEY_HEIGHT_CM = "profile_height_cm"
        private const val KEY_FITNESS_GOAL = "profile_fitness_goal"
        private const val KEY_ACTIVITY_LEVEL = "profile_activity_level"

        private const val KEY_TRAINING_EXPERIENCE = "profile_training_experience"
        private const val KEY_EQUIPMENT = "profile_equipment"
        private const val KEY_TRAINING_SPLIT = "profile_training_split"
        private const val KEY_FOCUS_AREAS = "profile_focus_areas"
        private const val KEY_SESSION_LENGTH = "profile_session_length"
        private const val KEY_TRAINING_LIMITATIONS = "profile_training_limitations"

        private const val KEY_REST_DURATION_SECONDS = "rest_duration_seconds"
        private const val KEY_WEIGHT_UNIT = "weight_unit"
        private const val KEY_AI_COACH_API_URL = "ai_coach_api_url"
        private const val KEY_AI_COACH_API_KEY = "ai_coach_api_key"
        private const val KEY_AI_COACH_MODEL = "ai_coach_model"
        private const val KEY_CLIP_RETENTION_DAYS = "clip_retention_days"
    }

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var gender: String
        get() = prefs.getString(KEY_GENDER, "Male") ?: "Male"
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    var age: Int
        get() = prefs.getInt(KEY_AGE, 25)
        set(value) = prefs.edit().putInt(KEY_AGE, value).apply()

    var weightKg: Float
        get() = prefs.getFloat(KEY_WEIGHT_KG, 70f)
        set(value) = prefs.edit().putFloat(KEY_WEIGHT_KG, value).apply()

    var heightCm: Float
        get() = prefs.getFloat(KEY_HEIGHT_CM, 170f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT_CM, value).apply()

    var fitnessGoal: String
        get() = prefs.getString(KEY_FITNESS_GOAL, "Get Fitter") ?: "Get Fitter"
        set(value) = prefs.edit().putString(KEY_FITNESS_GOAL, value).apply()

    var activityLevel: String
        get() = prefs.getString(KEY_ACTIVITY_LEVEL, "Moderately Active") ?: "Moderately Active"
        set(value) = prefs.edit().putString(KEY_ACTIVITY_LEVEL, value).apply()

    var trainingExperience: String
        get() = prefs.getString(KEY_TRAINING_EXPERIENCE, "Beginner (<6 months)") ?: "Beginner (<6 months)"
        set(value) = prefs.edit().putString(KEY_TRAINING_EXPERIENCE, value).apply()

    var equipment: Set<String>
        get() = prefs.getStringSet(KEY_EQUIPMENT, setOf("Full gym")) ?: setOf("Full gym")
        set(value) = prefs.edit().putStringSet(KEY_EQUIPMENT, value).apply()

    var trainingSplit: String
        get() = prefs.getString(KEY_TRAINING_SPLIT, "No preference") ?: "No preference"
        set(value) = prefs.edit().putString(KEY_TRAINING_SPLIT, value).apply()

    var focusAreas: Set<String>
        get() = prefs.getStringSet(KEY_FOCUS_AREAS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FOCUS_AREAS, value).apply()

    var sessionLengthPreference: String
        get() = prefs.getString(KEY_SESSION_LENGTH, "30-45 min") ?: "30-45 min"
        set(value) = prefs.edit().putString(KEY_SESSION_LENGTH, value).apply()

    var trainingLimitations: String
        get() = prefs.getString(KEY_TRAINING_LIMITATIONS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TRAINING_LIMITATIONS, value).apply()

    var restDurationSeconds: Int
        get() = prefs.getInt(KEY_REST_DURATION_SECONDS, 90)
        set(value) = prefs.edit().putInt(KEY_REST_DURATION_SECONDS, value).apply()

    var weightUnit: String
        get() = prefs.getString(KEY_WEIGHT_UNIT, "kg") ?: "kg"
        set(value) = prefs.edit().putString(KEY_WEIGHT_UNIT, value).apply()

    var aiCoachApiUrl: String
        get() = prefs.getString(KEY_AI_COACH_API_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_COACH_API_URL, value).apply()

    var aiCoachApiKey: String
        get() = prefs.getString(KEY_AI_COACH_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_COACH_API_KEY, value).apply()

    var aiCoachModel: String
        get() = prefs.getString(KEY_AI_COACH_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_COACH_MODEL, value).apply()

    var clipRetentionDays: Int
        get() = prefs.getInt(KEY_CLIP_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_CLIP_RETENTION_DAYS, value).apply()
}
