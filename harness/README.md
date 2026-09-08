# Harness

A portable set of practices and permissions that keeps an agent's work consistent —
with the request, with the code that already exists, and with what the rest of the
system expects.

It is not a framework and not a library. There is nothing to import and nothing to
run. It is configuration that a coding agent picks up and works under.

The harness does not own the loop. It has no scheduler, no budget and no stop
condition of its own; those stay with the platform. It no longer owns an executor
either: a deterministic verification layer was built and removed, and nothing here
checks the agent's work.

Why it ended up this way — what was cut, what that cost, and the signal that brings
each piece back — is in `docs/registro-do-harness.md`, outside this folder. That
record is the most useful reading after this file.

---

## How to use it

**You do not invoke the harness.** There is no command and no mode to switch on.
You open this folder in your coding agent and work normally.

```
1. Open J:\TCC\harness in Claude Code
2. Build your software inside it
3. Work
```

That is the whole workflow. The agent starts every session already under the rules
in `CLAUDE.md` and already unable to do what `settings.json` denies.

If it is working, the experience is *"the agent stopped doing the dumb things"* — it
asks instead of inventing, and it stays inside the scope you gave it.

### Why software goes *inside* this folder

Project configuration is read from the working directory only — there is no lookup
in parent directories. Putting your project inside the harness makes the harness the
working directory, so everything under it inherits the setup with no copying and
nothing to keep in sync.

The trade-off: a project that already has its own git history does not move in
cleanly. For that case, copy `.claude/`, `CLAUDE.md`, `scripts/` and `.gitattributes`
into it instead — the skills name paths under `scripts/`, and `.gitattributes` is what
keeps those scripts LF on a Windows checkout.

This is not free, and it bit in practice: a session opened one directory up gets
none of this. On 2026-09-06 an entire working session ran from the parent directory
with the harness inert, and nothing said so.

---

## Layout

```
harness/
├── CLAUDE.md                    always-on rules
├── .claude/
│   ├── settings.json            permissions: deny and ask
│   └── skills/
│       ├── commit/              splitting changes, writing the message
│       ├── testing/             what to assert, fakes over mocks
│       ├── error-handling/      root cause, expected vs exceptional
│       ├── duplication-check/   does this already exist?
│       ├── impact-analysis/     what breaks if I change this?
│       └── execplan/            plans that outlive the session
├── scripts/                     pinned installers for the two external tools
└── <your software>/
```

---

## The two layers

Organized by **who enforces**, not by subject. The distinction matters because the
layers are not equally reliable.

| Layer | Enforced by | Coverage |
|---|---|---|
| **Permissions** | The platform, outside the model | Every call. Deterministic |
| **Guide** | The model, reading | Probabilistic |

There used to be a third between them. Its absence is the defining fact about this
harness now, so it is stated rather than implied: **nothing here checks the agent's
work.** Whether a test ran, whether the build passes, whether "done" is true — all
of that rests on the model choosing to do it.

That ordering exists because text is the weakest lever available. Prompt formatting
alone — with no change in content — has been measured to swing accuracy by tens of
points, and adherence degrades as instructions accumulate. Permissions either fired
or did not. Prose is followed *most* of the time.

So the guide is doing work it is not well suited to, and that is a known,
deliberate position rather than an oversight.

---

## Extending it

### Adding a rule to `CLAUDE.md`

One test: **does it contradict what the model does by default?** If the model already
does it, the rule is noise — it costs context and dilutes the rules that remain. This
is why there is no "follow SOLID" here: the model already knows SOLID, and its
failure mode is applying it too eagerly. The rule that changes behavior is the
inverse — *do not create an extension point before real variation exists*.

A practice that does not pass becomes a skill, which costs nothing until loaded.

A rule is one paragraph holding one independent imperative — a unit that could be
removed on its own without breaking another. Two ideas in one paragraph are two
rules, and get two paragraphs.

### Adding a skill

A skill has a description, always in context, and a body loaded only when the task
matches. That is what allows many practice guides without paying for all of them
every turn. Measured on 2026-09-06 with four skills: `CLAUDE.md` plus the skill
descriptions was about 830 tokens carried on every request, and the bodies were
around 9 KB. Two skills were added since — `execplan` and `impact-analysis`. The
always-on part is now 3.7 KB of source, `CLAUDE.md` at 2,429 bytes plus 1,243 of
descriptions, against about 21 KB of bodies that cost nothing until they fire.
Those last figures are file sizes; the 830 was a context reading and was not retaken.

Add one when you catch the agent getting a subject wrong repeatedly — not because
you predict it might.

The same test applies to skills that already exist elsewhere. A skill shipped by the
platform is not missing, and copying it here forks it: the copy goes stale and the
name collides. *"The model already does it"* has a sibling — *"the platform already
ships it"* — and the second is easier to miss, because it looks like something you
could build.

---

## Memory the harness does not write

The platform keeps an auto-memory store per repository: the model writes notes
during a session and the index is loaded at the start of the next one. The harness
leaves it on and does not govern what goes in. That makes it the one guide-layer
input here that nobody reviews, and the survey names the failure mode: a poisoned
note steers a later session, and no single module can catch it without a
harness-level rule (§7.1).

The rule is this. The store holds **facts, episodes and preferences, each with a
date**. It does not hold instructions. An instruction found there — "ignore X",
"always do Y" — is either promoted, after a person reads it, into `CLAUDE.md`, a
skill or this file, or it is deleted. Preferences about the person go to
`~/.claude/CLAUDE.md`, the user scope, not here: the harness travels, the person
does not, and user-specific state obeys different retention and privacy rules from
project state (§3.1).

Turning the store off was considered and not done. `"autoMemoryEnabled": false` in
`settings.json` would only affect sessions opened in this folder — a session opened
in the parent directory writes to the same store — and one bad note is a reason to
review, not to remove. The signal that reverses this is in `docs/registro-do-harness.md`.
