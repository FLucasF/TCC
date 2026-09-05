# Working rules

Every rule here contradicts a default behavior. Practices a capable model already
follows on its own are deliberately absent: repeating them costs context and weakens
the rules that remain.

## Scope

Improve only what you touched. Do not refactor outside the scope of the request.
Do not add a dependency, abstraction, configuration, or behavior the request does
not require.

## Authority

The explicit request is the authority on behavior. Code, README, examples, and
similar features are not sources of requirement.

When a material definition is missing — a field, a rule, an authorization, a
contract — stop and ask only for what is missing. Do not infer it and do not
proceed on a plausible guess.

## Design

Prefer duplication over the wrong abstraction; extract on the third occurrence,
not the first. Do not create a hierarchy, interface, or extension point before
real variation exists.

## Correctness

Validate at the boundary and fail immediately. Never swallow an error silently.

Every call that leaves the process has a timeout. No exceptions — the default
without one is infinite.

## Verification

Run the commands declared in the project manifest (`.claude/validation.json`).
Never invent a build, lint, or test command, and never substitute one boundary's
suite for another's.

Every bug you fix gets the test that failed before the fix.

Unit tests use no randomness, no current date, and no network. Inject the clock
and the generators.

## Contracts

Before changing the unit, semantics, default ordering, default value, or accepted
value set of any field consumed by another boundary: say that this is a breaking
change, even when nothing fails and no signature changed. Then ask whether to
change both sides or preserve compatibility.

## Communication

Explain the change in your reply, not in the code. Leave a comment in the source
only for a non-obvious external constraint that outlives this session — the kind
nobody rediscovers by reading the code. Never leave commented-out code.
