# Plan 2: Cup Pong (local) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cup Pong playable 1v1 on one phone: flick the ball at the other player's 6 cups, a hand-written deterministic throw model, the loser draws a penalty, and with more than 2 players the winner stays on.

**Architecture:** Same pattern as Plan 1. The throw model (`flickToThrow`, `simulateThrow`) and the match rules (`CupPongReducer`) are pure Kotlin in `shared/commonMain` with no randomness in the throw, so in Plan 4 the referee can publish the `ThrowInput` and every phone replays the same result. Match picking (winner stays on) is a pure function too. The screen only draws the table, reads the flick and animates the `ThrowResult`.

**Tech Stack:** As Plan 1 (Kotlin Multiplatform, Compose Multiplatform, JetBrains Navigation/Lifecycle, kotlin.test). No new libraries.

**Spec:** `docs/superpowers/specs/2026-11-19-party-games-app-design.md` (§7.3 Cup Pong, local part; §9.3 motion and haptics; §10.5 store constraints)
**Roadmap:** `docs/superpowers/plans/2026-11-19-clink-roadmap.md` (this is Plan 2 of 5)

## Global Constraints

- **Git (CLAUDE.md, strict):** never run `git commit`, `git push`, `git rebase`, `git reset`, `git stash` or `git checkout -- <file>`. Every "Hand off" step means: stop, give Mehmet a short Turkish summary of what changed in which files and why, plus the commit message shown, and **wait for him to confirm he committed** before starting the next task.
- **Language:** talk to Mehmet in Turkish; code, identifiers, comments and commit messages in English.
- Android only; all code except `MainActivity` in `shared/src/commonMain`; no iOS target; only libraries that also support iOS.
- **1v1, one throw per turn, 6 cups per side in a 3-2-1 triangle** (spec §7.3). The player who loses all their cups loses and draws a **penalty** from the session's level.
- The throw is a **simple hand-written projectile model in shared Kotlin**, not a physics engine, and it is **deterministic**: the same `ThrowInput` and the same remaining cups always give the same `ThrowResult` (needed for Plan 4).
- **No drinking wording or imagery.** Cups are drawn without any drink in them (no amber/foam colors); a hit is a mint "splash" ring. "Cup Pong" stays as the game's name (spec decision).
- Haptics on: ball lands in a cup, penalty drawn (spec §9.3).
- Spicy keeps its 18+ confirmation on level select and on Play again (Plan 1 behavior, must not regress).
- Players per session 2–12. With exactly 2, no match picking screen.
- Test command: `./gradlew :shared:testAndroidHostTest` (add `--tests "<fqcn>"` for one class). On this machine run it as `.\gradlew.bat ...` from PowerShell.

## Review Focus

