# Plan 4b: UX Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A newcomer understands Clink and joins a lobby within 30 seconds: a clean home screen, two context-based onboarding tours with live in-app previews, and seven rough edges fixed.

**Architecture:** A small local preferences store (seen flags, remembered guest identity) behind `Platform`, a pure tour-pager state machine, one shared `OnboardingPager` composable that shows real composables scaled inside a `PhoneFrame`, and two tours wired in where they're needed (Join screen inline, Play as its own route). Lobby polish adds an `offline` flow to `LobbySession`.

**Tech Stack:** Kotlin 2.4 Multiplatform (Android target), Compose Multiplatform 1.12 (`HorizontalPager`, `FlowRow`), kotlinx.serialization, kotlinx-coroutines-test; MVI with pure reducers.

**Spec:** `docs/superpowers/specs/2026-11-23-ux-polish-design.md`

## Global Constraints

- Onboarding is never on the home screen; every card has **Skip** (`GEÇ` / `SKIP`); each tour shows automatically **once**; skip, finish or back-out all mark it seen; replays come only from "Nasıl oynanır?" (Play tour) and "Online nasıl işler?" (Join tour).
- Visuals are the app's own composables with sample data in a phone frame, never drawings or PNG screenshots.
- Join tour: 3 cards; Play tour: 4 cards. Titles exactly as the spec tables (§5.3, §5.4).
- All copy in TR (`values-tr`) and EN (`values`) string resources; informal "sen".
- System back: previous card; on the first card it leaves to where the user came from and never continues into sign-in.
- Local and online behavior otherwise unchanged; the existing 194 tests stay green.
- Git (Mehmet, 2026-11-23): this plan ends in **one commit by Mehmet**; nobody commits, pushes or rewrites history during execution, and tasks don't stop for commits.

## Review Focus

1. **Missing or corrupt preferences file** (first install, crash mid-write) → defaults (tours unseen, no guest identity), no crash — `PrefsRepositoryTest.corruptFileGivesDefaults` (Task 1).
2. **Back on the first card of the Play tour** → leaves to Welcome, marks seen, never opens Google sign-in — `TourStateTest.backOnFirstCardLeaves` (Task 3) and the nav wiring in Task 4.
3. **The phone's own connection drops for a moment** (or is still connecting at start) → no banner flash; the banner appears only after 2 s offline — `LobbySessionTest.offlineBannerWaitsTwoSeconds` (Task 7).
4. **A remembered guest name that's now invalid** (blank or longer than 20) → not prefilled raw; cleaned or empty — `GuestDefaultsTest.rememberedNameIsCleaned` (Task 6).
5. **A signed-in user with a profile joins** → the profile's name/avatar win over a remembered guest identity — `GuestDefaultsTest.profileWinsOverRememberedGuest` (Task 6).

---

