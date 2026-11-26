# "Kim Daha Çok?" online — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Most Likely To playable with everyone voting in secret from their own phone.

**Architecture:** The game keeps one state, shared by the one-phone and online versions, as every other Clink game does. It gains a voting phase and a penalty phase that can name several players; the host's referee stamps each vote with the sender's seat, reveals when everyone online has voted, and enforces the two host-only actions. The screens are new; the rules are not duplicated.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.12, kotlinx.serialization, Firebase Realtime Database through the existing `LobbyRepository`.

**Spec:** `docs/superpowers/specs/2026-11-26-most-likely-to-online-design.md` (and the one-phone design it extends, `2026-11-25-most-likely-to-design.md`)

## Global Constraints

- Three players minimum, twelve maximum, for this game online and on one phone.
- Only card ids are published, never card text.
- No screen, on any phone, ever shows who voted for whom; counts only.
- Game rules stay pure Kotlin functions so they can move to a server later.
- Turkish and English for every new line of copy. Dark theme only.
- No commits by the executor: Mehmet commits.
- Test runner: `scratchpad\suite.ps1 [-Filter "*X"] [-Build]`.

## Review Focus

1. **Everyone votes for someone different.** A twelve-way tie means twelve penalty takers; the result screen must list them and the round must still end. — Task 1 and Task 4.
2. **A vote arrives after the host passed the prompt.** It must not count towards, or resurrect, a prompt nobody is looking at. — Task 2.
3. **The same vote arrives twice** (a retry, or a double tap). The second must replace the first, never add to it. — Task 1.
4. **Reveal with no votes at all.** The host's button only appears after the first vote, but the action can still arrive; it must not pick a penalty taker out of nothing. — Task 1.
5. **The last online voter drops at the moment they vote.** The reveal must still happen, from the presence pass, and their vote must still count. — Task 2.

---

### Task 1: The voting phase and the tie rule

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/mostlikely/MostLikely.kt`, `MostLikelyReducer.kt`, `MostLikelyScreen.kt` (the penalty phase's `picked` is now a list)
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/mostlikely/MostLikelyReducerTest.kt`

**Interfaces:**
- Produces:
  - `MlPhase.Voting(cardId: String, votes: List<Int?>)` — one slot per player, in seat order, null until that player votes; the value is the seat they voted for. Only online games enter it.
  - `MlPhase.ShowingPenalty(picked: List<Int>, promptId: String, cardId: String?, counts: List<Int> = emptyList())` — `picked` was a single seat and is now every seat that tied for most votes; `counts` is votes received per seat this round, empty on one phone.
  - `MlAction.Vote(voter: Int, target: Int)` and `MlAction.Reveal`.
  - `MostLikelyState.voting: Boolean = false` — true for an online session, set by the initial state; it is what tells a finished round to go back to `Voting` rather than `ShowingPrompt`.
  - `initialMostLikelyState(players, level, library, random, voting: Boolean = false)` — `voting = true` starts in `Voting` instead of `ShowingPrompt`.
  - `MostLikelyState.allVoted(online: Set<Int>): Boolean` — true when every seat in `online` has a vote.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun aVoteFillsOnlyItsOwnSlot()                 // Vote(1, 0) -> votes == [null, 0, null]
@Test fun votingAgainReplacesTheFirstVote()          // Vote(1,0) then Vote(1,2) -> votes == [null, 2, null], one vote  (Review Focus 3)
@Test fun theLastVoteRevealsTheResult()              // all three vote -> ShowingPenalty, counts sum to 3
@Test fun theMostVotedTakesThePenalty()              // 2 votes for seat 0, 1 for seat 2 -> picked == listOf(0)
@Test fun everyoneTiedTakesThePenalty()              // one vote each for 0, 1, 2 -> picked == listOf(0, 1, 2)  (Review Focus 1)
@Test fun revealWithNoVotesDrawsTheNextPromptInstead() // Reveal from Voting with all slots null -> Voting with a different cardId, nobody picked  (Review Focus 4)
@Test fun revealClosesTheRoundEarly()                // two of three voted -> Reveal -> ShowingPenalty from those two
@Test fun theTallyCountsPenaltiesNotVotes()          // a round where seat 0 got 2 votes -> picks[0] == 1
@Test fun passFromVotingDropsTheVotes()              // Vote then Pass -> Voting, a different cardId, all slots null
@Test fun penaltyDoneAfterVotingGoesBackToVoting()   // online rounds return to Voting, not ShowingPrompt
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*MostLikelyReducerTest"`
Expected: FAIL to compile — `MlPhase.Voting`, `MlAction.Vote` and `allVoted` are unresolved.

- [ ] **Step 3: Implement the Interfaces block.** The phase machine gains, alongside the one-phone phases:

```
Voting --Vote(v,t)--> Voting          (slot v = t; when every slot is filled, reveal)
Voting --Reveal----> ShowingPenalty   (from the votes cast; with no votes, draw the next prompt instead)
Voting --Pass------> Voting           (next prompt, every slot cleared)
ShowingPenalty --PenaltyDone--> Voting if the round came from Voting, else ShowingPrompt
```
Revealing counts the votes, picks every seat with the highest count, adds one to `picks` for each of them, and carries the counts into the phase. `Pick` and `WrongPerson` stay one-phone only and do nothing from `Voting`.

- [ ] **Step 4: Update the one-phone call sites** for `picked` becoming a list: `MostLikelyReducer`'s own pick and undo, and `MostLikelyScreen`'s penalty phase, which now shows every name in `picked` (always one on this path).

