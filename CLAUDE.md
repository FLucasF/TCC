# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

The TCC (undergraduate thesis, UFPB) of Lucas Felipe, advised by Rodrigo. Since the
2026-09-06 pivot, the object of study is **the design principles of a coding-agent
harness**. The repository holds the harness itself plus the research trail around it.
There is no application code, build, lint or test suite at the root — do not invent one.

Documentation is mostly in Portuguese; the harness itself is written in English.

## Layout and the role of each part

- `harness/` — the artifact under study: `CLAUDE.md` (always-on rules),
  `.claude/settings.json` (permission deny/ask lists), `.claude/skills/` (practice
  guides loaded on demand) and `scripts/` (pinned installers for graphify and jscpd).
  `harness/README.md` is the manual: how to use it and the tests for adding a rule or
  a skill. Read it before changing anything under `harness/`.
- `docs/registro-do-harness.md` — the construction record: what was built, removed,
  what that cost, and the signal that brings each piece back. History goes here, not
  into `harness/README.md` (they were split on 2026-09-07 to keep the manual from growing).
- `docs/briefings/` — deprecated trail kept for the thesis. **None of these documents
  drive the harness**; the current state lives in the `harness/` files themselves.
- `docs/TCC - obsidian/` — Lucas's Obsidian vault (study notes, papers, meetings).
- `anotacoes de Rodrigo/` — the advisor's notes from orientation sessions (e.g. the
  2×2 experiment plan: model × default/harness condition).

## Things that are easy to get wrong

- **The harness only applies when the session's working directory is `harness/`.**
  A session opened at this root does not get `harness/.claude/settings.json` or its
  skills. This already invalidated a whole session once (2026-09-06). When working on
  or testing harness behavior, say which directory the session runs from.
- **Changing `harness/` changes the experiment.** Every rule must pass the harness's
  own subtractive test (does it contradict a default model behavior?). A practice that
  does not pass becomes a skill; a skill the platform already ships is not copied in.
  Record the reasoning for additions/removals in `docs/registro-do-harness.md`.
- The removed verification layer (`verify/`) is recoverable from commit `8b3cbdb`
  (removed in `52395cd`); the registro explains why a plain checkout/revert is not
  enough to restore it.
- `harness/apps/` (the guinea-pig project) and `graphify-out/` are gitignored on purpose.
- `harness/.gitattributes` forces LF on `*.sh`; CRLF breaks the pinned version strings.
- Obsidian plugins add `feature:` frontmatter to vault notes with paths relative to
  Rodrigo's vault, not this repo — these are editor noise, not content changes.

## Commits

Messages in Portuguese, Conventional Commits prefix (`docs:`, `feat(harness):`,
`refactor:`), imperative, lowercase. See `harness/.claude/skills/commit/SKILL.md` for
how changes are split.
