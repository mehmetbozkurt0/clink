# Plan 5b: Beer Pong 3D Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Beer Pong's 2D throw model with real 3D ball physics and a 3D-looking table, cups and ball, the same locally and online.

**Architecture:** A pure, deterministic physics model in shared Kotlin (`simulateThrow`) decides every throw and returns its flight path; the reducer stores it in `lastThrow`, so online the host publishes the path and every phone replays it. A camera class projects world points to the canvas for drawing and casts the finger's flick back onto the table for aiming. The table is drawn with Compose Canvas (no engine).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Canvas, kotlinx.serialization, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-24-beer-pong-3d-design.md`

## Global Constraints

- All values in spec §5.1–5.5 are binding (gravity 980, ball radius 2, cups 5.0/3.5/12 at the six positions, table 30.5 × 262, rest spot (0, 100, 2), pitch 50°, speed 300…400 cm/s, step 1/240 s, 60 samples/s, rim tube 0.3, "over the opening" = horizontal < 5.0 − 0.6, rim bounce 0.35 normal / 0.9 sliding, inner wall 1.5, outer wall 1.4, table ×0.88 / ×0.6 / hop stop 60 / friction 1.2 s⁻¹ / stop < 20 cm/s, end at 4 s, 40 cm below, 80 cm past; camera (0, 40, 85) → (0, 215, 0), focal 2.0 × width, horizon 30 % of height; flick: lower 45 %, 0.06 s look-ahead, ≥ 300 dp/s, |yaw| ≤ 45°, 300…3000 dp/s → 300…400 cm/s; splash 260 ms, pause 400 ms; wobble ±0.9 cm over 18 samples).
- Rules unchanged: 1v1, one throw per turn, 6 cups each, first to lose all cups loses and draws a penalty.
- Cups are empty (brand rule); graffiti palette only (`ClinkColors`); no new dependencies.
- Online the host publishes the path; phones never re-simulate a throw they replay.
- CLAUDE.md: Claude never commits; after each task a summary + Conventional Commit message, then wait for Mehmet's commit.

## Review Focus

1. **A slow throw that lands short and rolls** → the flight ends (stops, or at 4 s), never loops — `slowRollerEnds` (Task 1).
2. **A non-finite or out-of-range throw from a member** → ignored or clamped, never a broken published state — `nonFiniteThrowIsIgnored`, `outOfRangeThrowIsClamped` (Task 2).
3. **The longest possible flight published online** → the state stays well under RTDB's practical size — `longestFlightFitsInPublishedState` (Task 2).
4. **A rim bounce toward a cup already removed** → no hit on a removed cup — `removedCupsAreIgnored` (Task 1).
5. **Matches that quietly get longer after a physics tweak** → caught by `averagePlayerClearsSixCupsWithin35Throws` (Task 3).

---

### Task 1: 3D physics, flick and table (local play)