### Task 1: Local preferences store

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/prefs/LocalPrefs.kt` (`LocalStore`, `LocalPrefs`, `PrefsRepository`)
- Create: `androidApp/src/main/kotlin/com/mehmetbozkurt/clink/FileLocalStore.kt`
- Modify: `Platform.kt` (add `localStore`), `AppContainer.kt` (add `prefs`), `App.kt` (build and `load()` it), `MainActivity.kt` (pass `FileLocalStore(applicationContext)`)
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/prefs/PrefsRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `interface LocalStore { suspend fun read(): String?; suspend fun write(text: String) }` (same shape as `ContentCache`)
  - `@Serializable data class LocalPrefs(val joinTourSeen: Boolean = false, val playTourSeen: Boolean = false, val guestName: String? = null, val guestAvatar: String? = null)`
  - `class PrefsRepository(store: LocalStore) { val prefs: StateFlow<LocalPrefs>; suspend fun load(); suspend fun update(change: (LocalPrefs) -> LocalPrefs) }`
  - `AppContainer.prefs: PrefsRepository`

- [ ] **Step 1: Write the failing tests** (fake `LocalStore` holding a `String?`)
  - `emptyStoreGivesDefaults`: after `load()`, `prefs.value == LocalPrefs()`.
  - `corruptFileGivesDefaults`: store text `"{not json"` → `load()` doesn't throw, `prefs.value == LocalPrefs()`.
  - `updateIsSavedAndReloaded`: `update { it.copy(joinTourSeen = true, guestName = "Ayşe") }` → a new `PrefsRepository` on the same store, after `load()`, has `joinTourSeen == true` and `guestName == "Ayşe"`.
  - `unknownFieldsAreIgnored`: store text `{"playTourSeen":true,"later":1}` → `playTourSeen == true`.
- [ ] **Step 2: Run and see them fail**
  Run: `.\gradlew.bat :shared:testAndroidHostTest --tests "*PrefsRepositoryTest" -q`
  Expected: FAIL, unresolved `PrefsRepository`.
- [ ] **Step 3: Implement** `LocalPrefs.kt` (JSON with `ignoreUnknownKeys`; write failures are swallowed like `ContentCache` users do) and `FileLocalStore(context)` writing `local-prefs.json` in **`context.noBackupFilesDir`**, so Auto Backup can't restore seen flags after a reinstall (spec §5.2). Wire into `Platform`, `AppContainer`, `App.kt` (`prefs.load()` before the container is published), `MainActivity`.
- [ ] **Step 4: Run** the same command. Expected: PASS. Then the full suite: `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → 198 tests, 0 failures.
- [ ] **Step 5: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: add a local preferences store for onboarding and guest identity`

### Task 2: Welcome redesign and the Mode "ONLINE" fix

**Files:**
- Modify: `ui/screens/WelcomeScreen.kt`, `ui/screens/ModeSelectScreen.kt`, `ui/navigation/ClinkNavHost.kt` (new `onHowToPlay`), both `strings.xml`

**Interfaces:**
- Produces: `WelcomeScreen(profile: Profile?, onPlay: () -> Unit, onJoin: () -> Unit, onHowToPlay: () -> Unit, onAccount: () -> Unit)`. Until Task 4 exists, `onHowToPlay` is wired to `{}`.

- [ ] **Step 1: Welcome** per spec §5.1: remove the game `WallChip` list and `GameIconTints`; logo + drips + tape tagline vertically centred in the free space; `OYNA` (PrimaryButton) and `KODLA KATIL` (SecondaryButton, pink as today) full width and equal; below them a 48 dp text link `how_to_play` = "Nasıl oynanır?" / "How to play" in `ClinkColors.Muted`.
- [ ] **Step 2: Mode fix** (spec §5.5 #1): the mode titles stop going through `toUpperCase(Locale.current)`; add `mode_local_title` = "ARKADAŞLARLA" / "WITH FRIENDS" and `mode_online_title` = "ONLINE" / "ONLINE" written upper-case in the resources, and use those.
- [ ] **Step 3: Verify** `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → 198/198, build OK. On the emulator (TR): Welcome shows no game list, two equal buttons, the link; Mode shows "ONLINE" without a dotted İ.
- [ ] **Step 4: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: simplify the home screen and fix the Online label`

### Task 3: Tour pager (logic and shared UI)

**Files:**
- Create: `ui/onboarding/TourState.kt` (pure), `ui/onboarding/OnboardingPager.kt`, `ui/onboarding/PhoneFrame.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/ui/onboarding/TourStateTest.kt`

**Interfaces:**
- Produces:
  - `enum class TourExit { COMPLETED, SKIPPED, BACKED_OUT }`
  - `data class TourState(val page: Int, val pageCount: Int, val exit: TourExit? = null)` with `fun next(): TourState`, `fun back(): TourState`, `fun skip(): TourState`, `fun goTo(page: Int): TourState` (swipe).
  - `data class TourCard(val title: StringResource, val body: StringResource, val preview: @Composable () -> Unit)`
  - `@Composable fun OnboardingPager(cards: List<TourCard>, onExit: (TourExit) -> Unit)` — Skip top right, page dots, `next_button` "İLERİ"/"NEXT", last page `start_button` "BAŞLA"/"START"; system back calls `back()`.
  - `@Composable fun PhoneFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit)` — draws `content` at a fixed virtual size of **360 × 720 dp**, scaled to fit the frame, input disabled, hidden from screen readers.

- [ ] **Step 1: Write the failing tests**
  - `nextMovesOnAndCompletesOnLastPage`: `TourState(0, 3).next().next()` has `page == 2`, `exit == null`; one more `next()` → `exit == COMPLETED`.
  - `backOnFirstCardLeaves`: `TourState(0, 4).back().exit == BACKED_OUT`.
  - `backGoesToPreviousCard`: `TourState(2, 4).back()` → `page == 1`, `exit == null`.
  - `skipEndsFromAnyCard`: `TourState(1, 3).skip().exit == SKIPPED`.
  - `goToIsClamped`: `TourState(0, 3).goTo(9).page == 2`.
- [ ] **Step 2: Run and see them fail**: `.\gradlew.bat :shared:testAndroidHostTest --tests "*TourStateTest" -q` → FAIL, unresolved `TourState`.
- [ ] **Step 3: Implement** `TourState`, then `OnboardingPager` (Compose `HorizontalPager` kept in sync with `TourState`; title via `ScreenTitle`, body `bodyLarge` Paper; preview fills the upper half inside `PhoneFrame`) and `PhoneFrame` (rounded raised frame, 2 dp Paper/14% border). Strings: `skip_button`, `next_button`, `start_button`.
- [ ] **Step 4: Run** → PASS; full suite 203/203, build OK.
- [ ] **Step 5: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: add the onboarding pager and phone-frame previews`

