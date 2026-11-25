# Plan 6: Home Screen and Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A livelier home screen (three decorative game posters, a game count, a moving wall) and one tidy Settings screen (profile, language, haptics, tours, account, version) that replaces the Account screen.

**Architecture:** Pure decisions (which profile card, which language, featured games, haptics on/off) live in small common files with unit tests. The Android side adds a per-app language setting, the version name and a "reduce motion" flag through `Platform` → `AppContainer`. The home screen draws its posters and wall art on top of the existing `WallBackground`, which animates its two glows only while the home screen asks it to.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.12, Navigation Compose (type-safe routes), kotlinx.serialization, Android minSdk 26 (`ComponentActivity`, no AppCompat).

**Spec:** `docs/superpowers/specs/2026-11-24-home-and-settings-design.md`

## Global Constraints

- Dark only; no theme row in settings (light theme is the next round).
- Home posters are **not tappable**; the game count line is **not a link** (no arrow). OYNA is the only way to the games.
- Featured games: a constant in code, exactly three: Truth or Dare, Spin the Bottle, Cup Pong.
- Game count = `GameId.entries.size`, text "all games: N" / "tüm oyunlar: N".
- Wall motion (breathing glows 9 s / 11 s, light sweep every ~12 s, poster sway ±1° over ~4 s) on the **home screen only**, and none at all when the phone's animations are off.
- Spray tags "wow", "gg", "cheers" are not translated.
- Every new user-facing string in both `values/strings.xml` (English) and `values-tr/strings.xml` (Turkish).
- New `LocalPrefs` fields have defaults (old files must still load).
- Settings ✎ for a Google user goes through `AccountSession.gate()` first; it never opens an empty editor that could overwrite an unread profile.
- Commits: Mehmet's waiver for this plan — **no commit per task**; one commit at the end, message proposed by the executor, Mehmet commits. Never run `git commit`/`push`/`reset`/`stash`/`checkout --`.
- Nothing on Mehmet's phone without his OK in that turn. The emulator (emulator-5554) may be used; any emulator setting changed is restored.

## Review Focus

1. A `local-prefs.json` written by the current app (no `hapticsOn`) must load with haptics **on** → test in Task 1.
2. An anonymous guest (signed in anonymously after joining a lobby) must see the guest card and **no** account section → test in Task 1.
3. A guest saving a blank name in the guest editor must get the same "name required" error as the host, and nothing saved → the editor keeps its one validation path (`ProfileInput.validate`, covered by `ProfileTest.blankIsRejected`) before `onSave`; checked on the emulator in Task 6.
4. Haptics turned off must silence calls made by screens that captured the haptics object earlier (toggle read at call time, not at capture) → test in Task 2.
5. Changing the language while in an online lobby recreates the activity; the lobby must survive (`OnlineProcess` keeps the launcher) → manual check in Task 6. The Android 8–12 language path can't be checked on the available devices → reviewer reads it.

## Files

- Create `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/prefs/AppLanguage.kt` — `AppLanguage`, `LanguageSetting`.
- Create `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/FeaturedGames.kt`.
- Create `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/settings/SettingsModel.kt` — `ProfileCard`, `profileCard`, `showsAccountSection`.
- Create `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/components/SwitchableHaptics.kt`.
- Create `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/settings/SettingsScreen.kt` (replaces `ui/screens/AccountScreen.kt`, which is deleted).
- Create `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/HomePosters.kt`, `.../ui/screens/HomeWall.kt`.
- Create `shared/src/commonMain/composeResources/drawable/ic_settings.xml`.
- Create `androidApp/src/main/kotlin/com/mehmetbozkurt/clink/AndroidLanguageSetting.kt`, `androidApp/src/main/res/xml/locales_config.xml`.
- Modify `prefs/LocalPrefs.kt`, `Platform.kt`, `AppContainer.kt`, `App.kt`, `ui/components/WallBackground.kt`, `ui/screens/WelcomeScreen.kt`, `ui/screens/ProfileSetupScreen.kt`, `ui/navigation/Routes.kt`, `ui/navigation/ClinkNavHost.kt`, `MainActivity.kt`, `AndroidManifest.xml`, both `strings.xml`.
- Tests: `commonTest/.../prefs/AppLanguageTest.kt`, `.../prefs/PrefsRepositoryTest.kt` (add), `.../game/FeaturedGamesTest.kt`, `.../ui/settings/SettingsModelTest.kt`, `.../ui/components/SwitchableHapticsTest.kt`, `.../account/ProfileTest.kt` or a new `GuestProfileTest.kt` (Task 4).