1. **Boundary landings:** a ball landing just inside / just outside a cup's inner edge or rim, or exactly between two cups → always the same, documented result (nearest cup wins, ties go to the lowest cup id) (Task 1 tests).
2. **Flicks that are not throws:** too weak, sideways or downward → nothing happens and the player does not lose their turn (Task 1 tests, Task 4 check).
3. **Last cup with an empty penalty pool** (Mehmet's content may have no penalties at a level) → the match still ends, the player is told, no crash (Task 2 test).
4. **Winner stays on edge cases:** 2 players (rematch, first throw alternates), the loser was the last player in the list (challenger wraps and is never the winner) (Task 3 tests).
5. **Configuration change or a fast second flick during the throw animation** → no second throw is dispatched, the screen shows the state the reducer holds (Task 4 check).

---

## File Structure

```
shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/cuppong/
  CupPongPhysics.kt     — TablePoint, ThrowInput, ThrowResult, rack layout, constants, flickToThrow, simulateThrow
  CupPong.kt            — CupPongState, CpPhase, CpAction, CpNotice, initialCupPongState
  CupPongReducer.kt
  CupPongMatchups.kt    — Matchup, suggestedMatchup, matchupFromSelection
  CupPongViewModel.kt, CupPongScreen.kt, CupPongTable.kt, CupPongSetupScreen.kt
shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/navigation/Routes.kt, ClinkNavHost.kt   (modify)
shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/GameListScreen.kt, GameOverScreen.kt (modify)
shared/src/commonMain/composeResources/values/strings.xml, values-tr/strings.xml (modify)
shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/cuppong/
  CupPongPhysicsTest.kt, CupPongReducerTest.kt, CupPongMatchupsTest.kt
```

Table coordinates (used by every task): units on the table, origin at the thrower's ball, **+y toward the target rack**, +x to the thrower's right. The rack is always the **defender's** cups, seen from the thrower.

---

### Task 1: Throw model

**Files:**
- Create: `game/cuppong/CupPongPhysics.kt`
- Test: `game/cuppong/CupPongPhysicsTest.kt`

**Interfaces:**
- Produces:
  - `data class TablePoint(val x: Float, val y: Float)`
  - `@Serializable data class ThrowInput(val angleDegrees: Float, val range: Float)` — angle 0 = straight at the rack, positive = to the right; landing point is `(range·sin(angle), range·cos(angle))`
  - `data class ThrowResult(val input: ThrowInput, val touchdowns: List<TablePoint>, val cupHit: Int?)` — 1 touchdown = clean landing; 2 = hit a rim and bounced
  - `object CupPongPhysics` with: `CUP_RADIUS = 3f`, `BALL_RADIUS = 1f`, `RIM_BOUNCE = 4f`, `MIN_FLICK = 300f`, `MAX_FLICK = 3000f` (dp/s), `MIN_RANGE = 60f`, `MAX_RANGE = 110f`, `MAX_ANGLE = 30f`, `val cups: List<TablePoint>` = id 0 `(-6.2, 90)`, 1 `(0, 90)`, 2 `(6.2, 90)`, 3 `(-3.1, 84.6)`, 4 `(3.1, 84.6)`, 5 `(0, 79.2)`, `val ALL_CUPS: Set<Int> = (0..5).toSet()`
  - `fun flickToThrow(vxDpPerSec: Float, vyDpPerSec: Float): ThrowInput?` — screen velocity (vy < 0 = up toward the rack). `null` when `vy >= 0` or speed < `MIN_FLICK`. Angle = `atan2(vx, -vy)` in degrees clamped to ±`MAX_ANGLE`; range linear from `MIN_RANGE` at `MIN_FLICK` to `MAX_RANGE` at `MAX_FLICK`, clamped.
  - `fun simulateThrow(input: ThrowInput, remainingCups: Set<Int>): ThrowResult` — landing `p`; among remaining cups take the nearest (ties → lowest id). Distance ≤ `CUP_RADIUS - BALL_RADIUS` → in that cup. Distance ≤ `CUP_RADIUS + BALL_RADIUS` → rim: second touchdown `p + unit(p - cupCenter) · RIM_BOUNCE`, which counts only if it is *in* a remaining cup (no second rim). Otherwise a miss. No randomness.

The constants are a starting point for feel (spec §11 leaves them to the builder); Task 5 may retune them on a device, and the tests below use geometry, not flick speeds, except for the mapping endpoints.

- [ ] **Step 1: Write the failing tests**

```kotlin
private fun at(x: Float, y: Float) = ThrowInput(atan2(x, y) * 180f / PI.toFloat(), hypot(x, y))
private val all = CupPongPhysics.ALL_CUPS

@Test fun straightIntoBackMiddleCup() = assertEquals(1, simulateThrow(at(0f, 90f), all).cupHit)
@Test fun straightIntoFrontCup() = assertEquals(5, simulateThrow(at(0f, 79.2f), all).cupHit)
@Test fun angledIntoBackLeftCup() = assertEquals(0, simulateThrow(at(-6.2f, 90f), all).cupHit)
@Test fun justInsideInnerEdgeIsIn() = assertEquals(5, simulateThrow(at(0f, 79.2f - 1.99f), all).cupHit)
@Test fun justOutsideInnerEdgeHitsTheRim() = assertEquals(2, simulateThrow(at(0f, 79.2f - 2.01f), all).touchdowns.size)
@Test fun rimBounceOutIsAMiss() {
    val r = simulateThrow(at(0f, 76.2f), all)            // 3 from cup 5, bounces back toward the thrower
    assertEquals(listOf(TablePoint(0f, 76.2f), TablePoint(0f, 72.2f)), r.touchdowns.map { it.rounded() })
    assertNull(r.cupHit)
}
@Test fun rimBounceCanDropIntoTheNeighbour() {
    val r = simulateThrow(at(0f, 84.6f), all)            // 3.1 from cups 3 and 4: tie → cup 3's rim, bounces right into cup 4
    assertEquals(4, r.cupHit); assertEquals(2, r.touchdowns.size)
}
@Test fun removedCupsAreIgnored() = assertNull(simulateThrow(at(0f, 90f), all - 1).cupHit)
@Test fun shortThrowMisses() { val r = simulateThrow(at(0f, 60f), all); assertNull(r.cupHit); assertEquals(1, r.touchdowns.size) }

@Test fun weakFlickIsNotAThrow() = assertNull(flickToThrow(0f, -299f))
@Test fun downwardOrSidewaysFlickIsNotAThrow() { assertNull(flickToThrow(0f, 1000f)); assertNull(flickToThrow(1000f, 0f)) }
@Test fun straightUpFlickAimsStraight() = assertEquals(0f, flickToThrow(0f, -1000f)!!.angleDegrees, 0.001f)
@Test fun rangeFollowsSpeedAndIsClamped() {
    assertEquals(60f, flickToThrow(0f, -300f)!!.range, 0.01f)
    assertEquals(110f, flickToThrow(0f, -3000f)!!.range, 0.01f)
    assertEquals(110f, flickToThrow(0f, -6000f)!!.range, 0.01f)
}
@Test fun angleIsClamped() = assertEquals(-30f, flickToThrow(-1000f, -100f)!!.angleDegrees, 0.001f)
```

(`rounded()` = a test helper rounding both coordinates to 2 decimals.)

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.cuppong.*"`. Expected: FAIL, unresolved references.
- [ ] **Step 3: Implement** `CupPongPhysics.kt` as specified above.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add Cup Pong throw model`

---

### Task 2: Match rules

**Files:**
- Create: `game/cuppong/CupPong.kt`, `CupPongReducer.kt`
- Test: `game/cuppong/CupPongReducerTest.kt`

**Interfaces:**
- Consumes: `simulateThrow`, `ThrowInput`, `ThrowResult`, `CupPongPhysics.ALL_CUPS` (Task 1); `Reducer`, `SequenceRandom` (Plan 1 Task 4); `Deck`, `ContentLibrary`, `CardType`, `Level` (Plan 1 Task 2)
- Produces:
  - `data class CupPongState(val players: List<String>, val level: Level, val thrower: Int, val cups: List<Set<Int>>, val phase: CpPhase, val throwCount: Int, val lastThrow: ThrowResult?, val penaltyDeck: Deck, val notice: CpNotice?)` — `players` has exactly 2 names (index 0 throws first); `cups[i]` are the cups player `i` still has (the ones the other player aims at)
  - `sealed interface CpPhase { data object Aiming; data class MatchOver(val winner: Int, val loser: Int, val penaltyCardId: String?) }`
  - `enum class CpNotice { NO_PENALTY_CARDS }`
  - `sealed interface CpAction { data class Throw(val input: ThrowInput) }`
  - `fun initialCupPongState(players: List<String>, level: Level, library: ContentLibrary): CupPongState` (`thrower = 0`, all cups, `Aiming`, `throwCount = 0`)
  - `object CupPongReducer : Reducer<CupPongState, CpAction>`

Rules: `Throw` only in `Aiming`. Defender = `1 - thrower`. `result = simulateThrow(input, cups[defender])`; remove `result.cupHit` from the defender's cups; `throwCount + 1`, `lastThrow = result`. If the defender has no cups left → `MatchOver(winner = thrower, loser = defender, penaltyCardId = penaltyDeck.draw(random)?.first)`; an empty penalty deck gives `null` and `notice = NO_PENALTY_CARDS`. Otherwise `thrower = defender`. Any successful throw clears an older notice. The UI animates `lastThrow` whenever `throwCount` changes.

- [ ] **Step 1: Write the failing tests** (two players `Ayşe`, `Mert`; library with one CASUAL penalty `p1`; a second library with no penalties; `at(x, y)` helper as in Task 1)

```kotlin
@Test fun hitRemovesDefendersCupAndPassesTurn()    // Throw(at(0,90)): cups[1] == ALL_CUPS - 1, cups[0] == ALL_CUPS, thrower == 1, throwCount == 1, lastThrow!!.cupHit == 1
@Test fun missKeepsCupsAndPassesTurn()             // Throw(at(0,60)): cups unchanged, thrower == 1, throwCount == 1
@Test fun clearingTheLastCupEndsTheMatch()         // cups[1] = setOf(5), Throw(at(0,79.2)): phase == MatchOver(0, 1, "p1"), cups[1] empty
@Test fun emptyPenaltyPoolStillEndsTheMatch()      // no penalties: phase == MatchOver(0, 1, null), notice == NO_PENALTY_CARDS
@Test fun throwsAfterTheMatchAreIgnored()          // MatchOver state + Throw → same state
@Test fun secondPlayerCanWin()                     // thrower 1, cups[0] = setOf(1), Throw(at(0,90)) → MatchOver(1, 0, "p1")
```

Each test asserts exactly the fields in its comment.

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.cuppong.*"`. Expected: FAIL on the new class.
- [ ] **Step 3: Implement** the types and reducer.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add Cup Pong match rules`

---

### Task 3: Winner stays on

**Files:**
- Create: `game/cuppong/CupPongMatchups.kt`
- Test: `game/cuppong/CupPongMatchupsTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable data class Matchup(val first: Int, val second: Int)` — indexes into the session's full player list; `first` throws first
  - `fun suggestedMatchup(playerCount: Int, previousWinner: Int?, previousLoser: Int?): Matchup` — no previous match → `(0, 1)`. Otherwise the winner stays on: the challenger is the next index after `previousLoser` (wrapping) that is not the winner; the **challenger throws first** *(Claude's call: the newcomer gets the first throw)*. With 2 players this is a rematch with the first throw swapped.
  - `fun matchupFromSelection(selected: List<Int>, previousWinner: Int?): Matchup` — `selected` has 2 distinct indexes; the one who is not `previousWinner` throws first; if neither is, the lower index throws first.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun firstMatchIsTheFirstTwoPlayers() = assertEquals(Matchup(0, 1), suggestedMatchup(5, null, null))
@Test fun twoPlayersRematchSwapsTheFirstThrow() {
    assertEquals(Matchup(1, 0), suggestedMatchup(2, previousWinner = 0, previousLoser = 1))
    assertEquals(Matchup(0, 1), suggestedMatchup(2, previousWinner = 1, previousLoser = 0))
}
@Test fun winnerStaysOnAndTheNextPlayerChallenges() = assertEquals(Matchup(2, 0), suggestedMatchup(4, previousWinner = 0, previousLoser = 1))
@Test fun challengerWrapsAndSkipsTheWinner() = assertEquals(Matchup(1, 0), suggestedMatchup(4, previousWinner = 0, previousLoser = 3))
@Test fun challengerSkipsTheWinnerInTheMiddle() = assertEquals(Matchup(0, 2), suggestedMatchup(3, previousWinner = 2, previousLoser = 1))
@Test fun selectionPutsTheChallengerFirst() = assertEquals(Matchup(1, 3), matchupFromSelection(listOf(3, 1), previousWinner = 3))
@Test fun selectionWithoutWinnerPutsLowerIndexFirst() = assertEquals(Matchup(1, 3), matchupFromSelection(listOf(3, 1), previousWinner = null))
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.cuppong.CupPongMatchupsTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** both functions.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add Cup Pong winner-stays-on matchups`

---

### Task 4: Table, throw animation and Cup Pong screen

**Files:**
- Create: `game/cuppong/CupPongTable.kt`, `CupPongViewModel.kt`, `CupPongScreen.kt`
- Modify: `strings.xml` (EN/TR)

**Interfaces:**
- Consumes: Tasks 1–2; `Store` (Plan 1); `EndGameDialog`, `rememberPhaseTapGuard`, `ClinkCard`, `LevelTag`, `PrimaryButton`, `ClinkColors` (Plan 1); `LocalAppContainer`
- Produces:
  - `class CupPongViewModel(players: List<String>, level: Level, library: ContentLibrary, random: Random) : ViewModel` with `val state: StateFlow<CupPongState>`, `fun onAction(action: CpAction)` (the two names of the match, in throwing order)
  - `@Composable fun CupPongScreen(players: List<String>, level: Level, onMatchOver: (winner: Int, loser: Int) -> Unit, onEnd: () -> Unit)` — `players` are the 2 match players; `winner`/`loser` are 0/1 in that list

`CupPongTable(cups: Set<Int>, flight: ThrowResult?, progress: Float, splash: Float, flickEnabled: Boolean, onFlick: (ThrowInput) -> Unit, modifier)`:
- Draws the table in a simple perspective on a `Canvas`: a trapezoid in `Surface` (wide at the bottom, narrow at the far end). One helper maps a `TablePoint` to the screen: depth `d = y / 100`, screen y from bottom to top, x and sizes scaled by `lerp(1f, 0.55f, d)`.
- Cups: a coral (`Primary`) ring with a `SurfaceRaised` inside, nothing in them. Removed cups are not drawn.
- The ball sits at the bottom center. During a flight it moves along launch → touchdown 1 (→ touchdown 2 for a rim bounce), lifted by an arc `4·h·t·(1−t)` with a shadow on the table; the ball shrinks with depth.
- `splash` (0→1) draws an expanding mint ring at the cup that was hit.
- The flick: drag on the lower part of the table, `VelocityTracker` on release, convert px/s to dp/s, `flickToThrow(...)`; `null` → nothing happens.

`CupPongScreen`:
- Top bar: "Turn: {thrower}" (reuse `turn_of`) and "End". Under it, both players' remaining cups as "Ayşe 6 · Mert 4" (`bp_score`).
- The rack drawn is the **defender's** cups, so the view switches to the thrower's side every turn (spec §7.3 "the view flips").
- On a flick: `onAction(Throw(input))`, then animate `lastThrow` (≈900 ms flight, +350 ms for a rim bounce, `FastOutSlowInEasing`), showing the **previous** thrower's view until it lands; then `bp_hit` / `bp_rim_out` / `bp_miss` for ~700 ms; haptic on a hit. While animating, the flick is disabled (a second flick dispatches nothing). After a configuration change, skip the animation and show the current state.
- `MatchOver`: `bp_win` banner, then a `ClinkCard` with `LevelTag`, `penalty_title`, `bp_loser_penalty` and the penalty text in the app language, haptic when it appears, `PrimaryButton` "Penalty done" → `onMatchOver(winner, loser)`. With `penaltyCardId == null`: show `notice_no_penalty` and a "Continue" button that does the same.
- "End" and back → `EndGameDialog` → `onEnd()`.

Copy (EN / TR): `bp_score` "%1$s %2$d · %3$s %4$d" (same in TR); `bp_hint` "Flick the ball at the cups" / "Topu bardaklara fırlat"; `bp_hit` "In the cup!" / "Bardağa girdi!"; `bp_rim_out` "Off the rim!" / "Kenardan döndü!"; `bp_miss` "Miss" / "Iska"; `bp_win` "%1$s wins!" / "%1$s kazandı!"; `bp_loser_penalty` "%1$s takes a penalty" / "%1$s ceza çekiyor"; `continue_button` reused; reuse `penalty_title`, `penalty_done`, `end`, `notice_no_penalty`.

- [ ] **Step 1: Implement** `CupPongViewModel` (a `Store` built from the arguments, like Plan 1's view models), `CupPongTable` and `CupPongScreen`. No new unit tests here: the rules and the throw model are covered by Tasks 1–2.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: all PASS, BUILD SUCCESSFUL. The screen becomes reachable in Task 5, which does its device check.
- [ ] **Step 3: Hand off.** Commit message: `feat: add Cup Pong table and match screen`

---

### Task 5: Match picking, navigation and end-to-end check

**Files:**
- Create: `game/cuppong/CupPongSetupScreen.kt`
- Modify: `ui/navigation/Routes.kt`, `ClinkNavHost.kt`, `ui/screens/GameListScreen.kt` (enable Cup Pong), `ui/screens/GameOverScreen.kt` only if needed, `strings.xml` (EN/TR)

**Interfaces:**
- Consumes: Tasks 3–4; `ifResumed`, `AdultConfirmDialog` / Play again flow (Plan 1)
- Produces:
  - Routes: `CupPongSetupRoute(players: List<String>, level: Level, previousWinner: Int? = null, previousLoser: Int? = null)`, `CupPongRoute(players: List<String>, level: Level, first: Int, second: Int)` (`players` = full session list, `first`/`second` index into it); `GameOverRoute` gains `matchWinner: Int? = null, matchLoser: Int? = null` (full-list indexes). If the navigation library rejects nullable `Int` arguments, use `-1` for "none" and ledger it.
  - `@Composable fun CupPongSetupScreen(players: List<String>, suggested: Matchup, previousWinner: Int?, onStart: (Matchup) -> Unit)` — title `bp_setup_title`, hint `bp_setup_hint`; every player as a chip, the 2 from `suggested` preselected, tapping toggles (at most 2 selected); `bp_start_match` enabled only with 2 selected → `onStart(matchupFromSelection(selected, previousWinner))`.

Navigation:
- `startGame(CUP_PONG, players, level, winner?, loser?)`: 2 players → straight to `CupPongRoute` with `suggestedMatchup(2, winner, loser)`; more → `CupPongSetupRoute`.
- `CupPongRoute` → `CupPongScreen(listOf(players[first], players[second]), …)`; `onMatchOver(w, l)` maps 0/1 back to `first`/`second` and goes to `GameOverRoute(CUP_PONG, players, level, matchWinner, matchLoser)` popping the game; `onEnd` goes to `GameOverRoute` with no winner.
- GameOver "Play again" for Cup Pong passes `matchWinner`/`matchLoser` on, so the winner stays on. The Spicy 18+ question still comes first.
- All navigation through `entry.ifResumed { … }` (Plan 1 fix).

Copy (EN / TR): `bp_setup_title` "Who plays this match?" / "Bu maçı kim oynuyor?"; `bp_setup_hint` "Pick two players" / "İki oyuncu seç"; `bp_start_match` "Start match" / "Maçı başlat".

- [ ] **Step 1: Implement** the setup screen, routes and navigation; enable Cup Pong in the game list (remove its "Coming soon").
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: all PASS, BUILD SUCCESSFUL.
- [ ] **Step 3: Verify on the emulator** (`Pixel_9a`):
  - 2 players, Casual: straight flicks, a weak flick (nothing happens, same thrower), a sideways flick (nothing), hits remove cups, the rack switches to the thrower's side each turn, a rim bounce plays its second hop, the match ends at 0 cups, penalty card + haptic, Penalty done → GameOver → Play again → rematch with the other player throwing first.
  - 4 players: setup screen preselects players 1–2; after a match Play again preselects the winner and the next challenger; changing the selection works.
  - Spicy: 18+ on level select and on Play again.
  - Back / End → dialog → GameOver. A second fast flick during a flight does nothing. Switching the app language during a flight (`adb shell cmd locale set-app-locales com.mehmetbozkurt.clink --locales tr`) leaves cups and turn intact.
  - Tune `MIN_RANGE`, `MAX_RANGE`, `MAX_FLICK` if hitting a cup is far too easy or impossible; keep Task 1 tests green (they only fix the mapping endpoints — update those two asserted values if the constants change, and ledger it).
- [ ] **Step 4: Hand off.** Commit message: `feat: add Cup Pong match picking and navigation`

---

## Carried over from Plan 1 (not in this plan)

Deferred minor findings from Plan 1's final review, kept here so they aren't lost; Mehmet decides when to do them:
- Repeated taps on an empty deck queue identical snackbars (~20 s).
- Drinking-word check misses wine, vodka, tequila, whisk(e)y, chug, şarap, rakı, kadeh, votka, viski, tekila, içecek.
- No haptic when a penalty is drawn in Spin the Bottle (spec §9.3).
- Names overlap on the 12-player Spin circle with ~7+ character names.
- `take(20)` can split an emoji surrogate pair in player names.
- Coral "Take penalty" text on `SurfaceRaised` is 4.43:1, just under WCAG AA 4.5:1.
- `SpinTheBottle.kt` comment says the UI animates on `spinCount` change; it animates from the flick handler (matters for Plan 4).
- Gradle warns that `androidLibrary { }` in `shared/build.gradle.kts` is deprecated in favor of `android { }`.
- A malformed bundled content file still crashes at launch; only `BaselineContentFileTest` guards it (make tests run before release builds in Plan 5).