### Task 4: Play tour

**Files:**
- Create: `ui/onboarding/PlayTour.kt` (`fun playTourCards(): List<TourCard>` as a `@Composable` builder)
- Modify: `ui/navigation/Routes.kt` (`@Serializable data class PlayTourRoute(val replay: Boolean)`), `ClinkNavHost.kt`, both `strings.xml`; make the composables used as previews `internal` if they aren't.

**Interfaces:**
- Consumes: `OnboardingPager`, `TourCard`, `TourExit`, `PhoneFrame` (Task 3); `AppContainer.prefs` (Task 1); `WelcomeScreen.onHowToPlay` (Task 2).

- [ ] **Step 1: Cards** (spec §5.4), previews inside `PhoneFrame`:
  1. `tour_play_1_title` "Üç oyun, tek uygulama" / "Three games, one app"; preview `GameListScreen(onGame = {}, onBack = {})`.
  2. `tour_play_2_title` "Tek telefon ya da online" / "One phone or online"; preview `ModeSelectScreen(GameId.TRUTH_OR_DARE, {}, {}, {})`.
  3. `tour_play_3_title` "Seviyeni seç" / "Pick a level"; preview `LevelSelectScreen({}, {})`.
  4. `tour_play_4_title` "Kuranın hesabı olur" / "The host has an account"; preview a `Poster` with `AvatarBadge("cat", sample name, 64.dp)` and the sample name `tour_sample_host` ("Ayşe" / "Alex").
  Bodies `tour_play_1_body` … `tour_play_4_body` = the spec §5.4 body column.
- [ ] **Step 2: Wire it** in `ClinkNavHost`:
  - Welcome `onPlay`: if `!prefs.value.playTourSeen` → `navigate(PlayTourRoute(replay = false))`, else `goByGate()` as today.
  - Welcome `onHowToPlay` → `navigate(PlayTourRoute(replay = true))`.
  - `PlayTourRoute` exit: always `prefs.update { it.copy(playTourSeen = true) }`; then `replay` or `BACKED_OUT` → `popBackStack()`; `COMPLETED`/`SKIPPED` (not replay) → `goByGate { popUpTo<PlayTourRoute> { inclusive = true } }`.
- [ ] **Step 3: Verify**: suite 203/203, build OK. Emulator with app data cleared (`adb shell pm clear com.mehmetbozkurt.clink`): Oyna → 4 cards → BAŞLA → sign-in screen; second Oyna → no tour; back on card 1 → Welcome; "Nasıl oynanır?" → tour → back/finish → Welcome. Both languages.
- [ ] **Step 4: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: add the Play onboarding tour`

### Task 5: Join tour

**Files:**
- Create: `ui/onboarding/JoinTour.kt` (`fun joinTourCards(): List<TourCard>`)
- Modify: `ui/online/JoinScreen.kt`, `ui/online/OnlineRoomScreen.kt` (`MemberRow` and `CodeCard` become `internal`), both `strings.xml`

**Interfaces:**
- Consumes: Task 3 pager; `AppContainer.prefs`.
- Produces: `JoinScreen(initialCode: String?, profile: Profile?, onJoined: () -> Unit, onBack: () -> Unit)` — unchanged signature; the tour is inline.

- [ ] **Step 1: Cards** (spec §5.3):
  1. `tour_join_1_title` "Herkes kendi telefonunda" / "Everyone on their own phone"; preview two `MemberRow`s (host `tour_sample_host` with the Kurucu badge, `tour_sample_guest` "Mert"/"Sam"), both online.
  2. `tour_join_2_title` "Kod kurucudan gelir" / "The code comes from the host"; preview `CodeCard("X2KW2T", onShare = {})`.
  3. `tour_join_3_title` "Sıra sende olunca oynarsın" / "Play when it's your turn"; preview two `PhoneFrame`s side by side: one with the Truth/Dare `Choosing` panels, one with `WaitingLabel(online_waiting_for(tour_sample_host))`.
  Bodies `tour_join_1_body` … `tour_join_3_body` = spec §5.3.
- [ ] **Step 2: Wire it** in `JoinScreen`: `var touring by rememberSaveable { mutableStateOf(!prefs.value.joinTourSeen) }`; while touring show `OnboardingPager(joinTourCards())`. Exit: mark `joinTourSeen`; `BACKED_OUT` (not a replay) → `onBack()`; otherwise `touring = false`. The existing `LaunchedEffect` that looks up an invite code runs only when `!touring` (key it on `touring`). Add the text link `join_how_it_works` "Online nasıl işler?" / "How online works" under the code field → `touring = true` as a replay (back then just ends the replay).
- [ ] **Step 3: Verify**: suite 203/203, build OK. Emulator, data cleared: Kodla katıl → 3 cards → code screen; invite link (`adb shell am start -a android.intent.action.VIEW -d https://clink-game.com/join/ABC234 com.mehmetbozkurt.clink`) on a fresh install → 3 cards → the lookup starts after them; second time no cards; link replays.
- [ ] **Step 4: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: add the Join onboarding tour`

### Task 6: Remembered guest identity

**Files:**
- Create: `ui/online/GuestDefaults.kt`
- Modify: `ui/online/JoinScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/ui/online/GuestDefaultsTest.kt`

**Interfaces:**
- Consumes: `LocalPrefs`, `PrefsRepository.update` (Task 1); `Profile`, `Avatars.ids`, `ProfileInput.validate`.
- Produces: `data class GuestDefaults(val name: String, val avatarId: String)` and `fun guestDefaults(profile: Profile?, prefs: LocalPrefs, random: Random): GuestDefaults`.

- [ ] **Step 1: Write the failing tests**
  - `profileWinsOverRememberedGuest`: profile ("Mehmet", "dog") + prefs (guestName "Emu", guestAvatar "robot") → ("Mehmet", "dog").
  - `rememberedGuestIsUsed`: no profile, prefs ("Emu", "robot") → ("Emu", "robot").
  - `rememberedNameIsCleaned`: no profile, guestName `"  " + "x".repeat(30)` → name `"x".repeat(20)`; guestName `"   "` → name `""`.
  - `unknownRememberedAvatarFallsBack`: guestAvatar `"dragon"` → an id from `Avatars.ids`.
  - `firstTimeGuestGetsARandomAvatar`: empty prefs with `SequenceRandom` picking index 5 → avatar `Avatars.ids[5]`, name `""`.
- [ ] **Step 2: Run and see them fail**: `--tests "*GuestDefaultsTest"` → FAIL, unresolved `guestDefaults`.
- [ ] **Step 3: Implement** `guestDefaults`; in `JoinScreen` seed `name`/`avatar` from it; after a successful `JoinResult.Joined` for a user **without** a profile, `prefs.update { it.copy(guestName = cleanName, guestAvatar = avatar) }`.
- [ ] **Step 4: Run** → PASS; suite 208/208, build OK.
- [ ] **Step 5: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: remember a guest's name and avatar`

