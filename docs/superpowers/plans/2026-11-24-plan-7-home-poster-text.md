# Home Poster Text and Art Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the three home posters a highlighted title word, an ink line drawing and a players stamp (sketch C).

**Architecture:** A pure `accentRange` finds the highlighted word in the displayed title; per-language accent strings name the word. `GamePoster` in `HomePosters.kt` becomes title (with a tilted band drawn behind the word from the text layout) → drawing (new ink-only vector per game, fills the free space) → stamp. Nothing outside the home posters changes.

**Tech Stack:** Kotlin 2.4, Compose Multiplatform 1.12, compose resources (strings, vector drawables).

**Spec:** `docs/superpowers/specs/2026-11-24-home-poster-text-design.md`

## Global Constraints
- Highlight bands: Paper poster → Pink band; Acid poster → Ink band with Acid text; Pink poster → Acid band; band tilted about −2°.
- Accent words — EN: `Dare` / `Spin` / `Pong`; TR: `Cesaret` / `Şişe` / `Pong`. Not found in the title → no highlight.
- Drawings: ink (`#141414`) strokes only, no fills; two cards with ? and !, a bottle with spin arrows, three cups with a ball's arc.
- Stamp: players text upper-cased, Anton, 2.5 dp ink outline, tilted about −5°; bottom-start, except bottom-end on the second (top-right) poster.
- Unchanged: places, tilts, colors, tape, sway, not tappable, one screen-reader label (`cd_home_posters`).
- All poster sizes scale from the poster width, as today. Code/comments in English. No commit by the executor — Mehmet commits.

## Review Focus
1. Turkish titles ("DOĞRULUK MU CESARET Mİ") wrap to three lines on a small poster — the drawing must shrink, never push the stamp off the poster.
2. Short screen (720×1280 @320): posters are smallest; title, drawing and stamp must all stay inside the poster.
3. A highlighted word that wraps across lines must get a band on each line part, not one box spanning both lines.
4. Upper-casing with the current locale (Turkish dotted İ) must not break the match — both strings are upper-cased with the same locale before matching (covered by a test in Task 1).
5. The bottom poster must not cover the top-right poster's stamp.

---

### Task 1: Accent word lookup and strings

**Files:**
- Create: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/PosterAccent.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`, `values-tr/strings.xml` (after the `game_*_players` lines)
- Test: `shared/src/commonTest/kotlin/com/mehmetbozkurt/clink/ui/screens/PosterAccentTest.kt`

**Interfaces:**
- Produces: `internal fun accentRange(title: String, accent: String): IntRange?` — exact (case-sensitive) search; callers pass both already upper-cased with the same locale. Blank accent or no match → `null`.
- Produces: strings `game_tod_accent`, `game_spin_accent`, `game_pong_accent`; `@Composable internal fun GameId.accent(): String` in `ScreenParts.kt` next to `players()`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun findsTheWordInAnEnglishTitle() = assertEquals(9..12, accentRange("TRUTH OR DARE", "DARE"))
@Test fun findsTheWordInATurkishTitle() = assertEquals(12..18, accentRange("DOĞRULUK MU CESARET Mİ", "CESARET"))
@Test fun findsAWordWithDottedCapitalI() = assertEquals(0..3, accentRange("ŞİŞE ÇEVİRMECE", "ŞİŞE"))
@Test fun missingWordHasNoAccent() = assertNull(accentRange("BEER PONG", "DARE"))
@Test fun blankWordHasNoAccent() = assertNull(accentRange("BEER PONG", " "))
```

- [ ] **Step 2: Run** `suite.ps1 -Filter "*PosterAccentTest"` — Expected: compile failure, `accentRange` unresolved.
- [ ] **Step 3: Implement `accentRange`, the six strings (EN Dare/Spin/Pong, TR Cesaret/Şişe/Pong) and `GameId.accent()`.**
- [ ] **Step 4: Run** `suite.ps1 -Filter "*PosterAccentTest"` — Expected: 5/5 pass.

### Task 2: Ink line drawings

**Files:**
- Create: `shared/src/commonMain/composeResources/drawable/poster_tod.xml`, `poster_spin.xml`, `poster_pong.xml`
- Modify: `ScreenParts.kt` — add `internal fun GameId.posterArt(): DrawableResource` next to `art()`.

**Interfaces:**
- Produces: `GameId.posterArt()` → `Res.drawable.poster_tod | poster_spin | poster_pong`.

- [ ] **Step 1: Draw the three vectors** — viewport about 100×80, `strokeColor="#141414"`, stroke ≈ 2.6, round caps/joins, no `fillColor` except the small dots of ? and !. Shapes as in the approved mockup (`.superpowers/brainstorm/24270-1795531839/content/poster-text-v2.html`, option C): ToD two tilted cards that do not overlap; Spin a bottle at about −24° with two curved arrows; Pong three cups with a dashed arc and a ball.
- [ ] **Step 2: Add `posterArt()`.**
- [ ] **Step 3: Run** `suite.ps1 -Build` — Expected: `exit=0`, suite all green (resources compile).

### Task 3: New poster layout

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/HomePosters.kt` (`PosterLook`, `HomePosters`, `GamePoster`)

**Interfaces:**
- Consumes: `accentRange`, `GameId.accent()` (Task 1); `GameId.posterArt()` (Task 2); existing `GameId.players()`.

- [ ] **Step 1: Extend `PosterLook`** with `band: Color` and `accentText: Color` (Paper: Pink/Ink, Acid: Ink/Acid, Pink: Acid/Ink) and `stampEnd: Boolean` (true only for the second look).
- [ ] **Step 2: Rewrite `GamePoster`** as a Column inside the existing poster surface:
  - Title: upper-case title and accent with `Locale.current`; `accentRange` → a `SpanStyle(color = accentText)` on that range; band drawn in `drawBehind` from the captured `TextLayoutResult`: for each line the range touches, the union of `getBoundingBox` of its chars, padded by about `width * 0.025`, rotated −2° about its own center, filled with `band`.
  - Drawing: `Image(painterResource(game.posterArt()), contentDescription = null, contentScale = ContentScale.Fit)` with `Modifier.weight(1f).fillMaxWidth()` and small vertical padding — it takes whatever space the title leaves.
  - Stamp: `Text(players.toUpperCase(Locale.current))` in `MaterialTheme.typography.headlineSmall` sized `width * 0.085`, inside `border(2.5.dp, Ink)` + padding, `rotate(-5f)`, aligned start or end by `stampEnd`.
  - Remove the old players line. Keep tape, shadow, sway and semantics as they are.
- [ ] **Step 3: Run** `suite.ps1 -Build` — Expected: `exit=0`, all tests pass.
- [ ] **Step 4: Emulator check (emulator-5554 only)** — screenshots of home in English, Turkish (via Settings → Türkçe, then back to System), and at `wm size 720x1280` + `wm density 320` (reset both after). Expected: each poster shows a banded word, a drawing and a stamp, all inside the poster; the top-right stamp is visible; Review Focus 1–3 and 5 hold.
- [ ] **Step 5: Report** — summary and one commit message `feat: give the home posters a highlighted title, a drawing and a stamp`; no commit by the executor.
