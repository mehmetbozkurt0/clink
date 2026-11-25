# Plan 1: Foundation + Local Truth or Dare and Spin the Bottle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An Android app that runs offline, with the Midnight look, the full "welcome → game → mode → players → level → game → game over" flow, and Truth or Dare and Spin the Bottle playable on one phone, using bundled sample content in Turkish and English.

**Architecture:** One Kotlin Multiplatform library module `shared` holds everything (game rules, content, MVI store, Compose UI). A thin `androidApp` module only hosts `MainActivity`. Game rules are pure reducers `(state, action, random) -> state` with no Android or Firebase dependency, so the same code later runs on the online host (Plan 4) and on iOS.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3, Compose resources), JetBrains multiplatform Navigation Compose (type-safe routes) and Lifecycle ViewModel, kotlinx.serialization JSON, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-11-19-party-games-app-design.md`
**Roadmap:** `docs/superpowers/plans/2026-11-19-clink-roadmap.md` (this is Plan 1 of 5)

## Global Constraints

- **Git (CLAUDE.md, strict):** never run `git commit`, `git push`, `git rebase`, `git reset`, `git stash` or `git checkout -- <file>`. Every "Hand off" step means: stop, give Mehmet a short summary of what changed in which files and why, plus the commit message shown, and **wait for him to confirm he committed** before starting the next task.
- **Language:** talk to Mehmet in Turkish; code, identifiers, comments and commit messages in English.
- **Downloads** (Gradle distribution, fonts) need Mehmet's explicit OK first: say what, from where, and the size.
- Android application ID: `com.mehmetbozkurt.clink`. Kotlin package root: `com.mehmetbozkurt.clink`.
- Android only in v1. All code except `MainActivity` lives in `shared/src/commonMain`. Only use libraries that also support iOS. Do not add an iOS target.
- `minSdk = 26`. `compileSdk = targetSdk =` the highest platform installed under `C:\Users\ACER\AppData\Local\Android\Sdk\platforms`.
- Library versions: latest **stable** release of each at implementation time, checked on its official release page, recorded once in `gradle/libs.versions.toml`.
- Players per local session: **2–12**.
- Levels: `CASUAL`, `HARD`, `SPICY`. Choosing Spicy shows an 18+ confirmation **every time**.
- Never any drinking wording or mechanic. The loser/refuser takes a **penalty**.
- UI languages: Turkish and English. Cards are shown in the app language, falling back to English.
- Dark theme only. Colors and fonts exactly as in Task 7.

## Review Focus

1. **Thin decks:** a session draws more cards than a pack holds → the deck reshuffles, never crashes, and never shows the same card twice in a row across the reshuffle (Task 2 test).
2. **Missing content:** Mehmet's real content may leave a type/level empty (e.g. no Spicy Dares) → no crash; the player is told and can pick the other option; an empty penalty pool just moves the turn on (Task 5 and Task 6 tests).
3. **Unsupported language or missing translation:** device in German, or a card without `tr` → English text, then any available text (Task 2 test).
4. **Messy player names:** surrounding spaces, blank rows, duplicates differing only in case, a 13th player, a 25-character name → trimmed, rejected with a clear reason, or capped at 20 characters (Task 8 test).
5. **Two-player Spin the Bottle:** the bottle may land on the spinner; turns must alternate and never get stuck (Task 6 test).

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat), gradle/wrapper/*
androidApp/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/kotlin/com/mehmetbozkurt/clink/MainActivity.kt          — setContent { App() }
shared/
  build.gradle.kts
  src/commonMain/kotlin/com/mehmetbozkurt/clink/
    App.kt                                  — ClinkTheme + ClinkNavHost
    AppContainer.kt                         — app-wide singletons: ContentLibrary, Random
    core/mvi/Reducer.kt, Store.kt
    content/ContentModel.kt                 — Level, CardType, Access, Card, Pack
    content/ContentLibrary.kt, Deck.kt, ContentParser.kt, BundledContent.kt
    game/GameId.kt
    game/PlayerNames.kt                     — local player list validation
    game/truthordare/TruthOrDare.kt         — state, phases, actions, initial state
    game/truthordare/TruthOrDareReducer.kt
    game/truthordare/TruthOrDareViewModel.kt, TruthOrDareScreen.kt
    game/spinthebottle/SpinTheBottle.kt     — state, phases, actions, initial state
    game/spinthebottle/SpinTheBottleReducer.kt
    game/spinthebottle/SpinTheBottleViewModel.kt, SpinTheBottleScreen.kt, BottleCircle.kt
    ui/theme/ClinkColors.kt, ClinkTypography.kt, ClinkTheme.kt, LevelColors.kt
    ui/components/ClinkBackground.kt, ClinkButtons.kt, LevelTag.kt, ClinkCard.kt, EndGameDialog.kt
    ui/navigation/Routes.kt, ClinkNavHost.kt
    ui/screens/WelcomeScreen.kt, GameListScreen.kt, ModeSelectScreen.kt,
               PlayerSetupScreen.kt, LevelSelectScreen.kt, GameOverScreen.kt
  src/commonMain/composeResources/
    font/unbounded_semibold.ttf, unbounded_extrabold.ttf, manrope_medium.ttf, manrope_bold.ttf
    values/strings.xml, values-tr/strings.xml
    drawable/bottle.xml
    files/content/baseline.json
  src/commonTest/kotlin/com/mehmetbozkurt/clink/...   — pure logic tests
  src/androidHostTest/kotlin/com/mehmetbozkurt/clink/content/BaselineContentFileTest.kt
third_party/fonts/OFL-Unbounded.txt, OFL-Manrope.txt
docs/content-format.md                      — how Mehmet writes cards
```

