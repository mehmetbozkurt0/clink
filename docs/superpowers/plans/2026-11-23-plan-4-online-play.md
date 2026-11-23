# Plan 4: Online Play — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Friends play Truth or Dare, Spin the Bottle and Beer Pong on their own phones: the host opens a lobby, shares an invite link (`https://clink-game.com/join/<code>`) or the 6-character room code, joiners pick a nickname and avatar (guests sign in anonymously), everyone chats, the host's phone runs the rules and publishes the state, disconnects are handled, and the lobby outlives a single game.

**Architecture:** Spec §10.4, host-authoritative. Realtime Database holds one node per lobby: `meta` (host, game, level, status, host presence), `members`, `actions` (any member appends), `state` (only the host writes: the game state as JSON plus a sequence number), `chat`, and a top-level `codes/{code}` lookup. A `HostReferee` (pure Kotlin, tested) applies member actions to the existing game reducers, checks that the sender is allowed to act, skips players who are offline, and returns the new published state. The game screens are refactored to take a `GameController` (state + `onAction` + which seat this phone has) so one screen serves local and online play. Firebase is behind a `LobbyRepository` interface; everything that decides something is pure and covered by tests with fakes.

**Tech Stack:** As Plan 3, plus GitLive `firebase-database` 2.8.0; Firebase Hosting (static files) for `clink-game.com`; Android App Links.

**Spec:** `docs/superpowers/specs/2026-11-19-party-games-app-design.md` (§5.2 Online, §6.1 Spicy confirmation, §7 online rules, §8 extensibility, §10.3–10.4)
**Roadmap:** `docs/superpowers/plans/2026-11-19-clink-roadmap.md` (Plan 4 of 5)

## Global Constraints

- **Git, this plan only (Mehmet, 2026-11-23):** all tasks are done in one run without stopping for commits; Mehmet makes **one commit at the end**. Claude still never commits, pushes or rewrites history.
- **Mehmet's parts** (console, domain, deploy, devices) are listed in `docs/online-setup.md`; Claude never asks for passwords or key contents.
- **Host-authoritative:** only the host's app runs reducers and writes `state`; randomness (card draws, bottle target) is decided by the host and published. Members only append actions and chat (enforced by `firebase/database.rules.json`).
- **Lobby:** 2–12 players; Beer Pong 1v1 with the others watching; host can remove a player; host gone > 60 s → lobby closes; a non-host who drops is skipped in turn order and can rejoin with the same link; the lobby stays after a game (host can restart or switch game/level).
- **Spicy:** 18+ confirmation for the host when choosing it, and for each joiner when entering (or staying in) a Spicy lobby.
- **Chat:** available in the lobby and during the game as a panel over the screen; max 300 characters per message; its own module (§8, voice later).
- **Card IDs only** are published, never card text (§6.3); each phone shows the card in its own language.
- No drinking wording or imagery; Street / Graffiti look; TR and EN copy; AA contrast.
- Test command: `./gradlew :shared:testAndroidHostTest` (`.\gradlew.bat` from PowerShell here).

## Review Focus

1. **A member who is not allowed to act sends an action** (not their turn, a spectator, a forged uid) → the referee ignores it; the state does not change (Task 2 tests).
2. **The player whose turn it is goes offline** → the referee moves the turn to the next online player (Truth or Dare, Spin) or pauses the match (Beer Pong); nobody is stuck (Task 2 tests).
3. **The host goes offline** → members see "host reconnecting" with a countdown; after 60 s the lobby closes for everyone; the host coming back within 60 s resumes (Task 2 tests for the timing, Task 6 screen).
4. **Joining with a wrong, expired or closed code, a full lobby, or after being removed** → a clear message, no crash, no half-joined state (Task 2/4 tests).
5. **Two phones in different languages** in one lobby → each shows the same card in its own language (card ids only).

---

## File Structure

