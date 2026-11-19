# Clink: Implementation Roadmap

**Spec:** `docs/superpowers/specs/2026-11-19-party-games-app-design.md`

The spec covers several independent subsystems, so it is built as five plans. Each plan ends with software that runs and can be tested on its own. Each plan is written in full only when the previous one is done, so it can build on what actually exists.

| # | Plan | Ends with | Spec sections |
|---|---|---|---|
| 1 | **Foundation + local Truth or Dare and Spin the Bottle** | An Android app that runs offline: Midnight theme, welcome → game → mode → players → level flow, Truth or Dare and Spin the Bottle playable on one phone, bundled sample content in TR/EN | §3, §5.1, §6 (bundled), §7.1–7.2 local, §9, §10.1 (MVI, pure reducers) |
| 2 | **Beer Pong (local)** | Beer Pong playable 1v1 on one phone: flick physics, 6 cups, winner-stays-on | §7.3 local |
| 3 | **Firebase: accounts and content** | Google sign-in (required for the host), anonymous guests, profiles and avatars, account deletion, content packs from Firestore with local cache, content import script | §4, §6.3 remote, §10.2–10.3 (Auth, Firestore) |
| 4 | **Online play** | Lobbies on Realtime Database, host-as-referee, invite links on `clink-game.com` + room codes, chat, presence and reconnects, all three games online, spectators | §5.2, §7 online, §8, §10.4 |
| 5 | **Release** | Cloud Functions cleanup jobs, Crashlytics/Analytics, privacy policy and deletion pages, Play Console listing, Data safety form, signed release build on Google Play | §10.3 (Functions, Crashlytics), §10.5 |

Notes:
- Until Plan 3, local mode works without signing in. The "host must have an account" rule (§4) is enforced from Plan 3 on.
- Mehmet writes the real card content (§6.3) whenever he wants; the app ships with a sample set until then.