Test command used throughout (Task 1 confirms the exact task name; if it differs, use the real name everywhere):
`./gradlew :shared:testAndroidHostTest` (add `--tests "<fully.qualified.TestClass>"` to run one class).

---

### Task 1: Project skeleton that builds, tests and launches

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, Gradle wrapper files, `androidApp/build.gradle.kts`, `androidApp/src/main/AndroidManifest.xml`, `androidApp/src/main/kotlin/com/mehmetbozkurt/clink/MainActivity.kt`, `shared/build.gradle.kts`, `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/App.kt`
- Modify: `.gitignore` (append `build/`, `.gradle/`, `.kotlin/`, `local.properties`, `.idea/`, `*.iml`, `captures/`)
- Create (not committed): `local.properties` with `sdk.dir=C\:\\Users\\ACER\\AppData\\Local\\Android\\Sdk`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/SmokeTest.kt`

**Interfaces:**
- Produces: `@Composable fun App()` in `App.kt` (later tasks fill it); module `:shared` applies `org.jetbrains.kotlin.multiplatform`, `com.android.kotlin.multiplatform.library` (`androidLibrary { namespace = "com.mehmetbozkurt.clink.shared"; withHostTest {} }`), `org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`; `:androidApp` applies `com.android.application` with `applicationId = "com.mehmetbozkurt.clink"` and depends on `:shared`.

- [ ] **Step 1: Get Gradle.** Gradle is not installed. Ask Mehmet to OK downloading the latest stable Gradle `-bin.zip` from `services.gradle.org` (~130 MB) into the scratchpad, then run `gradle wrapper --gradle-version <that version>` in the repo root and delete the download. Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` and `.jar` exist.
- [ ] **Step 2: Write the Gradle files** listed above. Dependencies of `shared` commonMain: compose runtime, foundation, material3, ui, components-resources, JetBrains navigation-compose, JetBrains lifecycle-viewmodel-compose, kotlinx-serialization-json. commonTest: `kotlin("test")`. `androidApp`: `androidx.activity:activity-compose`. JVM target 17.
- [ ] **Step 3: Write the smoke test**

```kotlin
class SmokeTest {
    @Test fun buildRunsCommonTests() { assertEquals(4, 2 + 2) }
}
```

- [ ] **Step 4: Run** `./gradlew :shared:tasks --all` and confirm the host test task name (expected `testAndroidHostTest`). Then `./gradlew :shared:testAndroidHostTest`. Expected: `BUILD SUCCESSFUL`, 1 test passed.
- [ ] **Step 5: Implement** `App()` as a full-screen `Box` with background `#16142B` and centered text `clink.`; `MainActivity` calls `enableEdgeToEdge()` and `setContent { App() }`, portrait only (`android:screenOrientation="portrait"`), app label `Clink`.
- [ ] **Step 6: Verify on the emulator.** Start `Pixel_9a` (`$SDK/emulator/emulator -avd Pixel_9a`), run `./gradlew :androidApp:installDebug`, launch the app. Expected: dark screen with "clink." and no crash in `adb logcat -s AndroidRuntime`.
- [ ] **Step 7: Hand off.** Commit message: `chore: set up Kotlin Multiplatform project with shared and androidApp modules`

---

### Task 2: Content model, content library and shuffle deck

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/content/ContentModel.kt`, `ContentLibrary.kt`, `Deck.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/content/ContentLibraryTest.kt`, `DeckTest.kt`

**Interfaces:**
- Produces:
  - `@Serializable enum class Level { CASUAL, HARD, SPICY }`
  - `@Serializable enum class CardType { TRUTH, DARE, PENALTY }`
  - `@Serializable enum class Access { FREE }`
  - `@Serializable data class Card(val id: String, val text: Map<String, String>)` (keys are language codes `"tr"`, `"en"`)
  - `@Serializable data class Pack(val id: String, val type: CardType, val level: Level, val access: Access, val cards: List<Card>)`
  - `class ContentLibrary(packs: List<Pack>)` with `fun cardIds(type: CardType, level: Level): List<String>` (all cards of that type and level across packs, in file order) and `fun text(cardId: String, language: String): String` (fallback: `language` → `"en"` → first entry; unknown id → `IllegalArgumentException`)
  - `data class Deck(val all: List<String>, val remaining: List<String>, val last: String?)` with `companion fun of(ids: List<String>): Deck` (nothing drawn yet: `remaining = all`, `last = null`) and `fun draw(random: Random): Pair<String, Deck>?` (`null` only when `all` is empty)

- [ ] **Step 1: Write the failing tests**

```kotlin
// ContentLibraryTest
private val lib = ContentLibrary(listOf(
    Pack("t-c", CardType.TRUTH, Level.CASUAL, Access.FREE, listOf(
        Card("t1", mapOf("tr" to "Soru", "en" to "Question")),
        Card("t2", mapOf("tr" to "Sadece TR")))),
    Pack("t-c2", CardType.TRUTH, Level.CASUAL, Access.FREE, listOf(Card("t3", mapOf("en" to "Q3")))),
    Pack("d-h", CardType.DARE, Level.HARD, Access.FREE, listOf(Card("d1", mapOf("en" to "Dare"))))))

