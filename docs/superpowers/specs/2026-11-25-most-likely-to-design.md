# "Kim Daha Çok?" / Most Likely To — design

Clink's fourth game. Follows `2026-11-19-party-games-app-design.md` (§7 games, §8 extensibility) and the graffiti look of `2026-11-20-graffiti-visual-refresh-design.md`.

## 1. Intent

Clink's three games all put **one** person on the spot while everyone else watches: one player draws a card, one asks and one answers, one throws a ball. In a real room the laughter comes from the moments where everybody does something at the same time, and Clink has none of those. Mehmet, asking for the next game: "Parti oyunları böyle. Veya ortamlarda arkadaşlarla oynamalık."

A prompt appears ("Kim ilk evlenir?"). The group counts to three and everybody points at someone at once. The most-pointed player takes a penalty, and the next prompt comes up.

## 2. Goals

- Every player acts in every round.
- A round is short: read, point, penalty, next.
- It is recognisably Clink: the same levels, the same penalty cards, the same player list, the same wall and posters.
- Playable on one phone when this round of work is done.

## 3. Non-goals

- **Online play in this round.** The design leaves room for it (§8) but it is not built now.
- **A winner.** Like Truth or Dare and Spin the Bottle, nobody wins; the session ends when the players end it.
- **Tie handling.** The phone holder taps one name, so there is never a tie to resolve.
- **Secret voting on one phone.** Ruled out by Mehmet: the pointing is the game, and passing the phone around every round would kill the tempo.

## 4. Anti-goals

Things that would make this a failure even if it worked:

- The game degenerating into a prompt deck with no consequence, disconnected from Clink's penalty system.
- Anything that makes the phone travel around the room.
- Any drinking wording or imagery, in prompts or penalties (content rule 3).

## 5. Constraints

- **3 to 12 players.** With two there is nothing to compare; "who is more likely" has no meaning. Today `PlayerNames.MIN` is a single global minimum of 2, so a per-game minimum has to be added.
- **Content:** a new card type, Turkish and English for every card, three levels, no drinking words. The bundled test requires at least three cards for every type and level, so a sample set ships with the app. Mehmet writes the real prompts later.
- Each game is a self-contained module that plugs into the shared player, level, penalty and (later) lobby systems — spec 2026-11-19 §8.
- Dark theme only; the app has no light theme and will not get one.
- Code, identifiers and comments in English; all player-facing copy in Turkish and English.

## 6. Approaches considered

- **A. Its own game module, one phone first (chosen).** Built like `truthordare`, `spinthebottle` and `cuppong`: its own state, reducer, screens and card type. Online comes in a later round. Chosen because this game lives or dies on whether the prompts and the tempo land in a real room, and the fastest way to learn that is to put it in Mehmet's hands. It also matches how this project already grew: the games worked on one phone first, then online was added to all of them at once.
- **B. Its own module, one phone and online together.** One bigger round. The online rules would be pure functions covered by tests, but the online screens cannot be checked on a device: the emulator cannot sign in and Claude signs in to no account. Rejected for now in favour of a playable game sooner.
- **C. A mode inside Truth or Dare.** The cheapest build, but it mixes two games together. The home posters, the game list and the online lobby all treat a game as a thing of its own, and as a mode this game would appear in none of them. Dropped.

## 7. The game

### 7.1 Flow

Game list → player names → level. All three are the existing screens; the only change is that this game requires at least three players.

Each round:

1. A prompt card for the session's level is shown, with a fixed instruction line under it.
2. The group counts to three out loud and everyone points at someone.
3. The phone holder taps the most-pointed name. A prompt that does not suit the room can be **passed** instead: the next prompt comes up, nobody takes a penalty and no counter moves.
4. That player's name fills the screen with a penalty card from the same level under it. One button ends the round and draws the next prompt; a quieter second one says **the wrong person was tapped** and goes back to the same prompt with the counter put back.

Ending: the ✕ in the top bar, with the same confirmation other games use, leads to a tally screen — who was picked most tonight — and from there to the shared game-over screen, which already offers "play again" and "back to games". If the game ends before anybody has been picked there is nothing to show, so the tally is skipped.

### 7.2 On screen

The prompt round:

```
 ‹                    KİM DAHA ÇOK?        ✕

      ┌──────────────────────────┐
      │                          │
      │     KİM İLK EVLENİR?     │     ← poster card, like a Truth card
      │                          │
      └──────────────────────────┘

        Üçe kadar sayın, herkes
         aynı anda birini göstersin

      EN ÇOK GÖSTERİLEN KİM?

      [ Mert ② ] [ Ayşe ] [ Can ① ]
      [ Elif ]   [ Deniz ③ ]

              Bu soruyu geç
```

The penalty:

```
                 MERT

      ┌──────────────────────────┐
      │   30 saniye boyunca      │
      │   herkesi taklit et      │
      └──────────────────────────┘

             [ BİTTİ ]

            Yanlış kişi
```

The tally, when the game ends:

```
            BU AKŞAM

        1.  DENİZ      ③
        2.  MERT       ②
        3.  CAN        ①

             [ BİTTİ ]
```

### 7.3 Rules

