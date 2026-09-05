# Build Progress — FITBODY Functional Spec

Tracks execution of `docs/FUNCTIONAL_SPEC.md` §19 build order. One phase
per working session per §20.1. Updated as phases complete.

Codebase state was surveyed via the graphify knowledge graph
(`graphify-out/graph.json`) before starting, not assumed from the spec.

## Current entities (pre-existing, confirmed via graph + AppDatabase.kt)

`Routine`, `RoutineExercise`, `ExerciseSession`, `UserProfile`, `SetAnalysis` —
all in `app/src/main/java/com/example/gymformcoach/core/data/`. DB at version 4
(migrations `MIGRATION_2_3`, `MIGRATION_3_4`).

## Phase status

| Phase | Status | Notes |
|---|---|---|
| **0** — Conventions & repo refactor | **Done** | See below — largely pre-existing, one migration added |
| **A** — Exercise type system | **Done** | Schema + catalog consolidation + logging UI branching, `assembleDebug` verified |
| **B** — Session flow (plan/session/supersets/rest timer) | **Done** | See below |
| **C** — Progression engine, 1RM, PRs | **Done** | Plus 5 new screens matching a supplied reference design — see below |
| **D** — LLM analysis, clips, weak points | Skipped (user request) | |
| **E** — Effort, viz, notifications, equipment, theming, body weight | **Done** | Muscle map (§13.2) built as simplified geometric diagram, see Phase C detail |
| **F** — Export/import, plan sharing, importers | **Done** | See below |
| **G** — AI Coach | **Done** | See below |
| **H** — Robustness pass | **Partial — see below** | Code-level mitigations done; physical-device verification not possible in this environment |

## Phase 0 detail

Inventory (via `graphify query`, cross-checked against source):

- **§0.1 entity conventions (UUID id, createdAt, updatedAt, syncStatus, serverId):**
  Already present on all 5 entities. Confirmed in `MIGRATION_2_3` (Long→UUID
  remap for `routines`, `routine_exercises`, `exercise_sessions`; fresh
  `user_profile` table) and `MIGRATION_3_4` (`set_analyses`, born with the
  fields already in place). **No migration needed for this part.**
- **`performedAt`/`createdAt` split:** `ExerciseSession` already has a
  separate `date: Long` field distinct from `createdAt` — the split exists,
  but the field is named `date`, not `performedAt`. Every later spec section
  (§17.2 FitNotes import, §17A body weight) references the canonical name
  `performedAt`. **Action: rename `date` → `performedAt` via Migration 4→5.**
- **§0.2 repository pattern:** `RoutineRepository`, `ExerciseSessionRepository`,
  `ProfileRepository`, `SetAnalysisRepository` already wrap all 4 DAOs
  (`RoutineDao`, `ExerciseSessionDao`, `UserProfileDao`, `SetAnalysisDao`).
  Confirmed via graph query — **zero edges from any ViewModel or Composable
  directly to a DAO.** No repository introduction needed.
- **§0.2 ViewModel-per-screen:** Gap found. `ProfileScreen()`,
  `SetFingerprintScreen()`, `HomeScreen()`, `RoutineListScreen()`, and
  `ExerciseHistorySection()` call a repository directly from the Composable,
  with no ViewModel/`StateFlow<UiState>` in between. Deferred — user chose to
  prioritize schema + full phase sweep; revisit if a screen becomes hard to
  test or extend.
- **§0.3 canonical units:** Weight already stored as `Float` kg
  (`weightKg` throughout). Compliant.

### Phase 0 — completed
1. `Migration(4, 5)` added: table-rebuild renaming `exercise_sessions.date` →
   `performedAt` (not `ALTER TABLE ... RENAME COLUMN` — that needs SQLite
   3.25+/API 29+, and minSdk here is 24).
2. Updated `ExerciseSession` entity, `ExerciseSessionDao.getSessionsForExercise`
   query, `RoutineDao.getRoutineSummaries` raw SQL (`es.date` → `es.performedAt`),
   `CameraViewModel.logExerciseSessionAndCheckPr` constructor call, and
   `ExerciseHistorySection`'s `groupBy`.
3. `AppDatabase` bumped to version 5, `MIGRATION_4_5` registered.
4. Verified: `./gradlew compileDebugKotlin` succeeds (KSP Room codegen would
   fail on any entity/migration/query mismatch — clean compile is the
   verification signal here since there's no seeded device/emulator in this
   environment to run the migration against).

## Phase A detail

**Major deviation from spec assumption, found via graphify query + direct
read of `WorkoutListScreen.kt`:** there was no `Exercise` Room entity at all.
The "catalog" was a hardcoded `object ExerciseCatalog` /
`getWorkoutsForBodyPart()` returning a plain (non-persisted, no `id`) `Workout`
data class, 113 exercises across 7 body parts. `RoutineExercise.exerciseId`
and `ExerciseSession.exerciseId` reference exercises **by name string**, not
by any database id.

