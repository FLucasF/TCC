# Harness

A portable set of practices and permissions that keeps an agent's work consistent —
with the request, with the code that already exists, and with what the rest of the
system expects.

It is not a framework and not a library. There is nothing to import and nothing to
run. It is configuration that a coding agent picks up and works under.

The harness does not own the loop. It has no scheduler, no budget and no stop
condition of its own; those stay with the platform. The alternative — a loop of its
own over the Messages API — was cut on 2026-09-05 because everything it would add
existed only to measure, and confirmed cut on 2026-09-06.

**It no longer owns an executor either.** A deterministic verification layer was
built on 2026-09-06 and removed the same day. What that cost and what brings it
back is in *What was removed, and the signal that reverses it*, below — that section
is the most useful thing in this file.

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
cleanly. For that case, copy `.claude/` and `CLAUDE.md` into it instead.

This is not free, and it bit in practice: a session opened one directory up gets
none of this. On 2026-09-06 an entire working session ran from the parent directory
with the harness inert, and nothing said so.

---

## Layout

```
harness/
├── CLAUDE.md                    always-on rules (10, capped)
├── .claude/
│   ├── settings.json            permissions: deny and ask
│   └── skills/
│       ├── commit/              splitting changes, writing the message
│       ├── testing/             what to assert, fakes over mocks
│       ├── error-handling/      root cause, expected vs exceptional
│       └── duplication-check/   does this already exist?
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

## What was removed, and the signal that reverses it

On 2026-09-06 this harness had a `verify/` directory: a manifest mapping changed
paths to commands, an executor called from two hooks, per-edit checks, a completion
gate that blocked with exit code 2, and a JSONL trace. It was removed the same day.

**Why.** The verification layer was never observed changing an outcome. During the
session that built it, the hooks never fired — the session had been opened one
directory up — and every check ran anyway, by choice. That is one session and not
evidence of much, but the harness's own filter is subtractive: what the model does
on its own does not belong here. Nothing had been measured that put verification
outside that filter.

**What it cost to remove.** Three things it did are not recoverable by prose:

1. **The gate made "done" contingent rather than declared.** A claim that survived
   an `exit 2` is a different object from a claim. Prose asks; it cannot refuse.
2. **`BLOCKED` never became `FAIL`.** With Maven absent the model reads "failed" and
   goes on to *fix healthy code*. That is a specific destructive behavior, and the
   distinction only existed because it was written down in code.
3. **Output filtered to the touched file.** It is what made a duplication check cost
   ~30 tokens instead of a full report. Without an executor there is no filter.

**The signal that brings it back.** Any one of:

- you find a commit where the suite did not run and should have
- the agent "fixes" working code because a tool was missing from the environment
- you stop trusting "done" and start re-running things yourself before reading

The design is recorded in `docs/briefings/decisoes-do-harness.md` §10. The code is in
this repository's history: commit `8b3cbdb` is the last one that has the complete
`verify/`, and `52395cd` is the removal.

The executor and its 41 tests come back with `git checkout 8b3cbdb -- harness/verify`.
The rest of the layer — the manifest, the two hooks, and the `CLAUDE.md` section that
told the model it did not need to run the checks — is in that same commit and has to
be copied back by hand, because the prose files were rewritten afterwards and a plain
revert conflicts with them. The code is a checkout; the wiring is a decision.

---

## Extending it

### Adding a rule to `CLAUDE.md`

Two tests, both required:

1. **Does it contradict what the model does by default?** If the model already does
   it, the rule is noise — it costs context and dilutes the rules that remain. This
   is why there is no "follow SOLID" here: the model already knows SOLID, and its
   failure mode is applying it too eagerly. The rule that changes behavior is the
   inverse — *do not create an extension point before real variation exists*.

2. **Is there a cap?** Ten rules. The eleventh has to evict one. Practices that do
   not make the cut become skills, which cost nothing until loaded.

A rule is one paragraph holding one independent imperative — a unit that could be
removed on its own without breaking another. Two ideas in one paragraph are two
rules, and get two paragraphs.

**The cap is an intention again, not an invariant.** It used to be a test that
counted the paragraphs and failed past ten; the test lived in `verify/` and went
with it. Ten is the current count, verified by hand on 2026-09-06.

### Adding a skill

A skill has a description, always in context, and a body loaded only when the task
matches. That is what allows many practice guides without paying for all of them
every turn. Measured on 2026-09-06: `CLAUDE.md` plus the skill descriptions is about
830 tokens carried on every request; the skill bodies are around 9 KB and cost
nothing until they fire.

Add one when you catch the agent getting a subject wrong repeatedly — not because
you predict it might.

The same test applies to skills that already exist elsewhere. A skill shipped by the
platform is not missing, and copying it here forks it: the copy goes stale and the
name collides. *"The model already does it"* has a sibling — *"the platform already
ships it"* — and the second is easier to miss, because it looks like something you
could build.

---

## Where this sits, and what is missing

The survey *Externalization in LLM Agents* (arXiv:2604.08224) decomposes a harness
into six dimensions. It is worth stating plainly which of them this harness has,
because most of the rows are now empty, and a row that just said *present* would
hide which part is missing.

The paper is explicit that this is **"an analytical framework for comparing harness
architectures rather than an implementation checklist"** — so the goal is not to
fill every row. It is to know which rows are empty and why.

| Dimension | State here |
|---|---|
| **Skills** | Four, with progressive disclosure. Missing the paper's third attribute: revision driven by observed failure |
| **Verification / Control** | **Absent.** Built and removed on 2026-09-06 — see above |
| **Permission** | **Partial.** Declarative deny/ask rules, not isolation — see below |
| **Protocols** | Inherited via MCP. The hook contract went with the executor. Not designed |
| **Memory** | Semantic: `CLAUDE.md`, the skills and this file, all reviewed. Personalised: the platform's auto-memory, left on and not governed by the harness — see below. Episodic and working context: none |
| **Observability** | **Absent.** The trace was written by the executor and went with it |

Four of six empty is a real result and not a gap to apologise for. It is what the
subtractive definition produces when it is applied honestly, including to the
harness's own work.

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
review, not to remove. The signal that reverses this is in the table below.

Working context, the fourth dimension, is absent on purpose. InfiAgent
(arXiv:2601.03204) keeps files as the state and rebuilds context every step from the
files plus a fixed window of recent actions. Neither half is available here: the
platform's compaction is closed, and with a person in the loop, the person is the
window.

### Deferred, each with the signal that brings it back

| Deferred | Signal |
|---|---|
| The verification layer | See *What was removed*, above — three signals, any one of them |
| Turning auto-memory off | A second model-written instruction steers a session. Then the store is an unreviewed guide layer: off in `settings.json`, with `settings.local.json` as the per-machine opt-in |
| Episodic memory | You catch yourself correcting the same thing a third time |
| Code index | You watch it open eight files to answer one structural question |
| Clean-context reviewer | Reviewing diffs yourself becomes the bottleneck. Note that the platform already ships one; this row is a reminder to check before building |
| Evaluation cases | You are changing rules and cannot tell whether they help |

---

## Design notes

**The definition is subtractive on purpose.** "What the model does not do on its own"
generates a small harness by construction. An earlier version defined it as an
action interface, context selection, a control loop, and guards — three of which are
software, which is why that version generated software.

**The subtraction was eventually applied to the harness's own code.** A verification
layer was built, measured, and cut in one day. That is the definition working rather
than failing, but it is worth naming the asymmetry it exposes: the filter asks
whether the model does something on its own, and *"the model usually does it"* and
*"the model always does it"* are different answers that the filter cannot tell apart.
Everything the removed layer did lived in that gap.

**Rules are curated against defaults, not copied from a style guide.** Most of a
best-practices document confirms what a capable model already does. The parts that
change behavior are the ones that restrain it: prefer duplication over the wrong
abstraction, do not refactor beyond what you touched, do not infer a requirement
that was not stated.

**Asking has a cost curve.** The agent stops for what is expensive to reverse or
whose effect does not show up where the change is — a breaking change that fails
nothing qualifies; a comment you can delete after reading the diff does not. Ask too
often and the questions stop protecting anything, because you stop reading them.
