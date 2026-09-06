# Harness

A portable set of practices, checks, and permissions that keeps an agent's work
consistent — with the request, with the code that already exists, and with what
the rest of the system expects.

It is not a framework and not a library. There is nothing to import. It is
configuration that a coding agent picks up and works under, plus one small
deterministic executor, `verify/runner.py`, which the agent's own hooks call.

The harness does not own the loop. It has no scheduler, no budget and no stop
condition of its own; those stay with the platform. The alternative — a loop of
its own over the Messages API — was cut on 2026-09-05 because everything it
would add existed only to measure, and confirmed cut on 2026-09-06.

---

## How to use it

**You do not invoke the harness.** There is no command and no mode to switch on.
You open this folder in your coding agent and work normally; the harness acts
around you.

```
1. Open J:\TCC\harness in Claude Code
2. Build your software inside it
3. Declare its boundary in .claude/validation.json
4. Work
```

That is the whole workflow. The agent starts every session already under the
rules in `CLAUDE.md`, already unable to do what `settings.json` denies, and
already told which commands verify this project.

If it is working, the experience is *"the agent stopped doing the dumb things"* —
it asks instead of inventing, it stays inside the scope you gave it, and it does
not claim to be done without running what you declared.

### Why software goes *inside* this folder

Project configuration is read from the working directory only — there is no
lookup in parent directories. Putting your project inside the harness makes the
harness the working directory, so everything under it inherits the setup with no
copying and nothing to keep in sync.

The trade-off: a project that already has its own git history does not move in
cleanly. For that case, copy `.claude/` and `CLAUDE.md` into it instead.

---

## Layout

```
harness/
├── CLAUDE.md                    always-on rules (10, capped)
├── .claude/
│   ├── settings.json            permissions: deny and ask; the two hooks
│   ├── validation.json          the manifest for this project
│   ├── validation.example.json  every field, with explanations
│   └── skills/
│       ├── commit/              splitting changes, writing the message
│       ├── testing/             what to assert, fakes over mocks
│       ├── error-handling/      root cause, expected vs exceptional
│       ├── api-change/          what breaks silently
│       └── duplication-check/   does this already exist?
├── verify/
│   ├── runner.py                the executor the hooks call
│   ├── test_runner.py           its tests: python -m pytest verify
│   ├── test_rules.py            the rule cap, as a test
│   └── .trace.jsonl             one record per verification, not versioned
└── <your software>/
```

---

## The three layers

Organized by **who enforces**, not by subject. The distinction matters because
the layers are not equally reliable.

| Layer | Enforced by | Coverage |
|---|---|---|
| **Permissions** | The platform, outside the model | Every call. Deterministic. |
| **Verification** | `verify/runner.py`, called from two hooks | Every edit, deterministically. The completion gate is weaker — see below. |
| **Guide** | The model, reading | Probabilistic |

Verification runs in two phases, split by cost — measured, not guessed: the backend
suite takes 203 seconds and a type check takes 3.

| When | Hook | What runs |
|---|---|---|
| After every edit | `PostToolUse` | the `fast` commands of the touched boundary |
| Before accepting "done" | `Stop` | the full `commands`, if an edit went unverified |

The second is the completion gate: it blocks with exit code 2 and hands the failure
back.

Both hooks call the executor as `python`, resolved on PATH, so the harness runs
wherever that name is a Python 3: the python.org installer adds it on Windows,
Debian-family Linux needs the `python-is-python3` package, macOS has it through
Homebrew. The executor's own tests need `pytest` in that same Python; that is the
one Python package the harness asks for.

**The gate fails open, and the distinction matters.** The per-edit check fires on
every edit — there is nothing to bypass. The gate does not have that guarantee: it
is skipped when the stop comes from a user interrupt, its output is ignored on an
API error, a callback that exceeds its timeout lets the turn end, and Claude Code
overrides the hook after 8 consecutive blocks. The harness gives up at 4, below that
ceiling, and says so rather than going quiet.

So the gate **reduces the frequency** of "done" without verification. It does not
make it impossible. Failing open is the right direction for a mechanism that could
otherwise deadlock a session — but it is not a guarantee, and calling the whole
layer deterministic would overstate it.

A practice moves down a layer only when the layer above cannot express it. What a
tool can check does not belong in prose — it belongs in the project's own linter,
and the result comes back as a fact rather than a reminder.