- [ ] **Step 5: Run** `suite.ps1 -Filter "*MostLikely*"`
Expected: PASS, every test in the file, old and new.

### Task 2: The referee

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/online/SessionState.kt`, `OnlineAction.kt`, `HostReferee.kt`, `LobbySession.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/online/HostRefereeTest.kt`

**Interfaces:**
- Consumes: everything Task 1 produces.
- Produces:
  - `SessionState.mostLikely: MostLikelyState?` and `SessionState.hostUid: String = ""`.
  - `OnlineAction.MostLikely(action: MlAction)`.
  - `HostReferee.start(..., hostUid: String)` — the new parameter, passed by `LobbySession.startGame` as the host's own uid.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun mostLikelyStartsInVotingWithASeatPerPlayer()
@Test fun aVoteIsStampedWithTheSendersSeat()          // a member claiming voter = 0 still votes from their own seat
@Test fun onlyTheHostCanReveal()                      // Reveal from a non-host returns null
@Test fun onlyTheHostCanPass()                        // Pass from a non-host returns null
@Test fun aVoteAfterAPassDoesNotCountForTheOldPrompt() // Pass, then the in-flight vote arrives: the new prompt's slots stay empty  (Review Focus 2)
@Test fun theRoundRevealsWhenEveryOnlinePlayerHasVoted() // skipOffline with one player offline and the rest voted -> ShowingPenalty  (Review Focus 5)
@Test fun aVoteFromSomeoneWhoThenDroppedStillCounts()    // their vote is in the counts after the reveal  (Review Focus 5)
@Test fun aPenaltyNobodyPresentTakesAdvancesByItself()   // skipOffline from ShowingPenalty with every picked player offline -> next Voting
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*HostRefereeTest"`
Expected: FAIL to compile — `SessionState.mostLikely`, `hostUid` and `OnlineAction.MostLikely` are unresolved.

- [ ] **Step 3: Implement the Interfaces block and the rules.** `apply` routes `OnlineAction.MostLikely`, replacing any `voter` the sender claimed with their own seat, and returns null for `Reveal` and `Pass` from anyone but `hostUid`. A vote whose prompt id is not the one now showing is dropped. `skipOffline` gains two passes for this game: reveal once every online player has voted, and advance a penalty when no picked player is online.

- [ ] **Step 4: Run** `suite.ps1 -Filter "*HostRefereeTest"`
Expected: PASS, the whole file.

### Task 3: The lobby's minimum

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/online/LobbySession.kt:204-208`, `ui/online/OnlineRoomScreen.kt:290-293,361-368`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`, `values-tr/strings.xml`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/online/LobbySessionTest.kt` (or the existing lobby test file)

**Interfaces:**
- Produces: the start gate reading `GameId.minPlayers`; `lobby_need_players` replacing `lobby_need_two`, taking the number as `%1$d`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun aGameThatNeedsThreePlayersWillNotStartWithTwo()   // startGame returns false, no state published
@Test fun theSameLobbyStartsTruthOrDareWithTwo()            // the other games are unchanged
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*LobbySessionTest"` — Expected: FAIL, the three-player game starts with two.

- [ ] **Step 3: Replace the hard-coded two** in both the session's gate and the room screen's `canStart` with the game's own minimum (Cup Pong's exactly-two rule stays as it is), and make the explaining line take the number.

- [ ] **Step 4: Run** `suite.ps1` — Expected: PASS, everything.

### Task 4: The online screen, and switching the game on

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/online/OnlineGames.kt`, `OnlineRoomScreen.kt`, `game/GameId.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`, `values-tr/strings.xml`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/GameIdTest.kt`

**Interfaces:**
- Consumes: Tasks 1–3.
- Produces: `OnlineMostLikely(session: SessionState, me: OnlineSeat, send: (OnlineAction) -> Unit, onClose: () -> Unit, topBarExtras: @Composable RowScope.() -> Unit)`.

- [ ] **Step 1: Update `GameIdTest`** so `MOST_LIKELY_TO.playsOnline` is true, run it, watch it fail, then set the flag.

- [ ] **Step 2: Build the screen**, following `OnlineTruthOrDare`'s shape, with one layout per phase (spec §6):
  - `Voting`: the prompt on a `Poster`, "Pick someone", the player chips with your own choice marked, and a line counting votes ("3/5"). A spectator or a player who has voted still sees the chips; tapping again changes the vote. Host only, under the chips: "Show the result" once any vote is in, and "Pass this one".
  - `ShowingPenalty`: the split as name and count per seat, then who takes the penalty, then the penalty card or the "no penalty cards" notice. "Done" for the picked players and the host; everyone else sees the waiting label.
  - `Summary`: the night's tally, as on one phone; "Done" for the host only, which closes the game to the lobby.
- [ ] **Step 3: Route it** from `OnlineRoomScreen`'s game `when`, replacing the branch that throws today, and drop the online-is-coming path for this game on the mode screen.
- [ ] **Step 4: New strings in both languages:** "Pick someone", the "%1$d/%2$d voted" line, "Show the result", and the result's "takes the penalty" line for one and for several names.
- [ ] **Step 5: Run** `suite.ps1 -Build` — Expected: `exit=0`, every test green, APK built.
- [ ] **Step 6: Device check.** Online needs three signed-in phones, which this session does not have, so verify what one device can: the lobby refuses to start this game with two players and says why, and the mode screen no longer greys out online. Say plainly in the report that the voting round itself was not seen running.
- [ ] **Step 7: Report** — a summary, the rulings, what was not verified, and one commit message: `feat: play Most Likely To online`. The executor does not commit.