- The prompts and the penalties are two separate decks, drawn and reshuffled the same way Truth or Dare does it.
- Anyone can be tapped, the phone holder included.
- The tally counts how many times each player was tapped. A name chip shows its counter only once it is above zero, so the first round starts clean.
- A penalty is drawn **every** round. This is Mehmet's decision, made knowing the alternative: in Truth or Dare a penalty only appears when someone refuses, so penalties are occasional there. The risk he accepted is tempo — every round now contains a performance. If it drags in real play, the cheapest fix is to make the penalty a choice ("defend yourself, or take it"), which is a change to one phase, not to the game.
- **Passing** draws the next prompt and changes nothing else. **"Wrong person"** returns to the same prompt and takes the counter back down; the penalty card it had already drawn stays spent, which costs nothing because the deck reshuffles.
- Two dead ends have to be closed, and Truth or Dare's notices do not close them on their own: there a notice answers a tap and the player always has a second choice, while here the prompt is drawn for them when the round opens.
  - **No prompt cards at this level:** the game cannot start a round at all. It says so on a screen whose only way on ends the game.
  - **No penalty cards at this level:** the picked player's name still fills the screen, with the notice in place of a card, and the round ends normally.

## 8. Online, in a later round

Not built now, but nothing here should block it. When it comes:

- Every player votes secretly on their own phone; when the last vote lands, the result opens with the split ("Mert 4, Ayşe 1"). This is the one place where the game is genuinely better online than in the room, so it is worth doing properly.
- It needs a state in `SessionState`, a vote action in `OnlineAction`, and tally and penalty rules in `HostReferee`, all pure functions covered by tests, as the other games are.
- Ties become real online, because the phones count the votes rather than a person. That rule gets decided then, not now.

Until then the game must not be reachable online, and there are **two** doors into online play, not one:

- The lobby's game picker, which today simply lists every `GameId`.
- The mode screen that every game passes through, which today offers "one phone" and "online" for all of them. Picking online here really does create a lobby, and the host referee has no rules for this game yet.

`GameId` carries a flag saying whether a game plays online. The lobby picker leaves flagged-off games out; the mode screen keeps showing both cards but draws the online one dimmed and untappable, with a short "coming soon" line, the way the game list once showed Cup Pong before it was built. Both the flag and the line go away when online lands.

## 9. Content

A new card type sits beside `TRUTH`, `DARE` and `PENALTY`. Penalties are reused as they are; no new penalty content is needed.

`baseline.json` gets **fifteen** sample prompts for each of the three levels, in Turkish and English — forty-five in all. The bundled content test only demands three, but three would make the deck loop every third round, and this round exists to find out whether the game holds up over a real evening. Mehmet replaces them with his own set later. `docs/content-format.md` gains the new type and its tone notes.

Tone, following the existing levels:

| Level | Tone | Sample (TR / EN) |
|---|---|---|
| Casual | Anyone can play, funny, embarrasses nobody | "Kim ilk uyuyakalır?" / "Who falls asleep first?" |
| Hard | More personal, more pointed | "Kimin telefonunu karıştırmak en çok şaşırtır?" / "Whose phone would surprise you the most?" |
| Spicy | Flirty and bold, for adults, suggestive but not explicit | "Kim bir eski sevgilisine mesaj atar?" / "Who would text an ex tonight?" |

Every prompt is a question that points at a person, so it reads naturally with "kim" / "who". Prompts never name a player.

## 10. Name and art

- **Turkish:** "Kim Daha Çok?" **English:** "Most Likely To".
- Home poster: the highlighted word is "KİM" in Turkish and "MOST" in English — both appear in their titles, which is what the poster highlight needs.
- Three new drawings are needed even though only two are visible today, because the code that maps a game to its art must cover every game: the game-list illustration, the small icon used by the online room, and the ink line drawing for the home poster. The poster drawing is several hands pointing at one spot, ink lines only, like the other three.
- Short line under the game's name: "3–12 kişi" / "3–12 players". Longer line on the game list: "3–12 kişi · Üçe kadar say, göster" / "3–12 players · Count to three and point".

## 11. Copy that counts the games

A fourth game makes three existing texts wrong, and they are part of this round:

- The play tour's first card says "Üç oyun, tek uygulama" and then names the three games. It is rewritten so it does not count or list them, and no future game has to touch it again.
- `hosting/public/index.html` names the three games. The file is updated here; putting the site live stays Mehmet's.
- `docs/design/illustration-brief.md` lists three games and asks for four drawings. The new game's drawings are added to it.

The home screen's "all games" line counts `GameId` itself, so it becomes 4 on its own.

## 12. Calls I made, which Mehmet can overrule

1. **The home posters stay as they are.** The home screen shows three tilted posters and a line counting all games; that line becomes 4 while the posters stay Truth or Dare, Spin the Bottle and Cup Pong. The three-poster composition is fixed, and a brand-new game has not earned a slot yet. Swapping one in later is a one-line change.
2. **"Kim Daha Çok?" as the Turkish name**, because that is what we called it while designing. "En Çok Kim?" reads a little better in Turkish; the name is Mehmet's copy to set.
3. **A minimum of three players, enforced per game.** The alternative was to allow two and let the game be meaningless.
4. **The tally lives in the game**, not in the shared game-over screen, so that screen stays the same for all four games.
5. **Live counters on the name chips**, not only at the end, because the running count is what the room talks about.
6. **The mode screen keeps both cards**, with online dimmed, rather than hiding it or skipping the screen for this game. Hiding it would leave a single card on a screen built for two, and skipping the screen would make this one game navigate differently from the other three.
7. **The play tour stops counting games.** It could keep listing them and be edited with every new game, but that is a maintenance item that will be forgotten.
8. **"Wrong person" gives the counter back but not the penalty card.** Putting a drawn card back into the deck would complicate the deck for no gain, since it reshuffles anyway.

## 13. Left to the builder

Exact sizes and spacing; the wording of the instruction line; how the name chips wrap at twelve players on a short screen; how the tally screen orders players who are level; the shapes of the three drawings.