**Files:**
- First: revert the spike. Save copies of `game/beerpong/Pong3d.kt` and `BeerPongTable3d.kt` outside the repo for reference, delete them, and restore `BeerPong.kt`, `BeerPongReducer.kt`, `BeerPongScreen.kt`, `BeerPongTable.kt`, `ui/navigation/ClinkNavHost.kt` to their committed content (`git show HEAD:<path>`; never `git checkout`).
- Replace: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/beerpong/BeerPongPhysics.kt` (3D model), `BeerPongTable.kt` (3D drawing)
- Create: `game/beerpong/BeerPongCamera.kt`
- Modify: `BeerPongReducer.kt`, `BeerPongScreen.kt`, `ui/online/OnlineGames.kt` (replay timing only)
- Test: rewrite `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/beerpong/BeerPongPhysicsTest.kt`; update `BeerPongReducerTest.kt`; create `BeerPongCameraTest.kt` and test helper `ThrowAim.kt` (same test package)

**Interfaces:**
- Produces:
  - `data class Vec3(val x: Float, val y: Float, val z: Float)` with `plus`, `minus`, `times(Float)`, `dot`, `cross`, `length()`, `normalized()`.
  - `object BeerPongPhysics` with every §5.1/5.2 constant, `val cups: List<Vec3>` (ids as today), `val ALL_CUPS: Set<Int>`, `val REST: Vec3`, `fun cupRadiusAt(z: Float): Float`.
  - `@Serializable data class ThrowInput(val yawDegrees: Float, val speed: Float)`
  - `@Serializable data class RimContact(val sample: Int, val cup: Int)`
  - `@Serializable data class ThrowResult(val input: ThrowInput, val cupHit: Int?, val touchedRim: Boolean, val rimContacts: List<RimContact>, val path: List<Int>)` — `path` is x, y, z per sample in whole tenths of a cm; `fun points(): List<Vec3>`; `val durationMillis: Int get()` = (samples − 1) × 1000 / 60, at least 1.
  - `fun simulateThrow(input: ThrowInput, remainingCups: Set<Int>): ThrowResult`
  - `class BeerPongCamera(width: Float, height: Float)` with `val focal: Float`, `fun project(p: Vec3): Triple<Float, Float, Float>` (screen x, screen y, depth), `fun onTable(screenX: Float, screenY: Float): Vec3?`.
  - `fun flickToThrow(camera: BeerPongCamera, releaseX: Float, releaseY: Float, velocityX: Float, velocityY: Float, speedDpPerSecond: Float): ThrowInput?`
  - `BeerPongTable(cups: Set<Int>, flight: ThrowResult?, progress: Float, splash: Float, flickEnabled: Boolean, onFlick: (ThrowInput) -> Unit, modifier: Modifier)` — same signature as today; the splash cup is `flight.cupHit`.
  - Test helper `fun aimAt(target: Vec3, speedFactor: Float = 1f, sideOffset: Float = 0f): ThrowInput` — yaw from `REST` to the target (shifted sideways by `sideOffset` cm), speed found by bisection so the arc comes down through rim height at the target's distance, times `speedFactor`.

- [ ] **Step 1: Revert the spike** as listed under Files; `git status` shows no spike files; the app still builds (`.\gradlew.bat :androidApp:assembleDebug -q`).
- [ ] **Step 2: Write the failing tests**
  `BeerPongPhysicsTest` (all with `val all = BeerPongPhysics.ALL_CUPS`):
  - `straightThrowAtTheFrontCupGoesIn`: `simulateThrow(aimAt(cups[5]), all).cupHit == 5`.
  - `everyCupIsReachable`: for id in 0..5, `simulateThrow(aimAt(cups[id]), setOf(id)).cupHit == id`.
  - `tooShortMisses`: `simulateThrow(aimAt(cups[5], speedFactor = 0.85f), all).cupHit == null`.
  - `tooLongMisses`: `simulateThrow(aimAt(cups[1], speedFactor = 1.25f), all).cupHit == null`.
  - `ballOverTheOpeningDropsIn`: `simulateThrow(aimAt(cups[5], sideOffset = 3.5f), setOf(5)).cupHit == 5`.
  - `nearRimHitIsRecorded`: `aimAt(cups[5], sideOffset = 5.3f)` with `setOf(5)` → `touchedRim` and `rimContacts.first().cup == 5`.
  - `rimBounceCanEndInAnotherCup`: over yaw offsets −3°..3° step 0.25 and speed factors 0.96..1.04 step 0.01 aimed at each cup, at least one result has `cupHit != null && rimContacts.any { it.cup != cupHit }`.
  - `removedCupsAreIgnored`: `simulateThrow(aimAt(cups[5]), all - 5).cupHit != 5`.
  - `offTheSideTheBallFalls`: `simulateThrow(ThrowInput(40f, 400f), all)` → `cupHit == null`, last point z < 0.
  - `slowRollerEnds`: `simulateThrow(ThrowInput(0f, 300f), all)` → `durationMillis <= 4000`, `cupHit == null`.
  - `sameThrowSameResult`: two runs of `aimAt(cups[3])` are equal.
  - `pathIsInTenthsOfACentimetre`: `path.size % 3 == 0`; for a hit on cup 5, `points().last()` is within 5 cm of `cups[5]` horizontally.
  `BeerPongCameraTest` (camera 1000 × 1600; the ball's screen point `b = project(REST)`):
  - `straightUpFlickAimsStraight`: `flickToThrow(cam, b.x, b.y, 0f, -2000f, 1500f)!!.yawDegrees` within 0.5 of 0.
  - `rightwardFlickAimsRight`: velocity (300, −2000) → yaw > 0.
  - `slowFlickIsNotAThrow`: speed 299 → null.
  - `downwardFlickIsNotAThrow`: velocity (0, 500) → null.
  - `tooSidewaysIsNotAThrow`: velocity (2000, −200) → null.
  - `speedMapsToTheBand`: 300 → 300, 3000 → 400, 6000 → 400 (± 0.01).
  - `projectionRoundTrips`: `onTable` of `project(cups[5])`'s screen point is within 0.1 cm of `cups[5]`.
  Update `BeerPongReducerTest`: every `at(x, y)` aimed at a cup becomes `aimAt(BeerPongPhysics.cups[id])` for that cup; misses use `ThrowInput(0f, 300f)`.
- [ ] **Step 3: Run and see them fail**: `.\gradlew.bat :shared:testAndroidHostTest -q` → compile errors (unresolved `Vec3`, `yawDegrees`, `BeerPongCamera`, `aimAt` …).
- [ ] **Step 4: Implement** physics per spec §5.2 (rim before inside before outer wall per cup in ascending id, then the table; record a `RimContact` per new contact at the current sample index; a hit adds 6 sinking samples toward the cup's centre), camera and flick per §5.3–5.4, and the 3D `BeerPongTable` per §5.4 (painter's order, contact shadows, ball hidden under the table, wobble from `rimContacts`). Reducer: `simulateThrow(input, defenderCups)`; screens: replay over `result.durationMillis` with `LinearEasing`, "rim" text when `touchedRim`, splash 260 ms, pause 400 ms — local `BeerPongScreen` and `OnlineBeerPong` alike. Delete the old 2D code (`TablePoint`, `TableProjection`, old `flickToThrow`).
- [ ] **Step 5: Run** `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → all tests pass (old 2D physics tests replaced), build OK.
- [ ] **Step 6: Check on a device**: a local match needs a signed-in phone, so ask for Mehmet's OK and use his S22 (the emulator can't get past sign-in): cups and ball sit on the table, a flick follows the finger, a long throw disappears behind the table. Without his OK, say the visual check is still owed.
- [ ] **Step 7: Stop for Mehmet's commit** — `feat: play beer pong with real 3D ball physics`

