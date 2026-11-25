# Cup Pong 3D: real ball physics and a 3D table

**Date:** 2026-11-24 · **Builds on:** product spec `2026-11-19-party-games-app-design.md` §7.3, look `2026-11-20-graffiti-visual-refresh-design.md`, online play (Plan 4). **Replaces** the hand-written 2D throw model (`CupPongPhysics.simulateThrow`, `CupPongTable`).

## 1. Intent

Mehmet played Cup Pong with a friend and found the throws "didn't follow my hand" and felt "clunky". He wants it to
play like the swipe-to-throw cup pong games people know from phones: **real physics** (the ball flies, bounces on the
table, clips rims, can drop into a neighbouring cup) and **3D cups and ball**, in Clink's own look (theme and colours
are ours; no copied art or layout).

The feel was found by trying throwaway builds on his phone (2026-11-24). This design makes the last one permanent,
tested, and online.

**Success:** on his phone the throw follows the flick, the ball arcs and bounces believably, the cups and ball sit on
the table, a match is short enough for a party, and online every phone sees the same throw.

## 2. What the spikes taught (decisions already made with Mehmet)

| Tried | Verdict |
|---|---|
| Ball under the finger, carried anywhere before release | **Rejected:** "not realistic"; the ball stays at your end of the table |
| Faster pace (≈0.5 s flights, short pauses) | Kept: physics now sets the flight time (≈0.7 s) |
| Approach A: own 3D maths drawn with Compose Canvas (no engine) | **Chosen** ("A ile deneyelim" → kept) |
| Ball resting 8 cm above the table; cups without shadows | Rejected: "floating"; ball rests on the table, cups get contact shadows |
| Low camera | Rejected: "too flat"; camera raised to look down into the cups |
| Long table | Rejected: "too long"; ball's rest spot moved 40 cm closer, camera too |
| 3 cups | Rejected: "didn't look good"; back to **6 cups** |
| Hard (avg. player ≈ 47 throws to clear 6 cups) | Made easier: wider cups, forgiving rim, softer rim bounce (≈ 31) |

## 3. Goals, non-goals, anti-goals

**Goals**
1. Real 3D ball physics in shared Kotlin, deterministic, unit-tested.
2. A 3D-looking table, cups and ball drawn from 3D coordinates through a camera (Compose Canvas).
3. Flick → throw that follows the hand: direction from the finger's movement across the table, distance from flick speed.
4. Same gameplay rules as today: 1v1, one throw per turn, 6 cups each, hit removes the cup, first to lose all cups
   loses and draws a penalty; winner-stays-on and the local match picker unchanged.
5. Online: the host's simulation decides and **publishes the flight**; every phone replays exactly that flight.

**Non-goals:** two balls per turn, re-racks, "balls back", sounds, a physics engine or a 3D rendering engine,
throwing from a different spot on the table, spin/curve.

**Anti-goals (a failure even if it works):** the ball can be carried around the table; cups or ball look like they float;
phones in one online match see different throws; matches long enough to stall a party (an average player clearing 6
cups should stay around 30 throws or fewer).

## 4. Approaches (rendering)

1. **Chosen — A: own 3D maths, drawn with Compose Canvas.** World points are projected through a camera; cups are shaded
   tapered cylinders, the ball a lit sphere. Shared code, works for iOS later, theme stays in code.
2. B: Filament/SceneView 3D renderer with models — most realistic, Android-only, bigger app, models to source.
3. C: a full game engine — out of proportion for one screen.

## 5. Design

### 5.1 World (centimetres)
x to the thrower's right, y away from the thrower along the table, z up; the table's near edge is y = 0.

| Value | Spike-tuned number |
|---|---|
| Gravity | 980 cm/s² |
| Ball radius | 2 |
| Cup top / bottom radius, height | 5.0 / 3.5 / 12 |
| Table half-width, length | 30.5 / 262 |
| Ball rest spot (where every throw starts) | (0, 100, 2) — on the table |
| Cups (ids as today: back 0-1-2, middle 3-4, front 5) | (−10.4, 228), (0, 228), (10.4, 228); (−5.2, 219), (5.2, 219); (0, 210) |
| Launch pitch | 50° up |
| Launch speed range | 300 … 400 cm/s |

### 5.2 Physics (`simulateThrow3d`)
Fixed step 1/240 s, at most 4 s; samples kept at 60 per second for replay. Per step: gravity, then for each remaining
cup (ascending id):
- **Rim** (a thin torus, tube 0.3): if the ball's centre is already over the opening (horizontal distance < top radius
  − 0.6) it is not a rim hit (forgiving). Otherwise, touching the rim: the velocity part into the rim bounces back at
  0.35, the sliding part is kept at 0.9. Each rim contact is recorded (sample index, cup) for the wobble.
- **Inside** (below the rim, within the wall radius at that height): the inner wall pushes the ball back toward the
  middle (rattle, reflect at 1.5); once the centre is 4 cm below the rim the throw **hits that cup**; a few sinking
  samples finish the path.
- **Outer wall:** push out, reflect the horizontal velocity at 1.4.
Then the **table** (inside its bounds): bounce with horizontal ×0.88 and vertical ×0.6 (a vertical speed under 60 stops
hopping), rolling friction 1.2/s. The flight ends when the ball sinks in a cup, stops on the table (< 20 cm/s), falls
40 cm below the table, or passes 80 cm beyond its far end. A rim bounce can carry the ball into another cup — that cup
counts. Result: path, hit cup (or none), whether it touched a rim, rim contacts.

