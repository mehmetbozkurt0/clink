# Clink — Party Games App: Design

- **Date:** 2026-11-19
- **Status:** Draft, awaiting Mehmet's approval
- **Product name:** Clink (Mehmet; considered Tipsy and Say Less. Tipsy dropped because it points straight at drinking)
- **Android application ID:** `com.mehmetbozkurt.clink` (permanent after first release)
- **Domain:** `clink-game.com` (Mehmet is buying it; used for invite links and the privacy policy)

Marking used in this document: anything **Mehmet decided** is stated plainly. Anything marked *(Claude's call)* was proposed by Claude and is open to change.

---

## 1. Intent and why

A mobile app that bundles pub and party games, such as Truth or Dare, Spin the Bottle and Cup Pong, played either **on one phone with friends at the same table** or **online with friends** in a shared lobby.

Mehmet is building it for two reasons:

1. To get real, end-to-end experience shipping an app to the stores.
2. To put a product on the open market that can make money later.

So "it works on my phone" is not enough. It has to pass store review, feel good enough that people come back, and be built so that monetization and more games can be added later without a rewrite.

## 2. Goals, non-goals, anti-goals

**Goals (v1)**
- Three games: **Truth or Dare**, **Spin the Bottle**, **Cup Pong**.
- Two modes: **With friends (local, one phone)** and **Online (friends via invite link)**.
- Shared content system with three intensity levels (Casual / Hard / Spicy) and a shared **penalty pool**.
- Turkish and English from day one.
- Published on **Google Play**. iOS comes in a later release (see §10.2).
- A distinctive, attractive and easy-on-the-eyes look (Section 9).

**Non-goals for v1 (explicitly later)**
- iOS release (no Mac available yet). The code must stay iOS-ready (§10.2).
- Playing with strangers / matchmaking.
- Voice chat.
- In-app purchases and ads (v1 is completely free).
- Games beyond the three above (Mehmet will decide which and when).
- User-written custom question packs.

**Anti-goals (would make it a failure even if it works)**
- **Encouraging alcohol consumption.** No "drink" mechanics anywhere. The loser gets a **penalty** ("cezanı çek"), never a drink. Store policies (Apple in particular) reject apps that encourage excessive drinking.
- Explicit sexual content. Spicy is flirty and suggestive, never explicit.
- A sign-up wall in front of someone who just tapped an invite link.
- An architecture that has to be rewritten to add strangers, voice chat, monetization or new games.

## 3. Core user flow

```
[Welcome] → [Play]
   → [Game list: Truth or Dare · Spin the Bottle · Cup Pong]
      → [Mode: With friends | Online]      (only modes the game supports; all three support both in v1)
         → With friends: [Sign in if needed] → [Enter player names] → [Pick level] → [Game]
         → Online:       [Sign in if needed] → [Create lobby, pick level] → [Share invite link / code]
                         → [Friends join] → [Host starts] → [Game]
   → [Game over] → [Play again | Back to game list]
```

- **Game first, then mode** *(Claude's call)*. Not every future game will support both modes, so picking the game first avoids greyed-out games.
- Invited players skip the game list. The invite link opens the app straight into the lobby.

## 4. Accounts and identity

- **The host must have an account, in both modes** (local and online). Sign-in in v1 (Android): **Google** *(Claude's call)*. **Sign in with Apple** is added with the iOS release (it is required on iOS when Google sign-in is offered).
- **Joiners do not need an account.** An invited player picks a nickname and an avatar, and is signed in anonymously behind the scenes. They **may** create an account (upgrade the anonymous user by linking Google; Apple too once iOS ships) if they want.
- In local mode, the other players are only names typed on the host's phone. They are not users.
- **Every participant has a stable user ID from the start**, including anonymous guests. Reporting, blocking, matchmaking and purchases will be built on this later.
- **Profile:** display name and avatar chosen from a preset set *(Claude's call: no photo upload in v1)*.
- **In-app account deletion** is required (Google Play rule, also an App Store rule). Play also requires a **web page** where users can request deletion; it lives on `clink-game.com`. Deleting removes the profile and the user's data.

## 5. Modes

### 5.1 With friends (local)
- One phone. It sits in the middle of the table or gets passed around.
- **2–12 players** *(Claude's call)*. Names are entered at the start and the order can be changed.
- **Works offline** once content has been loaded at least once (a baseline content set ships inside the app, see 6.3). Signing in the host needs a connection the first time only.
- The app never checks whether a penalty or dare was actually done; it's up to the people at the table.

### 5.2 Online (friends only)
- The host creates a **lobby**, picks the game and the level, and shares an **invite link**. A **6-character room code** is a fallback for when links don't open the app.
- Joiners tap the link → the app opens (or the store page, if not installed) → nickname → lobby.
- **2–12 players per lobby** *(Claude's call)*. Cup Pong is 1v1: the host picks the two players and everyone else **watches** and can chat.
- **Text chat** is available from the lobby through the whole game, as a panel that slides over the game screen. Voice chat comes later and must slot into the same place.
- **The host's phone is the referee** (see Section 10). If the host disconnects, the others see a "host reconnecting" state; if the host doesn't return within **60 seconds** *(Claude's call)*, the lobby closes.
- If a non-host player disconnects, they are skipped in turn order and can rejoin the same lobby with the same link.
- The host can remove a player from the lobby.
- **The lobby outlives a single game** *(Claude's call)*. After a game ends, everyone stays in the lobby (members, chat, presence); the host can start the same game again or switch game or level without new invites. In the data model, a **lobby** contains a sequence of **game sessions**. Matchmaking (later) will put players into this same lobby model.

## 6. Content system

### 6.1 Levels
| Level | Tone | Color (see §9) |
|---|---|---|
| **Casual** | Fine with anyone; fun, not embarrassing | Mint |
| **Hard** | More personal questions, tougher penalties | Amber |
| **Spicy** | Flirty and bold, adults only, suggestive not explicit | Coral |

- The level is chosen per game session (by the host) and applies to **everything** drawn in that session: Truth/Dare cards and penalties.
- Choosing **Spicy** shows an **"I am 18 or older"** confirmation every time a Spicy session is started *(Claude's call)*. For online, it's shown to the host when choosing it, and to each joiner when entering a Spicy lobby.

### 6.2 Penalty pool
- One shared pool of penalties, split by level, used by every game:
  - Truth or Dare: a player refuses a card.
  - Spin the Bottle: the answering player refuses the question.
  - Cup Pong: the loser.
- Penalties are things to do (e.g. "do 10 squats", "speak in an accent until your next turn"), never drinking.
- Online, the penalty card is shown to everyone; doing it is on the honor system.

### 6.3 Where content lives
- Content is **written by Mehmet** (no user-generated content in v1), in **Turkish and English**. How many cards each pack has at launch is his call. The engineering plan covers the content format, the import script and bundling, **not** writing the cards; it should ship with a small sample set so every flow can be tested before the real content exists.
- Content is organized into **packs** (pack = card type + level + a list of cards; card types: Truth, Dare, Penalty). Each pack has an `access` field (`free` in v1) so paid packs can be added later without changing the model.
- **Each card has one ID and its text in every supported language** *(Claude's call)*, rather than one pack per language. Each phone shows cards in **its own app language**. Online, the host publishes the **card ID**, never the text, so a Turkish and an English player in the same lobby each read the card in their own language.
- **Source of truth for content is JSON files in the repo**, pushed to Firestore by an **import script** (Firebase Admin SDK) *(Claude's call)*. Adding cards = edit JSON → run the script; no app update, no admin panel in v1. The same JSON is used to generate the bundled baseline set.
- Packs are stored in **Firestore**, downloaded by the app and **cached locally**. New cards can be added without an app update.
- A **baseline set of packs is bundled inside the app** *(Claude's call)* so the first local game works offline and on a fresh install.
- Cards shouldn't repeat within a session until the deck runs out (shuffle-deck behavior).

## 7. Games (v1)

### 7.1 Truth or Dare
- Turn order rotates through the players.
- On their turn, a player picks **Truth** or **Dare**; a card from the session's level is drawn.
- They do it → next turn. They refuse → they draw a **penalty** → next turn.
- **Local:** the card is read out loud; buttons "Done" / "Take penalty".
- **Online:** the card is shown to everyone. A Truth answer is typed in chat (or said on a call if they're on one). Dares are on the honor system; the player taps "Done" or "Take penalty".
- No winner. The session ends when the host ends it.

### 7.2 Spin the Bottle
- The current player **flicks the bottle** to spin it. Players are arranged in a **circle on screen** (not their real seats).
- The bottle stops on someone. Two roles: **asker** (spinner) and **answerer** (who the bottle points at).
- The asker asks the answerer **any question they like** (it does not come from our content).
- The answerer answers, or refuses and draws a **penalty** from the session's level.
- The answerer spins next *(Claude's call, classic rule)*. If the bottle lands on the spinner, it spins again.
- **Local:** the question is asked out loud; buttons "Answered" / "Take penalty".
- **Online:** the asker **types the question**, and it appears as a large **question card** on everyone's screen (not buried in chat). The answerer types the answer or taps "Take penalty". The flick only drives the animation; the **referee picks the target** and every phone animates the bottle to that target.
- No winner. The session ends when the host ends it.

### 7.3 Cup Pong
- **1v1, turn-based, one throw per turn** *(one throw is Claude's call)*.
- **6 cups per side**, triangle layout *(Claude's call; the classic 10 makes phone matches drag)*.
- The player **flicks the ball with a finger**; speed and direction of the flick set the throw. A ball that lands in a cup removes it.
- First player to lose all their cups **loses** and draws a **penalty** from the session's level.
- **Local:** the phone is passed between the two players; the view flips to the side whose turn it is.
- **Local with more than 2 players** *(Claude's call)*: before each match the two players are picked from the player list. On "Play again", **winner stays on** by default (the winner is pre-selected and the next player in the list is suggested as the challenger); the selection can be changed. Same rule online, where the host picks.
- **Online:** each player plays on their own phone. The thrower's flick (the input) is sent to the referee; the referee computes the result (which cup, if any) and publishes it together with the input; every phone replays the same throw and ends on the referee's result. No real-time physics sync is needed.
- Physics is a **simple, hand-written projectile model in shared Kotlin code**, not a physics engine *(Claude's call)*. It should feel satisfying (arc, bounce on the rim, splash), not be a simulation.
- Spectators in an online lobby see the match live and can chat.

## 8. Extensibility constraints (what v1 must not block)

These are not built in v1, but the design must leave room for them:

- **Playing with strangers:** joining a lobby is one "entry door" (invite link / code). Matchmaking will be a second door into the **same** lobby model. Game rules must not assume players know each other. Stable user IDs (§4) exist so reporting and blocking can be added. Chat will need reporting and a profanity filter at that point.
- **Server-side referee:** game rules are pure Kotlin functions (§10) so they can be moved to the server when strangers arrive.
- **Voice chat:** chat is its own feature module, not part of any game.
- **Monetization:** content packs have an `access` field; ad placements and a store screen can be added without changing game code.
- **New games:** each game is a self-contained module that plugs into the shared lobby, turn, level, penalty and chat systems.

## 9. Visual direction — "A · Midnight" (chosen by Mehmet)

**Mood:** modern, stylish, "we're out tonight". These games are mostly played in the evening, often in dim light, so the app is **dark, but not harsh**: a deep violet-navy instead of black, soft glowing color blobs in the background, warm coral as the main accent. Lively without being tiring.

**Theme:** **dark only in v1** *(Claude's call)*.

### 9.1 Color palette
| Token | Hex | Use |
|---|---|---|
| `background` | `#16142B` | App background (bottom of the gradient) |
| `backgroundTop` | `#1F1A3D` | Top of the background gradient |
| `surface` | `#26224A` | Cards, chips, panels |
| `surfaceRaised` | `#3A3466` | Secondary buttons, tags, pressed states |
| `textPrimary` | `#F3F0FF` | Main text (no pure white) |
| `textMuted` | `#A9A3C9` | Secondary text, hints |
| `primary` | `#FF7A6B` | Coral: main buttons, brand accent |
| `onPrimary` | `#1A1530` | Text on coral |
| `secondary` | `#7C5CFF` | Violet: background glow, highlights |
| `mint` | `#5EE2C4` | Casual level, positive actions ("Done") |
| `amber` | `#FFC65C` | Hard level, warnings, timers |

**Level colors:** Casual = mint, Hard = amber, Spicy = coral.
Rule: text and controls must meet WCAG AA contrast against the surfaces they sit on.

### 9.2 Typography
- **Headings, buttons, card text:** **Unbounded** (600 / 800). Wide, rounded and confident; gives the brand its character.
- **Body and UI text:** **Manrope** (500 / 700). Very readable at small sizes.
- Both are free (SIL Open Font License) and are **bundled in the app**, not loaded from the network.
- Logo: lowercase "clink" in Unbounded 800 with a coral period: **clink.**

### 9.3 Shape, depth, motion
- Rounded corners: ~14–16 dp for cards and buttons, pill shape for chips and level tags.
- Depth comes from **soft, blurred color blobs** (violet, coral) behind content, not from heavy shadows.
- Motion is playful but short (roughly 200–300 ms). The big moments get more: the bottle spin, a card flip when drawn, a cup splash in Cup Pong.
- **Haptics** on the big moments: bottle stops, ball lands in a cup, penalty drawn.

### 9.4 Reference mockups
Welcome screen and a Truth or Dare card in this style are in `.superpowers/brainstorm/` (`visual-style.html`, option A). They are a direction, not a pixel spec. Mehmet may hand the final UI to a third-party design tool if he doesn't like the result; the color and type tokens above should make that hand-off easy.

## 10. Architecture

### 10.1 Approaches considered
| | Approach | Trade-off |
|---|---|---|
| **1 (chosen)** | **Host's phone is the referee.** Rules are pure Kotlin in shared code. Online, the host's app runs the rules and writes the game state; other players only send their actions. | One rules implementation shared by local, online and tests; no server code for gameplay. If the host's app closes the game pauses/ends; a host could in theory cheat (fine among friends). |
| 2 | **Server is the referee** (Cloud Functions). | Cheat-proof, independent of the host's connection, needed once strangers play. But rules are written a second time in TypeScript, there's a separate deploy, and every move has extra latency. |
| — | Every phone runs the rules itself (peer-to-peer) | Dropped: phones can disagree and there's no single source of truth. |

**Chosen: 1.** Rules are written as pure functions with no platform or Firebase dependencies, so they can move to the server when the app opens up to strangers.

**Presentation architecture: MVI** (Mehmet considered MVI and MVVM; MVI chosen). The games are state machines (whose turn, which phase, who is taking a penalty). In MVI, "a player did X → new state" maps directly to "a move arrived from another phone", so the **same reducer** runs in local mode, in online mode on the host, and in tests.

### 10.2 Stack
- **Kotlin Multiplatform.** v1 ships on **Android only**: there is no Mac, and iOS builds, signing and store upload need macOS. iOS is added later via a Mac or a cloud macOS build service (e.g. Codemagic, GitHub Actions). To keep that cheap:
  - All game rules, state, data access and UI live in **shared code** (`commonMain`); Android-specific code is limited to the app entry point and platform glue (Google sign-in, App Links, haptics).
  - Only libraries that support iOS are used in shared code.
  - The iOS target is not built in v1 *(Claude's call)*; it can't be compiled on Windows.
- **Compose Multiplatform** for all UI.
- **MVI** for screens and game logic (see above).
- **Firebase on the Blaze plan** (pay as you go, same free quotas as Spark) with a **budget alert**. Blaze removes the 100-simultaneous-connection limit on Realtime Database and enables Cloud Functions and Storage.
- **GitLive `firebase-kotlin-sdk`** to use Firebase from shared Kotlin code *(Claude's call)*.

### 10.3 Firebase services
| Service | Used for |
|---|---|
| **Authentication** | Google and Anonymous (guests) in v1; linking anonymous → Google. Apple added with iOS |
| **Realtime Database** | Lobbies, live game state, player actions, chat, presence (detecting disconnects) |
| **Firestore** | User profiles, content packs and cards |
| **Hosting** | Serves `clink-game.com`: invite links (`https://clink-game.com/join/<code>`, opened via Android App Links; iOS Universal Links later), a fallback page that shows the room code and the store link, the privacy policy and the account-deletion request page. (Firebase Dynamic Links was shut down in 2025, so we don't use it.) |
| **Cloud Functions** | Only side jobs: cleaning up abandoned lobbies and old chat, deleting a user's data on account deletion. **No gameplay logic on the server in v1.** |
| **Crashlytics / Analytics** | Crash reports and basic usage events *(Claude's call; useful for the "ship and learn" goal)* |

### 10.4 Online game data flow (host-authoritative)
```
Player phone ──(action: "picked Truth", "flick vector", "typed question")──▶ RTDB /lobbies/{id}/actions
Host phone   ──reads actions──▶ runs the pure reducer ──writes──▶ RTDB /lobbies/{id}/state
All phones   ──observe state──▶ render (animations replay from the published state)
```
- Only the host may write `state`; any lobby member may append to `actions`. This is enforced with **Realtime Database security rules**.
- Randomness (card draws, bottle target) is decided by the host and published in the state, so every phone shows the same result.

### 10.5 Store and policy constraints
- No alcohol-consumption mechanics or imagery that encourages drinking (Apple's guideline on excessive alcohol; Google Play's content rating also flags alcohol themes).
- Age rating will be higher because of Spicy content; Spicy is suggestive, never explicit, and behind an 18+ confirmation.
- In-app account deletion plus a web deletion-request page (Play). Sign in with Apple when iOS ships.
- A privacy policy page on `clink-game.com`, and the Play Console **Data safety** form filled in to match what the app collects.

## 11. Left to the builder
- Module/package layout, DI library, navigation library, local cache/database library.
- Exact RTDB/Firestore data schema and security rules, within §10.4.
- Exact physics constants and feel of the bottle spin and Cup Pong throw.
- Minimum Android version.
- How the baseline content is bundled (format and size).
- Exact animations and screen layouts, within §9.

## 12. Repository housekeeping
- Visual companion files live in `.superpowers/`, which is git-ignored. The mockups in §9.4 are therefore local only; the tokens in §9 are the source of truth.
