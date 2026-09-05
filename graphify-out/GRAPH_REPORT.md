# Graph Report - app-project  (2026-09-05)

## Corpus Check
- Large corpus: 164 files · ~1,148,728 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder.

## Summary
- 455 nodes · 816 edges · 24 communities (17 shown, 3 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 73 edges (avg confidence: 0.9)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Pose & ML Analysis
- Camera & Capture
- Core Utilities
- Exercise Knowledge
- UI Navigation Flows
- Workout Screens
- Settings & Preferences
- Meal Plans & Nutrition
- Dashboard & Home
- Onboarding Experience
- Content & Articles
- Progress Tracking
- Community Features
- Favorites & Bookmarks
- Help & Support
- Timer & Intervals
- Database Schema
- Form Validation
- Data Models
- Configuration

## God Nodes (most connected - your core abstractions)
1. `PreferenceManager` - 38 edges
2. `Screen` - 37 edges
3. `NavGraph()` - 34 edges
4. `PrimaryButton()` - 20 edges
5. `AppDatabase` - 18 edges
6. `CameraViewModel` - 17 edges
7. `RoutineRepository` - 15 edges
8. `SetupBaseScreen()` - 14 edges
9. `Workout` - 13 edges
10. `ExerciseSession` - 12 edges

## Surprising Connections (you probably didn't know these)
- `Articles & Tips - Detail` --semantically_similar_to--> `Article Detail - Strength Training`  [INFERRED] [semantically similar]
  Fitness App UI Kit for Gym Workout App Fitness Tracker Mobile App Gym Fitness Mobile App UI Kit (Community)/10 - B - Articles & Tips.png → Fitness App UI Kit for Gym Workout App Fitness Tracker Mobile App Gym Fitness Mobile App UI Kit (Community)/11.2.1 - B - Articles & Tips.png
- `Favorites - Articles` --semantically_similar_to--> `Article Detail - Strength Training`  [INFERRED] [semantically similar]
  Fitness App UI Kit for Gym Workout App Fitness Tracker Mobile App Gym Fitness Mobile App UI Kit (Community)/11.3.3 - A- Favorites - articles.png → Fitness App UI Kit for Gym Workout App Fitness Tracker Mobile App Gym Fitness Mobile App UI Kit (Community)/11.2.1 - B - Articles & Tips.png
- `FAQs - Detail with Categories` --semantically_similar_to--> `Article Detail - Strength Training`  [INFERRED] [semantically similar]
  Fitness App UI Kit for Gym Workout App Fitness Tracker Mobile App Gym Fitness Mobile App UI Kit (Community)/11.4.1 - B - Help & FAQs.png → Fitness App UI Kit for Gym Workout App Fitness Tracker Mobile App Gym Fitness Mobile App UI Kit (Community)/11.2.1 - B - Articles & Tips.png
- `HomeScreen()` --calls--> `GymBottomNavigation()`  [INFERRED]
  app/src/main/java/com/example/gymformcoach/features/home/HomeScreen.kt → app/src/main/java/com/example/gymformcoach/core/designsystem/components/GymBottomNavigation.kt
- `NavGraph()` --calls--> `RoutineBuilderScreen()`  [INFERRED]
  app/src/main/java/com/example/gymformcoach/core/navigation/NavGraph.kt → app/src/main/java/com/example/gymformcoach/features/routines/RoutineBuilderScreen.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Exercise Composition Pattern** — workout_database_body_parts, workout_database_training_modalities, workout_database_exercises [EXTRACTED 1.00]

## Communities (24 total, 3 thin omitted)

### Community 0 - "Pose & ML Analysis"
Cohesion: 0.11
Nodes (39): Modifier, PrimaryButton(), NavGraph(), PreferenceManager, LaunchScreen(), LoginScreen(), SignUpScreen(), SocialIcon() (+31 more)

### Community 1 - "Camera & Capture"
Cohesion: 0.07
Nodes (23): Routine, Flow, RoutineDao, RoutineExercise, Flow, RoutineRepository, RoutineSummary, Modifier (+15 more)

### Community 2 - "Core Utilities"
Cohesion: 0.05
Nodes (38): GymBottomNavigation(), ActivityLevel, Age, Camera, Equipment, Fingerprint, FocusAreas, Gender (+30 more)

### Community 3 - "Exercise Knowledge"
Cohesion: 0.09
Nodes (17): Context, PostSetAnalysisRunner, AppDatabase, Context, PrEstimator, CameraViewModel, AndroidViewModel, NormalizedLandmark (+9 more)

### Community 4 - "UI Navigation Flows"
Cohesion: 0.08
Nodes (26): Failure, StateFlow, Loading, PostSetAnalysisStatusBus, State, Success, SetAnalysis, Flow (+18 more)

### Community 5 - "Workout Screens"
Cohesion: 0.09
Nodes (17): DefaultRecommendationEngine, RecommendationEngine, WeightUnit, DraftRoutineExercise, RoutineBuilderScreen(), RoutineExerciseRow(), AndroidViewModel, RoutineBuilderViewModel (+9 more)

### Community 6 - "Settings & Preferences"
Cohesion: 0.16
Nodes (12): ExerciseSession, ExerciseSessionDao, Flow, ExerciseSessionRepository, Flow, DaySummary, EmptyHistoryPlaceholder(), ExerciseHeatmap() (+4 more)

### Community 7 - "Meal Plans & Nutrition"
Cohesion: 0.14
Nodes (12): PoseLandmarkerResult, LandmarkerListener, PoseLandmarkerHelper, CameraScreen(), LandmarkerListener, PoseLandmarkerResult, PoseOverlay(), BaseOptions (+4 more)

### Community 8 - "Dashboard & Home"
Cohesion: 0.14
Nodes (20): Article Detail - Strength Training, Customer Service, Favorites - All, Favorites - Articles, Favorites - Videos, Help & FAQs, FAQs - Detail with Categories, Home Dashboard (+12 more)

### Community 9 - "Onboarding Experience"
Cohesion: 0.10
Nodes (20): Back, Barbell / Free Weights, Bicep, Body Parts, Bodyweight / Calisthenics, Cable Variations, Chest, Core (+12 more)

### Community 10 - "Content & Articles"
Cohesion: 0.16
Nodes (9): GymFormCoachTheme(), BiometricHelper, AuthenticationCallback, Context, FragmentActivity, FragmentActivity, MainActivity, BiometricPrompt (+1 more)

### Community 11 - "Progress Tracking"
Cohesion: 0.19
Nodes (5): Flow, ProfileRepository, UserProfile, Flow, UserProfileDao

### Community 12 - "Community Features"
Cohesion: 0.13
Nodes (16): Animated Buttons, Articles & Tips, Icon Styles, Instruction Guide, Settings Variant AH, Settings Variant AI, Settings Variant AJ, Settings Variant AK (+8 more)

### Community 13 - "Favorites & Bookmarks"
Cohesion: 0.20
Nodes (12): Community Hub, Home Dashboard, Meal Plans - Nutrition, Onboarding - Active Lifestyle, Onboarding - Community Challenge, Onboarding - Nutrition Tips, Onboarding - Welcome to FITBODY, User Profile (+4 more)

### Community 14 - "Help & Support"
Cohesion: 0.36
Nodes (5): AiCoachApiClient, Connected, ConnectionResult, Unreachable, Result

### Community 15 - "Timer & Intervals"
Cohesion: 0.62
Nodes (3): CorrectiveKbEntry, CorrectivesKnowledgeBase, Context

### Community 18 - "Data Models"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **57 isolated node(s):** `Connected`, `Loading`, `Success`, `Failure`, `SyncStatus` (+52 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 103 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AppDatabase` connect `Exercise Knowledge` to `Pose & ML Analysis`, `Camera & Capture`, `UI Navigation Flows`, `Workout Screens`, `Settings & Preferences`, `Progress Tracking`?**
  _High betweenness centrality (0.154) - this node is a cross-community bridge._
- **Why does `PreferenceManager` connect `Pose & ML Analysis` to `Camera & Capture`, `Exercise Knowledge`, `UI Navigation Flows`, `Workout Screens`, `Settings & Preferences`, `Content & Articles`, `Progress Tracking`?**
  _High betweenness centrality (0.144) - this node is a cross-community bridge._
- **Why does `Screen` connect `Core Utilities` to `Pose & ML Analysis`, `Camera & Capture`?**
  _High betweenness centrality (0.139) - this node is a cross-community bridge._
- **Are the 21 inferred relationships involving `NavGraph()` (e.g. with `RoutineBuilderScreen()` and `RoutineDetailScreen()`) actually correct?**
  _`NavGraph()` has 21 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Connected`, `Loading`, `Success` to the rest of the system?**
  _57 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Pose & ML Analysis` be split into smaller, more focused modules?**
  _Cohesion score 0.1085972850678733 - nodes in this community are weakly interconnected._
- **Should `Camera & Capture` be split into smaller, more focused modules?**
  _Cohesion score 0.06901960784313725 - nodes in this community are weakly interconnected._