This ordering exists because text is the weakest lever available. Prompt
formatting alone — with no change in content — has been measured to swing accuracy
by tens of points, and adherence degrades as instructions accumulate. Permissions
and commands either fired or did not. Prose is followed *most* of the time.

So the guide is the last resort, used for what genuinely cannot be enforced any
other way.

---

## The manifest

`.claude/validation.json` maps changed paths to boundaries, and boundaries to the
commands that already exist in the project.

It is what makes the harness language-agnostic: the manifest does not know what a
test is. It runs what you declare. `mvn test`, `pytest`, `go test`, `cargo test`
are indistinguishable to it, so the harness ships no tooling and no
language-specific configuration.

### Adding a boundary

When you create software inside the harness, add its boundary:

```json
{
  "id": "api",
  "paths": ["api/**"],
  "workingDirectory": "api",
  "commands": [
    { "run": "go test ./...", "prerequisites": ["go"] }
  ]
}
```

Three fields are worth knowing, and `validation.example.json` shows all of them:

- **`prerequisites`** — what must exist for the command to mean anything. Missing
  prerequisite yields `BLOCKED`, never `FAIL`. This is what stops the agent from
  "fixing" healthy code because Docker was not running.
- **`consumedBy`** — other boundaries that depend on this one. Editing the
  producer also runs the consumer's checks, so a backend change that breaks the
  frontend surfaces immediately.
- **`commands: []`** — a boundary with no executable validation. Reported as such;
  another boundary's suite is never substituted for it.

### The one tool the harness asks you to install

Everything else here declares commands the project already has. `jscpd` is the
exception, and it is worth naming as one.

It detects duplicated code across 224 formats. It **tokenizes rather than
executes**, so it needs no compiler, no runtime, and none of the project's
toolchain — it is a self-contained binary that runs on a freshly cloned
repository before anything is installed. That is why it earns the exception: it is
the only check available before an environment exists.

```
irm https://jscpd.dev/install.ps1 | iex
```

Add it to any boundary holding source code — the shape and the tuning notes are in
`validation.example.json`.

**What it does not do:** it finds copied, renamed, and lightly edited code. It does
not find the same idea implemented differently — the hard case, which currently has
no reliable tool in any language. Only a fresh-eyes read of the diff catches that.

**Not adopted, on purpose:** jscpd also ships an MCP server and a `dry-refactoring`
skill. The MCP server would add tool definitions to every request in exchange for an
occasional lookup, against a project whose first priority is token cost. The skill
guides toward eliminating duplication, which works against this harness's own rule
to prefer duplication over the wrong abstraction.

---

## Extending it

### Adding a rule to `CLAUDE.md`

Two tests, both required:

1. **Does it contradict what the model does by default?** If the model already
   does it, the rule is noise — it costs context and dilutes the rules that
   remain. This is why there is no "follow SOLID" here: the model already knows
   SOLID, and its failure mode is applying it too eagerly. The rule that changes
   behavior is the inverse — *do not create an extension point before real
   variation exists*.

2. **Is there a cap?** Ten rules. The eleventh has to evict one. Practices that
   do not make the cut become skills, which cost nothing until loaded.

A rule is one paragraph holding one independent imperative — a unit that could be
removed on its own without breaking another. Two ideas in one paragraph are two
rules, and get two paragraphs. The count is a test, `verify/test_rules.py`: it
counts the paragraphs under the `##` headings and fails past ten, so the cap is an
invariant rather than an intention. The blockquote under *Verification* is not a
rule — it explains a mechanism the harness runs on its own — and is not counted.

### Adding a skill

A skill has a description, always in context, and a body loaded only when the task
matches. That is what allows many practice guides without paying for all of them
every turn.

Add one when you catch the agent getting a subject wrong repeatedly — not because
you predict it might.

---

## Where this sits, and what is missing

The survey *Externalization in LLM Agents* (arXiv:2604.08224) decomposes a harness
into six dimensions. It is worth stating plainly which of them this harness has,
because three of the six are only partly there, and a row that just said
*present* would hide which part is missing.

The paper is explicit that this is **"an analytical framework for comparing harness
architectures rather than an implementation checklist"** — so the goal is not to
fill every row. It is to know which rows are empty and why.