@Test fun cardIds_mergesPacksOfSameTypeAndLevel() = assertEquals(listOf("t1", "t2", "t3"), lib.cardIds(CardType.TRUTH, Level.CASUAL))
@Test fun cardIds_emptyWhenNoPack() = assertEquals(emptyList(), lib.cardIds(CardType.DARE, Level.SPICY))
@Test fun text_usesRequestedLanguage() = assertEquals("Soru", lib.text("t1", "tr"))
@Test fun text_fallsBackToEnglish() = assertEquals("Question", lib.text("t1", "de"))
@Test fun text_fallsBackToFirstAvailable() = assertEquals("Sadece TR", lib.text("t2", "en"))
@Test fun text_unknownIdThrows() { assertFailsWith<IllegalArgumentException> { lib.text("nope", "en") } }

// DeckTest
@Test fun draw_emptyDeckReturnsNull() = assertNull(Deck.of(emptyList()).draw(Random(1)))
@Test fun draw_coversEveryCardBeforeRepeating() {
    var deck = Deck.of(listOf("a", "b", "c", "d"))
    val seen = (1..4).map { val (id, next) = deck.draw(Random(7))!!; deck = next; id }
    assertEquals(setOf("a", "b", "c", "d"), seen.toSet())
}
@Test fun draw_afterExhaustion_reshufflesWithoutImmediateRepeat() {
    repeat(200) { seed ->
        var deck = Deck.of(listOf("a", "b", "c"))
        val random = Random(seed)
        val draws = (1..9).map { val (id, next) = deck.draw(random)!!; deck = next; id }
        draws.zipWithNext().forEach { (x, y) -> assertNotEquals(x, y, "seed $seed: $draws") }
    }
}
@Test fun draw_singleCardDeckRepeatsThatCard() {
    var deck = Deck.of(listOf("only"))
    repeat(3) { val (id, next) = deck.draw(Random(0))!!; assertEquals("only", id); deck = next }
}
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.content.*"`. Expected: FAIL, unresolved references.
- [ ] **Step 3: Implement** the types above. `Deck.draw`: if `remaining` is empty, refill with `all.shuffled(random)`, and if the first card equals `last` and `all.size > 1`, swap it with another position; then take the first card. `ContentLibrary` builds a `Map<String, Card>` once.
- [ ] **Step 4: Run** the same command. Expected: all tests PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add content model, content library and shuffle deck`

---

### Task 3: Bundled sample content, parser and content format guide

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/content/ContentParser.kt`, `BundledContent.kt`, `shared/src/commonMain/composeResources/files/content/baseline.json`, `docs/content-format.md`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/content/ContentParserTest.kt`, `shared/src/androidHostTest/kotlin/com/mehmetbozkurt/clink/content/BaselineContentFileTest.kt`

**Interfaces:**
- Consumes: `Pack`, `Card`, `CardType`, `Level`, `Access`, `ContentLibrary` (Task 2)
- Produces: `object ContentParser { fun parse(json: String): List<Pack> }` (throws `IllegalArgumentException` on a format version other than 1); `suspend fun loadBundledContent(): ContentLibrary` in `BundledContent.kt` (reads `Res.readBytes("files/content/baseline.json")`)

File format (copy into `docs/content-format.md` with a short Turkish explanation for Mehmet: one file, card IDs never change or get reused, text in both languages, no drinking, Spicy is suggestive not explicit):

```json
{
  "version": 1,
  "packs": [
    {
      "id": "truth-casual-base", "type": "TRUTH", "level": "CASUAL", "access": "FREE",
      "cards": [
        { "id": "truth-casual-001", "text": { "tr": "Bu odadaki kimin telefonuna bakmak isterdin?", "en": "Whose phone in this room would you most like to look through?" } }
      ]
    }
  ]
}
```

- [ ] **Step 1: Write the failing tests**

```kotlin
// ContentParserTest
@Test fun parse_readsPacksAndCards() {
    val packs = ContentParser.parse("""{"version":1,"packs":[{"id":"p","type":"DARE","level":"SPICY","access":"FREE",
        "cards":[{"id":"c1","text":{"tr":"a","en":"b"}}]}]}""")
    assertEquals(CardType.DARE, packs.single().type)
    assertEquals("b", packs.single().cards.single().text["en"])
}
@Test fun parse_rejectsUnknownVersion() {
    assertFailsWith<IllegalArgumentException> { ContentParser.parse("""{"version":2,"packs":[]}""") }
}

// BaselineContentFileTest (JVM: reads the real file from disk)
private val packs = ContentParser.parse(File("src/commonMain/composeResources/files/content/baseline.json").readText())
@Test fun everyTypeAndLevelHasAtLeastThreeCards() {
    for (t in CardType.entries) for (l in Level.entries)
        assertTrue(packs.filter { it.type == t && it.level == l }.sumOf { it.cards.size } >= 3, "$t/$l")
}
@Test fun cardIdsAreUnique() { val ids = packs.flatMap { it.cards }.map { it.id }; assertEquals(ids.size, ids.toSet().size) }
@Test fun everyCardHasTurkishAndEnglish() { packs.flatMap { it.cards }.forEach { assertTrue(it.text.keys.containsAll(setOf("tr", "en")), it.id) } }
@Test fun noCardMentionsDrinking() {
    // Whole words only ((?U) makes \b work with ç/ş), so "biraz" and "screenshot" don't match.
    val banned = Regex("(?iU)\\b(drink\\w*|drunk|shots?|beers?|alcohol\\w*|içki\\w*|alkol\\w*|sarhoş\\w*|bira(yı|ya|dan|nın|lar\\w*)?)\\b")
    packs.flatMap { it.cards }.forEach { c -> c.text.values.forEach { t -> assertNull(banned.find(t), "${c.id}: $t") } }
}
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.content.*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `ContentParser` with kotlinx.serialization (`ignoreUnknownKeys = true`) and `loadBundledContent()`. Write `baseline.json`: **3 sample cards for each of the 9 type × level combinations** (27 cards), TR and EN, tone per spec §6.1, IDs `<type>-<level>-NNN`. These are placeholders until Mehmet's content arrives.
- [ ] **Step 4: Run** the same command. Expected: all PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add bundled sample content, content parser and content format guide`

---

### Task 4: MVI core

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/core/mvi/Reducer.kt`, `Store.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/core/mvi/StoreTest.kt`, `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/testing/SequenceRandom.kt`