### Task 7: Lobby polish

**Files:**
- Modify: `online/LobbySession.kt`, `ui/online/OnlineRoomScreen.kt`, both `strings.xml`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/online/LobbySessionTest.kt`

**Interfaces:**
- Produces: `LobbySession.offline: StateFlow<Boolean>` — true only after `repo.connected()` has been false for **2 000 ms** (`OFFLINE_BANNER_DELAY_MILLIS`), false again as soon as it's true.

- [ ] **Step 1: Write the failing tests** (fake repo's `isConnected`, virtual time)
  - `offlineBannerWaitsTwoSeconds`: set `isConnected = false`, `advanceTimeBy(1_999)` → `offline.value == false`; `advanceTimeBy(2)` → `true`.
  - `offlineBannerClearsOnReconnect`: after it's true, `isConnected = true`, `runCurrent()` → `false`.
- [ ] **Step 2: Run and see them fail**: `--tests "*LobbySessionTest"` → FAIL, unresolved `offline`.
- [ ] **Step 3: Implement** `offline` (`connected().catch{}` + `transformLatest` with the delay, `stateIn(scope, Eagerly, false)`); in `OnlineRoom`:
  - an amber banner `offline_banner` "Bağlantı yok, yeniden bağlanılıyor…" / "No connection, reconnecting…" at the top while `offline`, in the lobby and in games (above the host-reconnecting chip when both show);
  - host alone (`players.size == 1`): `lobby_alone_hint` "Arkadaşlarına kodu ya da daveti gönder" / "Send your friends the code or the invite" above the code card;
  - the game and level chip rows become `FlowRow` (wrap) instead of horizontal scroll.
- [ ] **Step 4: Run** → PASS; suite 210/210, build OK.
- [ ] **Step 5: Done** — no commit (one commit at the end of the plan); summary line for the final message: `feat: show this phone's connection state and tidy the lobby`

### Task 8: Final check

- [ ] **Step 1:** Full suite + build: `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → 210/210, exit 0.
- [ ] **Step 2:** Emulator walk-through in TR and EN (switch with `adb shell cmd locale set-app-locales com.mehmetbozkurt.clink --locales en`, restore after): fresh install → both tours, skip/back/finish, replays, Welcome, Mode label, lobby banner (emulator airplane mode, restored after), alone hint, wrapped chips, remembered guest name.
- [ ] **Step 3:** With Mehmet's OK in that turn: the same tours on his S22 (`pm clear` only with his OK, it signs him out).
- [ ] **Step 4:** Whole-plan review on the most capable model; fix Critical/Important test-first; report rulings and deferred minors to Mehmet.
