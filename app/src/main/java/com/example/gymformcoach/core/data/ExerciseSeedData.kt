package com.example.gymformcoach.core.data

import java.util.UUID

/**
 * The 113-exercise catalog, ported from the formerly-hardcoded
 * ExerciseCatalog/getWorkoutsForBodyPart in WorkoutListScreen.kt and
 * reclassified with exerciseType/loadType/isUnilateral (§1, §20.3).
 *
 * IDs are deterministic (UUID v3 from the exercise name) so the seed insert
 * in MIGRATION_5_6 is idempotent and re-running it never creates duplicates.
 */
object ExerciseSeedData {

    private fun id(name: String): String =
        UUID.nameUUIDFromBytes(name.toByteArray()).toString()

    private fun seed(
        name: String,
        duration: String,
        difficulty: String,
        imageUrl: String,
        muscle: String,
        category: String,
        about: String,
        exerciseType: ExerciseType = ExerciseType.REPS,
        isUnilateral: Boolean = false
    ): Exercise {
        val loadType = if (category == "Bodyweight") LoadType.BODYWEIGHT else LoadType.WEIGHTED
        return Exercise(
            id = id(name),
            name = name,
            bodyPart = "", // filled in by withBodyPart()
            muscle = muscle,
            category = category,
            duration = duration,
            difficulty = difficulty,
            imageUrl = imageUrl,
            about = about,
            exerciseType = exerciseType,
            loadType = loadType,
            isUnilateral = isUnilateral,
            isCustom = false,
            muscleGroups = listOf(muscle)
        )
    }

    private fun List<Exercise>.withBodyPart(bodyPart: String): List<Exercise> =
        map { it.copy(bodyPart = bodyPart) }

