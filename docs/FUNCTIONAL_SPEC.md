# Functional Design & Engineering Document
## FITBODY / Gym Form Coach — Android Application

**Scope:** Android application layer only. No backend, no server, no
multi-user accounts. Single-user, local-first, offline-capable.

**Stack:** Kotlin, Jetpack Compose, Material 3, Room (SQLite), CameraX,
MediaPipe Tasks Vision, WorkManager, Retrofit/OkHttp (local network only).

**Document purpose:** implementation-ready specification for all features
not yet built. Each section defines behavior, data model, edge cases, and
Android-specific implementation notes.

---

# 0. Foundational Conventions

These apply to **every** entity and screen introduced in this document.
Establish them before building anything else.

## 0.1 Entity conventions

Every Room entity that stores user data carries:

| Field | Type | Purpose |
|---|---|---|
| `id` | `String` | UUID, generated client-side at creation (`UUID.randomUUID().toString()`) |
| `createdAt` | `Long` | Epoch millis, set once at creation |
| `updatedAt` | `Long` | Epoch millis, refreshed on **every** write to the row |
| `syncStatus` | `String` | `"SYNCED"` / `"PENDING"` / `"FAILED"` — defaults to `"SYNCED"`; unused until a backend exists |
| `serverId` | `String?` | Nullable, always null for now |

**Rationale:** no backend exists today, but these are cheap now and
expensive as a retrofit migration later. UUIDs specifically prevent ID
collisions once any sync exists; auto-increment integers would collide.

**Critical distinction:** `createdAt` is *when the row was written*.
Any user-facing date (when a workout actually happened) is a **separate,
explicit field** (`performedAt` / `date`). Never conflate them — imported
historical data has a `createdAt` of today and a `performedAt` of years
ago, and every graph, calendar and heatmap must read `performedAt`.

## 0.2 Architecture conventions

- **Repository pattern is mandatory.** No ViewModel or Composable calls a
  Room DAO directly. All data access goes through
  `ExerciseRepository`, `RoutineRepository`, `SessionRepository`,
  `ProfileRepository`, `PlanRepository`. A future network layer is added
  *inside* the repository without touching any UI code.
- **ViewModel per screen**, exposing `StateFlow<UiState>`. Screens are
  stateless Composables driven by that state.
- **Migrations are real.** Never `fallbackToDestructiveMigration()` —
  local workout history is the only copy of the user's data. Every schema
  change ships a `Migration` object and bumps the DB version.
- **All heavy work off the main thread.** Room queries, CSV parsing,
  JSON export, and LLM calls run in coroutines on `Dispatchers.IO`.

## 0.3 Canonical units

- **Weight is stored in kilograms, always**, regardless of display
  setting. Conversion happens at the UI layer only.
- **Duration stored in seconds** (`Int`).
- **Distance stored in meters** (`Float`), converted at display.
- The LLM analysis payload (§9) always receives kg and seconds with
  explicit unit labels, never the user's display unit.

---

# 1. Exercise Type System

**This is the foundational change.** Four exercise behaviors currently
collapse into one weight/reps model. Everything downstream (logging UI,
progression, 1RM, import) branches on this.

## 1.1 Model

Add to the exercise catalog entity:

```kotlin
enum class ExerciseType { REPS, TIMED, CARDIO }
enum class LoadType { WEIGHTED, BODYWEIGHT }

// on Exercise:
val exerciseType: ExerciseType   // default REPS
val loadType: LoadType           // default WEIGHTED
val isUnilateral: Boolean        // default false
val isCustom: Boolean            // default false
```

Extend the set/session log entity to cover all four shapes:

```kotlin
// SetLog — one row per set performed
val id: String                    // UUID
val exerciseSessionId: String     // FK
val setIndex: Int                 // order within the session, 0-based
val weightKg: Float?              // null for bodyweight without added load, null for cardio
val addedWeightKg: Float?         // dip belt / vest on a bodyweight exercise
val reps: Int?                    // null for TIMED and CARDIO
val durationSeconds: Int?         // TIMED: time held. CARDIO: session duration
val distanceMeters: Float?        // CARDIO only
val effortValue: Int?             // §10, nullable
val effortScale: String?          // "RIR" | "RPE" | null
val isPr: Boolean                 // computed at write time, §8
```

Nullable-by-type is deliberate: a single table with type-gated nullable
columns is simpler to query for history/graphs than four parallel tables.
Validation happens at the repository layer, not the schema.

## 1.2 Behavior per type

| Type | Logging UI | Progression variable | Counts toward PR/1RM |
|---|---|---|---|
| `REPS` + `WEIGHTED` | weight + reps steppers | weight | Yes |
| `REPS` + `BODYWEIGHT` | reps stepper only; optional "add weight" affordance | reps (or weight if added load present) | PR yes (reps), 1RM only when loaded |
| `TIMED` | work timer + optional weight | duration (or weight if loaded) | PR yes (duration), 1RM no |
| `CARDIO` | duration + distance/speed | none | No |

## 1.3 Bodyweight specifics

- No weight column, no working-weight prompt on session start. One
  stepper: reps.
- An **added weight** affordance (collapsed by default) reveals a weight
  input. When populated for a set, that set is treated as weighted for
  progression and 1RM purposes.
- **Rep ceiling:** user-configurable per exercise (default 20). Once the
  target rep count would exceed the ceiling, progression adds a *set*
  instead of a rep. Past a second threshold (ceiling + 1 extra set), the
  UI surfaces static guidance: "Consider adding load or a harder
  variation." This is a UI hint, not an automated change.

## 1.4 Unilateral specifics

- User logs the **total** across both sides. Storage is the total.
- Display always shows the split: `16 total` renders as `8 per side`.
- Progression targets step in increments of **2**, so a target never
  lands on an odd number that can't split evenly.
- Odd values entered manually are accepted (user may genuinely have done
  8/7) but display as `8/7` rather than a clean "per side" figure.

## 1.5 Timed specifics

- The **work timer** is distinct from the rest timer (§7). It counts the
  set itself.
- Supports count-up (hold as long as possible) and count-down (hold to a
  target) modes; the target from progression determines default mode.
- Logs **actual** time achieved, not the target — a 60s target where the
  user stopped at 47s logs 47.
