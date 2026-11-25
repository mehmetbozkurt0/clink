# Visual Refresh "Street / Graffiti" — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle every screen of Clink in the approved Street / Graffiti look, remove the "low resolution" feel, and add the UX upgrades of the design.

**Architecture:** A small design system first (tokens, typography, textures, and a handful of components: wall background, poster, neon button, tape label, wall chip, initial badge, top bar), then every screen moves onto it. Game rules, reducers and navigation logic don't change. The few new pure pieces (contrast check, badge colors, list reordering) are unit-tested; screens are checked on the emulator and on Mehmet's phone.

**Tech Stack:** As before (Kotlin Multiplatform, Compose Multiplatform resources). New assets: 4 fonts, Phosphor icons as vector drawables, 2–3 textures.

**Spec:** `docs/superpowers/specs/2026-11-20-graffiti-visual-refresh-design.md` (replaces §9 of `docs/superpowers/specs/2026-11-19-party-games-app-design.md`)

## Global Constraints

- **Git (CLAUDE.md, strict):** never commit or rewrite history. Every "Hand off" step: stop, give Mehmet a short Turkish summary and the commit message shown, wait for him to confirm the commit before the next task.
- **Language:** Turkish with Mehmet; code, identifiers, comments and commit messages in English.
- **Downloads need Mehmet's OK first** (what, from where, size). Nothing on Mehmet's phone without his OK in that turn.
- All UI in `shared/src/commonMain`; Compose Multiplatform APIs only; assets through Compose resources (except the launcher icon, which is Android `res/`).
- **Every font covers Turkish** (ğ Ğ ş Ş ı İ ç Ç ö ü); verified on screen in Task 9.
- **Behavior and navigation don't change:** ‹ and × do exactly what system back does today; Game over has no top bar; all existing tests stay green.
- **No drinking imagery or wording;** cups empty, bottle without a label.
- **AA contrast** for every text/background pair (Task 2 test).
- **Added assets ≤ 4 MB** total (fonts ≈ 2.1 MB + textures ≤ 1 MB + icons).
- Test command: `./gradlew :shared:testAndroidHostTest` (as `.\gradlew.bat` from PowerShell on this machine).

## Review Focus

1. **Turkish glyphs** in every font and every place text appears (logo excepted: "clink." only) → no fallback glyphs or boxes (Task 9 check on device, TR locale).
2. **Font scale 1.5 and a 360×640 dp screen** → no clipped text, primary action always visible, lists scroll (Task 9 check).
3. **Texture cost** → textures decoded once and drawn as a tiled shader; scrolling lists and the bottle/ball animations stay smooth (Task 3 implementation, Task 9 check).
4. **Contrast** → every pair the screens use is in the Task 2 contrast test.
5. **Navigation unchanged** → back/close mirror system back on every screen; Game over has no top bar; Cup Pong winner-stays-on still works (Task 9 check).

---

## File Structure

```
shared/src/commonMain/composeResources/
  font/   anton_regular.ttf, archivo_variable.ttf, rubik_spray_paint.ttf, caveat_brush.ttf   (Unbounded, Manrope removed)
  drawable/ ic_back.xml, ic_close.xml, ic_drag.xml, ic_remove.xml, ic_add.xml, ic_game_tod.xml, ic_game_spin.xml, ic_game_pong.xml,
            tex_grain.png, tex_wall.(webp|jpg), tex_paper.(webp|jpg), art_tod.xml, art_spin.xml, art_pong.xml (placeholders), bottle.xml (redrawn)
shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/
  ui/theme/  ClinkColors.kt (new palette), LevelColors.kt, ClinkTypography.kt, Contrast.kt
  ui/components/ WallBackground.kt (replaces ClinkBackground.kt), Poster.kt (replaces ClinkCard.kt), ClinkButtons.kt (neon), TapeLabel.kt,
                 WallChip.kt, InitialBadge.kt, TopBar.kt, LevelTag.kt, AdultConfirmDialog.kt, EndGameDialog.kt
  ui/screens/... and game/... screens restyled
  game/PlayerOrder.kt   — moved(from, to)
androidApp/src/main/res/  mipmap-anydpi-v26/ic_launcher.xml, drawable/ic_launcher_foreground.xml, values/ic_launcher_background.xml
third_party/fonts/ (licenses), third_party/icons/PHOSPHOR-LICENSE.txt, third_party/textures/README.md (source + CC0 note per file)
docs/design/illustration-brief.md
```

