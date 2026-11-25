# Home posters: livelier text and art — short design

Follows `2026-11-24-home-and-settings-design.md` §4 (the three featured-game posters on the home screen).

## Intent
Mehmet: the poster text looks too plain and the posters look too empty ("Hem yazı basit hem de afiş çok boş"), but the result must not become loud or tiring to read ("göz alan göz yoran bi yazıya dönüştürme").

## Approaches (shown side by side as mockups)
- **A — gig poster, type only:** big title split over lines, one word highlighted, players in a rubber stamp. Bold, but three highlighted posters together can shout.
- **B — illustrated:** plain title, an ink line drawing of the game in the empty space, players handwritten in the marker font. Calmest.
- **C — mixed (chosen by Mehmet):** A's highlighted word and stamp together with B's drawing.

## Design (C)
Each poster, top to bottom, sizes scaled from the poster width as today:
1. **Title** — Anton, upper case, as today; **one word is highlighted**: a band in the poster's "other" color behind it, tilted about −2°.
   - Paper poster (Truth or Dare): pink band. Acid poster (Spin the Bottle): ink band with acid text. Pink poster (Cup Pong): acid band.
   - The word comes from a new per-language string (EN: Dare / Spin / Pong; TR: Cesaret / Şişe / Pong), found in the title ignoring case. If it isn't found, the title is drawn without a highlight.
2. **Drawing** — a new ink-only line drawing per game in the free space (two cards with ? and !, a bottle with spin arrows, three cups with a ball's arc). Line only, no fills, so it works on any poster color. New vector drawables, separate from the colored `art_*` placeholders used on the game list.
3. **Stamp** — the players text ("2–12 KİŞİ", "1'E 1") in Anton, upper case, inside a 2.5 dp ink outline, tilted about −5°, at the bottom. On the top-right poster it sits bottom-right so the bottom poster doesn't cover it.

## Unchanged
Poster places, tilts, colors, tape, sway, tap behavior (decoration only) and the single screen-reader label.

## Left to the builder
Exact stroke widths, drawing proportions and spacing, checked on the emulator in both languages and on the short (720×1280) screen.