    private val back = listOf(
        seed("Deadlift", "45 mins", "Advanced", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Lats & Lower Back", "Free Weight", "The king of all exercises. Focus on keeping your back straight and driving through your heels."),
        seed("Lat Pulldown", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Lats", "Machine", "Pull the bar down to your upper chest while leaning back slightly. Squeeze your shoulder blades together."),
        seed("T-Bar Row", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1590239098509-e010d88db81a?auto=format&fit=crop&q=80&w=500", "Upper Back", "Free Weight", "A great mass builder for the mid-back. Keep your chest up and elbows tucked."),
        seed("Seated Cable Row", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Mid Back", "Cable", "Focus on the stretch and the squeeze. Don't use momentum to pull the weight."),
        seed("Pull-ups", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Lats", "Bodyweight", "A fundamental bodyweight exercise for back width. Use a wide grip for maximum lat engagement."),
        seed("Single Arm DB Row", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Lats & Rhomboids", "Dumbbell", "Perform one arm at a time to correct muscle imbalances and improve core stability.", isUnilateral = true),
        seed("Barbell Row", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Back", "Free Weight", "Hinge at the hips and pull the bar to your lower ribs. Keep your core tight."),
        seed("Straight Arm Pulldown", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Lats", "Cable", "Keep your arms straight and pull the bar to your thighs. Excellent for lat isolation."),
        seed("Face Pulls", "20 mins", "Beginner", "https://images.unsplash.com/photo-1591741535018-d042766c62eb?auto=format&fit=crop&q=80&w=500", "Rear Delts & Traps", "Cable", "Pull the rope towards your forehead, flaring your elbows out. Focus on the rear delts."),
        seed("Hyperextensions", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Lower Back", "Bodyweight", "Hinge at the hips on a 45-degree bench. Do not overextend your spine at the top."),
        seed("Chest Supported Row", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Mid Back", "Machine", "Lying face down on an incline bench, row dumbbells or use a machine to isolate the back."),
        seed("Dumbbell Pullover", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Lats & Serratus", "Dumbbell", "Lying on a bench, lower a dumbbell behind your head and pull it back over your chest."),
        seed("Renegade Row", "30 mins", "Advanced", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Back & Core", "Dumbbell", "Perform a row from a plank position. Great for stability and back strength.", isUnilateral = true),
        seed("Wide Grip Lat Pulldown", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Lats", "Machine", "Focuses on the outer lats to build a wide V-taper."),
        seed("Reverse Fly", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Rear Delts", "Dumbbell", "Bend forward and fly weights out to the side to target the upper back and rear delts."),
        seed("Rack Pulls", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Lower Back & Traps", "Free Weight", "A partial deadlift performed from the power rack. Allows for heavier loads."),
        seed("Meadows Row", "30 mins", "Advanced", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Back", "Free Weight", "A unique rowing variation using a landmine setup for intense back activation.")
    ).withBodyPart("Back")

    private val chest = listOf(
        seed("Bench Press", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Chest", "Free Weight", "The standard for chest strength. Keep your feet planted and arch your lower back slightly."),
        seed("Incline DB Press", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Dumbbell", "Targets the upper pectoral muscles. Use a 30-45 degree incline."),
        seed("Chest Fly", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Chest", "Machine", "Isolates the chest muscles. Focus on a wide arc and a deep stretch."),
        seed("Push-ups", "20 mins", "Beginner", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Chest & Triceps", "Bodyweight", "The classic bodyweight chest builder. Keep your body in a straight line."),
        seed("Pec Deck", "25 mins", "Beginner", "https://images.unsplash.com/photo-1546483875-ad9014c88eba?auto=format&fit=crop&q=80&w=500", "Chest", "Machine", "Provides constant tension on the chest. Great for finishing off a chest workout."),
        seed("Dips", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1583454110551-21f2fa29617b?auto=format&fit=crop&q=80&w=500", "Lower Chest", "Bodyweight", "Lean forward to target the chest more than the triceps."),
        seed("Decline Barbell Press", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Lower Chest", "Free Weight", "Targets the lower part of the chest. Helps build overall chest thickness."),
        seed("Cable Crossover", "30 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Chest", "Cable", "Provides constant tension. Cross your hands at the bottom for a peak contraction."),
        seed("Incline Barbell Press", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Free Weight", "Great for adding mass to the upper chest area."),
        seed("Dumbbell Fly", "25 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Chest", "Dumbbell", "Isolate the pecs by maintaining a slight bend in the elbows throughout."),
        seed("Landmine Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Free Weight", "Excellent for shoulder health and targeting the upper chest and inner pecs."),
        seed("Hammer Strength Press", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Chest", "Machine", "Allows for heavy weight with more stability than free weights."),
        seed("Diamond Push-ups", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Chest & Triceps", "Bodyweight", "A push-up variation with hands forming a diamond to emphasize triceps and inner chest."),
        seed("Low-to-High Cable Fly", "25 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Upper Chest", "Cable", "Targets the upper chest by pulling from a low position to head height."),
        seed("Floor Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Chest & Triceps", "Dumbbell", "Lying on the floor limits the range of motion, focusing on the lockout."),
        seed("Svend Press", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Chest", "Free Weight", "Press two plates together in front of your chest to create intense tension.")
    ).withBodyPart("Chest")

    private val shoulders = listOf(
        seed("Overhead Press", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Shoulders", "Free Weight", "The foundational vertical press for shoulder mass and strength."),
        seed("Lateral Raise", "25 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Side Delts", "Dumbbell", "Essential for building shoulder width. Keep your pinky fingers slightly higher."),
        seed("Front Raise", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Front Delts", "Dumbbell", "Isolate the front of the shoulders. Avoid using momentum."),
        seed("Arnold Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Shoulders", "Dumbbell", "A rotating press that targets all three heads of the deltoids."),
        seed("Rear Delt Fly", "25 mins", "Beginner", "https://images.unsplash.com/photo-1546483875-ad9014c88eba?auto=format&fit=crop&q=80&w=500", "Rear Delts", "Machine", "Focus on the back of the shoulder. Don't let your traps take over."),
        seed("Face Pull", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Rear Delts", "Cable", "Excellent for shoulder health and posture. Pull towards your face."),
        seed("Smith Machine Press", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Shoulders", "Machine", "Allows for heavy weight with controlled movement."),
        seed("Upright Row", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Traps & Side Delts", "Free Weight", "Pull the bar up towards your chin. Keep it close to your body."),
        seed("Cable Lateral Raise", "25 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Side Delts", "Cable", "Provides constant tension throughout the whole range of motion."),
        seed("Shrugs", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Traps", "Dumbbell", "Build the upper traps by shrugging your shoulders towards your ears."),
        seed("Pike Push-ups", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Shoulders", "Bodyweight", "A bodyweight alternative to the overhead press. Form a 'V' shape."),
        seed("Military Press", "35 mins", "Advanced", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Shoulders", "Free Weight", "Strict standing overhead press with feet together."),
        seed("Bus Drivers", "15 mins", "Beginner", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?auto=format&fit=crop&q=80&w=500", "Shoulders", "Free Weight", "Hold a plate in front of you and rotate it like a steering wheel."),
        seed("Push Press", "40 mins", "Advanced", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Shoulders & Legs", "Free Weight", "Use leg drive to help press heavy weight overhead."),
        seed("Cable Front Raise", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Front Delts", "Cable", "Provides consistent tension for isolating the front deltoids."),
        seed("Machine Shoulder Press", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Shoulders", "Machine", "Safe and effective way to target the entire shoulder complex.")
    ).withBodyPart("Shoulders")

    private val legs = listOf(
        seed("Squat", "50 mins", "Intermediate", "https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Free Weight", "The foundation of lower body training. Sit back into your heels and keep your chest up."),
        seed("Leg Press", "35 mins", "Beginner", "https://images.unsplash.com/photo-1574680676196-932109470a00?auto=format&fit=crop&q=80&w=500", "Quads", "Machine", "A great alternative to squats. Don't lock your knees at the top."),
        seed("Leg Extension", "25 mins", "Beginner", "https://images.unsplash.com/photo-1434682881908-b43d0467b798?auto=format&fit=crop&q=80&w=500", "Quads", "Machine", "Isolates the quadriceps. Focus on the squeeze at the top."),
        seed("Lying Leg Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1590239098509-e010d88db81a?auto=format&fit=crop&q=80&w=500", "Hamstrings", "Machine", "Targets the hamstrings. Keep your hips pressed into the pad."),
        seed("Romanian Deadlift", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Hamstrings & Glutes", "Free Weight", "Focus on the hinge at your hips. Keep the bar close to your shins."),
        seed("Calf Raises", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=500", "Calves", "Machine", "Fully stretch and contract your calves. Hold at the top for a second."),
        seed("Lunges", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1574680096145-d05b474e2155?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Dumbbell", "Step forward and lower your hips until both knees are bent at a 90-degree angle.", isUnilateral = true),
        seed("Bulgarian Split Squat", "35 mins", "Intermediate", "https://images.unsplash.com/photo-1583454110551-21f2fa29617b?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Dumbbell", "Elevate one foot behind you and squat. Incredible for building leg strength.", isUnilateral = true),
        seed("Goblet Squat", "30 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Quads", "Dumbbell", "Hold a dumbbell at your chest. Great for learning proper squat form."),
        seed("Calf Press", "20 mins", "Beginner", "https://images.unsplash.com/photo-1574680676196-932109470a00?auto=format&fit=crop&q=80&w=500", "Calves", "Machine", "Perform calf raises using the leg press machine for high volume."),
        seed("Glute Ham Raise", "25 mins", "Advanced", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Hamstrings & Glutes", "Bodyweight", "A powerful movement for the entire posterior chain."),
        seed("Step-ups", "25 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Quads & Glutes", "Bodyweight", "Step up onto a bench or box. Focus on pushing through the heel.", isUnilateral = true),
        seed("Hack Squat", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Quads", "Machine", "Allows you to target the quads with a fixed range of motion."),
        seed("Box Jumps", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Quads & Calves", "Bodyweight", "Explosive movement to build power in the legs."),
        seed("Seated Leg Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1434682881908-b43d0467b798?auto=format&fit=crop&q=80&w=500", "Hamstrings", "Machine", "Isolate the hamstrings from a seated position."),
        seed("Hip Thrusts", "40 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Glutes", "Free Weight", "The best exercise for building glute mass and strength.")
    ).withBodyPart("Legs")

    private val bicep = listOf(
        seed("Bicep Curls", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "The foundational bicep exercise. Don't swing your body."),
        seed("Hammer Curls", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Brachialis", "Dumbbell", "Targets the brachialis for thicker-looking arms. Keep palms facing each other."),
        seed("Preacher Curl", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Biceps", "Machine", "Eliminates momentum. Focus on the squeeze at the top."),
        seed("Concentration Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Dumbbell", "Performed while seated. Helps focus entirely on the bicep peak."),
        seed("Chin-ups", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Biceps & Back", "Bodyweight", "A compound movement that heavily involves the biceps. Use an underhand grip."),
        seed("Cable Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Biceps", "Cable", "Provides constant tension throughout the entire range of motion."),
        seed("EZ Bar Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "The angled bar is easier on the wrists than a straight bar."),
        seed("Incline DB Curl", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Dumbbell", "Lying on an incline bench puts the biceps in a stretched position."),
        seed("Spider Curl", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "Lying chest-down on an incline bench isolates the biceps completely."),
        seed("Zottman Curl", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps & Forearms", "Dumbbell", "A rotation curl that targets both the biceps and the forearms."),
        seed("Drag Curl", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "Drag the bar up your body to target the long head of the bicep."),
        seed("Reverse EZ Bar Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Brachioradialis", "Free Weight", "Overhand grip targets the forearms and outer biceps."),
        seed("High Cable Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Biceps", "Cable", "Flexing your arms from a high position to target the bicep peak."),
        seed("Barbell 21s", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Biceps", "Free Weight", "7 partial reps from bottom, 7 from top, and 7 full range of motion reps."),
        seed("Machine Bicep Curl", "25 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Biceps", "Machine", "Consistent resistance throughout the movement for maximum growth."),
        seed("Rope Cable Curl", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Biceps", "Cable", "Using a rope allows for a more natural wrist position and better squeeze.")
    ).withBodyPart("Bicep")

    private val tricep = listOf(
        seed("Tricep Dips", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "Focus on the triceps by keeping your body upright."),
        seed("Skull Crushers", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1597452485669-2c7bb5fef90d?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "Lowers the weight to your forehead. Keep your elbows tucked in."),
        seed("Rope Pushdown", "20 mins", "Beginner", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Pull the rope down and apart at the bottom for a full contraction."),
        seed("Overhead DB Extension", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps Long Head", "Dumbbell", "Great for stretching the long head of the tricep. Keep your core tight."),
        seed("Close Grip Bench", "30 mins", "Intermediate", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "A heavy compound movement for tricep thickness. Keep hands shoulder-width apart."),
        seed("Kickbacks", "20 mins", "Beginner", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Dumbbell", "Isolates the tricep. Keep your upper arm parallel to the floor."),
        seed("Bench Dips", "15 mins", "Beginner", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "A beginner-friendly way to target the triceps using a bench or chair."),
        seed("Straight Bar Pushdown", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Allows for heavier loads than the rope attachment."),
        seed("Single Arm Extension", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Isolate each tricep to correct imbalances and improve mind-muscle connection.", isUnilateral = true),
        seed("JM Press", "30 mins", "Advanced", "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "A hybrid between a close-grip bench and a skull crusher."),
        seed("French Press", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Free Weight", "Seated or standing extension using an EZ bar behind the head."),
        seed("Diamond Push-ups", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "Targets the triceps intensely with a narrow hand position."),
        seed("Tate Press", "25 mins", "Advanced", "https://images.unsplash.com/photo-1581009146145-b5ef03a94e77?auto=format&fit=crop&q=80&w=500", "Triceps", "Dumbbell", "Dumbbell extension focusing on the lateral head of the tricep."),
        seed("Machine Tricep Press", "30 mins", "Beginner", "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?auto=format&fit=crop&q=80&w=500", "Triceps", "Machine", "Similar to a dip but performed in a seated machine for stability."),
        seed("Overhead Cable Extension", "25 mins", "Beginner", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Triceps", "Cable", "Provides a deep stretch to the long head of the tricep."),
        seed("Bodyweight Extension", "20 mins", "Advanced", "https://images.unsplash.com/photo-1598971639058-fab3c32f850b?auto=format&fit=crop&q=80&w=500", "Triceps", "Bodyweight", "Lowering your head under a bar to target triceps using your body weight.")
    ).withBodyPart("Tricep")

    private val core = listOf(
        seed("Plank", "15 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Maintain a straight line from head to heels. Don't let your hips sag.", exerciseType = ExerciseType.TIMED),
        seed("Russian Twists", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Obliques", "Bodyweight", "Rotate your torso from side to side. Use a weight for extra challenge."),
        seed("Hanging Leg Raise", "25 mins", "Intermediate", "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?auto=format&fit=crop&q=80&w=500", "Lower Abs", "Bodyweight", "One of the best lower ab exercises. Don't swing your legs."),
        seed("Cable Crunch", "20 mins", "Beginner", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80&w=500", "Upper Abs", "Cable", "Kneel down and crunch your elbows toward your knees."),
        seed("Ab Wheel", "20 mins", "Advanced", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Roll forward slowly and pull back using your abs. Don't overextend."),
        seed("Bicycle Curls", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=500", "Abs & Obliques", "Bodyweight", "Focus on bringing your elbow to the opposite knee."),
        seed("Mountain Climbers", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Core & Cardio", "Bodyweight", "Drive your knees towards your chest in a plank position."),
        seed("V-Sits", "20 mins", "Advanced", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Lift your legs and torso simultaneously to form a 'V' shape."),
        seed("Dead Bug", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Core Stability", "Bodyweight", "Lower opposite arm and leg while keeping your back flat on the floor."),
        seed("Bird Dog", "15 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Lower Back & Core", "Bodyweight", "Extend opposite arm and leg from a hands-and-knees position.", isUnilateral = true),
        seed("Woodchoppers", "20 mins", "Intermediate", "https://images.unsplash.com/photo-1534367610401-9f5ed68180aa?auto=format&fit=crop&q=80&w=500", "Obliques", "Cable", "Rotate your torso against resistance, pulling from high to low or low to high."),
        seed("Leg Raises", "20 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Lower Abs", "Bodyweight", "Lying on your back, lift your legs to 90 degrees and lower them slowly."),
        seed("Flutter Kicks", "15 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Lower Abs", "Bodyweight", "Keep your legs straight and kick them in a small range of motion."),
        seed("Side Plank", "15 mins", "Beginner", "https://images.unsplash.com/photo-1566241142559-40e1bfc26eb7?auto=format&fit=crop&q=80&w=500", "Obliques", "Bodyweight", "Support your body on one forearm and the side of your foot.", exerciseType = ExerciseType.TIMED, isUnilateral = true),
        seed("Toe Touches", "20 mins", "Beginner", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "Reach for your toes with your legs straight in the air."),
        seed("Sit-ups", "20 mins", "Beginner", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&q=80&w=500", "Abs", "Bodyweight", "The classic core exercise. Focus on using your abs, not your neck.")
    ).withBodyPart("Core")

    val all: List<Exercise> = back + chest + shoulders + legs + bicep + tricep + core

    val bodyParts: List<String> = listOf("Back", "Chest", "Shoulders", "Legs", "Bicep", "Tricep", "Core")
}
