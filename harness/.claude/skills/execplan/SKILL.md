---
name: execplan
description: Use when a feature or refactor is large enough to outlive one session, or when the design has unknowns worth de-risking first. Covers writing the plan as a self-contained file and keeping it current while implementing it.
---

# ExecPlans

An ExecPlan is a design document a coding agent can execute. It is a file in the
repository, not a proposal in the conversation.

Two default failures it exists to prevent. The plan lives in the context window
and dies with it, so the next session starts over from the code. And the plan is
written once and never touched again, so by the third milestone it describes work
that no longer matches what was built.

## When a plan earns its place

The work spans more than one session, or the design has unknowns worth proving
before committing to them. A change that fits in your head does not need a plan —
say that and do the work.

## The file

`docs/plans/<short-name>.md`, relative to the working directory. One plan per
file, plain Markdown, no enclosing code fence. The skeleton is `template.md`, next
to this file.

The test that decides whether it is finished: someone holding **only the current
working tree and this one file** can produce the working result. That reader has
no memory of this conversation and no other context.

So there is no "as decided earlier" and no link to a blog, an internal doc, or a
prior chat. Knowledge the reader needs goes into the plan in your own words, even
when it repeats something. If it builds on a plan that is checked in, reference
that path; if the earlier plan is not checked in, copy in what is needed.

Define every term of art the first time it appears, and say where it shows up in
this repository — the file, the command, the module.

## Purpose before mechanism

Open with what someone can do after the change that they could not do before, and
how to see it working. The steps come after that.

Acceptance is phrased as behavior a human can verify — "start the server, POST
`/orders` with a negative amount, get HTTP 400 and body `amount must be positive`"
— not as an internal attribute like "added an `OrderValidator`". When the change
is internal, prove it with a named test that fails before and passes after, and
give the project's own test command.

## The living sections

Four sections, all mandatory, all updated **while** the work happens:

- **Progress** — checkboxes with timestamps. Every stopping point is recorded,
  splitting a partly done task into what is done and what remains. This is the one
  place in the plan where a checklist belongs.
- **Decision Log** — decision, rationale, date. Every course change, including the
  ones that contradict an earlier part of the plan.
- **Surprises & Discoveries** — unexpected behavior, with short evidence. Test
  output is the best kind.
- **Outcomes & Retrospective** — at each milestone and at the end, compared against
  the purpose you opened with.

A plan updated only at the end is a report. The point of these sections is that a
fresh agent can restart from the file alone, and that only works if the file is
current at every stopping point.

## Milestones

Each milestone leaves something working that did not exist before, and is
verifiable on its own. Introduce it in a paragraph — goal, work, result, proof —
rather than as a bare list of tasks.

When the design has real unknowns, spend a milestone on a prototype instead of
guessing. Label the scope as prototyping, say how to run it and what to observe,
and state up front what would promote it and what would discard it. When several
unfamiliar libraries are involved, prove them one at a time, in isolation, before
any of them carries weight.

## Prose, not bureaucracy

Sentences over lists everywhere except Progress. A table or an enumeration is
worth it only when it is genuinely shorter than the paragraph it replaces.

## Autonomy, and where it stops

While executing, do not halt at each milestone to ask what comes next. Move to the
next one and keep Progress current. Execution choices — ordering, file names, the
shape of a test — are yours to resolve; record them in the Decision Log.

This does not lift the rule in `CLAUDE.md`. A missing requirement — a field, a
rule, an authorization, a contract — is still a stop-and-ask, inside a plan exactly
as outside one. The Decision Log is for decisions that were yours to make, not a
place to log a guess at something nobody defined.

## With plan mode

Plan mode holds the tools read-only and gates on your approval. That is the
platform enforcing it, which prose cannot do — use it to arrive at the plan.

Approval ends plan mode; it does not leave a file behind. This skill is what makes
the result outlive the session.

## Revising a plan

A revision propagates through every section, the living ones included — a plan
whose Purpose and Decision Log disagree has stopped being self-contained. Add a
note at the bottom saying what changed and why.

## When to break these

A single-file fix, a rename, a dependency bump: no plan. Say that is the reason
rather than producing a document to satisfy the form.
