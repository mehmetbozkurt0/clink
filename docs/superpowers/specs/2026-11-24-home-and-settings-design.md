# Home Screen and Settings — Design

Date: 2026-11-24. Status: draft for Mehmet's approval (builder check done: nothing important left).

## 1. Why

Mehmet, after playing with a friend: the account and settings bits feel scattered ("dağınık"), and the home screen feels
empty ("çok boş"). The home screen is the user's first impression, so it should feel alive and show what to do, without
going back to a game list on the home screen (removed on purpose in Plan 4b) or a personal greeting.

Today:
- Home ([WelcomeScreen.kt](../../../shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/WelcomeScreen.kt)):
  the "clink." logo with drips and the tape tagline centred in empty space, PLAY and "Kodla katıl" at the bottom, a quiet
  "Nasıl oynanır?" link, and (only once the host has a profile) their avatar top-right, which opens the account screen.
- Account ([AccountScreen.kt](../../../shared/src/commonMain/kotlin/com/mehmetbozkurt/clink/ui/screens/AccountScreen.kt)):
  profile poster, edit, sign out, delete. Hard to find (an unlabelled avatar).
- A guest who joined with a code has no screen at all; their name and avatar live in `LocalPrefs` (`guestName`,
  `guestAvatar`) and can only be changed on the Join screen.
- No settings: the language follows the phone (Turkish or English), haptics can't be turned off, no app version shown.

## 2. Goals, non-goals, anti-goals

Goals
- A home screen that is lively at first sight and makes the one thing to do obvious: press OYNA (or join with a code).
- One tidy settings screen, reached from one visible button on the home screen, holding everything account- and
  preference-related.

Non-goals
- Light/dark theme: agreed as its own next round. The app stays dark only (spec 2026-11-19 §9). No theme row in settings
  yet; the settings layout just leaves room for it.
- New games, play-count tracking, a privacy-policy page, store listing.

Anti-goals (would make this a failure even if it works)
- Overdone motion (Mehmet rejected the busy "live scene" sketch B). Motion stays slow and in the background.
- A game list or game shortcuts on the home screen (Mehmet: the posters are decoration, "görsel olsun").
- A personal greeting ("Selam Mehmet") on the home screen.

## 3. Approaches considered

Home screen — six sketches shown, then refined with Mehmet:
- A poster wall (games as taped posters) — liked, but the posters must not be links, and the wall must not grow with
  the number of games.
- B live scene (spinning bottle, flying ball, 3 steps) — rejected: too much animation.
- C personal greeting with game rows — rejected: greeting and game entries on home were unwanted.
- D two "doors" (set up a table / got an invite?), E sticker wall, F card of the night — not picked.
- For A at 8 games: A1 sideways wall, A2 three featured posters + "all games", A3 vertical collage. **A2 picked**, with
  the "+N games" link moved (M1 round sticker, M2 paper stack, **M3 spray-painted line**, M4 quiet bottom row).
- Wall background: W0 today, W1 breathing glows, W2 faint spray tags, **W3 = W1 + W2 + a slow light sweep — picked.**

Featured games — chosen by Mehmet, kept as a constant list in code (my call). A remote setting was the alternative;
not worth it, since a new game only arrives with an app update anyway and the list can change in that same update.

Language switch — Android's own per-app language (my call): on Android 13+ through the system (the choice also shows in
the phone's app-language settings); on Android 8–12 the app stores the choice and applies it when the activity starts.
Alternatives: switching the whole app to AppCompat (theme and activity change just for this), or an in-app override of
Compose's locale only (would leave Android-side text and `Locale.current` inconsistent).

## 4. Home screen (decided)

Top to bottom:
- Top bar row: a **settings gear** (⚙) top-right, for everyone. The avatar button is removed (the profile moves into
  settings).
- The "clink." logo with drips and the tape tagline "Masadaki herkes için." (as today, a bit smaller to make room).
- A spray-painted line in acid green, Caveat Brush, slightly rotated, to the right above the posters, showing the game
  count: "tüm oyunlar: 3" / "all games: 3". **Not a link, no arrow** — it only says how many games there are. Count =
  number of games in the app (`GameId.entries.size`).
- **Three featured game posters**, tilted and overlapping like taped paper (sketch A): two on top, one below in the
  middle. Each shows the game's name and player count, on a paper / acid / pink background with a tape strip, and
  sways very slowly (about ±1° over ~4 s, each out of phase). **Not tappable** — purely visual, and not announced as
  buttons to screen readers (one content description for the group is enough, e.g. the game names).
- Featured list = a constant in code, today Truth or Dare, Spin the Bottle, Beer Pong (Mehmet changes it when games are
  added). Always exactly three posters.
- OYNA (primary) and "Kodla katıl" (secondary) at the bottom, unchanged in behaviour: OYNA goes through the existing
  gate (tour → sign-in → profile → game list); "Kodla katıl" opens Join.
- The poster shows only the name and player count: new short player-count strings, not the game list's existing
  meta line (which also carries a one-line rule).
- The quiet "Nasıl oynanır?" link leaves the home screen; it lives in settings (my call: the sketch Mehmet picked has
  no link there, and the Play tour still shows on the first OYNA).

