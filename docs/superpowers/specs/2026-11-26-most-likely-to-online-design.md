# "Kim Daha Çok?" online — design

Makes Clink's fourth game playable with everyone on their own phone. Follows `2026-11-25-most-likely-to-design.md` (the one-phone game, §8 "Online, in a later round") and `2026-11-19-party-games-app-design.md` (§5.2 lobbies, §7 games, §10.4 what the host publishes).

## 1. Intent

On one phone this game is "everyone points at once and the holder taps a name". Online it becomes what it always wanted to be: **everybody votes in secret and the room finds out together**. That is the one place this game is genuinely better online than in the room, and it is the first Clink game where the phones, not a person, decide the outcome.

It is also the game that makes the lobby worth having: the other three online games mirror the one-phone version, and this one does not.

## 2. Goals

- Every player votes from their own phone, in secret, and sees the split when it opens.
- A round is still short: read, vote, result, penalty, next.
- The rules stay pure Kotlin so they can move to a server later (spec 2026-11-19 §8).
- The one-phone game keeps working exactly as it does today.

## 3. Non-goals

- Changing anything about the one-phone game's feel.
- A vote timer. The host closes the round instead (§6).
- Giving a seat to somebody who joins after the game started. They watch until the host starts the next game, which is what already happens in the other three games.

## 4. Anti-goals

- A round that cannot end because one person is looking at something else.
- Any screen in the app, on any phone, that shows who voted for whom.
- Any extra tap between the result and the next prompt.

### What "secret" means here, exactly

Everything about a running game is published to every phone in the room — that is how all four games work — and a member's action sits in the lobby until the host consumes it. So the votes **are** in data the room's phones can reach; what the app guarantees is that **no screen ever shows them**, to anybody, the host included.

That is the promise Mehmet chose, and it is the right size for friends-only lobbies: reading someone's vote would mean pointing developer tooling at the database during the round. It stops being enough when Clink lets people play with strangers (spec 2026-11-19 §8). At that point the votes have to stop being published — the host keeps them, the room gets only "how many have voted" and the final counts — which also means per-player action buckets and new database rules. That is a change to the shared online layer under all four games, so it belongs with the strangers work, not here.

## 5. Constraints