- Timed exercises may carry load (weighted plank, loaded carry): weight
  input remains, reps input is replaced by the timer.

## 1.6 Migration note

§1 changes touch the exercise catalog and the set log. **Ship all of §1
as a single Room migration**, before any of §2–§13 are built. Seeding:
classify all existing 113 catalog exercises with correct
`exerciseType` / `loadType` / `isUnilateral` values as part of the
migration — do not default everything to `REPS`/`WEIGHTED` and expect the
user to fix it.

---

# 2. Weekly Plan

## 2.1 Purpose

A recurring routine per weekday, so opening the app on a Tuesday surfaces
Tuesday's session without manual selection.

## 2.2 Model

```kotlin
// WeeklyPlanEntry — the recurring template
val id: String
val dayOfWeek: Int          // java.time.DayOfWeek value, 1=Monday..7=Sunday
val routineId: String?      // null = rest day

// PlanOverride — a one-off change for a specific calendar date
val id: String
val date: LocalDate         // stored as epoch day (Long) in Room
val routineId: String?      // null explicitly means "rest this date"
```

**Resolution order for "what is today's workout":**
1. `PlanOverride` for today's date, if one exists (including a null
   routineId, which means an explicit rest day)
2. Otherwise `WeeklyPlanEntry` for today's `DayOfWeek`
3. Otherwise: no planned workout

## 2.3 Rescheduling

Moving Wednesday's session to Thursday creates **two** overrides:
`PlanOverride(Wed, null)` and `PlanOverride(Thu, wedRoutineId)`. The
recurring `WeeklyPlanEntry` rows are never mutated by a reschedule —
next week returns to the normal pattern automatically.

## 2.4 UI

- **Home screen:** "Today's Workout" card as the primary surface — routine
  name, exercise count, estimated duration, single Start action. Rest day
  renders as a distinct, calm state, not an empty error.
- **Plan editor:** seven rows (Mon–Sun), each assigning a routine or rest.
- **Reschedule:** accessible from the Today card overflow — a date picker
  targeting where to move it, with a confirmation line stating the effect
  ("Moving Push Day to Thursday. This week only.").

## 2.5 Edge cases

- Routine deleted while assigned to a weekday → the assignment resolves
  to a rest day and the plan editor shows the slot as unassigned. Never
  crash on a dangling FK; use `ON DELETE SET NULL` semantics.
- Overrides older than today are never auto-deleted (they're history for
  the calendar), but they're excluded from future resolution by date
  comparison.

---

# 3. Guided Workout Session

## 3.1 Session start sequence

1. **Body weight prompt** — optional, skippable, pre-filled with last
   logged value. Skipping is a first-class action, not a nag.
2. **Session screen opens** with the routine's exercises in order, each
   showing its computed target (§6) with the reasoning line.
3. Each exercise's weight/reps inputs are **pre-filled from the
   progression engine**, which itself derives from the last logged
   session for that exercise.

## 3.2 Screen-awake behavior

- Apply `FLAG_KEEP_SCREEN_ON` (Compose: `WindowCompat` / a
  `DisposableEffect` that sets and clears the flag) for the duration of
  an active session **only**.
- Released the instant the session completes, is abandoned, or the screen
  is disposed. This must be in a `DisposableEffect` `onDispose` block —
  leaking this flag drains the battery silently.
- Settings toggle: "Keep screen on during workouts" (default on).

## 3.3 Session state persistence

An in-progress session must survive process death (Android will kill a
backgrounded app with an active camera). Persist an
`ActiveSession(id, routineId, startedAt, currentExerciseIndex,
completedSetIds)` row on every set completion. On app launch, if an
`ActiveSession` exists and started within the last N hours (suggest 6),
offer to resume it.

## 3.4 Interruption handling

- Incoming call / backgrounding during a camera set: pause pose analysis,
  release the camera, retain buffered rep metrics in the ViewModel *and*
  persist them. On return, resume with the buffer intact.
- Never silently discard a partially-completed set.

---

# 4. Supersets

## 4.1 Model

```kotlin
// on RoutineExercise:
val supersetGroupId: String?   // UUID shared by grouped exercises; null = standalone
val orderIndex: Int            // ordering within the routine
```

Exercises sharing a `supersetGroupId` form one superset. Ordering within
the group follows `orderIndex`.

## 4.2 Behavior

- During a session, when a superset group is reached, all its exercises
  are presented as one unit.
- **No rest timer fires between exercises within the group.**
- The rest timer fires once, after the final exercise in the group
  completes a round.
- A superset with 3 sets means: A1→B1→rest→A2→B2→rest→A3→B3→rest.
  Round-based, not exercise-based sequencing.

## 4.3 Routine builder UI

- Multi-select two or more adjacent exercises → "Group as superset".
- Grouped exercises render with a connecting bracket/rail on the left and
  a shared header (`Superset A`).
- Ungrouping and reordering must both work; reordering an exercise out of
  a group's contiguous block removes it from the group.

## 4.4 Constraints

- Superset members must be contiguous in `orderIndex`. Enforce at the
  repository layer on save.
- Cardio exercises cannot be superset members (no meaningful round
  structure) — filter them out of the grouping selection.

---

# 5. Progression Engine

**The most logic-dense feature in this document.** A progression rule
computes the next session's target from logged history. It must be
deterministic, instant, and explainable — no LLM involvement.

## 5.1 Interface

```kotlin
data class ProgressionTarget(
    val weightKg: Float?,
    val reps: Int?,
    val sets: Int,
    val durationSeconds: Int?,
    val isAmrapTopSet: Boolean,
    val reasoning: String        // shown verbatim in the UI
)

interface ProgressionRule {
    fun nextTarget(
        history: List<ExerciseSessionWithSets>,  // most recent first
        config: ProgressionConfig,
        exercise: Exercise
    ): ProgressionTarget
}
```

Assignment: `progressionRule` is set **per routine**, with an optional
**per-exercise override**. Resolution: exercise override → routine
default → `NONE`.

`ProgressionConfig` carries the tunables: increment size, deload
percentage, rep range bounds, stall threshold, AMRAP double-jump
threshold.

## 5.2 Rules

### 5.2.1 `NONE`
Pre-fills last session's values verbatim. Reasoning: "Same as last
session." This is the current app behavior and remains the default.