### Task 2: Online — safe input and the published flight

**Files:**
- Modify: `game/beerpong/BeerPongReducer.kt` (input check)
- Test: `BeerPongReducerTest.kt`, `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/online/HostRefereeTest.kt`

**Interfaces:**
- Consumes: Task 1's `ThrowInput`, `ThrowResult`, `simulateThrow`.
- Produces: `fun ThrowInput.sanitized(): ThrowInput?` in `BeerPongPhysics.kt` — null if yaw or speed is NaN/infinite; else yaw clamped to ±45 and speed to 300…400.

- [ ] **Step 1: Write the failing tests**
  - `nonFiniteThrowIsIgnored` (reducer): `ThrowInput(Float.NaN, 350f)` and `ThrowInput(0f, Float.POSITIVE_INFINITY)` leave the state unchanged.
  - `outOfRangeThrowIsClamped` (reducer): `ThrowInput(80f, 5000f)` → `lastThrow!!.input == ThrowInput(45f, 400f)`.
  - `publishedThrowRoundTrips` (HostRefereeTest): a Beer Pong session after one `aimAt(cups[5])` throw from "a" → `decodeSession(encodeSession(s)) == s` and `lastThrow!!.path.isNotEmpty()`.
  - `longestFlightFitsInPublishedState` (HostRefereeTest): after `ThrowInput(0f, 400f)` (a long miss), `encodeSession(s).length < 12_000`.
  - Update existing Beer Pong tests there to the new `ThrowInput` meaning (e.g. `ThrowInput(0f, 350f)`).
- [ ] **Step 2: Run and see them fail**: `.\gradlew.bat :shared:testAndroidHostTest -q` → FAIL (NaN not ignored, no clamp).
- [ ] **Step 3: Implement** `sanitized()` and use it first in the reducer's throw handling (ignored → state unchanged).
- [ ] **Step 4: Run** the full suite + build → pass.
- [ ] **Step 5: Stop for Mehmet's commit** — `fix: clamp beer pong throws and publish the flight online`

### Task 3: Difficulty guard and the final check

**Files:**
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/beerpong/BeerPongDifficultyTest.kt`

**Interfaces:**
- Consumes: `simulateThrow`, `aimAt`, `BeerPongPhysics.cups`.

- [ ] **Step 1: Write the test** `averagePlayerClearsSixCupsWithin35Throws`: `Random(42)`; 300 racks; each throw aims at a random remaining cup with `aimAt(target)`, then adds Gaussian noise (yaw + N(0, 2.5°), speed × (1 + N(0, 0.035))) — Box–Muller on the seeded `Random`; at most 300 throws per rack; assert the average throws per rack ≤ 35.
- [ ] **Step 2: Run it** (a guard on what Task 1 built, so it is expected to pass at once): `.\gradlew.bat :shared:testAndroidHostTest --tests "*BeerPongDifficultyTest" -q` → PASS (the spike measured ≈ 31). If it fails, the physics differs from the spike: compare against spec §5.2 before touching the threshold.
- [ ] **Step 3: Full suite + build** → pass.
- [ ] **Step 4: Device check** with Mehmet's OK in that turn: on his S22 a local match (throws follow the flick, cups/ball sit on the table, misses vanish behind the table, pace quick); then online with the emulator as guest: both phones show the same flight and result.
- [ ] **Step 5: Stop for Mehmet's commit** — `test: guard beer pong difficulty with a simulated average player`
