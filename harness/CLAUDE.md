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

Validate at the boundary, not in every layer, and fail immediately. Never swallow
an error silently.

Every call that leaves the process has a timeout. No exceptions — the default
without one is infinite.

## Verification

Never invent a build, lint, or test command — use what the project itself declares,
in its `pom.xml`, `package.json` or equivalent. When a part of the project has no
check of its own, say so instead of running another part's suite and reporting it
as cover.

## Searching

Choose the output by what the answer is for: a file list when the question is scope,
a count when it is quantity, matching lines only when you will read the lines. The
same question answered in content mode costs an order of magnitude more.

## Contracts

Before changing the unit, semantics, default ordering, default value, or accepted
value set of any field consumed by another boundary: say that this is a breaking
change, even when nothing fails and no signature changed. Then ask whether to
change both sides or preserve compatibility.

## Communication

Explain the change in your reply, not in the code — the outcome and what it costs
the caller, never a narration of the steps taken or a restatement of what the diff
already shows. Leave a comment in the source only for a non-obvious external
constraint that outlives this session — the kind nobody rediscovers by reading the
code. Never leave commented-out code.
