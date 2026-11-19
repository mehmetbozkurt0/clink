# CLAUDE.md

## Git (strict)
- NEVER run `git commit`, `git push`, `git rebase`, `git reset`, `git stash`, `git checkout -- <file>` or any other command that creates commits or rewrites history. Mehmet does all commits.
- After each step, stop and give: (1) a short summary of what changed in which files and why, (2) a ready-to-use commit message in Conventional Commits format (`feat: ...`, `fix: ...`, `chore: ...`).
- Wait for Mehmet to confirm he committed before starting the next step, so each commit holds exactly one step.
- If a skill, plugin or plan (e.g. Superpowers) includes a commit step, do not commit; propose the message instead.

## Communication
- Talk to Mehmet in Turkish. Code, identifiers, comments and commit messages in English.
- Use numbers, but don't drown the explanation in them. First explain in plain language what happened and what it means, then give the few key numbers that back it up. Long tables and raw metric dumps only when he asks.
- Explain the reasoning behind decisions; mention alternatives briefly when there is a real trade-off.

## Coding principles (adapted from the Karpathy guidelines)
- **Think before coding.** State your assumptions. If something is unclear, ask instead of guessing. If a request has more than one reasonable reading, show the options instead of picking one silently. If there is a simpler way, say so.
- **Simplicity first.** Write the minimum code that does the job. No speculative features, abstractions or config "for later".
- **Surgical changes.** Touch only what the task requires. Don't "improve" adjacent code, comments or formatting; match the existing style. Report unrelated problems instead of fixing them.
- **Goal-driven.** Turn each task into a verifiable success criterion. Don't say "done" without having verified it.