### 5.2.2 `LINEAR`
- Last session hit all target reps across all sets → add
  `config.incrementKg` (default 2.5kg upper body / 5kg lower body;
  make it per-exercise configurable).
- Missed reps on any set → repeat the same weight. **Never advance on a
  missed session.**
- Stall (`config.stallThreshold` consecutive missed sessions, default 3)
  → deload by `config.deloadPercent` (default 10%), reset stall counter.
- Reasoning examples: `"Hit 3×5 @ 60kg → +2.5kg"` /
  `"Missed reps last session → repeating 62.5kg"` /
  `"3rd stall → deload 10% to 56.5kg"`.

### 5.2.3 `GREYSKULL_LP`
- Final set is **AMRAP** (as many reps as possible). `isAmrapTopSet` =
  true, and the UI must visibly mark that set.
- AMRAP reps ≥ `config.doubleJumpThreshold` (default 10) → apply
  **double** the normal increment.
- AMRAP reps ≥ target but < threshold → normal increment.
- AMRAP reps < target → no advance; increment stall counter.
- Stall threshold reached → deload 10%, reset.
- Reasoning: `"AMRAP hit 11 reps (≥10) → double jump +5kg"`.

### 5.2.4 `DOUBLE_PROGRESSION`
- Operates within a rep range (`config.repRangeMin`..`repRangeMax`,
  e.g. 8–12) at a fixed weight.
- All sets hit `repRangeMax` → increase weight by increment **and** reset
  target reps to `repRangeMin`.
- Otherwise → same weight, target = last achieved reps + 1, capped at
  `repRangeMax`.
- Reasoning: `"Hit 3×12 (top of range) → +2.5kg, back to 8 reps"`.

### 5.2.5 `TIME_BASED`
- Applies to `TIMED` exercises. Progresses `durationSeconds` by
  `config.incrementSeconds` (default 5s) when the last target was met.
- Same stall/deload semantics, with deload reducing duration.

### 5.2.6 Bodyweight override
For `LoadType.BODYWEIGHT` with no added weight, **all rules progress reps
instead of weight**, subject to the rep ceiling logic in §1.3. When added
weight is present, the rule reverts to progressing weight normally. This
override applies regardless of which rule is selected.

## 5.3 The reasoning string

Every target displays *why* it is that number. This is non-negotiable —
a pre-filled number with no explanation is indistinguishable from a bug
and destroys trust in the engine. Keep it to one line, generated by the
rule itself, never by an LLM.

## 5.4 Edge cases

- **No history** (first time performing an exercise): return no target;
  the UI prompts for a working weight. Never guess a starting load.
- **Sparse history** (last performed 4 months ago): if the gap exceeds
  `config.staleAfterDays` (default 30), suggest the last weight minus
  one deload step, with reasoning naming the gap.
- **Exercise switched between bodyweight and loaded** across sessions:
  the rule reads each session's actual load state; don't assume
  continuity.
- Progression **never** reads effort ratings (§10) or LLM analysis (§9).

---

# 6. Estimated 1RM

## 6.1 Calculation

Epley: `1RM = weight × (1 + reps / 30)`

**Eligibility rules:**
- Only sets with `reps ≤ 12` are eligible. Beyond 12 reps the formula's
  error grows unacceptably — exclude, don't extrapolate.
- Only `WEIGHTED` sets (or bodyweight sets with `addedWeightKg`).
- `TIMED` and `CARDIO` never produce a 1RM.

The displayed estimate is the **maximum** across eligible sets in the
lookback window, and the UI **names the source set**: "Est. 1RM 102kg —
from 85kg × 6, 12 Aug".

## 6.2 Presentation

- Per-exercise 1RM curve over time (reuse the progressive-overload chart
  component; different data series).
- A standalone **calculator**: weight + reps inputs → estimated 1RM, for
  sets performed outside the app. Refuses (with a clear message) above 12
  reps rather than returning a bad number.

---

# 7. Rest Timer

## 7.1 Behavior

- Starts automatically on set completion (per §4, after the full superset
  round rather than between members).
- Presets: 60s / 90s / 120s / 180s, plus custom. Last-used value persists
  as the default; per-exercise defaults are a later enhancement, not
  required now.
- Controls: skip, +30s, pause.
- Completion: vibration always; sound respects system silent mode.

## 7.2 Background reliability

The timer **must** complete correctly when the app is backgrounded or the
screen is off. Do not rely on a Compose-side coroutine that dies with the
UI. Implementation: schedule the completion notification via
`AlarmManager` (`setExactAndAllowWhileIdle`) or a foreground service, and
drive the visible countdown from a wall-clock end timestamp rather than a
tick counter — so returning to the app after 40s shows 50s remaining, not
90s.

## 7.3 UI

Legibility from arm's length is the design constraint: largest type on
the screen, high contrast, minimal competing chrome.

---

# 8. Personal Records

## 8.1 Definition per type

| Exercise shape | PR criterion |
|---|---|
| Weighted reps | Highest weight at any rep count; **and** highest estimated 1RM |
| Bodyweight reps | Highest rep count in a single set |
| Timed | Longest duration held |
| Cardio | Excluded from PR tracking |

Compute at set-write time, set `isPr` on the `SetLog`, so history views
don't recompute on every render.

## 8.2 Presentation

- Inline celebration on the results screen: brief scale/glow pulse in the
  accent color. **Not** a blocking modal — the user is mid-workout.
- A "Personal Records" section on the Profile screen listing current best
  per exercise, with the date achieved.

---

# 9. Post-Set LLM Analysis

**This is the feature the project exists for.** Real-time cues (already
built) are per-rep and rule-based. This is the cross-rep, post-set
diagnosis.

## 9.1 Architecture

```
Set ends
  → rep metrics already buffered by the pose pipeline
  → rule engine classifies issues (existing logic, reused not duplicated)
  → matched issues looked up in correctives_kb.json
  → single request to local Ollama endpoint
  → structured response parsed → SetAnalysis persisted
  → analysis card rendered on results screen
```

**Runtime:** local Ollama (Qwen3.5-4B or similar) on the user's PC over
LAN. OpenAI-compatible endpoint:
`http://<configured-ip>:11434/v1/chat/completions`.

## 9.2 Knowledge base