- **Three players minimum, twelve maximum** (Mehmet's decision). The one-phone game already refuses two, for the same reason: "who is more likely" needs someone to compare. The lobby's start button today lets any game start with two, so that gate has to ask the game for its own minimum, and a two-person lobby needs a line saying why it cannot start this one. Mehmet accepted that this makes his own two-device testing impossible.
- In this game everyone in the session votes, so there are no spectators except people who join after it started.
- The host's phone is the referee, and only card **ids** are published, never card text (§10.4).
- A player whose connection drops is skipped, not waited for (§5.2).
- Turkish and English for every new line of copy. Dark theme only.

## 6. How a round goes

1. The same prompt appears on every phone, with the player names under it.
2. Everyone taps a name. Nobody sees anybody else's choice; the screen shows only **how many have voted** ("3/5"). You may change your own vote until the result opens.
3. The result opens when **every player who is online has voted**, or when the **host taps "Show the result"**, which appears once the first vote lands.
4. The result shows the split — who got how many — and the penalty card. **The player with the most votes takes the penalty; if several tie, they all do** (Mehmet's decision).
5. Any of the players taking the penalty, or the host, taps "Done", and the next prompt comes up.

The host can also **pass a prompt** that does not suit the room. Passing throws away the votes cast so far and draws another prompt.

A vote already cast **stands even if that phone drops** before the result: the person did vote, and clearing it would make the room watch "4/5" turn back into "3/4" for no reason they can see. If a round reveals while nobody who has to take the penalty is online, the game moves on by itself, the way the other games walk past an absent player's penalty.

Ending: the host's ✕ publishes the tally — who was picked most tonight — to every phone, and the host's "Done" then returns everyone to the lobby. The tally counts **penalties taken**, one per round for each person who took one, exactly as it does on one phone — not votes received, which would be a different number with the same name on it.

```
  WHO FALLS ASLEEP FIRST?        |   WHO FALLS ASLEEP FIRST?
  ─────────────────────          |   ─────────────────────
  Pick someone                   |   Mert    ●●●●  4
                                 |   Ayşe    ●     1
  [Mert] [Ayşe] [Can]            |
  [Elif] [Deniz ✓]               |   MERT takes the penalty
                                 |   ┌─────────────────────┐
  3/5 voted                      |   │ Speak in an accent  │
                                 |   └─────────────────────┘
  Show the result   (host only)  |   [ Done ]   /  waiting for Mert
  Pass this one     (host only)  |
```

## 7. What changes in the code

The project gives every game **one** state, shared by the one-phone and online versions, with the host running the same pure rules (this is how Cup Pong's throws already work). So this is an extension of the existing game, not a second copy of it.

- **A voting phase.** The game gains a phase that holds the prompt and one slot per player for their vote, empty until they cast it. The one-phone game never enters it: it still starts at the prompt-and-chips screen.
- **The penalty phase takes several players**, because a tie means several penalties. On one phone that list always holds exactly one name, which is the only change the one-phone game sees.
- **The result's split travels with the penalty phase**, so every phone can show the same counts without recomputing them. It is empty in a one-phone game.
- **The published session gains the host's id.** The referee has no way to tell the host from anyone else today, and two of this game's actions — "show the result" and "pass" — belong to the host alone. Enforcing that inside the pure rules is better than trusting the screen to hide the buttons.
- **A vote action** goes to the host like Cup Pong's throw input does. The referee stamps it with the sender's own seat and ignores any seat the sender claims.
- **Resolving the round** sits in the two places the project already separates: recording a vote and revealing when everyone has voted is an action, and revealing once every *online* player has voted is part of the existing "skip the offline" pass, which is the only code that knows who is connected. That same pass also walks past a penalty nobody present has to take.
- **The lobby's start gate asks the game for its minimum.** Today it is hard-coded to two for every game and there is one line of copy saying so; both become per-game.
- **The game is switched on for online play**, which also removes the greyed-out "coming soon" card on its mode screen and the unreachable branches that stand in for its online rules today.

## 8. Calls I made, which Mehmet can overrule

1. **The host closes a stalled round, not a timer.** A countdown rushes people; the room's owner already has the authority. Costs: a room with an idle host and one idle player can stall, which the host's ✕ still ends.
2. **"Done" on the penalty is first-tap-wins** among the players taking it and the host, rather than waiting for each of them. Waiting for three people to confirm a penalty would undo the tempo the one-phone game was tuned for.
3. **Passing discards the votes already cast.** Keeping them would mean carrying votes for a prompt nobody sees any more.
4. **People who join mid-game watch until the next round.** Adding a seat mid-round would change the vote count everyone is waiting for.
5. **The split is shown as counts, never as who voted for whom** — including to the host, who could otherwise read the room's secrets off their own screen.
6. **The host's id is published in the session.** It is already visible in the lobby's member list, so it is not a new disclosure.
7. **The night's tally counts penalties, not votes.** It keeps one meaning for the number in both versions of the game, and keeps the one-phone tally screen usable as it is.
8. **A vote survives the voter dropping off**, rather than being cleared with their presence. Clearing would move the counts around while the room is watching them.
9. **A penalty with nobody present to take it advances by itself**, matching what the other three games already do, rather than resting on the host noticing.
10. **Joining mid-game means watching until the next game**, not getting a seat at the next round. Growing the player list inside a running session would be a first for this codebase, and the other three games all behave the way I chose.

## 9. Left to the builder

Exact layout and spacing; how the vote-progress line reads at twelve players; how the split is drawn (bars, dots or plain numbers); whether the result's own prompt is repeated above the split; the wording of the new lines in both languages.
