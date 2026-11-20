# Plan 3: Firebase — Accounts and Content — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The host signs in with Google and sets up a profile (name + preset avatar) before playing; accounts can be deleted in the app; content packs come from Firestore with a local cache and the bundled set as fallback; a script pushes the repo's content JSON to Firestore. Anonymous sign-in and linking to Google exist in the account layer, ready for Plan 4's guests.

**Architecture:** Firebase is reached only through small interfaces in `shared/commonMain` (`AuthService`, `ProfileRepository`, `ContentRemote`, `ContentCache`, `GoogleIdTokenProvider`). Their Firebase implementations use the GitLive `firebase-kotlin-sdk`; the two pieces that need Android (`GoogleIdTokenProvider` via Credential Manager, `ContentCache` on the app's files dir) live in `androidApp` and reach `App()` through a `Platform` object. Decisions (which content to use, whether the host can play, profile validation, which Firestore packs to delete on import) are pure functions tested on the JVM with fakes. Screens stay MVI-light like the existing ones.

**Tech Stack:** As before, plus GitLive `firebase-auth` and `firebase-firestore` (latest stable, 2.7.x at the time of writing — pin the current one from Maven Central), the Google Services Gradle plugin, `androidx.credentials` + `credentials-play-services-auth` + `com.google.android.libraries.identity.googleid` (latest stable), `kotlinx-coroutines-test` for tests, and the Firebase Admin SDK for Java in a JVM tool module.

**Spec:** `docs/superpowers/specs/2026-11-19-party-games-app-design.md` (§4 Accounts and identity, §6.3 Where content lives, §10.2 Stack, §10.3 Firebase services, §10.5 store constraints). Visual rules: `docs/superpowers/specs/2026-11-20-graffiti-visual-refresh-design.md`.
**Roadmap:** `docs/superpowers/plans/2026-11-19-clink-roadmap.md` (this is Plan 3 of 5)

## Global Constraints

- **Git (CLAUDE.md, strict):** never run `git commit`, `git push`, `git rebase`, `git reset`, `git stash` or `git checkout -- <file>`. Every "Hand off" step: stop, give Mehmet a short Turkish summary and the commit message shown, and wait for him to confirm the commit before the next task.
- **Language:** Turkish with Mehmet; code, identifiers, comments and commit messages in English. All new UI copy in TR and EN.
- **Mehmet's accounts:** the Firebase project, billing, Google Cloud keys and any console change are Mehmet's to do; Claude gives exact steps and never asks for passwords. Downloads need Mehmet's OK first (what, from where, size). Nothing on Mehmet's phone without his OK in that turn.
- **Secrets never enter git:** the Admin SDK service-account key lives in `secrets/` (git-ignored). `google-services.json` is committed (it holds no secret; Firebase restricts it by package and SHA-1).
- **The host must have an account, in both modes** (§4): from this plan on, *Play* requires a signed-in Google user with a profile. Signing in needs a connection the first time only; after that local play works offline.
- **Profile:** display name 1–20 characters after trimming (same limit as player names), avatar from a preset set of 12; no photo upload.
- **Account deletion in the app** removes the Firestore profile and the Firebase Auth user (§4). The web deletion page and server-side cleanup are Plan 5.
- **Content:** card IDs and the file format stay as in `docs/content-format.md`; the repo JSON (`shared/src/commonMain/composeResources/files/content/baseline.json`) is the single source of truth; Firestore content is read-only for the app. Remote content is used only if it passes the same checks as the bundled file; new content takes effect on the **next app start**, never in the middle of a session.
- **Shared code:** everything except `MainActivity`, the Credential Manager glue and the file cache stays in `shared/commonMain`; only libraries that also support iOS in shared code.
- Street / Graffiti look for every new screen (TopBar, posters, neon buttons, AA contrast — new color pairs go into `ContrastTest`).
- Test command: `./gradlew :shared:testAndroidHostTest` (as `.\gradlew.bat` from PowerShell on this machine); the import tool's tests: `./gradlew :tools:content-import:test`.

## Review Focus

1. **No connection on the first sign-in, or Google sign-in cancelled / no Google account on the device** → the sign-in screen shows a clear message and stays usable; no crash, no half-signed-in state (Task 4 gate tests, Task 5 check).
2. **Remote content that is broken or partial** (blank text, unknown type/level, a missing language, zero packs, Firestore unreachable) → the app keeps the cached or bundled set and never crashes (Task 3 tests).
3. **Account deletion that needs a recent sign-in** (Firebase `requires-recent-login`) → the user is asked to sign in with Google again and the deletion completes; afterwards the app is signed out and back on Welcome with no profile left (Task 6 check).
4. **Profile names**: blank, spaces only, 21+ characters, emoji, Turkish letters → same trimming and limit as player names; the Google display name prefills the field (cut to 20) (Task 4 tests).
5. **Signed in but the profile document is missing** (deleted elsewhere, first sign-in interrupted) → the gate sends the user to profile setup instead of the game list (Task 4 tests).

---

## File Structure

```
gradle/libs.versions.toml, build.gradle.kts, settings.gradle.kts           (modify: GitLive, google-services, credentials, coroutines-test, tools module)
androidApp/build.gradle.kts, androidApp/google-services.json (Mehmet)       (modify / add)
androidApp/src/main/kotlin/com/mehmetbozkurt/clink/
  MainActivity.kt                         (modify: builds Platform, passes it to App)
  CredentialManagerGoogleIdTokenProvider.kt
  FileContentCache.kt
shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/
  App.kt, AppContainer.kt, Platform.kt    (modify / create)
  content/ContentParser.kt                (modify: validate(packs) usable for remote packs)
  content/ContentRepository.kt            — startup choice + background refresh
  content/ContentCache.kt, ContentRemote.kt, FirestoreContentRemote.kt
  account/AuthService.kt, FirebaseAuthService.kt, GoogleIdTokenProvider.kt
  account/Profile.kt                      — Profile, ProfileInput.validate, Avatars
  account/ProfileRepository.kt, FirestoreProfileRepository.kt
  account/AccountGate.kt                  — pure: what the Play button leads to
  ui/screens/SignInScreen.kt, ProfileSetupScreen.kt, AccountScreen.kt
  ui/components/AvatarBadge.kt
  ui/navigation/Routes.kt, ClinkNavHost.kt, ui/screens/WelcomeScreen.kt (modify)
shared/src/commonMain/composeResources/drawable/av_*.xml (12 Phosphor icons), values*/strings.xml
shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/content/ContentRepositoryTest.kt
shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/account/{ProfileTest, AccountGateTest}.kt
tools/content-import/                     — JVM app: reads baseline.json, pushes to Firestore (Admin SDK)
firebase/firestore.rules                  — rules Mehmet pastes into the console
secrets/ (git-ignored), docs/firebase-setup.md, docs/content-format.md (modify)
third_party/icons/PHOSPHOR-LICENSE.txt (unchanged; new icons are the same set)
```

Firestore schema (every task uses it):
- `packs/{packId}`: `{ type, level, access, cards: [ { id, text: { tr, en } } ] }` — same fields as `Pack`.
- `meta/content`: `{ version: Int }` — bumped by every import.
- `users/{uid}`: `{ displayName: String, avatarId: String }`.

---

### Task 1: Firebase project and app wiring

**Files:**
- Create: `docs/firebase-setup.md` (Turkish, the checklist below), `androidApp/google-services.json` (from Mehmet)
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `androidApp/build.gradle.kts`, `shared/build.gradle.kts`, `.gitignore` (`secrets/`)

**Interfaces:**
- Produces: Firebase initialized on app start (auto-init from `google-services.json`); GitLive `firebase-auth`, `firebase-firestore` available in `commonMain`.

- [ ] **Step 1: Write `docs/firebase-setup.md`** — Mehmet's checklist, in order: create project `clink` (Analytics off for now); switch to **Blaze** and add a **budget alert** (e.g. $5); Authentication → enable **Google** and **Anonymous**; Firestore → create database (production mode, region `eur3` or `europe-west1`); Project settings → add Android app `com.mehmetbozkurt.clink` with the **debug SHA-1** (Claude prints it with `.\gradlew.bat :androidApp:signingReport`); download `google-services.json` into `androidApp/`; Project settings → Service accounts → generate a key, save as `secrets/firebase-admin.json` (never committed).
- [ ] **Step 2: Mehmet does the checklist.** Claude waits; then checks `androidApp/google-services.json` exists and its `package_name` is `com.mehmetbozkurt.clink`.
- [ ] **Step 3: Wire the build.** Add the Google Services plugin to `androidApp`; GitLive `firebase-auth` + `firebase-firestore` to `commonMain`; `kotlinx-coroutines-test` to `commonTest`; `secrets/` to `.gitignore`.
- [ ] **Step 4: Run** the full suite and build. Expected: PASS. Emulator: app starts, logcat shows `FirebaseApp initialization successful`, the existing flow still plays.
- [ ] **Step 5: Hand off.** Commit message: `chore: connect the app to Firebase`

### Task 2: Content import tool

**Files:**
- Create: `tools/content-import/build.gradle.kts`, `tools/content-import/src/main/kotlin/ContentImport.kt`, `tools/content-import/src/test/kotlin/ImportPlanTest.kt`, `firebase/firestore.rules`
- Modify: `settings.gradle.kts` (`include(":tools:content-import")`), `docs/content-format.md` (how to push)

**Interfaces:**
- Produces: `fun importPlan(localPackIds: Set<String>, remotePackIds: Set<String>): ImportPlan` with `data class ImportPlan(val upsert: Set<String>, val delete: Set<String>)`; Gradle task `:tools:content-import:run` that depends on `:shared:testAndroidHostTest` (content is validated before anything is pushed), reads `baseline.json`, writes every pack to `packs/{id}`, deletes remote packs missing locally, then sets `meta/content.version` to the previous value + 1. Reads the key from `secrets/firebase-admin.json`.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun newAndChangedPacksAreUpserted() = assertEquals(setOf("a", "b"), importPlan(setOf("a", "b"), setOf("b")).upsert)
@Test fun packsGoneFromTheRepoAreDeleted() = assertEquals(setOf("old"), importPlan(setOf("a"), setOf("a", "old")).delete)
@Test fun emptyLocalFileDeletesNothing() = assertEquals(emptySet(), importPlan(emptySet(), setOf("a")).delete)  // refuses: a broken read must not wipe Firestore
```

- [ ] **Step 2: Run** `./gradlew :tools:content-import:test`. Expected: FAIL (unresolved `importPlan`).
- [ ] **Step 3: Implement** `importPlan` and the `main` (Admin SDK: `FirebaseApp.initializeApp` with the key file; batch write). An empty local file aborts with a message before any write.
- [ ] **Step 4: Write `firebase/firestore.rules`:** `packs` and `meta` readable by any signed-in user (anonymous included), never writable; `users/{uid}` readable and writable only by that `uid`, writes limited to `displayName` (string, 1–20 chars) and `avatarId` (string).
- [ ] **Step 5: Run** the tests. Expected: PASS. With Mehmet: he pastes the rules into the console (Firestore → Rules → Publish) and runs `.\gradlew.bat :tools:content-import:run`; Firestore shows 9 packs and `meta/content.version = 1`.
- [ ] **Step 6: Hand off.** Commit message: `feat: add the content import tool and Firestore rules`

### Task 3: Content from Firestore with a local cache

**Files:**
- Create: `content/ContentRepository.kt`, `content/ContentCache.kt`, `content/ContentRemote.kt`, `content/FirestoreContentRemote.kt`, `Platform.kt`, `androidApp/.../FileContentCache.kt`
- Modify: `content/ContentParser.kt`, `App.kt`, `AppContainer.kt`, `MainActivity.kt`
- Test: `commonTest/.../content/ContentRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 2's schema.
- Produces:
  - `interface ContentCache { fun read(): CachedContent?; fun write(content: CachedContent) }` with `@Serializable data class CachedContent(val version: Int, val packs: List<Pack>)`
  - `interface ContentRemote { suspend fun version(): Int; suspend fun packs(): List<Pack> }`
  - `ContentParser.validate(packs: List<Pack>)` (throws like `parse` does today; `parse` calls it)
  - `class ContentRepository(cache, remote, bundled: suspend () -> List<Pack>)` with `suspend fun load(): ContentLibrary` (valid cache, else bundled) and `suspend fun refresh()` (fetch only when `remote.version()` > cached version; validate; write cache; never throws)
  - `class Platform(val contentCache: ContentCache)`; `App(platform: Platform)` (Task 4 adds `googleIdTokens`)

- [ ] **Step 1: Write the failing tests** (fakes for cache and remote; `runTest`)

```kotlin
@Test fun firstStartUsesBundled()             // cache empty → library has the bundled card ids
@Test fun validCacheWinsOverBundled()         // cache with version 3 → its card ids
@Test fun brokenCacheFallsBackToBundled()     // cache holds a card with blank text → bundled
@Test fun refreshStoresNewerRemoteContent()   // cache v1, remote v2 → cache now v2 with remote packs
@Test fun refreshSkipsSameVersion()           // cache v2, remote v2 → remote.packs() never called
@Test fun refreshIgnoresInvalidRemote()       // remote v2 with a blank card → cache unchanged
@Test fun refreshSurvivesRemoteFailure()      // remote throws → no exception, cache unchanged
@Test fun refreshIgnoresEmptyRemote()         // remote v2 with zero packs → cache unchanged
```

- [ ] **Step 2: Run** `--tests "com.mehmetbozkurt.clink.content.ContentRepositoryTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** the repository, `ContentParser.validate`, `FirestoreContentRemote` (GitLive, reads `meta/content` and `packs`), `FileContentCache` (JSON file in `filesDir`, written atomically via temp file + rename). `App` loads with `load()` and starts `refresh()` in the background after the first frame.
- [ ] **Step 4: Run** the full suite and build. Expected: PASS. Emulator (signed out is fine — Task 4 makes reads authenticated; until then the rules from Task 2 deny the read and the bundled set is used, which is the failure path of Review Focus 2): app starts, plays with bundled cards, logcat shows the refresh skipped with a permission error and no crash.
- [ ] **Step 5: Hand off.** Commit message: `feat: load content from Firestore with a local cache`

### Task 4: Account layer

**Files:**
- Create: `account/AuthService.kt`, `account/FirebaseAuthService.kt`, `account/GoogleIdTokenProvider.kt`, `account/Profile.kt`, `account/ProfileRepository.kt`, `account/FirestoreProfileRepository.kt`, `account/AccountGate.kt`, `androidApp/.../CredentialManagerGoogleIdTokenProvider.kt`
- Modify: `AppContainer.kt`, `Platform.kt` (add `val googleIdTokens: GoogleIdTokenProvider`), `MainActivity.kt`, `androidApp/build.gradle.kts` (credentials, googleid)
- Test: `commonTest/.../account/ProfileTest.kt`, `AccountGateTest.kt`

**Interfaces:**
- Produces:
  - `data class AuthUser(val uid: String, val isAnonymous: Boolean, val googleName: String?)`
  - `interface AuthService { val user: StateFlow<AuthUser?>; suspend fun signInWithGoogle(idToken: String); suspend fun signInAnonymously(); suspend fun linkGoogle(idToken: String); suspend fun reauthenticateWithGoogle(idToken: String); suspend fun deleteUser(); suspend fun signOut() }` — failures surface as `AuthException(kind: AuthFailure)` with `enum class AuthFailure { NETWORK, CANCELLED, RECENT_LOGIN_REQUIRED, OTHER }`
  - `fun interface GoogleIdTokenProvider { suspend fun request(): GoogleIdToken }` with `sealed interface GoogleIdToken { data class Ok(val idToken: String) ; data object Cancelled; data object NoAccount; data class Failed(val message: String) }`
  - `data class Profile(val displayName: String, val avatarId: String)`; `object Avatars { val ids: List<String> }` (12 ids, Task 5 adds the drawables); `object ProfileInput { fun validate(name: String): ProfileNameResult }` (`Valid(name)`, `Blank`)
  - `interface ProfileRepository { suspend fun load(uid: String): Profile?; suspend fun save(uid: String, profile: Profile); suspend fun delete(uid: String) }`
  - `sealed interface PlayGate { data object SignIn; data object SetUpProfile; data object Play }` and `fun playGate(user: AuthUser?, profile: Profile?): PlayGate` — anonymous users count as signed out for hosting
  - `fun suggestedName(googleName: String?): String` — trimmed, cut to 20, `""` when null

- [ ] **Step 1: Write the failing tests**

```kotlin
// ProfileTest
@Test fun trimsName() = assertEquals(ProfileNameResult.Valid("Ayşe"), ProfileInput.validate("  Ayşe "))
@Test fun blankIsRejected() = assertEquals(ProfileNameResult.Blank, ProfileInput.validate("   "))
@Test fun longNameIsCut() = assertEquals(ProfileNameResult.Valid("a".repeat(20)), ProfileInput.validate("a".repeat(25)))
@Test fun googleNameIsSuggestedAndCut() = assertEquals("Mehmet Bozkurt Uzun", suggestedName("  Mehmet Bozkurt Uzun İsim "))  // trim, take(20), trim again
@Test fun noGoogleNameSuggestsNothing() = assertEquals("", suggestedName(null))
@Test fun twelveDistinctAvatars() = assertEquals(12, Avatars.ids.toSet().size)
// AccountGateTest
@Test fun signedOutMustSignIn() = assertEquals(PlayGate.SignIn, playGate(null, null))
@Test fun anonymousCannotHost() = assertEquals(PlayGate.SignIn, playGate(AuthUser("u", true, null), null))
@Test fun signedInWithoutProfileSetsUp() = assertEquals(PlayGate.SetUpProfile, playGate(AuthUser("u", false, "M"), null))
@Test fun signedInWithProfilePlays() = assertEquals(PlayGate.Play, playGate(AuthUser("u", false, "M"), Profile("M", Avatars.ids[0])))
```

- [ ] **Step 2: Run** the two classes. Expected: FAIL.
- [ ] **Step 3: Implement** the pure pieces, then `FirebaseAuthService` (GitLive `Firebase.auth`; `GoogleAuthProvider.credential(idToken, null)`; map Firebase exceptions to `AuthFailure`), `FirestoreProfileRepository` (`users/{uid}`), and `CredentialManagerGoogleIdTokenProvider` (Credential Manager `GetGoogleIdOption` with `serverClientId = R.string.default_web_client_id`, `filterByAuthorizedAccounts = false`).
- [ ] **Step 4: Run** the full suite and build. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add the account layer: Google sign-in, guests and profiles`

### Task 5: Sign-in, profile and account screens

**Files:**
- Create: `ui/screens/SignInScreen.kt`, `ProfileSetupScreen.kt`, `AccountScreen.kt`, `ui/components/AvatarBadge.kt`, `composeResources/drawable/av_*.xml` (12)
- Modify: `ui/navigation/Routes.kt`, `ClinkNavHost.kt`, `ui/screens/WelcomeScreen.kt`, strings, `ContrastTest` if new pairs

**Interfaces:**
- Consumes: Task 4's `AuthService`, `ProfileRepository`, `playGate`, `GoogleIdTokenProvider`, `ProfileInput`, `suggestedName`, `Avatars`.
- Produces: routes `SignInRoute`, `ProfileSetupRoute(editing: Boolean)`, `AccountRoute`; `AvatarBadge(avatarId: String, name: String, size: Dp)` (unknown id → the player's `InitialBadge`).

Screens (Street look):
- **Play** on Welcome checks `playGate`: `SignIn` → SignInScreen; `SetUpProfile` → ProfileSetupScreen; `Play` → Game list (as today). Welcome shows the user's `AvatarBadge` top-right when signed in; tapping it opens AccountScreen.
- **SignInScreen:** TopBar(Back), title "Önce sen" / "You first", one line: "Oyunu kuran kişinin bir hesabı olmalı. Arkadaşların hesapsız oynar." / "The host needs an account. Your friends play without one.", `PrimaryButton` "Google ile devam et" / "Continue with Google"; while waiting, the button shows a loading state and can't be tapped twice; errors in Amber: no connection, no Google account on the phone, generic failure; cancelling shows nothing.
- **ProfileSetupScreen:** TopBar(Back), title "Sen kimsin?" / "Who are you?", name field prefilled with `suggestedName`, 4×3 avatar grid (selected = Acid ring), `PrimaryButton` "Kaydet" / "Save" → Game list (first time) or back (editing).
- **AccountScreen:** TopBar(Back), avatar + name poster, "Profili düzenle", "Çıkış yap" (secondary), "Hesabımı sil" (text-style, Amber) — deletion itself is Task 6; here it is visible but routes to Task 6's dialog stub that is filled in next task.

- [ ] **Step 1: Ask Mehmet before downloading** the 12 Phosphor (Bold) SVGs — `cat, dog, bird, fish, rabbit, butterfly, ghost, alien, robot, rocket, crown, cactus` from the Phosphor GitHub repository, about 1 KB each — then convert them like Task 1 of the visual refresh (`svg2vd.py`, viewport 256, white fill, tinted in code). Avatar ids are these names.
- [ ] **Step 2: Implement** the screens and navigation. Back from SignIn/ProfileSetup (first time) returns to Welcome; signing out returns to Welcome.
- [ ] **Step 3: Run** the full suite and build. Expected: PASS.
- [ ] **Step 4: Check.** Emulator: signed out → Play → SignIn; cancel → stays; no Google account on the emulator → the "no account" message. Real sign-in on Mehmet's phone (with his OK in that turn) or on the emulator after Mehmet adds a Google account there himself: sign in → profile setup prefilled → save → Game list → a full Truth or Dare round still works; restart the app in airplane mode → Play goes straight to the Game list (offline after first sign-in). Content refresh from Task 3 now succeeds (logcat), and a second start uses the cache.
- [ ] **Step 5: Hand off.** Commit message: `feat: add sign-in, profile and account screens`

### Task 6: Account deletion and end-to-end check

**Files:**
- Modify: `ui/screens/AccountScreen.kt`, strings
- Create: `account/DeleteAccount.kt` — `suspend fun deleteAccount(auth, profiles, reauth: suspend () -> Boolean): DeleteResult`
- Test: `commonTest/.../account/DeleteAccountTest.kt`

**Interfaces:**
- Consumes: Task 4's interfaces.
- Produces: `enum class DeleteResult { DELETED, CANCELLED, FAILED }`; order: delete the profile, then the auth user; on `RECENT_LOGIN_REQUIRED` call `reauth()` (Google sign-in again) once and retry; finally sign out.

- [ ] **Step 1: Write the failing tests** (fakes)

```kotlin
@Test fun deletesProfileThenUser()             // both gone, signed out, DELETED
@Test fun reauthenticatesOnceWhenRequired()   // first deleteUser throws RECENT_LOGIN_REQUIRED, reauth → true → DELETED
@Test fun cancelledReauthKeepsTheAccount()    // reauth → false → CANCELLED, user still signed in
@Test fun networkFailureReportsFailed()       // profiles.delete throws NETWORK → FAILED, nothing else touched
```

- [ ] **Step 2: Run** `--tests "com.mehmetbozkurt.clink.account.DeleteAccountTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** it and the AccountScreen dialog: a destructive `PosterDialog` "Hesabın silinsin mi?" / "Delete your account?" with "Profilin ve hesabın kalıcı olarak silinir. Bu geri alınamaz." / "Your profile and account are deleted for good. This can't be undone.", confirm → progress → Welcome.
- [ ] **Step 4: Run** the full suite and build. Expected: PASS.
- [ ] **Step 5: End-to-end** on Mehmet's phone (with his OK in that turn): fresh install → Play → sign in → profile → a round of each game → Account → edit profile → sign out → sign in again (profile kept) → delete account (re-auth if asked) → Firestore console shows no `users/{uid}` and Authentication shows no user; TR and EN copy; font scale 1.5 on the new screens.
- [ ] **Step 6: Hand off.** Commit message: `feat: delete the account from the app`

---

## Notes and calls

- **Guests:** `signInAnonymously` and `linkGoogle` are built and covered by the gate rules in Task 4 (an anonymous user can't host). The guest UI (nickname + avatar on joining a lobby, "create an account" upgrade) needs invites, so it comes with Plan 4. *(Claude's call: the roadmap lists guests under Plan 3; only the account layer can be finished without lobbies.)*
- **Content reads need sign-in** (rules in Task 2): first launch uses the bundled set; after sign-in the cache fills. *(Claude's call: keeps reads tied to real users, cheap to open later.)*
- **Content swaps only at the next app start** so a session never changes cards mid-game. *(Claude's call.)*
- **Import tool in Kotlin/JVM** with the Admin SDK *(Claude's call)*: no Node.js needed on this machine; it validates by depending on the content tests.
- Carried-over items from earlier plans stay in `docs/superpowers/plans/2026-11-20-visual-refresh.md` ("Carried over").
