---
name: commit
description: Use when writing a commit message, or when deciding how to split a set of changes into commits. Covers atomicity, what belongs in the body, and separating refactoring from behavior.
---

# Commits

The default failure is one large commit mixing behavior, refactoring, and
formatting, with a message describing what the diff already shows.

## Atomicity

One commit is one logical change, complete and coherent. If the description needs
an "and", it is probably two commits.

Refactoring goes in its own commit, separate from behavior change. This is what
makes review, `bisect`, and `revert` possible. A commit that both moves code and
changes what it does cannot be reverted without losing one or the other.

Formatting-only changes go in their own commit too, so they never hide a behavior
change inside noise.

## The message

Subject in the imperative, lowercase, no trailing period. It completes the
sentence "if applied, this commit ___".

The body explains **why**, and what the alternative was. The diff already shows
what changed; only the message can carry the intent, the discarded option, and
the known side effect.

```
fix(payments): treat gateway timeout as retryable

The gateway returns 504 under load and the client was marking the order
as declined, producing incorrect refunds. The timeout now enters the retry
queue with backoff.

Refs: #2291
```

## Format

Follow the convention already in the repository. If the project uses Conventional
Commits, the type prefix is verified by tooling — do not spend effort on the
format, spend it on atomicity and on the body, which no tool can check.

## When to break these

A merge commit, a generated changelog bump, or a mechanical rename across many
files does not need a body explaining why. Say so briefly rather than inventing
justification.
