# Plan 5a: Spin the Bottle Sectors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The bottle stops at a truly random angle and picks the player whose sector it points into; the circle shows one sector per player.

**Architecture:** The pure reducer draws the stop angle and maps it to a sector (`sectorAt`); `SpinState.lastAngle` carries it to the UI and, online, through the published session state. `BottleCircle` draws the sectors and both Spin screens animate to `lastAngle`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Canvas), kotlinx.serialization, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-24-spin-sectors-design.md`

## Global Constraints

- Sector *i* is centred on `playerAngleDegrees(i, N)` (player 0 at the top, clockwise) and spans ±180°/N; an angle exactly on a border belongs to the next sector clockwise.
- Stop angle: `random.nextInt(3600) / 10f`, in `playerAngleDegrees` terms, normalised to [0, 360).
- Landing in the spinner's own sector → `ReadyToSpin(landedOnSelf = true)` (unchanged rule).
- `SpinState.lastAngle: Float? = null` added; `lastTarget` kept. Older published states without `lastAngle` still decode (default null) and animate to the player's centre.
- CLAUDE.md git rule: Claude never commits. After each task: summary + Conventional Commit message, then wait for Mehmet's commit.

## Review Focus

1. **2 players** → two half-circle sectors; both sectors reachable; borders at left/right (`sectorAtTwoPlayers`).
2. **Angle exactly on a border** → next sector clockwise, never out of range (`borderBelongsToNextSector`).
3. **Angle near 360°/0° wrap** (e.g. 359.9°) → the sector around the top/its neighbour, not index N (`wrapAroundTop`).
4. **12 players** → narrow sectors still map correctly at their centres and edges (`twelvePlayersCentresAndEdges`).
5. **An online state published by the old version** (no `lastAngle`) → decodes, bottle falls back to the player's centre (`oldSessionWithoutAngleDecodes`).

---

### Task 1: Sector math and the random stop angle

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/spinthebottle/BottleCircle.kt` (add `sectorAt` next to `playerAngleDegrees`), `SpinTheBottle.kt` (`lastAngle`), `SpinTheBottleReducer.kt` (`spin`)
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/spinthebottle/BottleCircleTest.kt`, `SpinTheBottleReducerTest.kt`, `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/online/HostRefereeTest.kt` (only if its spins rely on the old draw)

**Interfaces:**
- Produces: `fun sectorAt(angleDegrees: Float, count: Int): Int` (angle in `playerAngleDegrees` terms, any real value); `SpinState.lastAngle: Float?`.

- [ ] **Step 1: Write the failing tests** (BottleCircleTest)
  - `sectorCentresMapToTheirPlayer`: for N = 4, `sectorAt(playerAngleDegrees(i, 4), 4) == i` for i in 0..3.
  - `sectorAtTwoPlayers`: `sectorAt(-90f, 2) == 0`, `sectorAt(90f, 2) == 1`, `sectorAt(-1f, 2) == 0`, `sectorAt(1f, 2) == 1`.
  - `borderBelongsToNextSector`: N = 4, border between 0 and 1 is at -45° → `sectorAt(-45f, 4) == 1`.
  - `wrapAroundTop`: N = 4 → `sectorAt(269.9f, 4) == 0`, `sectorAt(-90f + 360f, 4) == 0`, `sectorAt(224.9f, 4) == 3`.
  - `twelvePlayersCentresAndEdges`: N = 12, centre of 11 (`playerAngleDegrees(11, 12)`) → 11; that centre + 14.9° → 11; + 15° → 0.
  Reducer (SpinTheBottleReducerTest), with `SequenceRandom` values now in tenths of a degree (4 players; player 2's centre is 90° → `900`):
  - update `spin_landsOnOtherPlayer_asksThem` to `SequenceRandom(900)` → `Asking(0, 2)`, `lastTarget == 2`, `lastAngle == 90f`.
  - update `spin_landsOnSpinner_spinsAgain` to `SequenceRandom(2700)` (player 0's centre, 270°).
  - `spinCanStopOffCentre`: `SequenceRandom(1300)` (130°, inside player 2's sector 45°..135°) → `lastTarget == 2`, `lastAngle == 130f`.
  - update every other test that spins to a tenths value inside the intended player's sector (e.g. player 1 → `0`, player 3 → `1800`).
  Serialization (HostRefereeTest or a new `SpinStateSerializationTest` in online/): `oldSessionWithoutAngleDecodes` — `decodeSession` of the JSON of a Spin session with `"lastAngle"` removed gives `spin.lastAngle == null` and the same `lastTarget`.
- [ ] **Step 2: Run and see them fail**: `.\gradlew.bat :shared:testAndroidHostTest -q` → FAIL (unresolved `sectorAt`, `lastAngle`; updated reducer tests assert the new values).
- [ ] **Step 3: Implement** `sectorAt` (normalise `angle + 90 + 180/N` into [0, 360), divide by 360/N, floor, `% N`); `lastAngle` with default `null`; `spin` draws `angle = random.nextInt(3600) / 10f`, `target = sectorAt(angle, N)`, sets both. Fix any other test that relied on `nextInt(players.size)` for spins (HostRefereeTest's spin tests use real `Random(7)` — they assert spinCount/spinner, not targets; adjust only if they fail for that reason).
- [ ] **Step 4: Run** `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → all pass (211 + new tests), build OK.
- [ ] **Step 5: Stop for Mehmet's commit** — `feat: pick the spin the bottle player by a random stop angle`

### Task 2: Sectors on the circle and animating to the angle

**Files:**
- Modify: `game/spinthebottle/BottleCircle.kt` (draw sectors), `game/spinthebottle/SpinTheBottleScreen.kt` (local animation target), `ui/online/OnlineGames.kt` (online animation target)

**Interfaces:**
- Consumes: `sectorAt`, `SpinState.lastAngle` (Task 1).
- Produces: `BottleCircle(players, spinner, answerer, rotation, flickEnabled, onFlick, modifier)` — signature unchanged.

- [ ] **Step 1: Sectors** in `BottleCircle` (spec "Look"): a `Canvas` behind the names fills the circle (radius = 0.92 × half width) with N wedges alternating `ClinkColors.WallRaised` / `ClinkColors.Wall` (with N odd, the last wedge uses a third tone `WallRaised` at 60 % so no two neighbours match), `Paper` 14 % dividers 1.5 dp, an outer ring; spinner wedge tinted `Acid` at 22 %, answerer wedge `Pink` at 28 %. Wedge *i* spans `playerAngleDegrees(i, N) ± 180/N` (Compose `drawArc` uses the same 0° = 3 o'clock, clockwise convention). Names stay where they are (sector centres at 0.8 × radius) and keep the chip highlight.
- [ ] **Step 2: Animate to the angle**: in both screens the end rotation becomes `spinEndRotation(rotation.value, state.lastAngle ?: playerAngleDegrees(target, n), turns)`; the restored start rotation (after recreation) uses `lastAngle` the same way.
- [ ] **Step 3: Verify**: `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → pass, build OK. Visual check needs a signed-in phone (the emulator can't get past sign-in): with Mehmet's OK in that turn, on his S22 play a local Spin with 2, 5 and 12 players — sectors visible, bottle stops off-centre, the highlighted sector matches the asked player; then one online spin with the emulator as guest (same angle on both).
- [ ] **Step 4: Stop for Mehmet's commit** — `feat: draw a sector per player on the spin the bottle circle`
