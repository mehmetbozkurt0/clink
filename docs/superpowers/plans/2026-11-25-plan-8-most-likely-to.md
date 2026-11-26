# "Kim Daha Çok?" / Most Likely To (local) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clink's fourth game, playable on one phone: a prompt, everyone points at once, the most-pointed player takes a penalty.

**Architecture:** A self-contained module `game/mostlikely/` shaped like `truthordare/`: a serializable state, a pure reducer over phases, a Compose screen and a view model. `GameId` gains two properties so the game can declare three players minimum and no online play yet. The prompt is a new card type drawn from the existing `Deck`; penalties reuse the existing penalty cards.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.12, kotlinx.serialization, Navigation Compose with `@Serializable` routes.

**Spec:** `docs/superpowers/specs/2026-11-25-most-likely-to-design.md`

## Global Constraints

- 3 to 12 players; below three the game has no meaning.
- Every card carries Turkish and English text; no drinking words anywhere (content rule 3).
- Code, identifiers and comments in English; all player-facing copy in both languages.
- Dark theme only.
- No commits by the executor: Mehmet commits once at the end (his waiver for this round).
- Test runner: `scratchpad\suite.ps1 [-Filter "*X"] [-Build]`, which prints `exit=` and `suite: N/M`.
- Emulator checks use `emulator-5554` only; the phone is never touched without Mehmet's say-so in that turn.

## Review Focus

1. **Twelve players with 20-character names.** The chips must wrap and every one must stay tappable on a short screen; the prompt card shrinks rather than pushing chips off. — Task 4.
2. **Passing over and over.** The prompt deck reshuffles and never shows the card that was just on screen twice in a row. — Task 2.
3. **"Wrong person" on the first pick of the night.** The counter returns to zero and the chip's counter disappears again. — Task 2.
4. **Ending right after a "wrong person" undo.** The tally matches what the chips showed. — Task 2.
5. **A level with prompts but no penalty cards.** The round still completes: the name shows with a notice instead of a card. — Task 2.

---

### Task 1: The prompt card type and its sample content

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/content/ContentModel.kt` (the `CardType` enum)
- Modify: `shared/src/commonMain/composeResources/files/content/baseline.json`
- Modify: `docs/content-format.md`
- Test: `shared/src/androidHostTest/kotlin/com/mehmetbozkurt/clink/content/BaselineContentFileTest.kt` (already written; it drives this task)

**Interfaces:**
- Produces: `CardType.MOST_LIKELY`, and packs with ids `mostlikely-<level>-base` holding card ids `mostlikely-<level>-NNN`.

- [ ] **Step 1: Add `MOST_LIKELY` to `CardType`, after `PENALTY`.**

- [ ] **Step 2: Run** `suite.ps1 -Filter "*BaselineContentFileTest"`
Expected: FAIL — `everyTypeAndLevelHasAtLeastThreeCards` reports `MOST_LIKELY/CASUAL`, because the enum now has a type with no cards.

- [ ] **Step 3: Write the sample prompts into `baseline.json`:** three packs (`mostlikely-casual-base`, `mostlikely-hard-base`, `mostlikely-spicy-base`), `access: "FREE"`, **15 cards each**, every card with `tr` and `en`. Each prompt is a question that points at a person and reads naturally after "kim" / "who", and names no player. Tone per level is the table in spec §9.

- [ ] **Step 4: Run** `suite.ps1 -Filter "*BaselineContentFileTest"`
Expected: PASS, 4/4 — the ids are unique, both languages are present and no card trips the drinking filter.

- [ ] **Step 5: Document the type** in `docs/content-format.md`: add `MOST_LIKELY` to the `type` list and a line of tone guidance, in the Turkish the file is written in.

### Task 2: State and reducer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/mostlikely/MostLikely.kt`
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/mostlikely/MostLikelyReducer.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/mostlikely/MostLikelyReducerTest.kt`

**Interfaces:**
- Consumes: `CardType.MOST_LIKELY` (Task 1); the existing `Deck`, `ContentLibrary.cardIds(type, level)` and `Reducer<S, A>`.
- Produces:
  - `MostLikelyState(players: List<String>, level: Level, phase: MlPhase, promptDeck: Deck, penaltyDeck: Deck, picks: List<Int>)`, serializable, with `val hasPicks: Boolean` true when any count is above zero.
  - `MlPhase`: `NoPrompts`, `ShowingPrompt(cardId: String)`, `ShowingPenalty(picked: Int, promptId: String, cardId: String?)` where a null `cardId` means the level has no penalty cards, and `Summary`.
  - `MlAction`: `Pick(player: Int)`, `Pass`, `WrongPerson`, `PenaltyDone`, `End`.
  - `initialMostLikelyState(players: List<String>, level: Level, library: ContentLibrary, random: Random): MostLikelyState` — `picks` is zeros the length of `players`; it draws the first prompt, or lands in `NoPrompts` when the level has none.
  - `object MostLikelyReducer : Reducer<MostLikelyState, MlAction>`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun pickingShowsAPenaltyAndCountsThePlayer()      // Pick(1) from ShowingPrompt -> ShowingPenalty(picked=1, cardId != null), picks[1] == 1
@Test fun passingDrawsANewPromptAndCountsNobody()       // Pass -> ShowingPrompt with a different cardId, picks all zero
@Test fun passingNeverRepeatsThePromptJustShown()       // pass through a 2-card level 6 times: no two consecutive cardIds are equal   (Review Focus 2)
@Test fun wrongPersonGoesBackToTheSamePromptAndUncounts() // Pick(0) then WrongPerson -> ShowingPrompt(same promptId), picks[0] == 0, hasPicks false  (Review Focus 3)
@Test fun theTallySurvivesAnUndoBeforeTheEnd()          // Pick(2), PenaltyDone, Pick(0), WrongPerson, End -> Summary with picks == [0,0,1]  (Review Focus 4)
@Test fun penaltyDoneDrawsTheNextPrompt()               // ShowingPenalty --PenaltyDone--> ShowingPrompt
@Test fun aLevelWithNoPenaltyCardsStillFinishesTheRound() // library with prompts but no penalties: Pick -> ShowingPenalty(cardId = null)  (Review Focus 5)
@Test fun aLevelWithNoPromptsCannotStart()              // initial state -> MlPhase.NoPrompts
@Test fun actionsThatDoNotFitThePhaseChangeNothing()    // PenaltyDone while ShowingPrompt -> unchanged
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*MostLikelyReducerTest"`
Expected: FAIL to compile — `MostLikelyState` and friends are unresolved.

