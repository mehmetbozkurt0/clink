# Clink: UX polish (home screen, onboarding, rough edges)

**Date:** 2026-11-23 · **Builds on:** `2026-11-19-party-games-app-design.md` (product spec), `2026-11-20-graffiti-visual-refresh-design.md` (look), Plans 1–4 (all shipped).

## 1. Intent

Clink works end to end (local and online), but to Mehmet it "doesn't look like an app ready to use". Before the release
plan, the app gets a UX pass so a newcomer understands it and gets into a game quickly.

**Success, in Mehmet's words:** someone who opens Clink for the first time understands the basic idea and can **join a
lobby within 30 seconds**.

The typical newcomer is a friend who was sent an invite ("download this, join my lobby"). The second newcomer is
someone who opens the app to host a game.

## 2. Goals

1. **Home screen (Welcome) cleaned up.** The three game names stacked on it go away. Two clear ways in: **Play** (host)
   and **Join with a code** (guest).
2. **Onboarding where it's needed, never on the home screen** (Mehmet's call):
   - **Join onboarding**: the first time someone opens *Join with a code* (by the button or by an invite link), 3 short
     cards explain how online play works, then the code screen.
   - **Play onboarding**: the first time someone taps *Play*, 4 short cards explain the app (games, modes, levels, why the
     host signs in), then the existing sign-in / profile / game list flow.
3. **Visuals are the app's own screens, never drawings** (Mehmet's call): each card shows a small live preview of a
   real screen in a phone frame.
4. **Rough edges fixed** (§6).

## 3. Non-goals and anti-goals

- **Non-goals:** release work (Plan 5), the online optimizations saved in memory (offline-skip delay and the review
  minors), real card content, a settings screen, sound, new games.
- **Deferred to the release plan:** carrying the invite code through a Play Store install (Google Play Install
  Referrer). It only works once the app is on Play; until then a first-time guest from an APK types the code.
- **Anti-goals (a failure even if it technically works):**
  - onboarding on the home screen, or onboarding that can't be skipped;
  - a guest needing more than 3 cards before they can type a code;
  - illustrations or drawings (Mehmet: "Çizim olmasın");
  - onboarding showing again after it was seen or skipped once (except from "How to play").

## 4. Approaches considered

**Where onboarding sits**
1. Invite-link guests skip onboarding, a "How to play" card waits in the lobby — fastest, but guests never learn the idea up front.
2. One short tour for everyone before anything else — one door, but it blocks a guest whose friends are already waiting.
3. Guests skip, hosts get a tour — Claude's recommendation.
4. **Chosen (Mehmet): contextual.** Guests get 3 online cards right before the code screen; hosts get the general tour
   when they first tap Play. Nothing on the home screen. Each person sees only what their next step needs.

**What the visuals are**
1. Graffiti vector illustrations drawn by Claude — on-brand, but simple shapes.
2. **Chosen (Mehmet): the app's own screens.** Claude's call on the how: **live previews built from the real
   composables** with sample data, scaled into a phone frame, not PNG screenshots — sharp at any density, in the
   phone's language, and never out of date when a screen changes.
3. Artwork supplied by Mehmet — best quality, depends on him.

## 5. Design

### 5.1 Home screen (Welcome)

```
                              (avatar, if signed in)

     clink.          ← spray logo + drips, vertically centred in the free space
     ' | ' |
     [ Masadaki herkes için. ]   ← tape tagline

     [         OYNA          ]   ← acid, primary: host a game
     [      KODLA KATIL      ]   ← pink, same size: join a friend's lobby
          Nasıl oynanır?         ← small text link → Play onboarding (replay)
```

- The three game stickers are removed. The games are introduced by the Play onboarding and the game list instead.
- Both buttons stay full width and equal size (join is as important as play — success criterion).
- "How to play" (`Nasıl oynanır?`) replays the Play onboarding at any time and then returns to Welcome (it does not
  start the sign-in flow).

### 5.2 Onboarding pager (shared component)

One component used by both flows:

```
                                     GEÇ   ← skip, top right, always visible
   ┌───────────────────────┐
   │  ┌─────────────────┐  │
   │  │  live preview   │  │   ← phone frame with a real screen part, not interactive
   │  │  (scaled)       │  │
   │  └─────────────────┘  │
   └───────────────────────┘
   HERKES KENDİ                ← title, poster style (Anton, last word acid like ScreenTitle)
   TELEFONUNDA
   Kurucu lobiyi açar, sen      ← one or two lines of body text
   kodla katılırsın.

           ● ○ ○               ← page dots
   [        İLERİ        ]      ← last page: BAŞLA
```