User decision (asked via AskUserQuestion): promote the catalog to a real
`Exercise` Room entity, and add a `SetLog` entity now rather than deferring
to Phase C, since `TIMED`/`CARDIO` types can't be represented by
`ExerciseSession`'s single aggregate `weightKg`/`reps`/`sets` columns.

**Design call on the id/FK question:** rather than remapping every existing
`RoutineExercise.exerciseId` / `ExerciseSession.exerciseId` from name-string
to the new `Exercise.id` UUID (a large, risky FK remap across all historical
routine/session rows, same category of danger §20.2 calls out for Phase 0's
UUID conversion) — `Exercise.id` is a UUID but `exerciseId` FK columns
**continue to match by `Exercise.name`** (now enforced unique). Lower risk,
avoids a second big remap in the same phase; can revisit if/when a real
need for id-based FKs appears (e.g. renaming an exercise without breaking
history).

### Completed (schema layer)
1. `ExerciseType.kt`: `ExerciseType { REPS, TIMED, CARDIO }`, `LoadType { WEIGHTED, BODYWEIGHT }`.
2. `Exercise.kt` entity: id (UUID), name (unique), bodyPart, muscle, category,
   duration/difficulty/imageUrl/about (ported from `Workout`), exerciseType,
   loadType, isUnilateral, isCustom, `muscleGroups: List<String>` (seeded now
   per §13.2's "seed this alongside §1's classification pass" note, even
   though the muscle map UI itself is Phase E), plus §0.1 fields.
3. `SetLog.kt` entity: id, exerciseSessionId (FK → exercise_sessions,
   CASCADE), setIndex, nullable type-gated columns (weightKg, addedWeightKg,
   reps, durationSeconds, distanceMeters, effortValue, effortScale), isPr,
   plus §0.1 fields. Matches spec §1.1 exactly.
4. `ExerciseDao`/`ExerciseRepository`, `SetLogDao`/`SetLogRepository`.
5. `ExerciseSeedData.kt`: all 113 exercises ported from `WorkoutListScreen.kt`,
   reclassified:
   - `loadType = BODYWEIGHT` where `category == "Bodyweight"`, else `WEIGHTED`.
   - `exerciseType = TIMED` for `Plank`, `Side Plank` (the only two isometric
     holds in this catalog — no true `CARDIO` machines exist in it, e.g. no
     treadmill/bike/rower, since it's a strength-split catalog; `CARDIO`
     stays available for custom exercises).
   - `isUnilateral = true` for 7 exercises with clear single-limb/alternating
     patterns: Single Arm DB Row, Renegade Row, Lunges, Bulgarian Split
     Squat, Step-ups, Single Arm Extension, Side Plank, Bird Dog.
   - IDs are deterministic (`UUID.nameUUIDFromBytes(name)`), so the seed
     insert is idempotent.
6. `MIGRATION_5_6`: creates `exercises` (unique index on `name`) and
   `set_logs` tables, inserts all 113 seed rows. `AppDatabase` bumped to
   version 6, both DAOs registered.
7. Verified: `./gradlew compileDebugKotlin` succeeds — KSP validates `@Query`
   SQL against the registered entity schema at compile time, so this also
   catches column-name mismatches, not just Kotlin syntax errors.

### Completed (UI branching, §20.3 step 4)

User decision (asked via AskUserQuestion): keep `CameraScreen` as the single
logging entry point for all exercises rather than splitting camera-tracked
vs. manual-entry into separate screens — branch its controls by type instead.

Discovery that shaped this: `CameraScreen` was already the *only* logging
entry point in the app (every routine exercise routes to it), but
`CameraViewModel.processPoseForReps` only has real pose-tracking logic for
`"squat"` and `"bench press"/"pushup"` — the other 111 exercises fell
through with **no rep counting and nothing ever logged**. So this wasn't
branching 4 existing UI variants, it was building manual logging (rep
steppers, work timer, distance/duration input, added-weight toggle) that
didn't exist yet for anything but those two exercises.

1. `CameraViewModel`: loads the `Exercise` for the current `workoutType` via
   `ExerciseRepository.findByName`. `stopRecordingAndLog()` now branches
   three ways (`logRepsSet`/`logTimedSet`/`logCardioSet`) instead of one
   fixed weighted-reps path. Each writes both the existing `ExerciseSession`
   aggregate (so ProfileScreen/ExerciseHistorySection/PrEstimator keep
   working unchanged) **and** a new `SetLog` row carrying the real
   type-gated fields, so Phase C's progression/PR/1RM logic has real data
   to read once built. Added manual rep +/- , a count-up work timer
   (§1.5 — count-down mode deferred, it needs Phase C's progression targets
   to pick a default), and cardio distance input.
   PR logic: bodyweight-without-added-load sets are excluded from the
   weighted-1RM PR comparison (§5.2.6); TIMED PR criterion is longest
   duration (§8.1, reusing the `reps` column to hold seconds until Phase
   C/E move history reads onto `SetLog` directly); CARDIO never produces a
   PR (§8.1).
