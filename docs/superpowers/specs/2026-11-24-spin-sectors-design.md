# Spin the Bottle: player sectors and a truly random stop (short design)

**Date:** 2026-11-24 · **Approved by Mehmet in chat** ("Sıra ve tasarım uygun, şişeyle başla").

## Intent
Today the reducer picks a random *player* and the bottle animates to point exactly at that player's name, so it always
stops on a name. Mehmet wants it to feel like a real bottle: the circle is split into one sector per player, the bottle
stops at a truly random angle, and whoever's sector it points into is picked.

## Approaches
1. **Chosen: random stop angle, player = the sector it falls in.** The reducer draws the angle; the UI only animates to it.
   Keeps the reducer pure and the online host authoritative (the host draws the angle and publishes it).
2. Physics from the flick (speed and friction decide where it stops) — feels "real", but the result depends on the
   phone and the touch, and online every phone would have to agree on the physics. Rejected.
3. Keep picking a player, then add a random offset inside their sector — same look, but the player odds and the
   angle are two draws for one event. Rejected as needlessly indirect.

## Design
- **Sectors:** N equal sectors (N = players). Sector *i* is centred on today's name position
  (`playerAngleDegrees(i, N)`, player 0 at the top, clockwise) and spans ±180°/N around it. A bottle angle exactly on a
  border belongs to the next sector clockwise.
- **Draw:** the stop angle is uniform over the full circle with 0.1° resolution (`random.nextInt(3600) / 10f`, in
  `playerAngleDegrees` terms, normalised to [0, 360)). The picked player is the sector containing it. Landing on the
  spinner's own sector keeps today's rule: "spin again".
- **State:** `SpinState` gains `lastAngle: Float?` (null before the first spin); `lastTarget` stays (the picked player).
  Both are published online inside the session state, so every phone animates to the same angle.
- **Animation:** the bottle animates to `lastAngle`, not to the player's centre. Locally the flick speed still decides
  turns and duration (`flickToSpin`); online the fixed motion stays. Older states without `lastAngle` fall back to the
  player's centre.
- **Look:** the circle is drawn as N wedges in two alternating wall tones with thin paper-coloured dividers; each name
  sits at its sector's centre; the spinner's sector is tinted acid and the picked (answerer) sector pink, as the name
  chips are today. Compact names from 9 players on, as today.

## Non-goals
Beer Pong, new games, settings screens (later projects); changing the asking/answering rules.

## Done when
The bottle visibly stops at arbitrary angles inside sectors (not always on a name), the picked player is the sector it
points into, local and online, and all existing tests (updated for the new draw) pass.