Bundled asset `correctives_kb.json`, 15–20 entries covering the same
issue taxonomy the real-time feedback already detects — back rounding,
insufficient depth, knee valgus, excessive torso lean, bar path drift,
hip-rise timing, left/right asymmetry, tempo collapse.

```json
{
  "issue": "excessive_torso_lean_squat",
  "likely_causes": ["limited ankle dorsiflexion", "weak upper back/core"],
  "correctives": ["weighted ankle dorsiflexion stretch",
                  "goblet squat hold", "front squat"]
}
```

**The taxonomy must be shared** between the real-time rule engine and
this KB. Two separate vocabularies for the same phenomena would let the
live cue and the post-set analysis contradict each other.

## 9.3 Request payload

Every metric carries explicit units and semantics. Bare numbers force the
model to guess at biomechanical convention, which produces unreliable
output.

```json
{
  "exercise": "barbell_squat",
  "units": {"angles": "degrees", "duration": "seconds", "weight": "kg"},
  "metric_semantics": {
    "knee_angle_bottom_deg": "smaller value = deeper knee flexion",
    "torso_lean_deg": "larger value = more forward lean"
  },
  "reps": [
    {"rep": 1, "knee_angle_bottom_deg": 82, "torso_lean_deg": 38,
     "knee_lr_diff_deg": 4, "depth_ok": true, "tempo_down_s": 1.8}
  ],
  "matched_kb_entries": [ /* … */ ]
}
```

Set the model's **non-thinking mode** (`think: false` or equivalent).
This is synthesis over pre-structured data; visible chain-of-thought adds
latency and token cost with no quality gain here.

System prompt fixes: coach persona, strict JSON output schema, ≤100 words
of user-facing text.

## 9.4 Model

```kotlin
// SetAnalysis
val id: String
val exerciseSessionId: String
val mainIssue: String
val trendSummary: String
val correctives: List<String>     // Room TypeConverter to JSON string
val worstRepIndex: Int?           // §11
val clipPath: String?             // §11
```

## 9.5 Failure handling

The set and its raw metrics **always** persist, regardless of whether
analysis succeeds. Analysis is an enrichment, never a gate.

- Endpoint unreachable → designed inline empty state on the results card:
  "AI coach unavailable — check your local server connection." Never a
  spinner that never resolves, never a crash.
- Timeout: 30s ceiling.
- Malformed JSON response → retry once, then fall back to the rule
  engine's own issue classification rendered without LLM phrasing.

## 9.6 Server configuration

Settings field for the Ollama LAN IP, persisted, with a "Test Connection"
action showing reachable/unreachable plus last-successful timestamp.

**Known failure mode:** DHCP reassigns the PC's local IP, or the phone
joins a different network. A one-time test button masks this. Re-check
reachability on each session start and surface a stale-config warning
rather than failing at the moment analysis is requested.

---

# 10. Effort Rating (RIR / RPE)

- Optional third column per set: `effortValue: Int?` +
  `effortScale: String?`.
- **Off by default**, enabled in Settings, scale selectable (RIR = reps
  left in the tank; RPE = same judgement on a 10-point scale).
- Each set **retains the scale it was logged with**. Switching the
  Settings scale does not retroactively convert historical entries.
- **Strictly informational.** Nothing reads this value — not progression
  (§5), not 1RM (§6), not PR detection (§8), not the LLM payload (§9).
  This isolation is a deliberate design constraint.

---

# 11. Rep Video Clip Review

## 11.1 Behavior

- During a set, maintain a **short rolling frame buffer** (a few seconds).
  Never hold the full set's raw video in memory.
- On set end, the rule engine identifies the worst-form rep (largest
  deviation from thresholds — reuse existing scoring). That rep's frame
  window is written to app-private storage as a short clip.
- **One clip per set** (the worst rep only), not one per rep.
- Path stored on `SetAnalysis.clipPath`.

## 11.2 Storage discipline

- Auto-delete clips older than a configurable retention window (default
  30 days) via a periodic `WorkManager` job.
- Deleting a session deletes its clip. Verify the cleanup job actually
  runs — orphaned media files are a silent, growing bug.
- Show total clip storage used in Settings with a "clear all" action.

## 11.3 UI

Small thumbnail with a play affordance on the results card — a supporting
detail, not the headline. Tapping opens a simple full-screen player.

---

# 12. Recurring Weak-Point Tracking

- Query recent `SetAnalysis` rows grouped by `mainIssue`, scoped to an
  exercise or body part.
- An issue appearing in ≥3 of the last 5 sessions is flagged. **This is a
  frequency count, not an LLM call** — deterministic and instant.
- Surfaced as a "Recurring Focus Areas" card on the Progress screen:
  1–3 patterns, each with the count ("3 of last 5 sessions") and the
  corrective already associated with that issue in the KB.
- Optionally, one periodic (weekly, not per-set) LLM call may generate a
  richer narrative summary across sessions.

---

# 13. Visualization

## 13.1 Activity heatmap

- GitHub-style contribution grid, full-year view, horizontally
  scrollable.
- Cell shade = **time spent training** that day (not just presence).
  Cardio counts.
- Tap a cell → that day's sessions.
- Empty state (no history) renders as a muted outline grid with a
  caption, never as zeros or fabricated data.

## 13.2 Muscle map

- Front and back body diagram, muscle regions as independently colorable
  vector paths (static SVG/`VectorDrawable`, shaded programmatically).
- Shading = training volume per muscle group over week / month / all-time.
- **Names untrained groups** for the selected period — the actionable part.
- **Live preview in the routine builder**: shading updates as exercises
  are added, before the routine is saved.
- Post-session view: what was just trained.
- Male/female figure toggle in Settings.
- Requires a `muscleGroups: List<String>` + relative contribution mapping
  on each catalog exercise; seed this alongside §1's classification pass.

## 13.3 Progressive overload chart

Already built. Extend to plot the 1RM series (§6) as a selectable second
view.

---

# 14. Notifications

- **Rest timer completion** — fires with the app backgrounded or closed
  (§7.2).
- **Workout reminder** — optional, for days with a planned workout (§2)
  that has no logged session by a user-set time.
- Both opt-in, both configurable in Settings, both on a dedicated
  notification channel so the user can tune them at the OS level.
- Scheduling via `WorkManager` for the reminder (inexact, daily
  periodic); `AlarmManager` exact for the rest timer.