The wall behind the home screen (W3), **home screen only** — every other screen keeps today's calm wall (my call,
not objected to: game screens need focus):
- Faint spray tags on the wall (about 9–10 % opacity): "wow", "gg", "cheers", a crown, a star, placed in the gaps so
  they never sit behind text. They are wall art, the same in both languages (not translated).
- The two existing corner glows (pink top-right, acid bottom-left) slowly drift and breathe: 9 s and 11 s cycles, move
  ~10 % of the screen width, scale 1→1.15–1.2, opacity 0.7–1.
- Every ~12 s a soft, very faint light band (paper colour, ~6 % at its brightest) sweeps diagonally across the wall,
  like a passing car's headlight; the sweep itself takes ~3–4 s, the rest of the cycle is still.
- All of it stays behind the content and under the grain.
- If the phone has animations turned off (system "remove animations"), everything here is still (my call).

Reference mockups (local, git-ignored): `.superpowers/brainstorm/101-1795523445/content/home-a2-more.html` (M3) and
`home-wall.html` (W3).

## 5. Settings screen (decided)

Reached from the home gear. Replaces the Account screen (its route goes away; profile edit and delete flows are reused).
Title "Ayarlar" / "Settings", back arrow. Scrolls if needed. Sections:

```
AYARLAR                                  ←
┌───────────────────────────────────────┐
│ (avatar)  Mehmet                  ✎   │   profile card (poster style)
│           Google hesabıyla giriş      │
└───────────────────────────────────────┘
OYUN
  Dil                     Sistem / Türkçe / English
  Titreşim                [açık]
YARDIM
  Nasıl oynanır?
  Kodla katılma nasıl?
HESAP                                       (Google accounts only)
  Çıkış yap
  Hesabı sil              (quiet, last)
                      Clink · sürüm 0.1.0
```

Profile card, three cases:
- Signed in with Google and has a profile: avatar + display name, subtitle "Google hesabıyla giriş"; ✎ opens the
  existing profile editor (`ProfileSetupRoute(editing = true)`).
- Not signed in, or an anonymous guest: the guest identity from `LocalPrefs` (name, avatar); subtitle "Misafir · bu
  telefonda"; ✎ opens the same name + avatar editor, which saves to `guestName` / `guestAvatar` (so the next Join starts
  with it). With no guest name yet: avatar placeholder and "Misafir", ✎ still works.
- Signed in with Google but no profile in hand (still loading, couldn't be read offline, or setup never finished —
  `AccountSession.profile` is null in all three): show the Google name with the default avatar and the Google
  subtitle; ✎ goes through the same path as OYNA (`AccountSession.gate()` loads again; only if there is still no
  profile does it open setup). So settings adds no new way to overwrite a real profile beyond what OYNA already does
  (my call after the builder check; a three-state load with a retry card was the bigger alternative).

Rows:
- **Dil / Language**: System (follow the phone), Türkçe, English. Changing it applies at once (the screen reloads in
  the new language). Cards and game content follow the same language (today they use `Locale.current.language`).
- **Titreşim / Haptics**: on by default. Off silences every haptic in the app (Beer Pong, Spin the Bottle, Truth or
  Dare, online games, player setup). Stored in `LocalPrefs`.
- **Nasıl oynanır?**: replays the Play tour (`PlayTourRoute(replay = true)`), as the old home link did.
- **Kodla katılma nasıl?**: replays the Join tour on its own, returning to settings when done, without opening the Join
  screen's code entry (my call). Like the Play tour replay, it marks the tour seen.
- **Çıkış yap / Hesabı sil**: as on today's Account screen, same confirmation and re-authentication; only shown for a
  Google (non-anonymous) account. After either, back to home.
- Footer: "Clink · sürüm <versionName>" from the Android build.

All new text in English and Turkish string resources, like the rest of the app.

## 6. Constraints

- Kotlin Multiplatform, Compose Multiplatform 1.12, existing theme and components (Poster, TapeLabel, TopBar,
  PrimaryButton/SecondaryButton, AvatarBadge, PosterDialog), `ClinkColors` dark palette, fonts Rubik Spray Paint,
  Caveat Brush, Anton, Archivo.
- Android minSdk 26; `MainActivity` is a `ComponentActivity` (no AppCompat).
- Local preferences live in `LocalPrefs` (JSON file, `ignoreUnknownKeys`): new fields need defaults so older files
  still load.
- Tests: reducers and pure logic unit-tested in `commonTest`, as in earlier plans.
- Mehmet commits; for this round one commit at the end (same waiver as Plan 5b).

## 7. Left to the builder

- Exact poster sizes, angles and spacing, and how the posters and tags adapt to short screens (they must not overlap
  the buttons; on very short screens the posters may shrink).
- How the per-app language is wired on Android 8–12 and how the choice is read early enough at activity start.
- How haptics are routed through one place so the toggle covers every call.
- Whether the Join-tour replay is its own route or a mode of the existing tour composable.
- Settings row components and the language picker's form (dialog or inline options), in the app's existing style.
