# Cup Pong: easier to score — short design

Follows `2026-11-24-cup-pong-3d-design.md` (the 3D throw) and `2026-11-19-party-games-app-design.md` §7.3.

## 1. Intent

Mehmet's friends tried Cup Pong and struggled badly. Measuring the real simulator across the whole input range shows why: **only 7 % of possible throws land in a cup** (93 of 1275 across speed 300–400 and aim ±12°), and the ones that do fall in **two separate stripes** — speed 344–352 and 368–374 score, everything below, between and above misses. A player flicking harder goes from scoring, to missing, to scoring again, so no amount of practice builds an instinct.

Three things are wrong:
- **Most of the flick does nothing.** Finger speed 300–3000 dp/s spreads over ball speed 300–400 cm/s, but only roughly 1488–2298 dp/s can reach a cup at all. Over half the strength a player can put into a flick is wasted.
- **The scoring band is fragmented** by the rows of cups: one speed suits the front cup, a higher one the back row, and the gap between them lands nowhere.
- **The aim window is tiny next to what is allowed.** Throws are accepted up to ±45°, but only about ±6° can ever score.

A fourth thing is why nobody noticed: the existing difficulty guard models a player who **computes the correct speed and angle for a chosen cup** and then wobbles (±2.5°, ±3.5 %). That player clears six cups in 35 throws, and the test passes. Real players are not wobbling around the right answer; they have no idea what the right answer is. ±3.5 % of ~355 cm/s is ±12 cm/s, while the scoring stripes are about 8 cm/s wide.

## 2. Goals

- A comfortable party game: **about 40 % of reasonable throws score** (Mehmet's choice over keeping it a skill game).
- One continuous scoring band, so harder means further and a player can learn the feel.
- The gesture is visible while it happens.
- "Did it get easier?" is answered by a number, not a feeling.

## 3. Non-goals

- Changing the physics model, the table, the 3D view, the cups or the online path.
- Changing what the gesture is: it stays a flick, not a slingshot pull and not a drawn path.
- Making the game trivially winnable; 40 % is the target, not 90 %.

## 4. Anti-goals

- Aim correction strong enough that the throw stops feeling like the player's own.
- A preview that tells the player where the ball will land before they commit.
- Tuning by eye, with no measurement to show it worked or to catch it breaking later.

## 5. Constraints

- The rules stay pure and deterministic: online, the host simulates and every phone replays the same flight.
- Nothing is released, so changing the speed range needs no compatibility work.
- Dark theme only; the trail is drawn on the existing 3D canvas.

## 6. Approaches considered

- **A. Change the numbers and the tolerance (chosen).** Map the flick onto the band that can actually score, and make the cup's mouth forgiving enough to merge the stripes. The mechanic and the physics stay; the input mapping stops wasting most of its range.
- **B. Aim assist** — snap the aim to the nearest cup within a few degrees. Effective and common, but it weakens the sense that the throw was yours. **Kept as a fallback inside this work**, used only if A alone cannot reach 40 %.
- **C. A faint trail behind the finger (chosen, added by Mehmet).** Cosmetic: it shows the gesture, it does not predict the throw.
- **Dropped — a slingshot pull with a live arc.** The most controllable of all, and it was considered because a *stable* preview is impossible with a flick, but it replaces the mechanic the 3D design chose.
- **Dropped — more throws per turn, or a shorter table.** The first does not make a throw easier, only the match longer; the second changes how the table looks to fix something that lives in the input mapping.

## 7. The design

1. **Map the flick onto the useful band.** Finger speed maps to the ball-speed range that can reach the cups, instead of to 300–400 where both ends are dead. The exact band comes from the measurement, not from guessing.
2. **Widen the cup's forgiveness** so the two stripes become one: a ball arriving over the mouth drops in from a little further out, and the dead gap between the rows closes.
3. **Rewrite the difficulty guard** to model a player who does not know the answer: aim and strength errors expressed as a share of what a finger can control (a fraction of the flick range), not as a small percentage of the correct value. Then tune 1 and 2 until that player scores about 40 % of the time, with the grid measurement kept as a test so a future change cannot quietly break it.
4. **A faint trail behind the finger** while dragging on the throwing half of the table: a short, fading streak along the path the finger took, gone as soon as the ball is thrown. It never shows where the ball will go.
5. **Aim assist only if needed** — a few degrees, added last, and only if steps 1–3 fall short of the target.

**Order matters.** Tune the numbers first, then add the trail, then measure again: the trail makes the gesture legible and may itself shift how often people score, and the two together must not overshoot into a game nobody can lose.

## 8. Left to the builder

The exact constants; how long the trail is and how fast it fades; whether the forgiveness is widened at the rim, at the mouth, or both; and the shape of the error model in the rewritten guard, beyond it representing a player who is guessing.