```
gradle/libs.versions.toml, shared/build.gradle.kts            (GitLive firebase-database)
androidApp/src/main/AndroidManifest.xml                       (App Links intent filter for clink-game.com/join)
androidApp/.../MainActivity.kt, AndroidShare.kt               (deep-link code → Platform, share sheet)
firebase/database.rules.json, firebase.json, .firebaserc.example, hosting/public/{index.html, join/index.html, .well-known/assetlinks.json}
docs/online-setup.md                                          (Mehmet's checklist)
shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/
  online/RoomCode.kt            — code alphabet, generate, normalize, invite link build/parse
  online/LobbyModel.kt          — LobbyMeta, Member, ChatMessage, ActionEnvelope, PublishedState, HostStatus, lobby rules (seats, full, spectators)
  online/OnlineAction.kt        — serializable action envelope per game (+ Spin question/answer)
  online/SessionState.kt        — what the host publishes: game, level, player uids/names, game state JSON, Spin texts, seq
  online/HostReferee.kt         — validate actor, apply reducer, skip offline players, Beer Pong pause
  online/LobbyRepository.kt     — interface; online/RtdbLobbyRepository.kt — Realtime Database
  online/LobbySession.kt        — one active lobby: flows for meta/members/chat/state, host loop, commands
  game/GameController.kt        — state + onAction + Seat (Everyone / Player(i) / Spectator)
  game/*/…Screen.kt             (refactor: take a GameController; online extras)
  ui/screens/OnlineLobbyScreen.kt, JoinScreen.kt, ui/components/ChatPanel.kt
  ui/navigation/Routes.kt, ClinkNavHost.kt, ui/screens/ModeSelectScreen.kt, WelcomeScreen.kt
  game/*/… state classes        (@Serializable so the host can publish them)
shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/online/{RoomCodeTest, LobbyModelTest, HostRefereeTest, LobbySessionTest}.kt
```

Realtime Database schema (every task uses it):
- `codes/{CODE}`: `{ lobbyId, game, level, hostName }` — readable by any signed-in user; created once by the host.
- `lobbies/{id}/meta`: `{ hostUid, code, game, level, status: "lobby"|"playing"|"closed", hostOnline, hostSeenAt }`
- `lobbies/{id}/members/{uid}`: `{ name, avatarId, joinedAt, online }`; `lobbies/{id}/removed/{uid}: true`
- `lobbies/{id}/actions/{push}`: `{ uid, json }`; `lobbies/{id}/state`: `{ seq, json }`; `lobbies/{id}/chat/{push}`: `{ uid, name, text, at }`

---

### Task 1: Realtime Database, rules, hosting and App Links

**Files:** libs + build (database), `firebase/database.rules.json`, `firebase.json`, `.firebaserc.example`, `hosting/public/**`, `AndroidManifest.xml`, `MainActivity.kt`, `AndroidShare.kt`, `Platform.kt`, `docs/online-setup.md`
**Produces:** `Platform(contentCache, googleIdTokens, share: (String) -> Unit, joinCodes: StateFlow<String?>, consumeJoinCode: () -> Unit)`.

- [ ] Rules: `codes` create-once by a signed-in user, readable by signed-in users, deletable by that lobby's host; `meta` and `state` written only by the host; a member writes only their own `members/{uid}` (not if `removed/{uid}`, not if `status == "closed"`); the host may delete any member and write `removed`; `actions` and `chat` append-only by members with `uid == auth.uid`, chat text 1–300 characters; lobby readable by its host and members.
- [ ] Hosting: `index.html` (what Clink is + store link), `join/index.html` (shows the code from the URL and how to enter it in the app), `.well-known/assetlinks.json` with the debug SHA-256 (release SHA-256 added in Plan 5); `firebase.json` rewrites `/join/**` to `join/index.html`.
- [ ] Manifest: `https://clink-game.com/join/*` intent filter with `autoVerify`; `MainActivity` passes the code from the link (cold start and `onNewIntent`).
- [ ] `docs/online-setup.md` (Turkish): create the Realtime Database (europe-west1, locked mode), re-download `google-services.json`, deploy rules and hosting with the Firebase CLI, connect `clink-game.com`, test with two phones.
- [ ] Build. Expected: PASS.

### Task 2: Pure lobby model and host referee (TDD)

**Files:** `online/RoomCode.kt`, `online/LobbyModel.kt`, `online/OnlineAction.kt`, `online/SessionState.kt`, `online/HostReferee.kt`, game state classes `@Serializable`; tests `RoomCodeTest`, `LobbyModelTest`, `HostRefereeTest`.
**Produces:**
- `RoomCode.ALPHABET` (no 0/O/1/I/L), `RoomCode.generate(random): String` (6), `RoomCode.normalize(input): String?` (trim, upper, strip spaces/dashes, 6 valid chars or null), `inviteLink(code)`, `codeFromLink(url): String?`.
- `hostStatus(hostOnline: Boolean, hostSeenAt: Long?, now: Long): HostStatus` — `Online`, `Reconnecting(secondsLeft)`, `Gone` (60 s).
- `seats(members, players: List<String> /*uids*/)`, `playerOrder(members): List<Member>` (by `joinedAt`, first 12), `isFull`.
- `HostReferee.start(game, level, players: List<Member>, library, random, beerPongPair?) : SessionState`; `HostReferee.apply(session, envelope, online: Set<String>, library, random): SessionState?` (null = ignored); `HostReferee.skipOffline(session, online, library, random): SessionState?`.