---

# 15. Equipment Filter & Custom Exercises

## 15.1 Equipment filter

- Filters the catalog by the equipment captured during onboarding.
- **Adaptive options:** filter combinations that would return zero results
  are not offered. Compute available options from the current filter
  state rather than showing a static list.
- Filter state is a browsing preference, not a hard restriction — an
  explicit "show all" escape hatch always exists.

## 15.2 Custom exercises

- Minimum viable input: name + body part. Equipment, type flags, and
  muscle-group mapping optional (defaulting to `REPS`/`WEIGHTED`).
- Free-text description in place of the animated demo built-ins carry.
- **Behave identically to built-ins everywhere** — routine builder,
  progression, 1RM, PR, history, muscle map. The only difference is
  `isCustom = true` and the absence of a demo animation.

---

# 16. Theming

- Existing dark theme with neon-green `#D0FD3E` accent remains the
  default identity.
- Add: a light theme variant, and 6–7 additional accent options.
- Persisted in profile settings, applied through the Compose
  `MaterialTheme` color scheme. All new UI must read theme colors, never
  hardcode hex values.

---

# 17. Data Portability

## 17.1 JSON export / import

- One-tap full export: routines, weekly plan, overrides, sessions, sets,
  custom exercises, body-weight log, PRs, settings. Single JSON file via
  Storage Access Framework (`CreateDocument`) — user picks the location.
- Import validates structure **before** any write, and warns explicitly
  before overwriting existing data.
- Include a schema `version` field in the export so future importers can
  handle older files.
- No telemetry, no external service. Local file only.

## 17.2 FitNotes CSV import

**Source format** (verify against a real export — column sets have varied
across FitNotes versions):

```
Date, Exercise, Category, Weight (kg), Weight (lbs), Reps,
Distance, Distance Unit, Time, Comment
```

- `Date`: `YYYY-MM-DD`.
- **Each row is one set**, not one workout. Group consecutive rows by
  `Date` + `Exercise` into sessions, preserving set order.
- Only one of `Weight (kg)` / `Weight (lbs)` is populated per row.
  Convert lbs → kg at import; never store mixed units.
- Rows with `Distance`/`Time` populated instead of `Weight`/`Reps` import
  as `CARDIO`, not as zero-weight strength sets.

**Exercise name matching:** case-insensitive, whitespace-trimmed, with
handling for common variants ("Bench Press (Barbell)" ↔ "Barbell Bench
Press"). **Unmatched names become custom exercises (§15.2) — nothing in
the file is ever dropped.**

**Flow:** SAF file picker → parse off-main-thread → preview screen
(workouts found, date range, matched vs. newly-created exercises) →
explicit confirm → progress indicator → completion summary including a
count of skipped/malformed rows.

**Duplicate handling:** a session already existing for the same date +
exercise is **skipped**, not merged or overwritten. Re-importing the same
file must be a no-op.

**Imported rows** get `performedAt` from the CSV `Date` and `createdAt`
of now (§0.1).

---

# 17A. Body-Weight Tracking with Goal

Body weight is currently captured only as a session-start prompt (§3.1).
It is a tracked metric in its own right and needs its own surface.

## 17A.1 Model

```kotlin
// BodyWeightEntry
val id: String
val weightKg: Float
val recordedAt: Long        // the date the measurement applies to
val source: String          // "MANUAL" | "SESSION_PROMPT" | "IMPORT"

// on UserProfile:
val goalWeightKg: Float?    // null = no goal set
```

One entry per day maximum. A second entry for the same date overwrites
the first rather than creating a duplicate — weighing twice in a morning
is a correction, not two data points.

## 17A.2 Chart behavior

- Line chart over time, with a **horizontal goal line** at
  `goalWeightKg` when set.
- Each point's delta from the previous entry is **colored by direction
  relative to the goal**, not by up/down in absolute terms. Losing 0.3kg
  is favorable when the goal is below current weight, and unfavorable
  when the goal is above it. Compute against goal direction; never
  hardcode "down = green".
- No goal set → neutral coloring throughout, no goal line, and a
  non-nagging affordance to set one.
- Range selector: month / 6 months / year / all.

## 17A.3 Entry points

- Session-start prompt (§3.1), pre-filled with last value, skippable.
- Manual entry from the Progress screen at any time, with a date picker
  so a missed day can be backfilled.
- Import (§17.2, §17C).

## 17A.4 Edge cases

- Sparse data (weekly weigh-ins, not daily): the chart connects actual
  entries; do not interpolate or forward-fill invented values.
- A goal already reached: keep the line, keep tracking. Do not
  auto-clear the goal or switch to a congratulatory mode that hides the
  data.

---

# 17B. Plan Sharing (File + PDF)

Distinct from the full-data export in §17.1. This shares **structure
only** — routines and the weekly schedule — with no logged workouts, no
body-weight history, and no personal metrics.

## 17B.1 Share as file

- Payload: routines, their exercises with target sets/reps/progression
  config, superset groupings, and the weekly plan assignment.
- **Explicitly excluded:** session logs, sets, PRs, body-weight entries,
  analysis records, settings. Verify this by test — leaking a training
  log into what the user thinks is a plan share is a privacy failure,
  not a bug.
- Format: JSON with a `schemaVersion` and a distinct `type:
  "PLAN_SHARE"` discriminator so the importer can reject a full-data
  export handed to the wrong entry point (and vice versa).
- Emitted via Android's share sheet (`ACTION_SEND` with a
  `FileProvider` URI), so it can go to any messaging app, email, or
  file storage.

## 17B.2 Import a shared plan

- **Merging is additive. The recipient's existing plan is never
  overwritten.** Incoming routines are added alongside what exists;
  name collisions get a suffix (`Push Day (imported)`), not a silent
  replace.
- Weekly-plan assignments from the incoming file are offered, not
  applied automatically — the user confirms which days to adopt.
- Unknown exercises in the incoming file resolve the same way as
  §17.2's importer: match against the catalog, create as custom
  exercises where unmatched, drop nothing.

## 17B.3 Print as PDF

- Renders the same plan structure as a clean, printable document:
  routine name, exercises in order, target sets/reps, superset
  groupings visually indicated.