**Interfaces:**
- Produces:
  - `fun interface Reducer<S, A> { fun reduce(state: S, action: A, random: Random): S }`
  - `class Store<S, A>(initial: S, reducer: Reducer<S, A>, random: Random)` with `val state: StateFlow<S>` and `fun dispatch(action: A)` (synchronous; applies the reducer to the current value)
  - Test helper `class SequenceRandom(vararg values: Int) : Random()` — `nextInt(until)` returns the next queued value (asserting it is `< until`), `nextBits` throws. Used by Task 5 and 6 tests to pick exact outcomes.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun dispatch_appliesReducerInOrder() {
    val store = Store(0, Reducer<Int, Int> { s, a, _ -> s * 10 + a }, Random(0))
    store.dispatch(1); store.dispatch(2)
    assertEquals(12, store.state.value)
}
@Test fun dispatch_passesTheStoresRandom() {
    val store = Store(0, Reducer<Int, Unit> { _, _, r -> r.nextInt(100) }, SequenceRandom(42))
    store.dispatch(Unit)
    assertEquals(42, store.state.value)
}
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.core.mvi.*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `Reducer`, `Store` (`MutableStateFlow.update`), `SequenceRandom`.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add MVI reducer and store`

---

### Task 5: Truth or Dare rules

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/truthordare/TruthOrDare.kt`, `TruthOrDareReducer.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/truthordare/TruthOrDareReducerTest.kt`

**Interfaces:**
- Consumes: `Reducer` (Task 4), `Deck`, `ContentLibrary`, `CardType`, `Level` (Task 2), `SequenceRandom` (Task 4)
- Produces:
  - `data class TruthOrDareState(val players: List<String>, val level: Level, val currentPlayer: Int, val phase: TodPhase, val truthDeck: Deck, val dareDeck: Deck, val penaltyDeck: Deck, val notice: TodNotice?)`
  - `sealed interface TodPhase { data object Choosing; data class ShowingCard(val type: CardType, val cardId: String); data class ShowingPenalty(val cardId: String) }`
  - `enum class TodNotice { NO_TRUTH_CARDS, NO_DARE_CARDS, NO_PENALTY_CARDS }`
  - `sealed interface TodAction { data class Pick(val type: CardType); data object Done; data object Refuse; data object PenaltyDone }`
  - `fun initialTruthOrDareState(players: List<String>, level: Level, library: ContentLibrary): TruthOrDareState` (`currentPlayer = 0`, `Choosing`, decks from `library.cardIds(..., level)`)
  - `object TruthOrDareReducer : Reducer<TruthOrDareState, TodAction>`

Rules: `Choosing --Pick(TRUTH|DARE)--> ShowingCard`; `ShowingCard --Done--> next player, Choosing`; `ShowingCard --Refuse--> ShowingPenalty`; `ShowingPenalty --PenaltyDone--> next player, Choosing`. Next player wraps around. Any action that doesn't fit the current phase (or `Pick(PENALTY)`) returns the state unchanged. Any successful action clears `notice`. Empty deck on `Pick` → stay `Choosing`, set `NO_TRUTH_CARDS`/`NO_DARE_CARDS`. Empty penalty deck on `Refuse` → next player, `Choosing`, `NO_PENALTY_CARDS`.

- [ ] **Step 1: Write the failing tests** (build the library inline: truth `t1,t2`, dare `d1`, penalty `p1`, all CASUAL; three players `Ayşe, Mert, Can`)

```kotlin
@Test fun pickTruth_showsTruthCardForCurrentPlayer()      // phase == ShowingCard(TRUTH, "t1" or "t2"), currentPlayer == 0
@Test fun done_movesToNextPlayerAndChoosing()             // after Pick+Done: currentPlayer == 1, phase == Choosing
@Test fun refuse_showsPenalty_thenPenaltyDoneAdvances()   // ShowingPenalty("p1"), then currentPlayer == 1, Choosing
@Test fun turnWrapsAfterLastPlayer()                      // 3 full turns → currentPlayer == 0
@Test fun pickWithEmptyDeck_staysChoosingWithNotice()     // library without SPICY dares, level SPICY: Pick(DARE) → Choosing, notice == NO_DARE_CARDS, currentPlayer unchanged
@Test fun refuseWithEmptyPenaltyDeck_advancesWithNotice() // no penalties: Pick+Refuse → currentPlayer == 1, Choosing, notice == NO_PENALTY_CARDS
@Test fun actionsOutOfPhaseAreIgnored()                   // Done while Choosing and Pick while ShowingCard return the same state
@Test fun pickPenaltyTypeIsIgnored()
@Test fun nextSuccessfulActionClearsNotice()
```