---

### Task 1: Assets

**Files:** the fonts, icons and textures above; `third_party/...`; remove `unbounded_*.ttf`, `manrope_*.ttf` and their OFL files only in Task 2 (when the code stops using them).

- [ ] **Step 1: Ask Mehmet's OK** for these downloads, with sizes:
  - Fonts from `github.com/google/fonts`: `ofl/anton/Anton-Regular.ttf` (171 KB), `ofl/archivo/Archivo[wdth,wght].ttf` (659 KB, variable), `ofl/rubikspraypaint/RubikSprayPaint-Regular.ttf` (942 KB), `ofl/caveatbrush/CaveatBrush-Regular.ttf` (296 KB), each family's `OFL.txt`. All list the `latin-ext` subset (Turkish).
  - 8 Phosphor **Bold** SVGs from `github.com/phosphor-icons/core/assets/bold/` (`caret-left`, `x`, `dots-six-vertical`, `trash`, `plus`, `cards`, `arrows-clockwise`, `ping-pong`; < 1 KB each) and its MIT `LICENSE`.
  - Two CC0 textures (a concrete/plaster wall and a paper) from Poly Haven or ambientCG; name the exact asset pages and sizes in the request. Each is cropped to a 512×512 seamless tile, desaturated, ≤ 200 KB.
- [ ] **Step 2: Fetch into a fresh scratch folder**, check each TTF is a TrueType file and each SVG is a single-path 256×256 icon, then copy into the repo.
- [ ] **Step 3: Convert the SVGs to vector drawables** with a small script (viewport 256×256, the SVG `d` → `android:pathData`, fill `#FFFFFFFF`; tint in code). Names as in File Structure.
- [ ] **Step 4: Generate `tex_grain.png`** with a script (256×256, mid-grey luminance noise, fixed seed, tileable by construction: each pixel independent). Record "generated, no license needed" in `third_party/textures/README.md`, with the source page and CC0 note of the two downloaded textures.
- [ ] **Step 5: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: PASS, BUILD SUCCESSFUL (resources only; nothing uses them yet). Report the total added size.
- [ ] **Step 6: Hand off.** Commit message: `chore: add Street visual refresh fonts, icons and textures`

---

### Task 2: Tokens, typography and the contrast check

**Files:**
- Modify: `ui/theme/ClinkColors.kt`, `LevelColors.kt`, `ClinkTypography.kt`, every file that uses an old token
- Create: `ui/theme/Contrast.kt`
- Test: `ui/theme/LevelColorsTest.kt` (update), `ui/theme/ContrastTest.kt`