- Implementation: `PrintManager` + `PrintDocumentAdapter` with a
  `PdfDocument` canvas, or render Compose content to a bitmap page.
  Prefer drawing the document directly rather than screenshotting UI —
  screen-sized screenshots print badly.
- Output is print-oriented: light background regardless of app theme,
  legible at A4, no accent-color-dependent information.

---

# 17C. Additional Importers (Strong, Hevy, Apple Health)

§17.2 covers FitNotes. These three follow the same architecture and
should reuse it rather than being written independently.

## 17C.1 Shared importer architecture

Build one pipeline with a pluggable parser per source:

```kotlin
interface WorkoutImporter {
    val sourceName: String
    fun canParse(header: String): Boolean
    fun parse(input: InputStream): ImportResult   // sessions + sets + unmatched names
}
```

`ImportResult` then flows through **one** shared path: exercise-name
matching → unit normalization → duplicate detection → preview screen →
commit. Only the parser differs per source. Writing four separate
end-to-end importers is the failure mode to avoid here.

## 17C.2 Per-source notes

**Strong** — CSV export. Row-per-set like FitNotes, but with its own
column names and a combined date-time field; workout name is a column,
which FitNotes lacks. Duration and distance columns exist for cardio.

**Hevy** — CSV export, row-per-set, includes workout title, exercise
notes, and an RPE column that maps directly onto §10's effort field
(`effortScale = "RPE"`).

**Apple Health** — **body weight only**, not workouts. The export is a
zip containing `export.xml`; parse `HKQuantityTypeIdentifierBodyMass`
records into `BodyWeightEntry` (§17A). This is a materially different
parser (XML, large file, streaming) — do not try to force it through
the CSV path. Stream-parse rather than loading the whole XML into
memory; a multi-year Apple Health export is routinely hundreds of MB.

## 17C.3 Verification requirement

Column names and formats for all three have shifted across app
versions. **Verify each parser against a real sample export before
finalizing it**; do not build against documentation alone. Where a real
sample is unavailable, ship the parser behind a clear "experimental"
label rather than guessing silently.

---

# 17D. AI Training Plan Coach

**Distinct from §9.** §9 analyzes *form* from camera data after a set.
This designs *programming* — which exercises, what volume, what
progression — and revises it from logged performance over time.

They share the LLM runtime (§9.6) and nothing else. Keep them separate
in code; conflating form analysis with programming decisions produces a
system where neither is debuggable.

## 17D.1 Scope and boundary

The Coach **proposes**; the user **disposes**. It never silently
mutates a routine or a weekly plan. Every output is a proposal the user
reviews, edits, and explicitly accepts or rejects.

**Off by default.** With the Coach disabled, the app behaves exactly as
specified everywhere else in this document — deterministic progression
(§5), manual routine building, no LLM involvement in programming.

## 17D.2 Plan generation

**Inputs** (all already collected):
- Profile: training experience, available equipment, split preference,
  session length preference, priority body parts (from onboarding)
- Catalog: available exercises filtered by equipment (§15.1)
- History, when it exists: recent volume per muscle group, current
  working weights, recurring weak points (§12)

**Output — a structured proposal, not prose:**

```json
{
  "planName": "Upper/Lower — 4 days",
  "rationale": "one short paragraph, user-facing",
  "weeklyPlan": [{"dayOfWeek": 1, "routineName": "Upper A"}],
  "routines": [{
    "name": "Upper A",
    "progressionRule": "DOUBLE_PROGRESSION",
    "exercises": [{
      "exerciseId": "<must be an id from the supplied catalog>",
      "sets": 3, "repRangeMin": 8, "repRangeMax": 12,
      "supersetGroup": null,
      "why": "one line"
    }]
  }]
}
```

**Hard constraint — exercise IDs must come from the supplied catalog.**
The model receives the filtered catalog in-prompt and may only reference
those IDs. Validate every returned ID against the catalog before
rendering the proposal; drop unrecognized entries and surface the count
("2 suggestions were skipped"). A model-invented exercise name that
silently becomes a custom exercise is a data-quality failure.

Validate equally: set counts within sane bounds, rep ranges ordered,
progression rule is one of the five defined in §5, day-of-week values
valid, no duplicate exercise within a routine.

## 17D.3 Plan revision

Runs **on request or on a slow cadence (weekly)** — never per session.
Programming decisions are made from trends, and a coach that rewrites
the plan after one bad session is worse than no coach.

Input: adherence (planned vs. logged sessions), progression stalls and
deloads per exercise (§5), recurring form issues (§12), muscle-group
volume distribution (§13.2), body-weight trend (§17A).

Output: a **diff**, not a replacement plan — "swap X for Y because of
recurring issue Z", "reduce squat volume, 3 stalls in 4 weeks", "add
posterior chain work, untrained 3 weeks". Each item independently
acceptable or dismissable. Applying a diff writes normal routine
changes through the existing repository, so nothing downstream needs to
know a Coach was involved.

## 17D.4 Safety and honesty constraints

- The Coach must not produce medical, injury-rehabilitation, or
  diagnostic guidance. Onboarding captures "movements to avoid" as
  exclusions; those are applied as a **filter on the catalog before the
  prompt**, never as context inviting the model to reason about why.
- Where the model's rationale would be speculative, prefer omitting it.
  A confident-sounding invented justification is worse than none.
- The proposal UI states plainly that this is generated and unreviewed,
  and that progression targets remain deterministic (§5) regardless of
  what the Coach proposed.

## 17D.5 Failure handling

Identical posture to §9.5: the Coach is an enrichment layer. Endpoint
unreachable, malformed JSON, or failed validation → a designed empty
state and the app continues fully functional with manual planning.
Never block routine creation on Coach availability.

---

# 18. Robustness Requirements

These are not features, but they determine whether the features work.

## 18.1 Pose detection confidence

MediaPipe returns a `visibility` score per landmark. The app currently
feeds results into rep counting regardless of confidence.

**Required:** when key landmarks for the current exercise fall below a
confidence threshold, surface an actionable overlay — "Move fully into
frame" / "More light needed" — and **suspend rep counting** rather than
logging garbage reps. Bad landmarks corrupt every downstream layer:
rep counts, metrics, LLM analysis, progression, PRs.

## 18.2 Rep segmentation edge cases