Test commands (PowerShell, repo root): one class `.\gradlew.bat :shared:testAndroidHostTest --tests "*<Class>" -q`; full `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q`, results in `shared\build\test-results\testAndroidHostTest\*.xml`. Baseline: 231 tests, all passing.

---

### Task 1: Pure settings and home models

**Files:** Create `prefs/AppLanguage.kt`, `game/FeaturedGames.kt`, `ui/settings/SettingsModel.kt`; modify `prefs/LocalPrefs.kt`. Tests as listed.

**Interfaces — Produces:**
- `enum class AppLanguage(val tag: String) { SYSTEM(""), TURKISH("tr"), ENGLISH("en"); companion object { fun fromTag(tag: String?): AppLanguage } }` — matches on the language part (`"tr-TR"` → TURKISH); null, blank or unknown → SYSTEM.
- `interface LanguageSetting { fun current(): AppLanguage; fun set(language: AppLanguage) }` (same file; implemented in Task 3).
- `object FeaturedGames { val ids: List<GameId> }` = `[TRUTH_OR_DARE, SPIN_THE_BOTTLE, CUP_PONG]`.
- `LocalPrefs.hapticsOn: Boolean = true`.
- `sealed interface ProfileCard { data class Google(val name: String, val avatarId: String) : ProfileCard; data class Guest(val name: String?, val avatarId: String?) : ProfileCard }`
- `fun profileCard(user: AuthUser?, profile: Profile?, prefs: LocalPrefs): ProfileCard` — non-anonymous user → `Google(profile?.displayName ?: user.googleName.orEmpty(), profile?.avatarId ?: Avatars.ids.first())`; otherwise `Guest(validated prefs.guestName or null, prefs.guestAvatar if in Avatars.ids else null)`.
- `fun showsAccountSection(user: AuthUser?): Boolean` — true only for a signed-in, non-anonymous user.

- [ ] **Step 1: Write the failing tests**

```kotlin
// AppLanguageTest
@Test fun tagsRoundTrip() = AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
@Test fun regionIsIgnored() = assertEquals(AppLanguage.TURKISH, AppLanguage.fromTag("tr-TR"))
@Test fun unknownIsSystem() { assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("de")); assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null)) }

// FeaturedGamesTest
@Test fun threeDistinctRealGames() { assertEquals(3, FeaturedGames.ids.distinct().size); assertTrue(GameId.entries.containsAll(FeaturedGames.ids)) }

// PrefsRepositoryTest (add)
@Test fun oldFileWithoutHapticsKeepsThemOn() = runTest {
    val repository = PrefsRepository(FakeStore("""{"joinTourSeen":true,"guestName":"Ayşe"}"""))
    repository.load()
    assertTrue(repository.prefs.value.hapticsOn)
    assertEquals("Ayşe", repository.prefs.value.guestName)
}

// SettingsModelTest
private val google = AuthUser("u1", isAnonymous = false, googleName = "Mehmet B")
private val anon = AuthUser("u2", isAnonymous = true, googleName = null)
@Test fun googleUserWithProfileShowsProfile() = assertEquals(ProfileCard.Google("Mehmet", "cat"), profileCard(google, Profile("Mehmet", "cat"), LocalPrefs()))
@Test fun googleUserWithoutProfileShowsGoogleName() = assertEquals(ProfileCard.Google("Mehmet B", Avatars.ids.first()), profileCard(google, null, LocalPrefs()))
@Test fun anonymousGuestShowsGuestIdentity() = assertEquals(ProfileCard.Guest("Ayşe", Avatars.ids[1]), profileCard(anon, null, LocalPrefs(guestName = "Ayşe", guestAvatar = Avatars.ids[1])))
@Test fun signedOutWithNothingIsBlankGuest() = assertEquals(ProfileCard.Guest(null, null), profileCard(null, null, LocalPrefs()))
@Test fun unknownGuestAvatarIsDropped() = assertEquals(ProfileCard.Guest("Ayşe", null), profileCard(null, null, LocalPrefs(guestName = "Ayşe", guestAvatar = "nope")))
@Test fun accountSectionOnlyForGoogle() { assertTrue(showsAccountSection(google)); assertFalse(showsAccountSection(anon)); assertFalse(showsAccountSection(null)) }
```

(`"cat"` is a real id in `Avatars.ids`.)