**Interfaces:**
- Produces:
  - `object ClinkColors` with exactly: `Wall = Color(0xFF1C1C1F)`, `WallRaised = Color(0xFF26262A)`, `Paper = Color(0xFFF4EFE3)`, `Ink = Color(0xFF141414)`, `Muted = Color(0xFFA8A49A)`, `Acid = Color(0xFFC6FF00)`, `Pink = Color(0xFFFF2E88)`, `Amber = Color(0xFFFFB020)`, `Tape = Color(0xFFE8DCB5)`. The old names are removed; every use moves to the new token that plays the same role (background → `Wall`, cards/panels → `WallRaised` until Task 3's poster, text → `Paper`, secondary text → `Muted`, coral primary → `Acid`, violet/mint accents → `Pink`/`Acid`, text on buttons → `Ink`).
  - `fun Level.color()`: CASUAL → `Acid`, HARD → `Amber`, SPICY → `Pink`.
  - `fun contrastRatio(foreground: Color, background: Color): Double` (WCAG relative luminance).
  - Typography: display/headline/title/label → Anton; body → Archivo (variable font, weights 500/700/800 via `FontWeight`); `ClinkFonts.spray` (Rubik Spray Paint) and `ClinkFonts.marker` (Caveat Brush) exposed for the logo and tape labels.

- [ ] **Step 1: Write the failing tests**

```kotlin
// LevelColorsTest (replace the old assertions)
assertEquals(Color(0xFFC6FF00), Level.CASUAL.color()); assertEquals(Color(0xFFFFB020), Level.HARD.color()); assertEquals(Color(0xFFFF2E88), Level.SPICY.color())

// ContrastTest
@Test fun blackOnWhiteIs21() = assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
@Test fun everyTextPairMeetsAA() {
    val pairs = listOf(
        ClinkColors.Paper to ClinkColors.Wall, ClinkColors.Paper to ClinkColors.WallRaised,
        ClinkColors.Muted to ClinkColors.Wall, ClinkColors.Muted to ClinkColors.WallRaised,
        ClinkColors.Ink to ClinkColors.Paper, ClinkColors.Ink to ClinkColors.Tape,
        ClinkColors.Ink to ClinkColors.Acid, ClinkColors.Ink to ClinkColors.Pink, ClinkColors.Ink to ClinkColors.Amber,
        ClinkColors.Acid to ClinkColors.Wall, ClinkColors.Amber to ClinkColors.Wall,
    )
    pairs.forEach { (fg, bg) -> assertTrue(contrastRatio(fg, bg) >= 4.5, "$fg on $bg: ${contrastRatio(fg, bg)}") }
}
```

Any pair a later task adds goes into this list in that task.

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.ui.theme.*"`. Expected: FAIL (unresolved `contrastRatio`, old level colors).
- [ ] **Step 3: Implement** the tokens, `contrastRatio`, the typography; move every usage to the new tokens; delete the Unbounded/Manrope files and licenses.
- [ ] **Step 4: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: all PASS. Launch once on the emulator: every screen opens, recolored (still the old layouts).
- [ ] **Step 5: Hand off.** Commit message: `feat: switch to the Street palette and typography`

---

### Task 3: Design-system components

**Files:**
- Create: `ui/components/WallBackground.kt` (replaces `ClinkBackground.kt`), `Poster.kt` (replaces `ClinkCard.kt`), `TapeLabel.kt`, `WallChip.kt`, `InitialBadge.kt`, `TopBar.kt`
- Modify: `ClinkButtons.kt`, `LevelTag.kt`, `AdultConfirmDialog.kt`, `EndGameDialog.kt`, `App.kt` (loading state)
- Test: `ui/components/InitialBadgeTest.kt`

**Interfaces:**
- Produces:
  - `WallBackground(content: @Composable BoxScope.() -> Unit)` — `Wall` color, the wall texture and the grain each drawn as a **tiled `ImageShader` brush created once** (`remember`), two small glows under the grain (pink top-right, acid lower-left, radius ≤ 35% of width), content inside safe-drawing insets.
  - `Poster(modifier, rotation: Float = 0f, content: ColumnScope.() -> Unit)` — `Paper` + paper texture, a fixed irregular "torn" outline (static path built once from the size), `Ink` content color.
  - `PrimaryButton` / `SecondaryButton` → **neon buttons**: `Acid` (primary) / `Pink` (secondary) fill, `Ink` text in Anton, 2 dp `Ink` inner outline, 4 dp hard offset shadow in the other neon; while pressed the shadow offset animates to 0. `ClinkButton(text, onClick, modifier, enabled, container, content)` stays for custom colors.
  - `TapeLabel(text)` — `Tape` strip, ragged ends, −2.5° rotation, `ClinkFonts.marker`, `Ink`.
  - `WallChip(text, selected: Boolean = false, color: Color? = null, onClick: (() -> Unit)? = null)` — `WallRaised` + hairline border, or filled with `color`/`Acid` when selected.
  - `fun badgeColor(index: Int): Color` — cycles `Acid, Pink, Amber, Paper`; `InitialBadge(name, index, size = 28.dp)` — circle in `badgeColor(index)` with the name's first letter (`uppercase(Locale.current)`-safe for Turkish: "ilker" → "İ") in Anton, `Ink`.
  - `TopBar(navigation: TopBarNav, onNavigate: () -> Unit, title: String? = null, trailing: @Composable () -> Unit = {})` with `enum class TopBarNav { Back, Close }`; 48 dp touch targets, icons from Task 1 with content descriptions (`cd_back` "Back"/"Geri", `cd_close` "Close"/"Kapat").
  - Dialogs (18+, End game) as posters with Anton title and neon buttons.
  - `App()` shows the spray logo on the wall while content loads.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun badgeColorsCycle() = assertEquals(
    listOf(ClinkColors.Acid, ClinkColors.Pink, ClinkColors.Amber, ClinkColors.Paper, ClinkColors.Acid),
    (0..4).map { badgeColor(it) })
@Test fun initialUsesTurkishCapitalI() = assertEquals("İ", initialOf("ilker", "tr"))
@Test fun initialOfBlankIsQuestionMark() = assertEquals("?", initialOf("  ", "en"))
```
(`fun initialOf(name: String, language: String): String` lives in `InitialBadge.kt`; the composable calls it with `Locale.current.language`.)

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.ui.components.*"`. Expected: FAIL on the new tests.
- [ ] **Step 3: Implement** the components; switch `App.kt` and every `ClinkBackground`/`ClinkCard` use to `WallBackground`/`Poster`; delete the old files.
- [ ] **Step 4: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: PASS. Emulator: Welcome shows grain and wall texture; a dialog shows as a poster.
- [ ] **Step 5: Hand off.** Commit message: `feat: add Street design-system components`

---

### Task 4: Flow screens

**Files:**
- Modify: `ui/screens/WelcomeScreen.kt`, `GameListScreen.kt`, `ModeSelectScreen.kt`, `LevelSelectScreen.kt`, `GameOverScreen.kt`, `ScreenParts.kt`, `ui/navigation/ClinkNavHost.kt` (transitions only), strings EN/TR

**Interfaces:**
- Consumes: Task 3 components; `GameId.title()`.

Per the design §4: Welcome (spray logo + drip vectors, tape tagline, three sticker rows with game icons, `OYNA` at the bottom, no top bar); Game list (title "Ne oynuyoruz?" / "What are we playing?", one poster per game with its placeholder art from Task 8 slot — until then the game icon — name, players line and rule line); Mode (two posters, Online "Yakında"); Level (three posters with level pill and description; 18+ dialog unchanged in behavior); Game over (no top bar, Anton "OYUN BİTTİ" / "GAME OVER", game name as `TapeLabel`, CTAs). `TopBar(Back)` on Game list, Mode, Level. Screen transitions: `NavHost` enter/exit = slide 1/8 width + fade, ≈250 ms; pop reverses.

Copy (EN / TR): `games_title` "What are we playing?" / "Ne oynuyoruz?"; `game_tod_meta` "2–12 players · Draw a card, do it or take a penalty" / "2–12 kişi · Kart çek, yap ya da ceza"; `game_spin_meta` "2–12 players · Whoever the bottle picks" / "2–12 kişi · Şişe kimi gösterirse"; `game_pong_meta` "1v1 · Flick the ball into the cups" / "1'e 1 · Topu bardağa fırlat"; `game_over` "GAME OVER" / "OYUN BİTTİ".

- [ ] **Step 1: Implement** the five screens and the transitions. No new unit tests (layout only).
- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: PASS.
- [ ] **Step 3: Emulator walk** Welcome → Game list → Mode → (Players unchanged) → Level (Spicy dialog Cancel/Yes) → a game → Game over → Play again / Back to games; ‹ on each flow screen does what system back does. Screenshots of Welcome, Game list, Level.
- [ ] **Step 4: Hand off.** Commit message: `feat: restyle the flow screens`

---

### Task 5: Players screen

**Files:**
- Create: `game/PlayerOrder.kt`
- Modify: `ui/screens/PlayerSetupScreen.kt`, strings
- Test: `game/PlayerOrderTest.kt`

**Interfaces:**
- Produces: `fun <T> List<T>.moved(from: Int, to: Int): List<T>` (item at `from` ends at `to`, others keep their order; out-of-range → same list).

Screen: `TopBar(Back)` with counter "n / 12" (`players_count` "%1$d / %2$d"); rows on `WallChip`-style surfaces: `InitialBadge`, the name field, a drag handle (`ic_drag`, long-press + drag moves the row; rows animate into place) and a remove icon (`ic_remove`, disabled at 2 rows); dashed "+ Add player" (`ic_add`); errors in `Amber`; `DEVAM` at the bottom. The ↑/↓ buttons are removed. Keyboard Next still goes to the next name field (Plan 1 fix) — the handle and remove icon are not focusable.

- [ ] **Step 1: Write the failing test**

```kotlin
@Test fun movesDown() = assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moved(0, 1))
@Test fun movesUpToTop() = assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, 0))
@Test fun outOfRangeIsUnchanged() = assertEquals(listOf("a", "b"), listOf("a", "b").moved(0, 5))
```

- [ ] **Step 2: Run** `./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.game.PlayerOrderTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `moved` and the screen.
- [ ] **Step 4: Run** the full suite and build. Expected: PASS. Emulator: add 4 players, drag the 4th to the top, remove one, keyboard Next walks the name fields, duplicate error shows; screenshot.
- [ ] **Step 5: Hand off.** Commit message: `feat: restyle the players screen with drag to reorder`

---

### Task 6: Truth or Dare and Spin the Bottle

**Files:**
- Modify: `game/truthordare/TruthOrDareScreen.kt`, `game/spinthebottle/SpinTheBottleScreen.kt`, `BottleCircle.kt`, `composeResources/drawable/bottle.xml`, strings

Per design §4: `TopBar(Close)` with a turn badge (`InitialBadge` + "SIRA: MERT" in Anton) in both games. ToD: choosing = two large neon panels (Truth `Acid`, Dare `Pink`); card = `Poster` sized to its content (not full height), level + type pills, question in Archivo 800, `TapeLabel`-style hint `tod_hint` "Answer or take a penalty" / "Cevap ver ya da ceza çek"; `YAPTI` (acid) / `CEZA ÇEK` (pink); penalty as a poster. The flip is replaced by a "slap": scale 1.04→1 with ±1° rotation, ≈200 ms. Spin: names as `WallChip`s around the circle (spinner `Acid`, answerer `Pink`), redrawn bottle (plain glass bottle, no label, acid/pink highlights), prompt and buttons as in ToD; **add the penalty haptic** in Spin (carried-over minor, design §3.4).

- [ ] **Step 1: Implement.** No new unit tests (rules unchanged, Plan 1 tests cover them).
- [ ] **Step 2: Run** the full suite and build. Expected: PASS.
- [ ] **Step 3: Emulator:** play a ToD round (Truth → Done, Dare → Take penalty → Penalty done) and a Spin round with 5 players (bottle still stops on the named player); × opens the End-game dialog; screenshots.
- [ ] **Step 4: Hand off.** Commit message: `feat: restyle Truth or Dare and Spin the Bottle`

---

### Task 7: Cup Pong

**Files:**
- Modify: `game/cuppong/CupPongTable.kt`, `CupPongScreen.kt`, `CupPongSetupScreen.kt`, strings

Restyle only — throw model and constants untouched. Table: a paper/plywood surface (paper texture tinted) on the wall with an `Ink` border; cups with `Acid` (defender's cups) rims, `WallRaised` inside, empty; ball `Paper` with a hard shadow; hit splash `Acid`. `TopBar(Close)` + turn badge + score line. Match picker: player stickers (`WallChip`, selected = `Acid`), `TopBar(Back)`. Match over: winner banner in Anton `Acid`, penalty poster. Carried-over copy fixes: no turn header on match over; with an empty penalty pool the poster shows only `notice_no_penalty` (no "Penalty!" / "takes a penalty" lines).

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Run** the full suite and build. Expected: PASS.
- [ ] **Step 3: Emulator:** 2-player match to the end (aimed throws), match over → Play again; 4-player picker; screenshots.
- [ ] **Step 4: Hand off.** Commit message: `feat: restyle Cup Pong`

---

### Task 8: Launcher icon, placeholder art and the illustration brief

**Files:**
- Create: `androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `drawable/ic_launcher_foreground.xml`, `values/ic_launcher_background.xml` (`#1C1C1F`); `composeResources/drawable/art_tod.xml`, `art_spin.xml`, `art_pong.xml`; `docs/design/illustration-brief.md`
- Modify: `AndroidManifest.xml` (`android:icon`, `android:roundIcon`), `GameListScreen.kt` (use the art)

- Launcher icon: adaptive icon, foreground = a sprayed acid "c" with a pink dot and two drips inside the 66 dp safe zone; monochrome layer = the same "c" (Android 13+ themed icons).
- Placeholder art (vector, ≈ 120×120 dp): ToD = two fanned cards with "?" and "!", Spin = the plain bottle at an angle with motion lines, Pong = three empty cups and a ball. Palette tokens only, `Ink` outlines 2–3 dp.
- `illustration-brief.md` (Turkish, for Mehmet to hand to a designer or an AI tool): the 3 game illustrations + launcher artwork; style (graffiti sticker, thick ink outline, flat color + light grain), palette with hex codes, no text, no alcohol (bottle without label, cups empty), transparent background, deliverable 1024×1024 PNG + source file, commercial-use license required (for AI tools Mehmet checks the tool's terms), where each image appears with a screenshot of the placeholder.

- [ ] **Step 1: Implement.**
- [ ] **Step 2: Run** the full suite and build. Expected: PASS.
- [ ] **Step 3: Emulator:** launcher shows the new icon (round and themed variants); Game list shows the three placeholders.
- [ ] **Step 4: Hand off.** Commit message: `feat: add launcher icon, placeholder game art and illustration brief`

---

### Task 9: End-to-end check

**Files:** none planned (fix-forward; each fix its own hand-off).

- [ ] **Step 1:** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --rerun-tasks`. Expected: all PASS.
- [ ] **Step 2: Emulator, TR and EN:** every screen once; screenshots; look for fallback glyphs in ğ ş ı İ ç ö ü (Review Focus 1).
- [ ] **Step 3: Emulator, font scale 1.5 and a 360×640 dp screen** (`wm size 945x1680`): every screen, nothing clipped, CTA visible (Review Focus 2); restore the emulator settings afterwards.
- [ ] **Step 4: Navigation:** ‹/× and system back on every screen behave as before; Game over has no top bar; Cup Pong winner-stays-on (Review Focus 5).
- [ ] **Step 5: Mehmet's phone (with his OK in that turn):** install, Welcome + a game screen screenshot; Mehmet judges the "low resolution" feel and dark areas at low brightness (design §5 "Done when").
- [ ] **Step 6: Report** to Mehmet in Turkish; the APK size before/after.

---

## Carried over (not in this plan)

Open items from Plans 1 and 2, kept here so they aren't lost; Mehmet decides when to do them. (Done in this plan: Spin penalty haptic — Task 6; Cup Pong "Turn: winner" header and empty-pool copy — Task 7. The coral contrast item is replaced by Task 2's contrast test.)
- **Cup Pong throw feel and physics rework** — Mehmet: "still hard", own dedicated effort later. Includes drawn cups not matching the physics depth.
- Cup Pong: back/End on the match-over panel loses winner-stays-on; the "Penalty done" tap guard starts ~2 s before the panel shows.
- Repeated taps on an empty deck queue identical snackbars (~20 s).
- Drinking-word check misses wine, vodka, tequila, whisk(e)y, chug, şarap, rakı, kadeh, votka, viski, tekila, içecek.
- Names overlap on the 12-player Spin circle with ~7+ character names.
- `take(20)` can split an emoji surrogate pair in player names.
- KDocs that mislead Plan 4: `SpinTheBottle.kt` and `CupPong.kt` say the UI animates on a counter change (it animates from the flick handler); `ThrowInput` says it is "published as-is" — Plan 4 should render the referee's `ThrowResult`.
- Missing Cup Pong tests: outer rim edge (distance 4), rim bounce into a removed cup, rim bounce sinking the last cup.
- Spin's VelocityTracker still uses `addPosition` (Cup Pong moved to `addPointerInputChange`).
- Gradle: `androidLibrary { }` in `shared/build.gradle.kts` is deprecated in favor of `android { }`.
- A malformed bundled content file still crashes at launch; only `BaselineContentFileTest` guards it (make tests run before release builds in Plan 5).
