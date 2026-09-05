# Harness

A portable set of practices, checks, and permissions that keeps an agent's work
consistent — with the request, with the code that already exists, and with what
the rest of the system expects.

It is not a framework and not a library. There is nothing to import and, right
now, nothing to run. It is configuration that a coding agent picks up and works
under.

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
│   ├── settings.json            permissions: deny and ask
│   ├── validation.json          the manifest for this project
│   ├── validation.example.json  every field, with explanations
│   └── skills/
│       ├── commit/              splitting changes, writing the message
│       ├── testing/             what to assert, fakes over mocks
│       ├── error-handling/      root cause, expected vs exceptional
│       └── api-change/          what breaks silently
└── <your software>/
```

---

## The three layers

Organized by **who enforces**, not by subject. The distinction matters because
the layers are not equally reliable.

| Layer | Enforced by | Coverage |
|---|---|---|
| **Permissions** | The platform, outside the model | Every call. Deterministic. |
| **Verification** | Tools the project already has, declared in the manifest | Whatever is checkable |
| **Guide** | The model, reading | Probabilistic |

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

### Adding a skill

A skill has a description, always in context, and a body loaded only when the task
matches. That is what allows many practice guides without paying for all of them
every turn.

Add one when you catch the agent getting a subject wrong repeatedly — not because
you predict it might.

---

## What is not built yet

**Nothing executes the manifest automatically.** There is no hook, by choice. The
agent is instructed to consult and run the declared commands, which means
verification currently lives in the guide layer and carries the guide layer's
reliability. The manifest format is ready for an executor whenever one is added;
until then, the guarantee is probabilistic.

**Deliberately deferred**, each with the signal that brings it back:

| Deferred | Signal |
|---|---|
| Verification hook | You catch the agent skipping the declared commands |
| Code index | You watch it open eight files to answer one structural question |
| Clean-context reviewer | Reviewing diffs yourself becomes the bottleneck |
| Session logging | You want to know what a task cost |
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