- [ ] **Step 2: Run, expect FAIL** (unresolved references `AppLanguage`, `FeaturedGames`, `profileCard`, `hapticsOn`).
- [ ] **Step 3: Implement the five interfaces above.** Guest name validation reuses `ProfileInput.validate` exactly as `guestDefaults` does.
- [ ] **Step 4: Run the new test classes, expect PASS; full suite green.**

### Task 2: Haptics switch

**Files:** Create `ui/components/SwitchableHaptics.kt`; modify `App.kt`. Test `ui/components/SwitchableHapticsTest.kt`.

**Interfaces — Consumes:** `LocalPrefs.hapticsOn`. **Produces:** `class SwitchableHaptics(private val platform: HapticFeedback, private val enabled: () -> Boolean) : HapticFeedback` — forwards `performHapticFeedback` only while `enabled()` is true, read at each call.

- [ ] **Step 1: Write the failing tests** with a recording fake `HapticFeedback`:
  - `offIsSilent`: enabled = false → fake records 0 calls.
  - `onForwards`: enabled = true → fake records the type passed.
  - `toggleIsReadAtCallTime`: one instance, flag flipped true→false between two calls → exactly 1 recorded.
- [ ] **Step 2: Run, expect FAIL** (no `SwitchableHaptics`).
- [ ] **Step 3: Implement**, then in `App.kt` provide it next to `LocalAppContainer`: capture `LocalHapticFeedback.current` outside, `remember` one `SwitchableHaptics(platformHaptics) { loaded.prefs.prefs.value.hapticsOn }`, and provide it as `LocalHapticFeedback`. No game screen changes.
- [ ] **Step 4: Run, expect PASS; full suite green.**

### Task 3: Platform: language, version, reduce motion

**Files:** Modify `Platform.kt`, `AppContainer.kt`, `App.kt`, `MainActivity.kt`, `AndroidManifest.xml`; create `AndroidLanguageSetting.kt`, `res/xml/locales_config.xml`.

**Interfaces — Consumes:** `AppLanguage`, `LanguageSetting` (Task 1). **Produces:** `Platform` and `AppContainer` each gain `language: LanguageSetting`, `versionName: String`, `reduceMotion: Boolean`; `App.kt` passes them through.

- [ ] **Step 1: `AndroidLanguageSetting(private val activity: Activity) : LanguageSetting`**
  - API 33+: `LocaleManager.applicationLocales` / `setApplicationLocales(LocaleList.forLanguageTags(tag))` (empty list for SYSTEM); the system recreates the activity.
  - API 26–32: the tag in `SharedPreferences("clink_language")`, key `"tag"`, written with `commit()`, then `activity.recreate()`; plus a `companion fun wrap(base: Context): Context` used by `MainActivity.attachBaseContext` that, below API 33 and with a saved non-blank tag, calls `Locale.setDefault` and returns `base.createConfigurationContext(config with setLocale)`.
- [ ] **Step 2: `locales_config.xml`** with `en` and `tr`; manifest `<application android:localeConfig="@xml/locales_config">`.
- [ ] **Step 3: `MainActivity`** — `attachBaseContext(AndroidLanguageSetting.wrap(newBase))`; `Platform(…, language = AndroidLanguageSetting(this), versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(), reduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f)`.
- [ ] **Step 4: Verify** — full suite green and `:androidApp:assembleDebug` builds. (Behaviour is checked on the emulator in Task 6.)

### Task 4: Settings screen and navigation

**Files:** Create `ui/settings/SettingsScreen.kt`, `drawable/ic_settings.xml` (a gear, same stroke style as `ic_back`); delete `ui/screens/AccountScreen.kt`; modify `ProfileSetupScreen.kt`, `Routes.kt`, `ClinkNavHost.kt`, both `strings.xml`. Test: `account/GuestProfileTest.kt`.

**Interfaces — Consumes:** Task 1 `profileCard`, `showsAccountSection`, `AppLanguage`; Task 3 `container.language`, `container.versionName`. **Produces:**
- Routes: remove `AccountRoute`; add `data object SettingsRoute`, `data object GuestProfileRoute`, `data object JoinTourRoute`.
- `ProfileSetupScreen(initialName: String, initialAvatar: String, onSave: suspend (Profile) -> Unit, onSaved: () -> Unit, onBack: () -> Unit)` — the host route passes `session::save`; `GuestProfileRoute` passes a save that writes `guestName`/`guestAvatar` to `container.prefs`.
- `suspend fun saveGuestProfile(prefs: PrefsRepository, profile: Profile)` in `account/` (so the test can reach it).
- `SettingsScreen(session: AccountSession, onEditProfile: () -> Unit, onPlayTour: () -> Unit, onJoinTour: () -> Unit, onSignedOut: () -> Unit, onDeleted: () -> Unit, onBack: () -> Unit)` — reads `container` for prefs, auth user, language and version; keeps today's sign-out and delete code (moved from `AccountScreen`, unchanged).