2. `CameraScreen`: branches the bottom control area on
   `exercise.exerciseType`/`loadType` — pose-tracked squat/pushup keep the
   unchanged auto-count record button; other `REPS` exercises get a manual
   stepper + "Log Set"; `TIMED` gets a work-timer start/stop; `CARDIO` gets
   a distance field + start/stop. Bodyweight exercises get a collapsed
   "+ Add weight" affordance above the primary control (§1.3). Header weight
   chip shows "BODYWEIGHT" / "+NkG" instead of "0KG" for unloaded bodyweight
   sets.
3. **Catalog consolidation**: `WorkoutListScreen.kt`'s old hardcoded
   `getWorkoutsForBodyPart()` (113 `Workout` literals, the same data now in
   `ExerciseSeedData`) deleted entirely — `WorkoutListScreen` now sources
   from `ExerciseRepository.getByBodyPart()` via `collectAsState`, mapped
   through a new `Exercise.toWorkout()` extension so the existing
   `WorkoutCard`/`SelectableWorkoutCard` Composables needed no changes.
   `RoutineDetailExerciseRow` similarly moved from the removed
   `ExerciseCatalog.findByName()` to an async `ExerciseRepository.findByName`
   load. `ExerciseCatalog.bodyParts` (the 7 body-part hero images — a fixed
   nav taxonomy, not per-exercise data) was left as-is; it isn't duplicated
   by `Exercise`.
4. Verified: `./gradlew assembleDebug` succeeds (full pipeline: KSP, Kotlin
   compile, dex, package — not just `compileDebugKotlin`).

### Not yet done
Per-exercise history/PR/1RM views (`ExerciseHistorySection`, `ProfileScreen`)
still read only `ExerciseSession`'s aggregate columns, so TIMED/CARDIO sets
will show a plausible-looking but not-quite-right number (duration stored in
the `reps` column) until Phase C/E update those views to read `SetLog`
directly for non-REPS exercises. Documented in code comments at the write
site (`CameraViewModel.logTimedSet`) so this isn't a silent gap.

## Phase B detail