Explicitly test and handle: user racks the bar mid-set; a partial/aborted
rep; a long pause between reps; the user leaving frame and returning.
Each should either segment correctly or be excluded, never silently
produce a malformed rep.

## 18.3 Live cue vs. post-set analysis agreement

Both read the same metrics through different logic paths. They must not
contradict — a live cue reporting good depth while the post-set analysis
flags depth as the main issue destroys trust in both. Shared issue
taxonomy (§9.2) plus a manual verification pass across each supported
exercise.

## 18.4 Thermal and battery

Continuous camera + pose inference is demanding. Measure actual drain
across a realistic 45–60 minute session, not a two-minute demo. If
sustained inference causes thermal throttling, consider reducing
`ImageAnalysis` target resolution or frame rate during rest periods
(pose detection is not needed between sets).

## 18.5 Empty states

Every list, chart, and history view needs a designed empty state:
routines with no exercises, Progress before any session, PRs before any
record, exercise history before first performance, muscle map with no
data. Muted outline + caption, never zeros or placeholder data that could
be mistaken for real.

---

# 19. Build Order

Dependencies dictate sequence. Do not parallelize across phases.

| Phase | Contents | Rationale |
|---|---|---|
| **0** | §0 conventions: UUID keys, sync fields, `updatedAt`, repository refactor | Touches every existing entity. Everything after builds on it. One migration. |
| **A** | §1 exercise type system + catalog reclassification | Second migration. All logging, progression, and import branch on these flags. |
| **B** | §2 weekly plan → §3 guided session → §4 supersets → §7 rest timer | Session flow. Each depends on the previous. |
| **C** | §5 progression engine → §6 1RM → §8 PRs | Intelligence layer. Needs A's type flags and B's session data. |
| **D** | §9 LLM analysis → §11 clips → §12 recurring weak points | The core differentiator. §11 and §12 both require §9's `SetAnalysis`. |
| **E** | §10 effort, §13 visualization, §14 notifications, §15 equipment/custom, §16 theming, §17A body weight | Independent; any order. §13.2 muscle map needs A's muscle-group mapping. |
| **F** | §17.1 export/import, §17B plan sharing, §17.2 FitNotes, §17C other importers | Last deliberately — importers must target a settled schema. |
| **G** | §17D AI Coach | Needs C's progression config, E's muscle-group data, and D's §12 weak points as inputs. |
| **H** | §18 robustness pass | Continuous, but a dedicated hardening pass before calling it done. |

**Phase 0 and Phase A are both schema migrations touching existing user
data.** Verify after each that the app builds, runs, and preserves all
existing routines, sessions, and PRs. These are refactors; no existing
functionality may regress.

---

# 20. Build Process

How each phase is actually executed. This section is procedure, not
specification — it assumes the sections above define *what* to build and
answers *how* to sequence, verify, and avoid the failure modes specific
to this codebase.

## 20.1 Working rules that apply to every phase

**One phase per working session.** Do not start Phase B while Phase A is
unverified. Phases are ordered by hard dependency; skipping ahead means
building on a schema that is still moving.

**Every phase ends with a verification pass, not a build success.**
"It compiles" is not done. Done is: app launches, existing routines and
session history are intact, the previously-working flows still work, and
the new behavior does what §-spec says under both its happy path and its
named edge cases.

**Migrations are one-way in practice.** Before any phase containing a
schema change, export the current database from the device
(`adb exec-out run-as <package> cat databases/<db>` or the app's own
§17.1 export once it exists). A failed migration with no backup means
reconstructing test history by hand.

**Commit per task, not per phase.** Each numbered section is a separate
commit with the section number in the message (`feat(§4): supersets`).
When a later phase surfaces a regression, this makes bisecting tractable.

**Never regenerate a file wholesale to make a small change.** These are
large Compose screens; a full rewrite to add one field loses unrelated
work and is the most common way state handling silently breaks.

## 20.2 Phase 0 — Conventions and repository refactor

The riskiest phase, because it touches everything and delivers no
visible feature. Do it anyway; retrofitting it later costs several times
more.

**Order within the phase:**

1. **Inventory first.** List every Room entity, every DAO, and every
   place a DAO is called from outside a repository. Do this before
   changing anything — the refactor's scope is defined by that list.
2. **Introduce repository interfaces** for entities that lack them,
   wrapping existing DAO calls verbatim. No behavior change. Build and
   verify the app still works. Commit.
3. **Migrate ViewModels** off direct DAO access onto the repositories,
   one ViewModel at a time. Verify after each. Commit.
4. **Then** the schema change: UUID keys, `syncStatus`, `serverId`,
   `updatedAt`, and the `performedAt`/`createdAt` split (§0.1).

**The UUID conversion is the dangerous part.** Existing rows have
integer keys and integer foreign keys. The migration must:
- create new tables with `TEXT` primary keys
- generate a UUID per existing row
- **remap every foreign key** using the old→new integer→UUID mapping
- copy data across, then drop the old tables

Write this as an explicit `Migration` with hand-written SQL. Do not rely
on `AutoMigration` — it does not know how to remap keys.

**Verification:** seed the app with several routines, sessions, and PRs
*before* migrating. After migrating, every one of them must still be
present, correctly linked, and rendering in history and graphs. A
routine whose exercises vanished is a foreign-key remap that silently
failed.

## 20.3 Phase A — Exercise type system

1. Add the enum fields to the exercise catalog with safe defaults
   (`REPS`, `WEIGHTED`, `isUnilateral = false`).
2. **Reclassify the catalog as part of the migration**, not as a
   follow-up. All 113 exercises get correct type, load type, unilateral
   flag, and muscle-group mapping (§13.2 depends on the last one). This
   is data entry, and it is tedious; doing it now avoids a second
   migration when the muscle map is built in Phase E.
3. Extend the set log with the nullable type-gated columns (§1.1).
4. Branch the logging UI by type — this is where the four shapes become
   visible. Build one shape at a time: weighted reps (already works) →
   bodyweight → timed → cardio. Verify each in the running app before
   starting the next.

**Verification:** log one real set of each of the four shapes. Confirm
each appears correctly in history, that bodyweight sets show no weight
column, that a timed set stores actual held duration rather than the
target, and that a cardio set does not appear in PR or 1RM surfaces.

## 20.4 Phase B — Session flow

Build in strict order; each depends on the previous.

