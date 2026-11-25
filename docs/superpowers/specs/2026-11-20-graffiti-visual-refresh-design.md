# Clink — Visual Refresh "Street / Graffiti": Design

- **Date:** 2026-11-20
- **Status:** Draft, awaiting Mehmet's approval
- **Changes:** replaces §9 "A · Midnight" of `docs/superpowers/specs/2026-11-19-party-games-app-design.md`. Everything else in that spec stands.
- **Reference mockup:** `.superpowers/brainstorm/1800-1795172543/content/graffiti-refined.html` (local, git-ignored). It is a direction, not a pixel spec; this document is the source of truth.

Marking used in this document: anything **Mehmet decided** is stated plainly. Anything marked *(Claude's call)* was proposed by Claude and is open to change.

---

## 1. Intent and why

Plans 1 and 2 shipped working games in the "Midnight" look. On his phone Mehmet found the app looks **low-resolution**, and it doesn't yet pull people in. This step gives Clink a distinctive, polished look and raises the UX of every screen, so that a group opening it for the first time thinks "wow" **and** uses it without thinking.

Mehmet compared six directions side by side (Midnight, Candypop, Retro Pub, Grunge/Graffiti, Pop-art, Cel-shaded anime) and chose **Grunge / Graffiti**, refined: it must look **stylish and tidy — a well-made street poster, not a messy wall**. Looks must never cost usability: **UI and UX both have to be top level.**

## 2. Goals, non-goals, anti-goals

**Goals**
- Every screen of the app in the new Street / Graffiti look: Welcome, Game list, Mode, Players, Level, Truth or Dare, Spin the Bottle, Cup Pong (match picker, table, match over), Game over, and all dialogs.
- Remove the low-resolution feel (see §5).
- The UX upgrades in §6.
- Real assets instead of text, emoji or hand-drawn glyphs (see §7).

**Non-goals (this step)**
- Cup Pong's throw feel and physics. Mehmet set it aside as its own piece of work; this step only restyles the table, cups and ball.
- New features or screens beyond §6 (no settings screen, no sound, no onboarding tour).
- Firebase, accounts, online play (Plans 3–4).

**Anti-goals** (failure even if it technically works)
- Handwritten or spray fonts on anything people must read quickly (card questions, buttons, names, rules).
- Texture so strong that text or buttons are hard to see, or that looks dirty rather than designed.
- Any drinking imagery or wording (spec §10.5). Cup Pong cups are drawn empty; the Spin bottle is a plain bottle with no label.
- Slower screens: no per-frame heavy effects; textures are static images.

## 3. Visual system

### 3.1 Colors *(Claude's call, from the approved mockup)*
| Token | Hex | Use |
|---|---|---|
| `wall` | `#1C1C1F` | App background |
| `wallRaised` | `#26262A` | Rows, chips, panels on the wall |
| `paper` | `#F4EFE3` | Posters (game cards, question card, penalty card), text on dark |
| `ink` | `#141414` | Text on paper and on neon buttons |
| `muted` | `#A8A49A` | Secondary text, hints, inactive icons on the wall |
| `acid` | `#C6FF00` | Primary action, Casual level, highlights |
| `pink` | `#FF2E88` | Secondary accent, "Take penalty", Spicy level |
| `amber` | `#FFB020` | Hard level, warnings |
| `tape` | `#E8DCB5` | Masking-tape labels |

- Dark only, as before.
- Level colors: **Casual = acid, Hard = amber, Spicy = pink** (pills use `ink` text).
- Every text/background pair used must meet **WCAG AA** (4.5:1 for normal text, 3:1 for text ≥ 18.5 sp bold / 24 sp). The builder checks each pair and darkens/lightens the pair that fails.

### 3.2 Typography
| Role | Font | Where |
|---|---|---|
| Logo | **Rubik Spray Paint** | the "clink." logo only |
| Headings, buttons, pills, player names on badges | **Anton** (uppercase for headings/buttons) | screen titles, CTAs, level/type pills, "TURN: MERT" |
| Everything people read | **Archivo** 500 / 700 / 800 | card questions, penalties, rules, inputs, body |
| Small accents | **a handwritten marker font that covers Turkish** *(Claude's call)* | tape labels and one-line notes only (never more than ~6 words) |

- **Every font must cover Turkish (ğ, Ğ, ş, Ş, ı, İ, ç, Ç, ö, ü).** The mockup's Permanent Marker does not (basic Latin only), so the accent font is a Turkish-capable marker/brush font in the same spirit, chosen and glyph-checked when it is downloaded. Rubik Spray Paint, Anton and Archivo include Latin Extended; the builder checks these glyphs too.
- All fonts must have an open license that allows bundling in a commercial app (SIL OFL or Apache 2.0); each license goes into `third_party/fonts/`. Bundled in the app, not loaded from the network. Unbounded and Manrope are removed. Downloading needs Mehmet's OK first (what, from where, size).

### 3.3 Shape and surfaces
- **Wall:** `wall` color + a **grain texture** + a very faint brick/joint texture (§5, §7). Two soft spray glows (pink top-right, acid lower-left) stay, but under the grain.
- **Posters** (`paper`): torn/uneven edges (a fixed irregular outline, not random each frame), rotated by about ±1°, used for game cards, the question card and the penalty card. Text on posters is `ink`.
- **Neon buttons:** rounded 14 dp, `ink` 2 dp inner outline, a hard offset shadow in the other neon (acid button → pink shadow, pink → acid), no blur.
- **Tape labels:** `tape` strip with a ragged clip, slightly rotated, Permanent Marker text.
- **Rows/chips on the wall:** `wallRaised` at ~7% paper tint with a hairline border, 12 dp corners.
- Hard edges everywhere; no soft drop shadows. Rotation never applies to text people must read quickly beyond ±1.5°.

### 3.4 Motion and haptics
- Screen transitions: short slide + fade (≈250 ms) instead of the default fade *(Claude's call)*.
- A new card/penalty "slaps" onto the wall: quick scale 1.04→1 with the ±1° rotation (≈200 ms), replacing the flip *(Claude's call)*.
- Buttons press down onto their shadow (offset shadow → 0) while pressed.
- Haptics unchanged: bottle stops, ball lands in a cup, penalty drawn (now also in Spin the Bottle — carried-over minor).

## 4. Screens

Same flow and copy as today unless stated. Every screen gets the shared top bar (§6).

- **Welcome:** spray logo with drips, tape tagline, the three games as stickers with their icons, `OYNA` acid CTA at the bottom.
- **Game list:** title "Ne oynuyoruz?" (EN "What are we playing?"); one poster per game with illustration (§7), name, player count and a one-line rule:
  - Truth or Dare — "2–12 kişi · Kart çek, yap ya da ceza" / "2–12 players · Draw a card, do it or take a penalty"
  - Spin the Bottle — "2–12 kişi · Şişe kimi gösterirse" / "2–12 players · Whoever the bottle picks"
  - Cup Pong — "1'e 1 · Topu bardağa fırlat" / "1v1 · Flick the ball into the cups"
- **Mode:** two posters (With friends / Online "Coming soon").
- **Players:** title + counter "3 / 12"; rows with a colored initial badge, the name field, a drag handle and a remove icon; dashed "+ Add player"; `DEVAM` CTA. Error text in `amber`.
- **Level:** three posters with the level pill and description; Spicy keeps the 18+ dialog every time.
- **Truth or Dare:** top bar with close (×) and a turn badge (initial + "SIRA: MERT"); choosing = two large neon panels (Truth acid, Dare pink); card = paper poster with level + type pills, the question in Archivo 800, a Permanent Marker hint ("Cevap ver ya da ceza çek"); buttons `YAPTI` (acid) / `CEZA ÇEK` (pink). The card is sized to its content, not stretched to the full height *(fixes the Plan 1 look note)*.
- **Spin the Bottle:** names as wall chips around the circle (spinner acid, answerer pink); the bottle illustration (§7); prompt and buttons as in ToD.
- **Cup Pong:** table drawn as a paper/plywood surface on the wall, cups with acid/pink rims, empty; ball white with a hard shadow; match picker with chips as stickers; match-over panel as a poster. Also fixes the carried-over copy issues: no "Turn: winner" header on match over; with an empty penalty pool the card doesn't say "Penalty!".
- **Game over:** no top bar; big Anton "OYUN BİTTİ", game name as a tape label, CTAs.
- **Dialogs** (18+, End game): paper poster dialogs with Anton title and neon buttons.

## 5. The low-resolution fix

**Checked on Mehmet's phone** (Samsung Galaxy S22, SM-S901E, Android 16, 1080×2340 at 480 dpi, font scale 0.9; screenshot of Welcome taken over adb on 2026-11-20):
- The app's pixels are fine: text is crisp, and the glows are already dithered (along a line through the coral glow: 304 distinct colors in 701 px, longest flat run 6 px). So there is **no banding in the rendered image**, and nothing is scaled up.
- What the screenshot does show: about two thirds of the screen is large, out-of-focus glow over near-black, and apart from the logo, three chips and one button there is almost nothing with a crisp edge or detail. The eye reads a mostly blurry screen as "low resolution". *(Claude's reading.)*
- On an OLED panel, very dark smooth gradients (here #16142B–#2F2560) can also show blotchy steps on the panel itself at low brightness, which a screenshot cannot capture. *(Possible, not verified.)*

So the fix is about **detail and crispness**, not resolution:
- A **grain texture** (tileable, bundled image) drawn over the whole background at low opacity, plus the faint wall texture: every part of the screen has fine detail, and any panel-level blotching in dark areas is masked.
- Glows become small accents that sit under the grain; no large soft gradient fills most of the screen.
- More crisp surfaces on every screen: posters, stickers, tape, icons, hard offset shadows (§3.3).
- Hard edges and ≥1 dp strokes; nothing drawn at fractional pixel widths below 1 dp.
- **Done when:** on Mehmet's phone (S22), Welcome and the game screens no longer feel "low resolution" to him, and no blotchy steps are visible in dark areas at low brightness.

## 6. UX upgrades

- **Shared top bar** on every screen except Welcome and Game over: back (‹) on flow screens (Game list, Mode, Players, Level, Cup Pong match picker), close (×) on game screens (close opens the End-game dialog, like system back). 48 dp touch targets. **Navigation doesn't change:** ‹ does exactly what system back does today. Game over has no top bar *(Claude's call)*: it already has two explicit buttons, and a ‹ there would lead back to Level select, which is easy to hit by mistake. *(Claude's call: the match picker counts as a flow screen.)*
- **Primary action always at the bottom**, full width, in thumb reach.
- **Real icons** everywhere (back, close, drag handle, remove, game icons) — see §7.
- **Players:** reorder by **long-press and drag** on the handle, replacing the ↑/↓ buttons; remove with the × icon; colored initial badges (colors cycle acid, pink, amber, paper); counter "n / 12".
- **Turn badge** (initial + name) on every game screen.
- **Loading:** while content loads at start, show the logo on the wall instead of an empty background.
- **Launcher icon:** a new adaptive icon (spray "c." on the wall). *(Claude's call: it's the first thing people see.)*
- **Accessibility:** AA contrast (§3.1); layouts work at font scale 1.5 (screens scroll rather than clip); all icons have content descriptions.

## 7. Assets

Mehmet chose the **mixed approach**:

| Asset | Source | Format |
|---|---|---|
| UI icons (back, close, drag, remove, add, game icons) | **Phosphor Icons** (MIT) — one consistent weight (Bold) | converted to Android vector drawables |
| Wall grain, faint brick/joint texture, paper texture, tape | **CC0 texture libraries** (ambientCG / Poly Haven) or generated, cropped and made tileable | WebP, each ≤ 200 KB, total textures ≤ 1 MB |
| Spray splatters / drips around the logo | CC0 or drawn as vectors | vector drawables |
| Game illustrations (Truth or Dare cards, bottle, cups+ball), launcher icon artwork | **Mehmet gets them made** (designer or AI image tool); until they arrive, **Claude draws vector placeholders** in the same style | final: transparent WebP/PNG, see brief |

**Brief for the illustrations Mehmet commissions** (Claude writes the full brief as a separate file in this step):
- 3 game illustrations + 1 icon artwork; graffiti/sticker style on transparent background, palette §3.1, no text inside, no alcohol (bottle without label, cups empty), consistent line weight across all four.
- Delivery: 1024×1024 px transparent PNG (Claude converts/resizes), plus the source file if any.
- License: must allow commercial use in an app store product; for AI tools, Mehmet confirms the tool's terms.

All third-party assets get their license/attribution recorded in `third_party/` (as with the fonts).

## 8. Constraints

- Compose Multiplatform code only (shared), no Android-only UI APIs; assets via Compose resources.
- App size: added assets (fonts + textures + illustrations) ≤ 4 MB *(Claude's call)*.
- Plan 1/2 behavior and tests must not change; this is a restyle plus the §6 UX items.
- CLAUDE.md rules apply (one step per commit, Mehmet commits).

## 9. Approaches considered

- **Re-skin only** (swap colors/fonts on today's layouts): fastest, but keeps the plain layouts and doesn't reach "top level" UX. Rejected.
- **Full restyle with a small design system** (tokens + a handful of components — top bar, poster, neon button, tape label, wall chip, badge — used by every screen): chosen *(Claude's call)*. One place to change the look later, consistent screens, and an easy hand-off if Mehmet later brings in a designer.
- **Third-party UI kit / design tool first:** possible later; not needed to reach this look.

## 10. Left to the builder

- Exact component APIs and file layout within `ui/theme` and `ui/components`.
- How the torn poster edge is drawn (shape path vs. 9-patch-like image), as long as it is crisp and static.
- Exact texture opacities and glow positions, within §3 and §5.
- Exact animation curves within §3.4.
- Placeholder illustration details, until Mehmet's art arrives.