- Swipe or the button moves on. **System back** goes to the previous card; on the first card it leaves the pager
  back to where the user came from (Welcome, or wherever they were when an invite link arrived) and does **not**
  continue into the flow. Leaving this way still marks the onboarding as seen *(Claude's call: back must never drop
  someone onto Google sign-in, and the tour shouldn't keep reappearing)*. In a replay, back simply returns.
- **Skip and finishing both mark that onboarding as seen**; it is not shown automatically again. Seen flags are stored on
  the phone (survive restarts; a reinstall shows it again).
- Previews use the real composables with sample data, taps disabled, scaled to fit. Sample names come from string
  resources (TR: Ayşe, Mert; EN: Alex, Sam), so each phone shows its own language. Sample card text comes from string
  resources too, not from the content library.
- Same Street/Graffiti look as the rest (graffiti spec §3).

### 5.3 Join onboarding (3 cards)

Shown the first time the Join screen opens, before the code step. If an invite link opened it, the code is kept; once
the cards are done or skipped the lobby is looked up straight away, as today, so the guest lands on the name/avatar step
without retyping or confirming the code *(Claude's call: one screen and one tap fewer toward the 30 s goal; a returning
guest with a link behaves the same, just without the cards)*.

| # | Title (TR / EN) | Body | Preview |
|---|---|---|---|
| 1 | Herkes kendi telefonunda / Everyone on their own phone | The host opens a lobby, you join it with a code. | Lobby player list: host + you, online dots |
| 2 | Kod kurucudan gelir / The code comes from the host | Type the 6 characters or tap the invite link. No account needed. | The code card (`X 2 K W 2 T`, Invite button) |
| 3 | Sıra sende olunca oynarsın / Play when it's your turn | Your buttons show up on your turn; the others see who they're waiting for. | Two mini phones side by side: Truth/Dare panels · "Ayşe bekleniyor…" |

The Join screen also gets a small **"Online nasıl işler?"** link that replays these 3 cards.

### 5.4 Play onboarding (4 cards)

Shown the first time Play is tapped, before the account gate (sign-in, profile, game list). If the user is already
signed in with a profile, the 4th card still shows; its button just continues to the game list.

| # | Title (TR / EN) | Body | Preview |
|---|---|---|---|
| 1 | Üç oyun, tek uygulama / Three games, one app | Truth or Dare, Spin the Bottle and Cup Pong. | Game list posters |
| 2 | Tek telefon ya da online / One phone or online | Pass one phone around the table, or everyone joins from their own phone. | Mode posters |
| 3 | Seviyeni seç / Pick a level | Casual, Hard or Spicy (18+). | Level cards |
| 4 | Kuranın hesabı olur / The host has an account | You sign in with Google to host; your friends join without one. | Profile poster (avatar + name) |

The last button reads **BAŞLA** / **START** and continues into the existing gate (`goByGate`).

### 5.5 Rough edges (§6 list) — all in scope

| # | Where | Today | Change |
|---|---|---|---|
| 1 | Mode screen | "ONLİNE" (Turkish upper-casing dots the i) | Show "ONLINE"; the mode titles are written upper-case in the string resources instead of being upper-cased at runtime (or upper-case with a locale that doesn't dot the i) |
| 2 | Online lobby (host) | The game chip row runs off the right edge | Chips wrap onto two lines (flow layout) |
| 3 | Join screen | A guest types the name every time; everyone starts as the cat avatar | The last name and avatar used to join are remembered on the phone and filled in; a first-time guest gets a random avatar |
| 4 | Online lobby + game | No sign this phone itself lost the connection (only the host's countdown exists) | A small amber banner "Bağlantı yok, yeniden bağlanılıyor…" while this phone is offline (`LobbyRepository.connected()`) |
| 5 | Online lobby (host, alone) | Only "At least 2 players needed" | While the host is alone, a short hint above the code: "Arkadaşlarına kodu ya da daveti gönder" |
| 6 | Welcome | Game names look tappable but do nothing | Removed (§5.1) |
| 7 | Play → sign-in | Lands on sign-in with no context | Explained by Play onboarding card 4 (§5.4) |

## 6. Constraints

- Kotlin Multiplatform shared UI (Compose Multiplatform), MVI; existing components (`ScreenColumn`, `ScreenTitle`,
  `Poster`, `PrimaryButton`/`SecondaryButton`, `TopBar`, `WallChip`) and the graffiti palette and fonts.
- All new text in TR and EN string resources; no hard-coded copy.
- Local and online play must behave exactly as today apart from the listed changes; the existing 194 tests stay green.
- Accessibility as elsewhere: 48 dp touch targets, content descriptions on icon-only controls; previews are decorative
  for screen readers (the title and body carry the meaning).
- Git rule from CLAUDE.md: Claude never commits; Mehmet commits per step.

## 7. Left to the builder

- How the seen flags and the remembered guest name/avatar are stored (a small key-value file through `Platform`, like
  `ContentCache`, is fine).
- How previews are scaled and framed, and which existing private composables become `internal` to be reused.
- Card animations (a light slide/fade between pages is enough).
- Exact copy wording beyond the titles above, in the app's voice (short, plain, informal "sen").

## 8. Done when

- Fresh install, Join button: 3 cards (skippable) → code screen → name/avatar → lobby; with an invite link the code
  screen is skipped (code already known). Comfortably within 30 s for someone who doesn't stop to read every word.
- Fresh install, Play: 4 cards (skippable) → sign-in → profile → game list.
- Neither onboarding shows again after it was seen or skipped; both can be replayed from their links.
- Welcome shows no game list; the rough edges in §5.5 are fixed.
- Checked on Mehmet's S22 and the emulator, in Turkish and English.