- [ ] **Step 3: Write `MostLikely.kt` and `MostLikelyReducer.kt`** to the Interfaces block above. The phase machine, mirroring `TruthOrDareReducer`'s shape and its "actions that don't fit the phase leave the state unchanged" rule:

```
NoPrompts            (dead end; only the screen's end button leaves)
ShowingPrompt --Pick(i)------> ShowingPenalty   picks[i] + 1
ShowingPrompt --Pass---------> ShowingPrompt    (next prompt, picks untouched)
ShowingPenalty --WrongPerson-> ShowingPrompt    (the same promptId, picks[picked] - 1)
ShowingPenalty --PenaltyDone-> ShowingPrompt    (next prompt)
any --End-------------------> Summary
```
A penalty card is drawn when `Pick` happens; `WrongPerson` does not put it back, because the deck reshuffles anyway (spec §12.8).

- [ ] **Step 4: Run** `suite.ps1 -Filter "*MostLikelyReducerTest"`
Expected: PASS, 9/9.

### Task 3: The game's place in the app

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/GameId.kt`, `game/PlayerNames.kt`
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/ScreenParts.kt`, `ModeSelectScreen.kt`, `PlayerSetupScreen.kt`, `ui/online/OnlineRoomScreen.kt`
- Create: `shared/src/commonMain/composeResources/drawable/art_mostlikely.xml`, `ic_game_mostlikely.xml`, `poster_mostlikely.xml`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`, `values-tr/strings.xml`
- Modify: `hosting/public/index.html`, `docs/design/illustration-brief.md`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/GameIdTest.kt` (create), existing `PlayerNamesTest`

**Interfaces:**
- Produces: `GameId.MOST_LIKELY_TO`; `GameId.minPlayers: Int` (2 by default, 3 for this game) and `GameId.playsOnline: Boolean` (true by default, false for this game); `PlayerNames.validate(raw: List<String>, min: Int = MIN)` with `PlayerNamesResult.TooFew(val min: Int)`; `GameId.posterArt()`, `art()`, `icon()`, `title()`, `players()`, `meta()`, `accent()` all answering for the new game.

- [ ] **Step 1: Write the failing tests**

```kotlin
// GameIdTest
@Test fun theNewGameNeedsThreePlayersAndHasNoOnlineYet() {
    assertEquals(3, GameId.MOST_LIKELY_TO.minPlayers)
    assertFalse(GameId.MOST_LIKELY_TO.playsOnline)
}
@Test fun theOtherGamesAreUnchanged() {
    listOf(GameId.TRUTH_OR_DARE, GameId.SPIN_THE_BOTTLE, GameId.CUP_PONG).forEach {
        assertEquals(2, it.minPlayers); assertTrue(it.playsOnline)
    }
}
// PlayerNamesTest
@Test fun twoPlayersAreTooFewWhenAGameWantsThree() =
    assertEquals(PlayerNamesResult.TooFew(3), PlayerNames.validate(listOf("Mert", "Ayşe"), min = 3))
@Test fun threePlayersPassWhenAGameWantsThree()  // returns PlayerNamesResult.Valid with the three names
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*GameIdTest"` then `suite.ps1 -Filter "*PlayerNamesTest"`
Expected: FAIL to compile — `MOST_LIKELY_TO`, `minPlayers`, `playsOnline` and the `min` parameter are unresolved.