1. **§2 weekly plan** — model and plan editor first, then the Home
   "Today's Workout" card that reads from it. Test the resolution order
   (§2.2) explicitly, including an override that means "rest".
2. **§3 guided session** — body-weight prompt, pre-filled inputs,
   screen-awake flag. Then §3.3 session persistence.
   **Test process death directly:** start a session, background the app,
   force-stop it from the OS, relaunch. The in-progress session must be
   offered for resume with completed sets intact. This will not work by
   accident.
3. **§4 supersets** — model and builder grouping UI first; then the
   round-based session sequencing (§4.2), which is the part that is easy
   to get subtly wrong. Verify with a 2-exercise, 3-set superset that
   the rest timer fires exactly three times, after each round.
4. **§7 rest timer** — build the wall-clock implementation (§7.2) from
   the start. Do not build a tick-counter version intending to fix it
   later; the background behavior is the feature.

**Verification:** run one full guided session end to end, from Home card
through a superset, with rest timers, on a real device, with the screen
locking and the app backgrounding at least once mid-session.

## 20.5 Phase C — Progression and derived metrics

**Build the rules as pure functions, and test them without the UI.**
`ProgressionRule` implementations take history and config and return a
target; they touch no Android APIs. This makes them unit-testable, and
they are the one part of this app where a silent logic error produces
wrong numbers that look plausible.

1. Implement `NONE` and `LINEAR` first; wire them into session pre-fill;
   verify the reasoning string renders.
2. Add `GREYSKULL_LP`, `DOUBLE_PROGRESSION`, `TIME_BASED`.
3. Add the bodyweight override (§5.2.6) last, since it cross-cuts all
   rules.
4. **Write unit tests per rule** covering: target hit, target missed,
   stall reaching deload, no history, stale history. These five cases
   per rule catch essentially every progression bug worth catching.
5. Then §6 1RM (pure calculation, also unit-testable — test the 12-rep
   eligibility cutoff explicitly) and §8 PRs.

**Verification:** for each rule, hand-construct a short history, compute
the expected next target on paper, and confirm the app matches —
including the reasoning string. If the number and the explanation ever
disagree, the bug is in the rule, not the string.

## 20.6 Phase D — Camera analysis layer

1. **§9.2 knowledge base first.** Write `correctives_kb.json` before any
   integration code, using the *existing* real-time rule engine's issue
   identifiers as the keys (§18.3). If the live engine and the KB
   disagree on names, fix that now — it is the whole basis of the two
   layers agreeing.
2. Wire the Retrofit call, `SetAnalysis` persistence, and the results
   card.
3. **Build the failure states before the happy path is polished** —
   server unreachable, timeout, malformed JSON (§9.5). These are the
   states the user will actually hit first, given DHCP and wifi
   switching.
4. §11 clips, then §12 recurring weak points (which needs `SetAnalysis`
   rows to aggregate).

**Verification:** with the PC powered off, complete a set. The set and
its metrics must persist, and the card must show the designed
unavailable state. This is the single most important test in this
phase — analysis is an enrichment, and a set lost because a PC was off
is unacceptable.

## 20.7 Phase E — Independent features

No ordering constraint between them. Two notes:

- **§13.2 muscle map** depends on Phase A's muscle-group mapping. If
  that data entry was deferred, it lands here — do not start the map
  without it.
- **§14 notifications** must be tested with the app force-stopped, not
  merely backgrounded. Doze mode behavior differs between the two, and
  a rest-timer notification that only fires when the app is alive is the
  bug this section exists to prevent.

## 20.8 Phase F — Portability

Deliberately last, because an importer written against a moving schema
gets rewritten.

1. **§17.1 full export first.** It is also the backup mechanism for
   testing every importer safely.
2. **§17B plan sharing.** Test the exclusion list (§17B.1) by exporting
   a plan from an account with substantial history and inspecting the
   file — confirm no session, set, or body-weight data appears in it.
3. **§17.2 FitNotes**, building the shared pipeline (§17C.1) even though
   only one parser exists at that point.
4. **§17C other parsers** slotting into that pipeline.

**Get a real sample export for every source before writing its parser.**
Column names have drifted across versions of all four sources; parsing
from documentation alone reliably produces a parser that fails on the
first real file.

**Verification per importer:** import a real file, confirm the preview
counts match the file's contents, confirm re-importing the same file is
a no-op (§17.2 duplicate handling), and confirm unmatched exercise names
became custom exercises rather than being dropped.

## 20.9 Phase G — AI Coach

1. **Build the validator before the renderer** (§17D.2). Catalog-ID
   checking, bounds checking, and rule-name checking must exist before
   any proposal reaches the screen — an unvalidated proposal that
   creates invented exercises is a data-quality problem that outlives
   the feature.
2. Proposal review UI: every item independently acceptable or
   dismissable.
3. §17D.3 revision diffs last.

**Verification:** feed the generator a deliberately constrained profile
(bodyweight-only equipment, 15–30 minute sessions) and confirm every
proposed exercise is actually available under that constraint. Then
disable the Coach and confirm the app behaves identically to Phase F.

## 20.10 Phase H — Robustness

A dedicated pass, not something absorbed into feature work.

Work §18 top to bottom on a real device, in a real gym environment
where possible:

1. **§18.1 pose confidence** — test in poor lighting, at an angle, and
   partially out of frame. Rep counting must suspend, not produce
   garbage.
2. **§18.2 rep segmentation** — deliberately rack mid-set, do a partial
   rep, pause 30 seconds mid-set, walk out of frame and back.
3. **§18.3 agreement** — for each supported exercise, run a set with a
   deliberate form fault and confirm the live cue and the post-set
   analysis identify the same issue.
4. **§18.4 thermal** — one full 45–60 minute session with the camera
   active across multiple sets; measure battery drain and check for
   throttling.
5. **§18.5 empty states** — install fresh, and walk every screen before
   logging anything.

## 20.11 Definition of done

The app is complete when, on a fresh install with no data:

- every screen renders a designed empty state, never a crash or a zero
- a full session can be planned, guided, logged, analyzed, and reviewed
  without touching a settings screen mid-workout
- with the PC off and the phone in airplane mode, everything except §9
  analysis and §17D coaching works unchanged
- a full export, uninstall, reinstall, and import restores the app to
  its prior state exactly