- [ ] **Step 1: Write the failing tests** (`GuestProfileTest`, with the `FakeStore` pattern from `PrefsRepositoryTest`):
  - `guestSaveIsRemembered`: `saveGuestProfile(prefs, Profile("Ayşe", Avatars.ids[2]))` → prefs hold `guestName == "Ayşe"`, `guestAvatar == Avatars.ids[2]`.
  - `guestSaveKeepsOtherPrefs`: with `joinTourSeen = true` and `hapticsOn = false` already stored, a guest save leaves both unchanged.
- [ ] **Step 2: Run, expect FAIL** (no `saveGuestProfile`).
- [ ] **Step 3: Implement.** Screen layout per spec §5 (title "Ayarlar", back arrow, scrolls):
  - Profile card (poster style, `AvatarBadge` 64 dp; `Guest` with null avatar shows the first avatar dimmed, null name shows "Misafir"/"Guest"); subtitle by card type; ✎ icon button → `onEditProfile`.
  - Section "OYUN": Language row with three `WallChip`s (System / Türkçe / English), the current one selected, tap → `container.language.set(...)` when different; Haptics row with a Switch bound to `hapticsOn` (acid when on).
  - Section "YARDIM": "Nasıl oynanır?" → `onPlayTour`; "Kodla katılma nasıl?" → `onJoinTour`.
  - Section "HESAP" only when `showsAccountSection(user)`: sign out, then the quiet delete text, as on today's Account screen.
  - Footer "Clink · sürüm <versionName>".
- [ ] **Step 4: Navigation in `ClinkNavHost`:**
  - `SettingsRoute` → `SettingsScreen`. `onEditProfile`: for a Google user run `session.gate()` (guarded like `goByGate`, one job at a time); `PlayGate.SignIn` → `SignInRoute`, otherwise → `ProfileSetupRoute(editing = true)`. For everyone else → `GuestProfileRoute`. `onSignedOut`/`onDeleted` → pop to `WelcomeRoute` (as today).
  - `GuestProfileRoute` → `ProfileSetupScreen` prefilled from `guestDefaults(null, prefs, random)`; saved → pop back.
  - `JoinTourRoute` → `OnboardingPager(joinTourCards(), lastLabel = done_button)`; on exit mark `joinTourSeen = true` and pop (only if still on top, as the Play tour does).
  - `onPlayTour` → `PlayTourRoute(replay = true)`.
- [ ] **Step 5: Strings** (EN / TR): `settings_title` Settings / Ayarlar; `cd_settings` Settings / Ayarlar; `cd_edit_profile` Edit profile / Profili düzenle; `settings_signed_in_google` Signed in with Google / Google hesabıyla giriş; `settings_guest` Guest · on this phone / Misafir · bu telefonda; `settings_guest_name` Guest / Misafir; `settings_section_game` GAME / OYUN; `settings_language` Language / Dil; `language_system` System / Sistem; `language_tr` Türkçe / Türkçe; `language_en` English / English; `settings_haptics` Haptics / Titreşim; `settings_section_help` HELP / YARDIM; `settings_how_to_join` How does joining work? / Kodla katılma nasıl?; `settings_section_account` ACCOUNT / HESAP; `settings_version` Clink · version %1$s / Clink · sürüm %1$s. Reuse `how_to_play`, `account_sign_out`, `account_delete`, delete dialog strings. Remove `account_title` and `cd_account` if nothing uses them any more.
- [ ] **Step 6: Run the new test, expect PASS; full suite green.**

### Task 5: Home screen

**Files:** Modify `WelcomeScreen.kt`, `WallBackground.kt`, `ClinkNavHost.kt`, both `strings.xml`; create `HomePosters.kt`, `HomeWall.kt`.