Each test asserts the exact fields named in its comment.

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.truthordare.*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the types and reducer.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add Truth or Dare game rules`

---

### Task 6: Spin the Bottle rules

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/game/spinthebottle/SpinTheBottle.kt`, `SpinTheBottleReducer.kt`
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/spinthebottle/SpinTheBottleReducerTest.kt`

**Interfaces:**
- Consumes: `Reducer`, `SequenceRandom` (Task 4), `Deck`, `ContentLibrary`, `Level`, `CardType` (Task 2)
- Produces:
  - `data class SpinState(val players: List<String>, val level: Level, val spinner: Int, val phase: SpinPhase, val penaltyDeck: Deck, val spinCount: Int, val lastTarget: Int?, val notice: SpinNotice?)`
  - `sealed interface SpinPhase { data class ReadyToSpin(val landedOnSelf: Boolean); data class Asking(val asker: Int, val answerer: Int); data class ShowingPenalty(val answerer: Int, val cardId: String) }`
  - `enum class SpinNotice { NO_PENALTY_CARDS }`
  - `sealed interface SpinAction { data object Spin; data object Answered; data object Refuse; data object PenaltyDone }`
  - `fun initialSpinState(players: List<String>, level: Level, library: ContentLibrary): SpinState` (`spinner = 0`, `ReadyToSpin(false)`, `spinCount = 0`, `lastTarget = null`)
  - `object SpinTheBottleReducer : Reducer<SpinState, SpinAction>`

Rules: `Spin` (only in `ReadyToSpin`): `target = random.nextInt(players.size)`, `spinCount + 1`, `lastTarget = target`; if `target == spinner` → `ReadyToSpin(landedOnSelf = true)`, else `Asking(spinner, target)`. `Answered` (in `Asking`) → `spinner = answerer`, `ReadyToSpin(false)`. `Refuse` (in `Asking`) → `ShowingPenalty(answerer, card)`; empty penalty deck → same as `Answered` plus `NO_PENALTY_CARDS`. `PenaltyDone` → `spinner = answerer`, `ReadyToSpin(false)`. Out-of-phase actions: unchanged. The UI animates the bottle whenever `spinCount` changes, toward `lastTarget`.

- [ ] **Step 1: Write the failing tests** (`SequenceRandom` decides targets)

```kotlin
@Test fun spin_landsOnOtherPlayer_asksThem()          // SequenceRandom(2), players 4: phase == Asking(0, 2), spinCount == 1, lastTarget == 2
@Test fun spin_landsOnSpinner_spinsAgain()            // SequenceRandom(0): phase == ReadyToSpin(true), spinner == 0, spinCount == 1
@Test fun answered_answererBecomesSpinner()           // Asking(0,2) + Answered → spinner == 2, ReadyToSpin(false)
@Test fun refuse_showsPenalty_thenAnswererSpins()     // ShowingPenalty(2, "p1") → PenaltyDone → spinner == 2
@Test fun refuseWithEmptyPenaltyDeck_actsLikeAnswered() // spinner == 2, ReadyToSpin(false), notice == NO_PENALTY_CARDS
@Test fun twoPlayers_alternateAndNeverStick() {
    // players A,B; targets 0 (self), 1, then from B: 1 (self), 0
    // assert sequence: ReadyToSpin(true) → Asking(0,1) → Answered → spinner 1 → ReadyToSpin(true) → Asking(1,0)
}
@Test fun actionsOutOfPhaseAreIgnored()               // Answered while ReadyToSpin, Spin while Asking → same state
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.spinthebottle.*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the types and reducer.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Hand off.** Commit message: `feat: add Spin the Bottle game rules`

---

### Task 7: Midnight theme and base components