Tests (each written first and seen failing):
```kotlin
// RoomCodeTest
generatesSixCharsFromTheAlphabet; normalizesSpacesDashesAndCase ("ab-c 2d3" → null? no: "abc2d3" → "ABC2D3"); rejectsAmbiguousOrShort; linkRoundTrip; foreignLinkIsNull
// LobbyModelTest
hostOnlineIsOnline; hostAwayCountsDown (30 s gone → Reconnecting(30)); hostAwayOver60sIsGone; playerOrderByJoinTimeFirst12; thirteenthIsFull
// HostRefereeTest
todOnlyCurrentPlayerMayAct; todSpectatorActionIgnored; todOfflineCurrentPlayerIsSkipped;
spinOnlySpinnerSpins; spinOnlyAskerAsksAndOnlyAnswererAnswers; spinQuestionAndAnswerArePublished; spinOfflineSpinnerIsSkipped;
beerPongOnlyThrowerThrows; beerPongSpectatorCannotThrow; beerPongPausesWhileThrowerOffline;
publishesCardIdsNotText; seqGoesUpOnEveryChange
```
- [ ] Run tests. Expected: all PASS.

### Task 3: Lobby repository and session

**Files:** `online/LobbyRepository.kt` (interface), `online/RtdbLobbyRepository.kt`, `online/LobbySession.kt`, `LobbySessionTest` (fake repository).
**Produces:** `LobbyRepository { createLobby(host: Member, game, level): Pair<lobbyId, code>; resolve(code): CodeInfo?; join(lobbyId, me: Member); leave(lobbyId, uid); remove(lobbyId, uid); observe(lobbyId): Flow<LobbySnapshot>; sendAction(lobbyId, uid, json); publish(lobbyId, state); observeActions(lobbyId): Flow<Pair<key, envelope>>; deleteAction(lobbyId, key); sendChat(...); setMeta(...); close(lobbyId); goOnline(lobbyId, uid, isHost) /* presence + onDisconnect */ }` and `LobbySession` (host loop: collect actions → referee → publish; skip offline on member changes; commands `startGame`, `endGame`, `switchGame`, `removePlayer`, `leave`, `close`).
- [ ] Tests: hostAppliesActionsAndPublishes; nonHostNeverPublishes; leavingAsHostClosesTheLobby; removedMemberCannotRejoin (fake honours `removed`); startNeedsTwoPlayers.
- [ ] Run tests + build. Expected: PASS.

### Task 4: Game controllers (local and online)

**Files:** `game/GameController.kt`, the three `…Screen.kt` (take a controller and a `Seat`), local ViewModels implement it, `online/OnlineControllers.kt`.
- [ ] Local play behaves exactly as before (all existing tests stay green; emulator round of each game).
- [ ] Online: buttons only for the seat that may act; others see "Waiting for …"; Spin: asker types the question → large question card for everyone; answerer types the answer or takes a penalty; Beer Pong: only the thrower flicks, everyone replays the published throw, spectators see a "watching" label.

### Task 5: Lobby, join and chat screens

**Files:** `OnlineLobbyScreen.kt`, `JoinScreen.kt`, `ChatPanel.kt`, routes/nav, Mode (Online enabled), Welcome ("Kodla katıl"), strings.
- [ ] Host: Mode → Online → (sign-in gate) → Level (Spicy 18+) → lobby created → lobby screen: code (big), Share, members with avatar and online dot, remove (host), Beer Pong pair picker, Start (≥ 2), Close lobby.
- [ ] Joiner: link or Welcome → "Join with code" → code check (wrong / closed / full messages) → nickname + avatar (guests sign in anonymously; a signed-in user's profile prefills) → Spicy 18+ if the lobby is Spicy → lobby.
- [ ] Chat: button on lobby and game top bars, panel slides over the screen, messages with avatar and name, 300 limit.
- [ ] After a game ends everyone returns to the lobby; the host can start again or switch game/level.

### Task 6: Presence and reconnect

- [ ] `goOnline` sets `online` (and the host's `hostOnline/hostSeenAt`) with `onDisconnect`; members' screens show "Host reconnecting · 42" and leave after 60 s; the host's app re-claims presence when it reconnects; a dropped player can rejoin from the same link and gets their seat back.
- [ ] Full suite + build. Expected: PASS. Two-device check is Mehmet's (after his setup).

### Task 7: Final review

- [ ] Whole-plan review on the most capable model; fix Critical/Important test-first; report to Mehmet with his checklist and the single commit message.