**Interfaces — Consumes:** `FeaturedGames.ids`, `container.reduceMotion`, `GameId.title()` (ScreenParts). **Produces:**
- `WelcomeScreen(animated: Boolean, onPlay: () -> Unit, onJoin: () -> Unit, onSettings: () -> Unit)` — the `profile`, `onHowToPlay`, `onAccount` parameters are removed; NavHost passes `animated = !container.reduceMotion`, `onSettings` → `SettingsRoute`.
- `val LocalWallMotion: ProvidableCompositionLocal<MutableState<Boolean>>` in `WallBackground.kt`; `WallBackground` provides one state and, while it is true, animates its two glows: pink 9 s and acid 11 s cycles (`infiniteRepeatable`, reverse), drift ≈ 10 % of the width toward the centre, scale 1 → 1.15 (pink) / 1.2 (acid), alpha 0.7 ↔ 1. `WelcomeScreen` sets it to `animated` in a `DisposableEffect` and back to false on dispose.
- `@Composable fun HomePosters(games: List<GameId>, animated: Boolean, modifier: Modifier)`.
- `@Composable fun HomeWallArt(animated: Boolean, modifier: Modifier)`.

- [ ] **Step 1: Strings** — `home_game_count` all games: %1$d / tüm oyunlar: %1$d; `game_tod_players` 2–12 players / 2–12 kişi; `game_spin_players` 2–12 players / 2–12 kişi; `game_pong_players` 1v1 / 1v1; `cd_home_posters` Games: %1$s / Oyunlar: %1$s. A `GameId.players()` helper next to `GameId.meta()` in `ScreenParts.kt`.
- [ ] **Step 2: `HomeWallArt`** (drawn first in `WelcomeScreen`, fills it, behind everything): spray-font tags "wow" (Pink), "gg" (Paper), "cheers" (Acid) at 9–10 % alpha, rotated −10° / +8° / −6°, plus a crown and a star stroked in Acid / Paper at 10 % — placed in the corners and gaps (top-right under the gear row, bottom-left and bottom-right above the buttons) so none sits behind text. If `animated`, a light band (Paper, peak alpha 0.06, ~40 % of the width, rotated 18°) crosses the screen left→right during the last ~3.5 s of a 12 s cycle and is absent the rest of the time.
- [ ] **Step 3: `HomePosters`** — three posters in the arrangement of the approved sketch: two on top (left tilted −6°, right +5°, the right one a little lower), one below in the middle (−2°) overlapping both. Backgrounds in order Paper, Acid, Pink; ink text: the game title upper-cased in the headline style and `players()` small and bold below; a semi-transparent tape strip across each top edge; a drop shadow. If `animated`, each sways ±1° around its tilt over ~4 s, phases 0, −1.3 s, −2.4 s. Size from the available box: width ≈ 40 % of it, height = width × 150 / 118, shrunk so the three always fit the box height (never under the buttons). Not clickable; the group has one `contentDescription` from `cd_home_posters` with the titles joined by ", ".
- [ ] **Step 4: `WelcomeScreen` layout** — top row 48 dp with the ⚙ button (`ic_settings`, `cd_settings`, 48 dp touch target) at the end; logo + drips (logo ~60 sp) and the tape tagline; then a weighted box holding the game-count line (Caveat Brush, Acid, rotated −3°, aligned right above the posters) and `HomePosters(FeaturedGames.ids, …)`; OYNA and "Kodla katıl" at the bottom, unchanged. The "Nasıl oynanır?" link is removed.
- [ ] **Step 5: Verify** — full suite green, `assembleDebug` builds (screens are checked on the emulator in Task 6).

### Task 6: Device check and handoff

- [ ] **Step 1:** Full suite + build: `.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug -q` → all tests pass (231 + new).
- [ ] **Step 2: Emulator (emulator-5554), install the debug APK and check:**
  - Home: posters, count "tüm oyunlar: 3", tags, gear; glows breathe and the sweep passes (two screenshots ~6 s apart); tapping a poster does nothing; OYNA still goes through tour/sign-in.
  - ⚙ → Settings as a signed-out guest: guest card, no account section, version shown. ✎ → guest editor; blank name shows the error; a name saves and shows on the card and later on the Join screen.
  - Language English → the app reloads in English, home and settings and a card text in English; back to System.
  - Haptics off → (no crash; haptic itself can't be seen on the emulator).
  - "Nasıl oynanır?" and "Kodla katılma nasıl?" replay their tours and come back to Settings; the Join tour doesn't open code entry.
  - Short screen: `adb shell wm size 720x1280` and `wm density 320`, screenshot home (posters not under the buttons), then `wm size reset` and `wm density reset`.
  - Animations off: read `settings get global animator_duration_scale`, set `0`, relaunch: nothing moves on home; restore the read value.
- [ ] **Step 3:** Propose one commit message to Mehmet (no commit), e.g. `feat: add settings and a livelier home screen` with the `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` line.