Added `WeeklyPlanEntry`/`PlanOverride`/`ActiveSession` entities (`MIGRATION_6_7`,
DB v7) and `BodyWeightEntry` + `UserProfile.goalWeightKg` (`MIGRATION_7_8`, DB v8
— pulled forward from Phase E since §3.1's session prompt needs it).

- **§2 weekly plan**: `PlanRepository.resolveDay()` implements the override→
  weekly→unplanned resolution order exactly; `reschedule()` writes the two-override
  pattern (§2.3) and never touches the recurring template. `WeeklyPlanScreen`
  (7-row editor) + `TodayWorkoutCard` on Home (workout/rest/unplanned states, all
  designed, none fabricated).
- **§3 guided session**: `BodyWeightPrompt` (skippable, pre-filled from
  `BodyWeightRepository.getLatest()`), `KeepScreenOn` (`DisposableEffect`-scoped
  to the camera screen, toggle in Settings, default on), `ActiveSession`
  persisted on session start and every set completion, `ResumeSessionPrompt` on
  Home offers resume/discard within the 6-hour window then self-clears.
  **Not done**: §3.4 interruption handling (pause pose analysis + retain buffer
  on backgrounding mid-set) — the persistence layer exists but `CameraViewModel`
  doesn't yet hook lifecycle pause/resume to buffer state.
- **§4 supersets**: `RoutineExercise.supersetGroupId` (additive column,
  `MIGRATION_6_7`). `SupersetGrouping` (pure functions, no Android/Room deps):
  `group`/`ungroup`/`repairAfterReorder`/`buildSequence`. Builder UI in
  `RoutineBuilderScreen` — multi-select mode, contiguous+non-cardio validation,
  connecting rail + "Superset A/B" header, ungroup action.
  **The navigation rework**: `RoutineCamera`/`RoutineResults` previously treated
  their int route arg as an *exercise* index and only ever logged one set per
  exercise regardless of `targetSets` (a pre-existing gap, not introduced here).
  Rewired both to index into `RoutineDetailViewModel.sessionSteps` — the routine
  flattened into individual sets via `SupersetGrouping.buildSequence()` — so
  multi-set exercises and round-based superset sequencing
  (A1→B1→rest→A2→B2→rest) both actually work now. `ResultsScreen` takes
  `showRestTimer` and suppresses the rest card between superset members.
- **§7 rest timer**: wall-clock end-timestamp design throughout (`RestTimerCard`,
  `RestTimerScheduler`) — completion is scheduled via `AlarmManager.
  setExactAndAllowWhileIdle` (falls back to inexact if exact-alarm permission is
  denied on S+) and fires through `RestTimerReceiver` even with the app
  backgrounded/closed. Returning to the app mid-rest recomputes remaining time
  from the wall clock, never from a paused tick counter. Presets 60/90/120/180s,
  +30s, pause/resume, skip.
- Added `WorkManager` + core library desugaring (`java.time` at minSdk 24) to
  `build.gradle.kts` — needed for §2's `LocalDate`/`DayOfWeek` and §14's
  reminder worker.
- Verified: `./gradlew assembleDebug` (full pipeline) after each meaningful
  chunk, not just `compileDebugKotlin`.

**Correction during this phase**: an early edit wholesale-overwrote the
pre-existing `RestTimer.kt` (`RestTimerCard`, called from `ResultsScreen`)
instead of editing it — caught via `git status` showing it as merely `M`
instead of `??`, and reading `git show HEAD:...` to recover the original
before reconciling. Restored the original's exact public API and rebuilt the
wall-clock/AlarmManager behavior on top of it rather than replacing it again.

## Phase E detail

- **§10 effort rating**: `SetLog.effortValue`/`effortScale` (already in the
  Phase A schema). Off by default, Settings toggle + RIR/RPE scale picker.
  `EffortPicker` in `CameraScreen`, shown only for REPS sets. Strictly
  informational — not read by progression (doesn't exist yet), PR calc, or the
  LLM payload, per the spec's explicit isolation requirement.
- **§13.1 activity heatmap**: `ActivityHeatmapSection` on `ProgressScreen`,
  full corpus (not one exercise) via `ExerciseSessionRepository.getAllSessions()`.
  Shade uses **set count per day as a proxy for time spent training** —
  `ExerciseSession` has no duration column for weighted REPS sets (only
  `SetLog.durationSeconds` does, and only for TIMED/CARDIO); noted in code, real
  duration-based shading is a Phase C/E-follow-on once history views read
  `SetLog` directly.
- **§13.2 muscle map**: **not built.** This needs real anatomical vector art
  (front/back figure, independently colorable muscle regions) that I can't
  responsibly fabricate as inline SVG without it looking wrong — flagged rather
  than shipping a bad placeholder. `Exercise.muscleGroups` is already seeded
  (Phase A) so the data side is ready whenever real art is available.
- **§13.3 progressive overload chart**: already existed; the 1RM second-series
  extension is deferred to Phase C where the 1RM system actually gets built.
- **§14 notifications**: `WorkoutReminderWorker` (periodic `WorkManager`,
  checks `PlanRepository.resolveDay()` + whether anything's logged today before
  notifying), `RestTimerReceiver`/`RestTimerScheduler` (§7, built in Phase B).
  Both on separate channels (`Notifications.ensureChannels`). Settings toggle +
  wiring to schedule/cancel the reminder worker. Time-of-day picker for the
  reminder is not built (hour/minute fields exist in `PreferenceManager` with
  defaults, but Settings only exposes the enable/disable toggle today).
- **§15.1 equipment filter**: already existed (`DefaultRecommendationEngine.
  filterExercisesByEquipment`, used in `WorkoutListScreen`'s selection mode).
  The "adaptive options" refinement (hide filter combinations that would return
  zero results) was **not** added — lower priority given scope, and the
  existing always-available "Full gym" escape hatch already satisfies the
  spec's "never a hard restriction" requirement.
- **§15.2 custom exercises**: `ExerciseRepository.addCustomExercise` (existed
  from Phase A) wired to a minimal add-exercise dialog on `WorkoutListScreen`
  (name + body part from context; equipment/type default to REPS/WEIGHTED/
  Bodyweight per spec). Behaves identically to built-ins everywhere since it's
  just another row in the same `exercises` table.
- **§16 theming**: restructured `Primary`/`Background`/`Surface`/`TextPrimary`/
  `TextSecondary`/`Error` from plain top-level `val`s into `@Composable`
  CompositionLocal-backed properties (`LocalThemeColors`) — every one of the 28
  existing call sites (`color = Primary`, etc.) kept working unchanged, since a
  property-getter read is syntactically identical to a constant read. The one
  place this broke: `Theme.kt` itself built its `ColorScheme` from those globals
  in a top-level `val` initializer, which isn't a `@Composable` context — moved
  that construction inside `GymFormCoachTheme` instead, resolved from
  `PreferenceManager.themeMode`/`accent` on every recomposition. Also broke 5
  call sites inside `Canvas { ... }` draw-scope lambdas (not `@Composable`
  either) — fixed by capturing the color into a local `val` just before
  entering the lambda. Light theme + 7 accent colors (`AccentPalette`, "neon"
  green is the unchanged default identity), both pickers in Settings.
- **§17A body weight**: schema in Phase B pull-forward.
  `BodyWeightViewModel`/`BodyWeightScreen` — range selector (month/6mo/year/
  all), goal-direction-colored chart (delta color depends on which side of the
  goal you're moving, never hardcoded "down=green"; neutral coloring with no
  goal line when no goal is set), manual entry dialog, goal-set/clear dialog.
  Linked from Profile.

## Phase C detail

Per §20.5, the rules live in `core/progression/` with zero Android/Room
dependencies, so they're unit-tested without a device.

- `Progression.kt`/`ProgressionRules.kt`: `NoneRule`, `LinearRule`,
  `GreyskullRule`, `DoubleProgressionRule`, `TimeBasedRule`, plus the §5.2.6
  bodyweight-reps override applied outside the individual rules (cross-cuts
  all five). Every target carries its `reasoning` string verbatim per §5.3.
- `OneRepMax.kt`: Epley, with the 12-rep eligibility cutoff enforced in
  `isEligible` rather than left to callers to remember.
- `PersonalRecords.kt`: shape-aware PR criterion (§8.1) — weighted
  load/1RM, bodyweight reps, timed duration, cardio always excluded.
- `ProgressionRulesTest.kt`: 26 tests, 5 cases per rule (hit/miss/stall/no-history/
  stale) plus the bodyweight-override cases. All passing
  (`./gradlew testDebugUnitTest`).
- `ProgressionRepository.kt` bridges Room to the pure engine: resolves
  §5.1's rule assignment (exercise override → routine default → NONE, new
  `Routine.progressionRule`/`RoutineExercise.progressionRuleOverride` columns,
  `MIGRATION_8_9`), builds history from `SetLog` with a fallback to
  `ExerciseSession`'s aggregate columns for pre-SetLog rows.
- `CameraViewModel`'s PR check now goes through `PersonalRecords` instead of
  the old ad-hoc weighted-only 1RM comparison — TIMED and bodyweight-reps
  sets get correct PR detection for the first time.

### User-supplied reference design → 5 new/rebuilt screens

The user provided a 5-screen mockup ("openGym") partway through this phase.
Built to match its structure using the existing design system (theme tokens,
not the mockup's exact hex values) rather than a pixel clone, since a full
visual rebrand was out of scope:

1. **Set-logging table** (`SetLoggingScreen`/`SetLoggingViewModel`) — new.
   The manual counterpart to `CameraScreen`'s pose-tracked flow: a multi-row
   weight/reps/RIR table for one exercise's full set count, pre-filled from
   `ProgressionRepository`, showing "Last time" and the reasoning banner
   verbatim, warm-up/add/remove-set actions, per-row PR badges. Wired into
   the routine nav graph for standalone (non-superset) exercises with no
   camera tracking — superset members still use the existing round-sequenced
   `CameraScreen` flow, since interleaving two exercises into one table is a
   larger redesign than this pass covers. **Not done**: no rest timer between
   this screen's own rows (only between exercises via the existing flow).
2. **Home** enhancements — `WeekCalendarStrip` (Mo–Su, logged-day dots),
   `BodyWeightPreviewCard` (mini trend line, taps through to the full
   `BodyWeightScreen`), `StreakCard`/`StreakCalculator` (pure function: a
   week counts if ≥1 session was logged in it; zero sessions → honest zero,
   never fabricated per §18.5).
3. **Plan** — `WeeklyPlanScreen` converted from a pushed/back-arrow screen to
   a bottom-nav tab (matching Home/Progress/Profile's pattern), with a
   "Routines" list section added below the weekly schedule.
4. **Stats** (`StatsScreen`) — now the Progress tab's destination, replacing
   the old `ProgressScreen` (deleted along with its `CalendarHeader`/
   `StatItem`, which had hardcoded fabricated numbers like "45,200 kg" —
   good riddance). Three tabs: Muscle balance / Fatigue / Strength (folds in
   the existing §13.1 activity heatmap + §13.3 overload chart).
   - **§13.2 muscle map, reconsidered**: earlier in this session I said this
     needed real anatomical art I couldn't responsibly fabricate. With a
     concrete reference now in hand, built `MuscleFatigueDiagram` as an
     explicitly-labeled *simplified geometric* figure (rounded rects in a
     rough body layout, not anatomical art) shaded by a real, pure
     `MuscleFatigue.calculate()` (days since a muscle group was last
     trained → fatigued/recovering/ready). Real vector art can replace the
     region shapes later without touching the fatigue logic.
   - Discovered mid-build: `Exercise.muscleGroups` is seeded as free text
     ("Lats & Lower Back", "Upper Chest") from the original catalog, not a
     fixed taxonomy — added keyword-based `normalizeMuscleGroup()` in
     `MuscleFatigueViewModel` to map it onto the diagram's canonical
     regions, rather than reseeding the catalog.
   - Effort/RIR stats section is real: average effort + "% at RIR≤3 or
     RPE≥7" + rated/total set counts, windowed (30d/90d/1Y/All), computed
     from `SetLog.effortValue` — reads as "no rated sets" honestly when
     effort tracking is off rather than showing a fake number.
5. **Exercises** (`ExerciseLibraryScreen`) — new, added as a 4th bottom-nav
   tab (`GymBottomNavigation` now has 5 items: Home/Plan/Stats/Exercises/
   Profile, up from 3). Single searchable list across all body parts with
   body-part + equipment filter chips, distinct from the existing
   body-part-first `WorkoutListScreen` flow. "+Plan" starts a quick
   single-exercise session (existing `Screen.Camera` route). "Create your
   own exercise" dialog wired to the same `addCustomExercise` (§15.2) as
   `WorkoutListScreen`'s.

Verified: `./gradlew assembleDebug testDebugUnitTest` — full pipeline plus
all 26 unit tests, after each meaningful chunk of this phase.

## Phase F detail

Added `kotlinx.serialization` (plugin + `kotlinx-serialization-json`) — the
first new library this build needed since `desugar`/`work` in Phase B.

- **§17.1 full export/import**: `core/export/ExportModels.kt` — DTOs
  deliberately decoupled from the Room entities (own `schemaVersion`, not
  tied to the DB's migration count) so a future column rename doesn't
  silently change what an old export file means. `DataExportRepository`
  reads every table (routines + their exercises, weekly plan + overrides,
  sessions + set logs, custom exercises, body weight, profile, and the
  relevant `PreferenceManager` settings), serializes to one JSON file via
  SAF `CreateDocument`. Import (`OpenDocument`) validates the `type` field
  and structure *before* any write, and `DataPortabilityScreen` warns
  explicitly and requires confirmation before importing over existing data
  (§17.1's two explicit requirements). Whole import runs inside one Room
  transaction.
- **§17B plan sharing**: `PlanShareModels.kt`/`PlanShareRepository` — a
  separate, smaller DTO with a `PLAN_SHARE` type discriminator distinct from
  `FULL_EXPORT`, so the importer rejects a full backup handed to the wrong
  entry point (verified: `parse()` throws with a message pointing at the
  right screen if the type doesn't match). Explicitly excludes session
  logs, sets, PRs, body weight, and settings — only routine structure +
  weekly assignments. Shared via `FileProvider` + `ACTION_SEND` (added the
  provider + `file_paths.xml`, not previously in the manifest). Import is
  additive with name-collision suffixing (`"Push Day (imported)"`, then
  `"(imported 2)"`, ...); weekly-plan days are offered via checkboxes, not
  auto-applied. Unknown exercise names become custom exercises, matching
  §17.2's importer convention rather than a separate rule.
- **§17B.3 PDF**: `PlanPdfDocumentAdapter` draws directly onto a
  `PdfDocument` canvas (light background, fixed A4 layout) rather than
  screenshotting UI — per spec, a screenshot prints badly and would carry
  the app's dark theme onto paper. Single-page; a routine list long enough
  to overflow is truncated rather than crashing (a second page would be the
  next increment, not attempted here).
- **§17C.1 shared importer pipeline**: `core/importer/` — `WorkoutImporter`
  interface (`canParse`/`parse`) + `ImportPipeline` doing everything
  source-agnostic: exercise-name matching (case-insensitive, whitespace-
  trimmed, and matches word-set variants like "Bench Press (Barbell)" ↔
  "Barbell Bench Press" — unit-tested), duplicate detection (existing
  session for the same exerciseId+date is skipped, so re-importing the same
  file is a no-op), and commit. Only `parse()` differs per source.
- **§17.2 FitNotes**: `FitNotesImporter`, built exactly to the column set
  the spec names. Each row is one set; grouping into sessions by
  date+exercise happens in the shared pipeline, not the parser. lbs→kg
  conversion, distance/time-without-weight/reps rows import as CARDIO not
  zero-weight strength sets. Unit-tested (detection, weighted-reps parsing,
  lbs conversion, cardio-shape detection, malformed-row counting).
- **§17C.2/§17C.3 Strong, Hevy, Apple Health — all EXPERIMENTAL, by
  necessity**: this environment has no way to fetch a real sample export
  from any of the three services (no internet access), and the spec is
  explicit that guessing silently is the failure mode to avoid here.
  `StrongImporter`/`HevyImporter` are written against each service's
  publicly-documented CSV column names, not a verified file — both set
  `isExperimental = true`, and `ImportScreen`'s preview shows a visible
  "EXPERIMENTAL" badge plus a warning line naming exactly this, rather than
  presenting an unverified guess with the same confidence as the verified
  FitNotes parser. `AppleHealthImporter` (body weight only, per spec — not
  routed through the CSV `WorkoutImporter` interface at all) stream-parses
  `export.xml` out of the exported zip via `XmlPullParser` rather than
  loading it into memory, since a multi-year export runs to hundreds of MB;
  wired into `BodyWeightScreen` as a separate zip-file import action, also
  marked experimental. **If real sample exports become available, these
  three parsers are exactly where to verify/correct column names** — the
  shared pipeline underneath doesn't need to change.
- `ImportScreen`: pick file → sniff header → auto-select the matching
  importer → preview (session count, date range, matched vs. new exercise
  counts, malformed-row count, experimental badge) → confirm → commit →
  completion summary (imported/skipped-as-duplicate/created-exercises
  counts).

Verified: `./gradlew assembleDebug testDebugUnitTest` — 34 unit tests total
now (26 progression + 6 FitNotes + 2 name-matching), all passing.

## Phase G detail

Per §20.9, built the validator before the renderer.

- **`PlanValidator`** (`core/coach/`): pure, no Android/LLM dependency, 9 unit
  tests. Every field from the model is treated as untrusted: an exerciseId
  outside the exact catalog the model was given is dropped and counted
  (never created as a custom exercise — a model-invented name silently
  becoming real data is the failure mode §17D.2 calls out by name), a
  duplicate exercise within one routine is dropped, an inverted rep range is
  dropped, an out-of-bounds set count is clamped rather than dropped
  (recoverable), an unrecognized progression rule is coerced to `NONE`
  rather than discarding the whole routine, and a routine that ends up with
  zero surviving exercises is dropped entirely (its name surfaced so the UI
  can say "N suggestions were skipped").
- **`TrainingCoachRepository`**: shares §9.6's `AiCoachApiClient` (the same
  user-configured OpenAI-compatible endpoint settings as the pre-existing
  post-set analysis feature) and nothing else — kept in its own package so
  form analysis and programming decisions stay independently debuggable, per
  spec. §17D.4 safety: onboarding's `trainingLimitations` ("movements to
  avoid") is applied as a catalog filter *before* the prompt is built —
  matching exercises are simply never in the list the model sees, never
  passed as context inviting it to reason about why. Equipment filtering
  reuses the same §15.1 rule. System prompts explicitly forbid medical/
  injury/diagnostic advice and instruct omitting speculative rationale
  rather than inventing one.
  - Plan generation (§17D.2): profile + filtered catalog → structured JSON
    proposal → `PlanValidator` → `applyPlan()` writes through the normal
    `RoutineRepository`/`PlanRepository`, identical to a manual save.
  - Plan revision (§17D.3): on-request only (no automatic weekly-cadence
    scheduler was added — a future `WorkManager` job could call this the
    same way §14's reminder worker calls `PlanRepository`, but wasn't built
    here). Requires ≥4 logged sessions before attempting one — not enough
    signal otherwise. Input: sets-per-muscle-group in the last 28 days,
    muscle groups untrained >14 days (reusing Phase C's `MuscleFatigue`),
    body-weight trend, session count. Output is a diff (`PlanDiffItem`:
    swap/adjust-volume/add), each item independently checkbox-acceptable in
    the UI; only checked items are ever applied. Applying a swap/adjust
    needed a real DAO fix along the way — `RoutineDao.insertRoutineExercises`
    is a plain `@Insert` (Room's default ABORT-on-conflict), which would
    have thrown trying to "insert" a row whose id already exists; added
    `updateRoutineExercise` (`@Update`) instead.
  - §17D.5 failure handling: identical posture to §9.5 — unreachable
    endpoint, malformed/non-JSON response, or a validator result with zero
    surviving routines all produce a designed "Coach unavailable" state
    (`CoachUiState.Unavailable`) rather than a crash or an infinite spinner,
    and the app is fully usable with the Coach off or failing.
- **Found and fixed while wiring this in**: `RoutineRepository.saveRoutine`
  never had a `progressionRule` parameter (added in Phase C's schema but not
  threaded through) — the Coach's first draft accidentally passed the
  progression rule string into the `description` field. Fixed by adding an
  optional `progressionRule: String = "NONE"` parameter (default preserves
  every existing call site's behavior) and correcting `PlanShareRepository`
  (§17B), which had the same latent gap — an imported shared routine was
  silently losing its progression rule.
- **`CoachScreen`**: off by default (§17D.1, `PreferenceManager.
  aiCoachEnabled`), toggle at the top; with it off, generation/revision UI
  doesn't render at all and a line states the app behaves exactly as
  everywhere else. Proposal review states plainly "generated and
  unreviewed" and that progression stays deterministic regardless (§17D.4's
  literal wording). Linked from Profile → "AI Coach".

Verified: `./gradlew assembleDebug testDebugUnitTest` — 43 unit tests total
now (26 progression + 6 FitNotes + 2 name-matching + 9 plan-validator), all
passing.

## Phase H detail

**Honest framing up front**: §18's own verification steps are physical —
"test in poor lighting," "deliberately rack the bar mid-set," "one full
45-60 minute session," "run a set with a deliberate form fault." This
environment has no `adb`, no emulator, no camera, no device (confirmed
early in this build). None of that verification was performed, and nothing
below should be read as claiming it was. What follows is the code-level
work that these checks exist to verify — done because it's real,
independently-valuable engineering, not as a substitute for the physical
pass.

- **§18.1 pose confidence**: there was no landmark-visibility gating
  anywhere before this — rep counting trusted every MediaPipe result
  unconditionally. Added `PoseUtils.isConfident`/`allConfident` (reads
  `NormalizedLandmark.visibility()`, an `Optional<Float>` in this MediaPipe
  version — confirmed by compiling, not by documentation I don't have
  access to verify against). Wired into both `processSquat` and
  `processPushup`: below the 0.6 confidence threshold on any key landmark,
  rep counting is suspended and `_formFeedback` shows "Move fully into
  frame" instead of advancing the state machine on bad data.
- **§18.2 rep segmentation**: found that leaving the camera's frame
  entirely (`result.landmarks().isEmpty()`) was previously a silent no-op —
  the rep state machine kept whatever state it was in, so returning to
  frame after being gone could combine a stale mid-rep state with a fresh
  landmark position. Added `resetRepIfStuck()`: any rep sitting mid-motion
  (racked, paused, out of frame) for more than 4 seconds aborts back to
  `START` and is discarded, never counted — called both when landmarks
  vanish and on every frame while actively tracking, so a long pause with
  the person still in frame is caught too, not just leaving entirely.
- **§18.3 live-cue/post-set agreement**: reviewed rather than changed. The
  torso-lean checks already share the same threshold and the same
  `minBackAngleDeg` field on both paths (`backAngle < 45` live,
  `minBackAngleDeg < 45.0` in `PostSetAnalysisRunner.detectIssueKeys`) — no
  divergence found. Depth can't produce a live/post-set contradiction
  either: the state machine structurally cannot complete a rep without
  passing through the depth threshold (`DOWN` only reaches `BOTTOM`, and
  therefore `UP`, below that same angle), so an insufficiently-deep
  attempt now times out via §18.2's new stuck-rep reset rather than
  completing — meaning post-set analysis never sees a rep the live path
  didn't also implicitly gate on depth. The "manual verification pass
  across each supported exercise" the spec asks for still needs a real
  camera and a real person — not attempted.
- **§18.4 thermal**: identified the mitigation spec asks for (reduce
  `ImageAnalysis` frame rate when pose detection isn't needed) but did
  **not** implement it — the CameraX/MediaPipe `ImageProxy` close lifecycle
  in `LIVE_STREAM` mode isn't something I could verify without a device,
  and guessing at it risks stalling the analyzer pipeline or leaking a
  frame in code I can't test. Flagged rather than shipped. Battery
  measurement over a real 45-60 minute session wasn't attempted (no
  device).
- **§18.5 empty states**: audited every screen via code reading (this part
  *is* fully achievable without a device). Found and fixed two real
  violations:
  - `HistoryScreen` rendered **four hardcoded fake sessions
    unconditionally** ("Aug 22, Squat, 12 reps", ...) — a fresh install
    with zero logged sets showed fabricated history indistinguishable from
    real data, a direct violation of "never zeros or fabricated data that
    could be mistaken for real." Rewired to `ExerciseSessionRepository`
    with a genuine "No sessions logged yet" empty state.
  - `RoutineDetailScreen` had no empty state for a routine with zero
    exercises — would have rendered a blank scroll region. Added a
    designed "No exercises in this routine yet" state.
  Also added smaller polish empty-states that weren't outright violations
  (nothing was blank or fake) but were missing a designed message:
  `ExerciseLibraryScreen` (search/filter with zero matches),
  `WeeklyPlanScreen`'s Routines section (no routines yet). `ProfileScreen`'s
  PR section already correctly hides itself rather than showing anything
  when empty — left as-is.

Verified: `./gradlew assembleDebug testDebugUnitTest` — full pipeline, all
43 unit tests still passing. **Not verified**: anything requiring a
physical Android device or camera.

---
*This file is a working log, not a spec — the spec lives in `FUNCTIONAL_SPEC.md`
and is not modified as phases complete.*