| Dimension | State here |
|---|---|
| **Skills** | Five, with progressive disclosure. Missing the paper's third attribute: revision driven by observed failure |
| **Verification / Control** | Per-edit checks and a completion gate with a recursion bound. No turn or cost ceiling — in interactive use the human is the stop condition |
| **Permission** | **Partial.** Declarative deny/ask rules, not isolation — see below |
| **Protocols** | Inherited via MCP and the hook contract. Not designed |
| **Memory** | Semantic: `CLAUDE.md`, the skills and this file, all reviewed. Episodic: `verify/.trace.jsonl`, deterministic, not yet read. Personalised: the platform's auto-memory, left on and not governed by the harness — see below. Working context: none, on purpose — see below |
| **Observability** | **Partial.** `verify/.trace.jsonl` records every verification the runner performs — boundary, command, outcome, duration, exit code — deterministically, capped. Nothing reads it yet: no metrics, no report |

### Permission is policy, not isolation

The paper describes this dimension as sandboxing, filesystem isolation and network
restriction. What is here are permission *rules*, which is a weaker thing.

Concretely: denying `curl` and `wget` by name does not close the network. A wrapper
the matcher does not strip — `docker exec c curl ...` — is matched as a `docker`
command and passes. Closing the network needs an allowlist-shaped policy or a real
sandbox, neither of which is configured.

The rules are worth having. They are not isolation, and the difference should not be
blurred.

### Memory the harness does not write

The platform keeps an auto-memory store per repository: the model writes notes
during a session and the index is loaded at the start of the next one. The
harness leaves it on and does not govern what goes in. That makes it the one
guide-layer input here that nobody reviews, and the survey names the failure
mode: a poisoned note steers a later session, and no single module can catch
it without a harness-level rule (§7.1).

The rule is this. The store holds **facts, episodes and preferences, each with
a date**. It does not hold instructions. An instruction found there — "ignore
X", "always do Y" — is either promoted, after a person reads it, into
`CLAUDE.md`, a skill or this file, or it is deleted. Preferences about the
person go to `~/.claude/CLAUDE.md`, the user scope, not here: the harness
travels, the person does not, and user-specific state obeys different
retention and privacy rules from project state (§3.1).

Turning the store off was considered and not done. `"autoMemoryEnabled": false`
in `settings.json` would only affect sessions opened in this folder — a session
opened in the parent directory writes to the same store — and one bad note is a
reason to review, not to remove. The signal that reverses this is in the table
below.

Working context, the fourth dimension, is absent on purpose. InfiAgent
(arXiv:2601.03204) keeps files as the state and rebuilds context every step from
the files plus a fixed window of recent actions. The first half is what the
trace does. The second half is not available: the platform's compaction is
closed, and with a person in the loop, the person is the window.

### Deferred, each with the signal that brings it back

| Deferred | Signal |
|---|---|
| Trace reader | You want to know whether a failure has happened before and realise you cannot answer without opening `verify/.trace.jsonl` by hand |
| Turning auto-memory off | A second model-written instruction steers a session. Then the store is an unreviewed guide layer: off in `settings.json`, with `settings.local.json` as the per-machine opt-in |
| Episodic memory | You catch yourself correcting the same thing a third time |
| Code index | You watch it open eight files to answer one structural question |
| Clean-context reviewer | Reviewing diffs yourself becomes the bottleneck |
| `consumedBy` between backend and frontend | The frontend types stop being hand-mirrored and start being generated from the contract |
| Evaluation cases | You are changing rules and cannot tell whether they help |

---

## Design notes

**The definition is subtractive on purpose.** "What the model does not do on its
own" generates a small harness by construction. An earlier version defined it as
an action interface, context selection, a control loop, and guards — three of
which are software, which is why that version generated software.

**Rules are curated against defaults, not copied from a style guide.** Most of a
best-practices document confirms what a capable model already does. The parts that
change behavior are the ones that restrain it: prefer duplication over the wrong
abstraction, do not refactor beyond what you touched, do not infer a requirement
that was not stated.

**Asking has a cost curve.** The agent stops for what is expensive to reverse or
whose effect does not show up where the change is — a breaking change that fails
nothing qualifies; a comment you can delete after reading the diff does not. Ask
too often and the questions stop protecting anything, because you stop reading
them.