Determinism: same input and cups → same result on the same build. Online does not rely on phones re-simulating (§5.6).

### 5.3 Throw input (`ThrowInput`)
`ThrowInput(yawDegrees, speed)` — yaw 0 = straight down the table, speed in cm/s. (Replaces `angleDegrees`/`range`.)

Flick → throw: the drag must start on the lower 45 % of the table area; on release, the release point and the point the
finger would reach 0.06 s later (from the release velocity) are both cast onto the table plane through the camera; the
direction between them is the yaw. Not a throw: moving down, slower than 300 dp/s, or |yaw| > 45°. Speed: 300…3000 dp/s
maps linearly to 300…400 cm/s. The ball never follows the finger.

### 5.4 Camera and drawing (`CupPongTable`)
Camera at (0, 40, 85) looking at (0, 215, 0); focal length 2.0 × canvas width; horizon point at 30 % of the height.
Painter's order: a ball that has dropped below the table top is drawn **before** the table (hidden by it); then the
table (board in tape colour, darker toward the far end, ink border and centre line, a short far-edge face); the ball's
shadow on the board (fades with height); then cups and the ball sorted far → near.
- **Cup:** contact shadow on the board; body = outline around the projected rim and base circles, pink with a left-lit
  horizontal shading and an ink edge; the opening dark (empty — brand rule: no drinks); paper-coloured rim; splash ring
  on a hit; a cup the ball just clipped **wobbles** sideways (±0.9 cm, decaying over 18 samples).
- **Ball:** paper sphere with a highlight to the upper left and an ink edge; rests on the table with its shadow under it.
- Graffiti palette only (`ClinkColors`).

### 5.5 Match flow and timing
Unchanged rules and screens. A throw replays its path linearly over its own duration (path length ÷ 60 s), then the
outcome text (hit / rim / miss), splash 260 ms on a hit, a 400 ms pause, then the turn passes. Haptic on a hit, as today.

### 5.6 Online
The host runs `simulateThrow3d` (via the reducer) and publishes the result **including the flight path** (rounded to
0.1 cm) in `CupPongState.lastThrow`; every phone, the host included, replays the published path — no re-simulation, so
phones can't disagree. A throwing member still sends only `ThrowInput` (well under the 2 000-character action limit).
The camera is always the thrower's view, for everyone, as today. Old and new app versions can't share a Cup Pong match
(the input means something else) — both phones must be updated.

### 5.7 Removed
`CupPongPhysics.simulateThrow`, `flickToThrow`, the 2D `TableProjection` drawing, and their tests; the spike's test
door (`/join/spike-pong`).

### 5.8 Decisions settled after the builder check (Claude's calls)
- **Untrusted input online:** the host's reducer ignores a throw whose yaw or speed is NaN/infinite, and clamps yaw to
  ±45° and speed to 300…400 cm/s before simulating, so a modified client can't send a cannon shot.
- **Published replay data:** `lastThrow` carries the input, the hit cup, whether a rim was touched, the **rim contacts**
  (sample index, cup) — so every phone shows the same wobble — and the path as whole tenths of a centimetre in one flat
  integer list (x, y, z per sample). A 4 s path is at most ~240 samples ≈ 4 KB of JSON in `state`, which has no size
  rule (only member actions are capped at 2 000 characters).
- **Old and new versions together:** no version gate. Session decoding already fails soft (an undecodable state is
  ignored, no crash), so a mismatched phone just doesn't see the Cup Pong match; both phones must be updated.
- **Difficulty guard:** a deterministic simulation test with a fixed-seed "average player" (aim noise 2.5°, speed noise
  3.5 %, aiming at a random remaining cup with the ideal speed for its distance) asserts that clearing 6 cups takes at
  most 35 throws on average over 300 racks, so later physics changes can't quietly make matches drag.
- **Spike code:** the plan starts from the committed code — spike edits are reverted and the spike files removed
  (copies kept outside the repo for reference) — and rebuilds test-first from this design.
- **Replays:** driven by `throwCount` as today; a phone that opens mid-throw shows the current rack without replaying.

## 6. Constraints
Kotlin Multiplatform shared code, Compose Multiplatform Canvas, MVI pure reducer, kotlinx.serialization for published
state; no new dependencies. TR/EN strings unchanged unless a new text is needed. Existing tests outside Cup Pong stay
green. CLAUDE.md: Mehmet commits.

## 7. Left to the builder
Code structure inside the files; how path rounding is encoded; exact shading gradients within the palette; how a
recreated screen restores (showing the current rack without replaying the last flight is fine).

## 8. Done when
- Unit tests pin the physics (straight throw at the right speed lands in the front cup; too short / too long miss; a rim
  hit on the near side bounces; a ball over the opening drops in; a throw can end in a cup it wasn't aimed at; an
  off-table ball falls; deterministic), the flick mapping (direction, speed band, rejects), and the published path.
- On Mehmet's S22: throws follow the flick, cups and ball sit on the table, missed balls vanish behind the table, the
  pace feels quick; online with a second phone both see the same flight and result.