**Files:**
- Create: `ui/theme/ClinkColors.kt`, `ClinkTypography.kt`, `ClinkTheme.kt`, `LevelColors.kt`, `ui/components/ClinkBackground.kt`, `ClinkButtons.kt`, `LevelTag.kt`, `ClinkCard.kt` (all under `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/`), the four font files, `third_party/fonts/OFL-Unbounded.txt`, `OFL-Manrope.txt`
- Modify: `App.kt` (wrap content in `ClinkTheme` + `ClinkBackground`)
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/ui/theme/LevelColorsTest.kt`

**Interfaces:**
- Consumes: `Level` (Task 2)
- Produces:
  - `object ClinkColors` with exactly: `Background = Color(0xFF16142B)`, `BackgroundTop = Color(0xFF1F1A3D)`, `Surface = Color(0xFF26224A)`, `SurfaceRaised = Color(0xFF3A3466)`, `TextPrimary = Color(0xFFF3F0FF)`, `TextMuted = Color(0xFFA9A3C9)`, `Primary = Color(0xFFFF7A6B)`, `OnPrimary = Color(0xFF1A1530)`, `Secondary = Color(0xFF7C5CFF)`, `Mint = Color(0xFF5EE2C4)`, `Amber = Color(0xFFFFC65C)`
  - `fun Level.color(): Color` — CASUAL → `Mint`, HARD → `Amber`, SPICY → `Primary`
  - `@Composable fun ClinkTheme(content: @Composable () -> Unit)` — Material 3 `darkColorScheme` mapped from `ClinkColors`, typography: display/headline/title/label = Unbounded (600/800), body = Manrope (500/700)
  - `@Composable fun ClinkBackground(content: @Composable BoxScope.() -> Unit)` — vertical gradient `BackgroundTop` → `Background`, plus two soft radial-gradient blobs (violet top-right, coral lower-left, ~45% alpha). Use radial gradients, not `Modifier.blur` (blur is a no-op below Android 12).
  - `PrimaryButton(text, onClick, modifier, enabled)` (coral, `OnPrimary` text, Unbounded 800, 14 dp corners), `SecondaryButton(...)` (`SurfaceRaised`), `LevelTag(level)` (pill in `level.color()`), `ClinkCard(modifier, content)` (`Surface`, 16 dp corners)

- [ ] **Step 1: Get the fonts.** Ask Mehmet to OK downloading the Unbounded and Manrope families from fonts.google.com (OFL, each < 1 MB). From each zip's `static/` folder take Unbounded SemiBold + ExtraBold, Manrope Medium + Bold, rename to the lowercase names in File Structure, and save each family's `OFL.txt` under `third_party/fonts/`.
- [ ] **Step 2: Write the failing test**

```kotlin
@Test fun levelColors_matchSpec() {
    assertEquals(Color(0xFF5EE2C4), Level.CASUAL.color())
    assertEquals(Color(0xFFFFC65C), Level.HARD.color())
    assertEquals(Color(0xFFFF7A6B), Level.SPICY.color())
}
```

- [ ] **Step 3: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.ui.theme.*"`. Expected: FAIL.
- [ ] **Step 4: Implement** the theme and components above.
- [ ] **Step 5: Run** the test. Expected: PASS. Then `./gradlew :androidApp:installDebug`, launch: "clink." in Unbounded on the gradient with the two glows, matching option A in `.superpowers/brainstorm/*/content/visual-style.html`.
- [ ] **Step 6: Hand off.** Commit message: `feat: add Midnight theme, fonts and base components`

---

### Task 8: App flow screens and navigation