- [ ] **Step 3: Add the enum value and the two properties** as enum constructor parameters with defaults, and give `PlayerNames.validate` its `min` parameter and `TooFew` its `min` field. `err_too_few` takes the number as `%1$d` in both languages; `PlayerSetupScreen` passes `game.minPlayers` and formats the message with it.

- [ ] **Step 4: Run** `suite.ps1`
Expected: PASS everywhere; the `when (game)` blocks that are now non-exhaustive are the compile errors to fix next, not test failures.

- [ ] **Step 5: Fill in every `when (game)`** — `ScreenParts.kt`'s `title`, `players`, `meta`, `accent`, `art`, `icon`, `posterArt`, and `ClinkNavHost`'s game start (Task 4 owns the route it points at) — and draw the three vectors. The poster drawing is several hands pointing at one spot, ink `#141414` strokes only, no fills, 100×80 viewport, matching `poster_tod.xml`.

- [ ] **Step 6: Close both doors to online play.** `OnlineRoomScreen`'s picker lists `GameId.entries.filter { it.playsOnline }`. `ModeSelectScreen` keeps both cards but passes `enabled = game.playsOnline` to the online one and shows a short "yakında" / "coming soon" line under it when it is off.

- [ ] **Step 7: New strings in both languages:** `game_mostlikely` ("Kim Daha Çok?" / "Most Likely To"), `game_ml_players` ("3–12 kişi" / "3–12 players"), `game_ml_meta` ("3–12 kişi · Üçe kadar say, göster" / "3–12 players · Count to three and point"), `game_ml_accent` ("Kim" / "Most"), and `mode_online_soon`.

- [ ] **Step 8: Fix the copy that counts games** (spec §11): rewrite the play tour's first card (`tour_play_1_title`, `tour_play_1_body`) so it neither counts nor lists the games; update the three game names in `hosting/public/index.html`; add the new game and its drawings to `docs/design/illustration-brief.md`.

- [ ] **Step 9: Run** `suite.ps1 -Build`
Expected: `exit=0`, every test green and the APK built.

### Task 4: Screen, view model and navigation

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/mostlikely/MostLikelyScreen.kt`, `MostLikelyViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/navigation/Routes.kt`, `ClinkNavHost.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`, `values-tr/strings.xml`

**Interfaces:**
- Consumes: everything Tasks 2 and 3 produce.
- Produces: `MostLikelyScreen(players: List<String>, level: Level, onEnd: () -> Unit)` and `MostLikelyRoute(players: List<String>, level: Level)`.

- [ ] **Step 1: Build the screen** on `MostLikelyViewModel`, following `TruthOrDareScreen`'s shape (`GameChrome` top bar, the same end-of-game confirmation, `rememberPhaseTapGuard`), with one layout per phase as drawn in spec §7.2:
  - `ShowingPrompt`: the prompt on a `Poster`, the fixed instruction line under it, then the player names as `WallChip`s, each showing its counter only once above zero, then a quiet "Bu soruyu geç" / "Pass this one".
  - `ShowingPenalty`: the picked player's name large, the penalty card under it or the "no penalty cards at this level" notice when `cardId` is null, a primary "Bitti" / "Done", and a quiet "Yanlış kişi" / "Wrong person".
  - `NoPrompts`: the "no prompts at this level" notice and one button that ends the game.
  - `Summary`: "Bu akşam" / "Tonight" and the players who were picked, most first, with their counts, and one button that ends the game.
  - The ✕ confirmation calls `onEnd()` straight away when `state.hasPicks` is false, and otherwise dispatches `MlAction.End` to show the summary.
  - New strings for every line above, in both languages.

- [ ] **Step 2: Wire the route** — `MostLikelyRoute(players, level)` in `Routes.kt`, the `GameId.MOST_LIKELY_TO` branch of `ClinkNavHost`'s game start, and a `composable<MostLikelyRoute>` whose `onEnd` navigates to `GameOverRoute(GameId.MOST_LIKELY_TO, players, level)` popping itself, exactly as `TruthOrDareRoute` does.

- [ ] **Step 3: Run** `suite.ps1 -Build`
Expected: `exit=0`, all tests green, APK built.

- [ ] **Step 4: Emulator check** (`emulator-5554`): install, then play the game with **three** players — prompt, pick a name, penalty, done, next prompt — and check that pass, "wrong person", the counters on the chips, the summary and the game-over screen all behave. Then set up **twelve** players with 20-character names on a short screen (`wm size 720x1280`, `wm density 320`, both reset afterwards) and confirm every chip wraps, stays tappable and keeps the prompt card on screen (Review Focus 1). Check the game list and the mode screen show the game with online dimmed, and that the lobby's picker does not list it. Do the whole pass once in Turkish and once in English, restoring the app's language afterwards.

- [ ] **Step 5: Report** — a summary, the rulings, what was not verified, and one commit message: `feat: add the Most Likely To game`. The executor does not commit.
