# Cup Pong: easier to score — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make about 40 % of a real player's throws land in a cup, instead of the 7 % the simulator scores today.

**Architecture:** Nothing about the physics, the table or the online path changes. The flick is mapped onto the ball speeds that can actually reach the cups, the cup's mouth is made forgiving enough to merge the two scoring stripes into one band, and the difficulty guard is rewritten to model a player who is guessing rather than one who knows the right answer. A faint trail behind the finger is added last.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.12.

**Spec:** `docs/superpowers/specs/2026-11-26-cup-pong-easier-design.md`

## Global Constraints

- Target: a simulated guessing player scores **about 40 %** of throws; the band that counts as done is 35–50 %.
- The gesture stays a flick: no slingshot pull, no drawn path, no preview of where the ball will land.
- The rules stay pure and deterministic; the host simulates and every phone replays the same flight.
- The trail never shows where the ball will go.
- Aim assist is a fallback, used only if the tuning alone cannot reach the target, and never more than a few degrees.
- No commits by the executor: Mehmet commits.
- Test runner: `scratchpad\suite.ps1 [-Filter "*X"] [-Build]`.

## Review Focus

1. **A full-force flick** (someone swipes as hard as they can) must still be a plausible throw that can score, not an automatic miss over the table. — Task 2.
2. **The gentlest accepted flick**, just above the throw threshold, must also be a plausible throw rather than one that dies on the near half. — Task 2.
3. **Online sanitising must match the new band**: a member's throw must not be clamped into a different throw than the one they made. — Task 2.
4. **The last cup standing**, front row or back row, must still be reachable within the band. — Task 1's player model covers both rows; Task 2 checks it.
5. **The trail must not interfere**: it never appears on the defender's half, never while the ball is in flight, and never swallows the flick itself. — Task 3.

---

### Task 1: An honest difficulty measure

**Files:**
- Modify: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/cuppong/CupPongDifficultyTest.kt`
- Delete: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/cuppong/TempHitWindowProbe.kt` (the throwaway probe, replaced by the test below)

**Interfaces:**
- Produces: a guessing-player model the later tasks tune against. The player picks a random remaining cup, takes `aimAt(cup)` as the intent, and misses it by **aim N(0, 4°)** and **strength N(0, 0.15) × (MAX_SPEED − MIN_SPEED)** — the error of a finger expressed as a share of what the control can express, not as a percentage of the right answer.

- [ ] **Step 1: Rewrite the guard's player and add the band check**

```kotlin
// replaces averagePlayerClearsSixCupsWithin35Throws
@Test fun aGuessingPlayerScoresAboutFortyPercent()   // 2000 throws at a full rack: hit rate in 0.35..0.50
@Test fun aGuessingPlayerClearsSixCupsWithin20Throws()  // 300 racks, average throws <= 20
@Test fun mostStraightFlicksScore()                  // 51 flick speeds across the accepted range, yaw 0: at least half score
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*CupPongDifficultyTest"`
Expected: all three FAIL — today the hit rate is near 0.07, the rack takes about 35 throws, and only 9 of 51 straight flicks score.

- [ ] **Step 3: Delete the throwaway probe** now its measurement lives in `mostStraightFlicksScore`.

### Task 2: Tune the flick and the cup's mouth

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/cuppong/CupPongPhysics.kt` (the speed band, the rim's forgiveness)
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/cuppong/CupPongCamera.kt:38-59` (`flickToThrow`)
- Modify: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/cuppong/CupPongCameraTest.kt:36-41,62-67` (the two tests that pin 300 and 400)

**Interfaces:**
- Consumes: Task 1's three failing tests as the target.
- Produces: `CupPongPhysics.MIN_SPEED` and `MAX_SPEED` narrowed to the band that reaches the cups, and a more forgiving cup mouth. `ThrowInput.sanitized()` keeps clamping to those same constants, so online stays consistent (Review Focus 3).

- [ ] **Step 1: Narrow the speed band** to the ball speeds that can reach the cups. Today's measurement says scoring lives between about 340 and 380 cm/s; the implementer confirms the ends by running Task 1's `mostStraightFlicksScore` and widening or narrowing until it passes. `flickToThrow` keeps mapping flick speed linearly onto the band, so no new mapping shape is needed. The band must still contain every speed the test helper `aimAt` solves for, front cup and back row alike, or the player model's intended throw gets clamped into a different one (Review Focus 4).
- [ ] **Step 2: Widen the cup's mouth** so the dead gap between the front cup and the back row closes: the "already over the opening" test in `simulateThrow` accepts a ball whose centre is further from the middle, and the rim's grab is softened. One constant each, named, not magic numbers inline.
- [ ] **Step 3: Run** `suite.ps1 -Filter "*CupPong*"`
Expected: Task 1's three tests PASS and every existing Cup Pong test still passes, except the two camera tests that pin the old 300/400 — update those to the new band's ends in the same step.
- [ ] **Step 4: Check the two ends of the flick** (Review Focus 1 and 2) by adding to `CupPongCameraTest`:

```kotlin
@Test fun aFullForceFlickCanStillScore()   // flick(0, -40_000, 14_000) -> simulateThrow sinks a cup
@Test fun theGentlestFlickCanStillScore()  // flick at the throw threshold -> the ball reaches the cups' half
```

- [ ] **Step 5: Aim assist, only if needed.** If `aGuessingPlayerScoresAboutFortyPercent` still sits below 0.35 after Steps 1–3, add at most a few degrees of pull toward the nearest cup in `flickToThrow`, with a named constant and a test pinning that a throw more than that far off is not moved. Skip this step entirely if the target is already met, and say so in the report.

### Task 3: The trail behind the finger

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/cuppong/CupPongTable.kt:46-72` (the drag handler and the canvas)

**Interfaces:**
- Consumes: nothing from Tasks 1–2.
- Produces: no new public API; the trail is local state in `CupPongTable`.

- [ ] **Step 1: Keep the finger's path** while a drag that started on the throwing half is in progress, as Compose state the canvas can read, cleared when the drag ends or is cancelled.
- [ ] **Step 2: Draw it** as a short fading streak along that path, under everything else the canvas draws, in a muted paper tint. It is drawn only while a drag is in progress, so it cannot appear during a flight or on a turn that is not yours (Review Focus 5); the existing `flickEnabled` guard already decides whether the gesture is accepted at all.
- [ ] **Step 3: Run** `suite.ps1 -Build`
Expected: `exit=0`, every test green, APK built.
- [ ] **Step 4: Emulator check** (`emulator-5554`): play a rack in a local two-player game — the trail follows the finger, fades, never survives the throw, and the flick still throws as before. Confirm the game now feels winnable rather than only measuring as such.
- [ ] **Step 5: Re-measure and report.** Run `suite.ps1` once more and quote the final hit rate and average rack length from Task 1's tests. If adding the trail changed nothing numerically, say so; the design asks for the check, not for a second round of tuning. Report the rulings, what was not verified, and one commit message: `fix: make Cup Pong throws land often enough to be fun`. The executor does not commit.