**Files:**
- Create: `game/GameId.kt`, `game/PlayerNames.kt`, `AppContainer.kt`, `ui/navigation/Routes.kt`, `ClinkNavHost.kt`, `ui/screens/WelcomeScreen.kt`, `GameListScreen.kt`, `ModeSelectScreen.kt`, `PlayerSetupScreen.kt`, `LevelSelectScreen.kt`, `GameOverScreen.kt`, `composeResources/values/strings.xml`, `values-tr/strings.xml`
- Modify: `App.kt` (host `ClinkNavHost`)
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/PlayerNamesTest.kt`

**Interfaces:**
- Consumes: theme and components (Task 7), `Level` (Task 2), `loadBundledContent()` (Task 3)
- Produces:
  - `@Serializable enum class GameId { TRUTH_OR_DARE, SPIN_THE_BOTTLE, CUP_PONG }`
  - `object PlayerNames { const val MIN = 2; const val MAX = 12; const val MAX_LENGTH = 20; fun validate(raw: List<String>): PlayerNamesResult }`
  - `sealed interface PlayerNamesResult { data class Valid(val names: List<String>); data object TooFew; data object TooMany; data class Duplicate(val name: String) }` — `validate` trims, drops blank rows, caps each name at 20 characters, then checks count (on the remaining names) and duplicates (case-insensitive)
  - `class AppContainer(val library: ContentLibrary, val random: Random = Random.Default)` and `val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }`. `App()` calls `loadBundledContent()` once (`produceState`), shows only `ClinkBackground` until it's loaded, then provides `LocalAppContainer` around `ClinkNavHost`. Game ViewModels get `library` and `random` from it.
  - Routes (`@Serializable`): `WelcomeRoute`, `GameListRoute`, `ModeRoute(game: GameId)`, `PlayersRoute(game: GameId)`, `LevelRoute(game: GameId, players: List<String>)`, `TruthOrDareRoute(players: List<String>, level: Level)`, `SpinTheBottleRoute(players: List<String>, level: Level)`, `GameOverRoute(game: GameId, players: List<String>, level: Level)`. Game routes are registered in Tasks 9 and 10; until then `LevelRoute` navigates to `GameOverRoute`.

Screens:
- **Welcome:** logo `clink.` (Unbounded 800, coral period), tagline, three game chips, `PrimaryButton` "PLAY" → GameList.
- **GameList:** three cards. Cup Pong disabled with "Coming soon" until Plan 2.
- **ModeSelect:** "With friends" (enabled) and "Online" (disabled, "Coming soon" until Plan 4).
- **PlayerSetup:** starts with two empty rows; add row (up to 12), remove row, move up/down; "Continue" runs `PlayerNames.validate` and shows the error for `TooFew`/`TooMany`/`Duplicate`.
- **LevelSelect:** three level cards with `LevelTag` and description; tapping Spicy opens the 18+ dialog every time; only "Yes" continues.
- **GameOver:** "Game over", "Play again" (same game, players, level), "Back to games" (pops to GameList).

Copy (EN / TR):

| Key | EN | TR |
|---|---|---|
| tagline | For everyone at the table. | Masadaki herkes için. |
| play | PLAY | OYNA |
| game_tod / game_spin / game_cuppong | Truth or Dare / Spin the Bottle / Cup Pong | Doğruluk mu Cesaret mi / Şişe Çevirmece / Cup Pong |
| coming_soon | Coming soon | Yakında |
| mode_local / mode_local_sub | With friends / One phone, take turns | Arkadaşlarla / Tek telefon, sırayla |
| mode_online | Online | Online |
| players_title / add_player / continue | Who's playing? / Add player / Continue | Kim oynuyor? / Oyuncu ekle / Devam |
| err_too_few / err_too_many / err_duplicate | At least 2 players needed / 12 players max / "%s" is already playing | En az 2 oyuncu gerekli / En fazla 12 oyuncu / "%s" zaten oyunda |
| level_title | Pick a level | Seviye seç |
| level_casual_desc / hard_desc / spicy_desc | Fun with anyone / More personal, tougher penalties / Flirty and bold, 18+ | Herkesle eğlenceli / Daha kişisel, daha zor cezalar / Flörtöz ve cesur, 18+ |
| adult_title / adult_body | 18 and over / Spicy has bold, adult content. Is everyone at the table 18 or older? | 18 yaş ve üzeri / Spicy yetişkinlere yönelik cesur içerik barındırır. Masadaki herkes 18 yaşından büyük mü? |
| adult_yes / cancel | Yes, we're all 18+ / Cancel | Evet, hepimiz 18+ / Vazgeç |
| game_over / play_again / back_to_games | Game over / Play again / Back to games | Oyun bitti / Tekrar oyna / Oyun listesine dön |

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun trimsAndDropsBlankRows() = assertEquals(PlayerNamesResult.Valid(listOf("Ayşe", "Mert")), PlayerNames.validate(listOf("  Ayşe ", "", "   ", "Mert")))
@Test fun tooFewAfterDroppingBlanks() = assertEquals(PlayerNamesResult.TooFew, PlayerNames.validate(listOf("Ayşe", " ")))
@Test fun tooMany() = assertEquals(PlayerNamesResult.TooMany, PlayerNames.validate((1..13).map { "P$it" }))
@Test fun twelveIsAllowed() = assertIs<PlayerNamesResult.Valid>(PlayerNames.validate((1..12).map { "P$it" }))
@Test fun duplicatesAreCaseInsensitive() = assertEquals(PlayerNamesResult.Duplicate("mert"), PlayerNames.validate(listOf("Mert", "mert")))
@Test fun longNamesAreCappedAt20() {
    val result = PlayerNames.validate(listOf("A".repeat(25), "Can")) as PlayerNamesResult.Valid
    assertEquals(20, result.names[0].length)
}
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.PlayerNamesTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `GameId`, `PlayerNames`, `AppContainer`, routes, nav host, screens and strings.
- [ ] **Step 4: Run** the test. Expected: PASS.
- [ ] **Step 5: Verify on the emulator:** install, walk Welcome → Truth or Dare → With friends → enter `Ayşe`, `ayşe` (duplicate error shown) → fix → Spicy (18+ dialog, Cancel stays, Yes continues) → GameOver → Play again / Back to games. Then `adb shell cmd locale set-app-locales com.mehmetbozkurt.clink --locales tr`, relaunch, confirm Turkish copy.
- [ ] **Step 6: Hand off.** Commit message: `feat: add app flow screens, navigation and TR/EN strings`

---

### Task 9: Truth or Dare screen

**Files:**
- Create: `game/truthordare/TruthOrDareViewModel.kt`, `TruthOrDareScreen.kt`, `ui/components/EndGameDialog.kt`
- Modify: `ui/navigation/ClinkNavHost.kt` (register `TruthOrDareRoute`; `LevelRoute` for TRUTH_OR_DARE navigates to it), `strings.xml` (EN/TR)

**Interfaces:**
- Consumes: `TruthOrDareReducer`, `initialTruthOrDareState`, `TodPhase`, `TodAction`, `TodNotice` (Task 5), `Store` (Task 4), `ContentLibrary.text` (Task 2), `AppContainer` (Task 8), components (Task 7)
- Produces: `class TruthOrDareViewModel(players: List<String>, level: Level, library: ContentLibrary, random: Random) : ViewModel` exposing `val state: StateFlow<TruthOrDareState>` and `fun onAction(action: TodAction)`; `@Composable fun EndGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit)` (reused by Task 10)

Screen: top bar with "Turn: {name}" and an "End" button; `Choosing` → two large buttons Truth / Dare; `ShowingCard` → `ClinkCard` with `LevelTag`, type tag and the card text in the app language (`Locale.current.language`), buttons "Done" (mint) / "Take penalty" (`SurfaceRaised`, coral text); `ShowingPenalty` → card titled "Penalty!" with penalty text and "Penalty done". A card appears with a short flip animation (~250 ms). Haptic feedback when a penalty is drawn. Notices show as a snackbar. "End" and the system back button open `EndGameDialog`; confirm → `GameOverRoute`. State survives rotation and config changes (ViewModel).

Copy (EN / TR): `turn_of` "Turn: %s" / "Sıra: %s"; `truth` Truth / Doğruluk; `dare` Dare / Cesaret; `done` Done / Yaptı; `take_penalty` Take penalty / Ceza çek; `penalty_title` Penalty! / Ceza!; `penalty_done` Penalty done / Cezayı çekti; `end` End / Bitir; `end_confirm` End this game? / Oyunu bitirmek istiyor musun?; `notice_no_truth` No truth cards at this level / Bu seviyede doğruluk kartı yok; `notice_no_dare` No dare cards at this level / Bu seviyede cesaret kartı yok; `notice_no_penalty` No penalties at this level, moving on / Bu seviyede ceza yok, devam ediyoruz.

- [ ] **Step 1: Implement** the ViewModel (holds a `Store` built from the arguments), the screen and the dialog. No new unit tests: the rules are covered by Task 5; this task is wiring and UI.
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest`. Expected: all PASS (nothing regressed).
- [ ] **Step 3: Verify on the emulator:** 3 players, Casual: pick Truth → Done → next player; pick Dare → Take penalty → Penalty done → next player; after the last player, back to the first. While a card is shown, force a configuration change with `adb shell cmd locale set-app-locales com.mehmetbozkurt.clink --locales tr` (the app is portrait-locked, so rotation can't be used) → the same card and turn are still shown, now in Turkish. Back button → dialog → confirm → GameOver → Play again starts fresh.
- [ ] **Step 4: Hand off.** Commit message: `feat: add Truth or Dare screen`

---

### Task 10: Spin the Bottle screen

**Files:**
- Create: `game/spinthebottle/SpinTheBottleViewModel.kt`, `SpinTheBottleScreen.kt`, `BottleCircle.kt`, `composeResources/drawable/bottle.xml`
- Modify: `ui/navigation/ClinkNavHost.kt` (register `SpinTheBottleRoute`; `LevelRoute` for SPIN_THE_BOTTLE navigates to it), `strings.xml` (EN/TR)

**Interfaces:**
- Consumes: `SpinTheBottleReducer`, `initialSpinState`, `SpinPhase`, `SpinAction`, `SpinNotice` (Task 6), `Store` (Task 4), `ContentLibrary.text` (Task 2), `EndGameDialog` (Task 9), components (Task 7)
- Produces: `class SpinTheBottleViewModel(players: List<String>, level: Level, library: ContentLibrary, random: Random) : ViewModel` with `val state: StateFlow<SpinState>` and `fun onAction(action: SpinAction)`; `fun playerAngleDegrees(index: Int, count: Int): Float` in `BottleCircle.kt` = `-90f + index * 360f / count` (0° points right, clockwise; player 0 at the top)

Behavior:
- Players' names sit evenly on a circle; the bottle (simple two-tone vector in `bottle.xml`, coral and violet, no external assets) is in the center. The spinner's name is highlighted.
- **Flick to spin:** drag on the bottle; on release, read the fling speed with a `VelocityTracker`. Below ~300 dp/s nothing happens. Otherwise dispatch `SpinAction.Spin`, then animate an `Animatable` rotation to `playerAngleDegrees(lastTarget, n)` plus `extraTurns` full turns, where flick speed maps linearly to 3–8 turns and 1.8–3.2 s, with a decelerating easing. Controls are disabled while it spins.
- When the animation ends: haptic feedback, then show the phase: `ReadyToSpin(landedOnSelf = true)` → "Landed on yourself, spin again!"; `Asking(asker, answerer)` → "{asker} asks → {answerer} answers" with "Answered" (mint) / "Take penalty"; `ShowingPenalty` → penalty card + "Penalty done".
- "End" button and back button → `EndGameDialog` → `GameOverRoute`.

Copy (EN / TR): `spin_hint` "%s spins — flick the bottle" / "%s çeviriyor, şişeyi fırlat"; `landed_on_self` Landed on yourself, spin again! / Kendine denk geldi, tekrar çevir!; `asks_answers` "%1$s asks → %2$s answers" / "%1$s soruyor → %2$s cevaplıyor"; `answered` Answered / Cevapladı; reuse `take_penalty`, `penalty_title`, `penalty_done`, `end`, `notice_no_penalty`.

- [ ] **Step 1: Write the failing test** (`shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/game/spinthebottle/BottleCircleTest.kt`)

```kotlin
@Test fun firstPlayerIsAtTop() = assertEquals(-90f, playerAngleDegrees(0, 4))
@Test fun playersAreEvenlySpacedClockwise() = assertEquals(0f, playerAngleDegrees(1, 4))
@Test fun twelvePlayers() = assertEquals(240f, playerAngleDegrees(11, 12))
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.spinthebottle.*"`. Expected: FAIL on the new test.
- [ ] **Step 3: Implement** `playerAngleDegrees`, the ViewModel, `BottleCircle` and the screen.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Verify on the emulator:** with 2, 5 and 12 players the bottle visibly stops pointing at the name the prompt names; a weak drag does nothing; a strong flick spins longer; Refuse → penalty → the answerer becomes the spinner; back button → dialog.
- [ ] **Step 6: Hand off.** Commit message: `feat: add Spin the Bottle screen with flick-to-spin`

---

### Task 11: End-to-end check

**Files:** none planned (fix-forward only if something fails; each fix gets its own hand-off).

- [ ] **Step 1: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: `BUILD SUCCESSFUL`, all tests pass.
- [ ] **Step 2: Fresh install with no network:** `adb uninstall com.mehmetbozkurt.clink`, turn on airplane mode in the emulator, install, play one round of each game. Expected: works offline with the sample content.
- [ ] **Step 3: Both languages:** play the Step 2 round once with `--locales en` and once with `--locales tr`. Expected: all UI copy and cards in that language; no English left in the Turkish run except "Cup Pong", "Online", "Casual/Hard/Spicy".
- [ ] **Step 4: Look:** screenshots of Welcome, Level select and a Truth or Dare card (`adb exec-out screencap -p > <scratchpad>/x.png`); compare with option A in the brainstorm mockup. Report differences to Mehmet; don't redesign.
- [ ] **Step 5: Report** to Mehmet in Turkish: what works, what was checked, anything that didn't match. Nothing to commit unless a fix was made.
