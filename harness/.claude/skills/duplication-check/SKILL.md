---
name: duplication-check
description: Use before creating a new function, helper, or utility longer than a few lines — checks whether equivalent code already exists in the project. Also use when asked to find or reduce duplication.
---

# Checking whether the code already exists

## Why not just search

Searching by name does not work for this. You will search for the name *you* would
have chosen, and the code that already exists is probably called something else.

A measured example: a string normalizer already existed as
`Food.normalizeToSearch`, a public static method living inside a domain entity of
a different module. Two importers in other modules reimplemented it. No search for
`normalize` finds `normalizeToSearch` in `food/domain/`.

`jscpd` compares token structure with identifiers stripped, so it matches code that
does the same thing under a different name. That is the property a search does not
have.

## The command

```
jscpd <source-root> --min-tokens 100 --reporters ai
```

- Point it at the source root, never at the repository root — `node_modules`,
  `target`, `dist` and `build` produce garbage.
- Exclude the test tree. Duplication between tests is usually the same arrange
  block repeated, which is correct: a test should read without chasing helpers.
- `--min-tokens 100` is calibrated, not arbitrary — see the trap below.
- `--reporters ai` is the compact format. The console format costs several times
  more tokens for the same facts.

If `jscpd` is not installed, **say so and stop**. Do not silently skip the check,
and do not fall back to a grep: a grep that finds nothing is not evidence that
nothing exists, and reporting "no duplication found" on that basis is worse than
reporting nothing.

## Two traps, both measured on a real codebase

**A reported pair understates the real count.** jscpd reports pairs, not families.
When it says A duplicates B, the honest next step is to grep for a distinctive
token from that block and count the real occurrences. Measured on one project: a
helper reported as a single pair existed in four places; a UI fragment reported as
five pairings existed in thirteen.

Never report the pair count as the size of the problem.

**Raising `--min-tokens` filters backwards in some languages.** In Java, an import
block is twenty-plus lines and many tokens, while a genuinely duplicated method may
be seventeen lines and fewer. Measured: at `--min-tokens 150` the real finding
disappeared and every survivor was an import block. Higher is not stricter — it is
differently wrong. Leave the value alone unless you re-measure.

File headers, import blocks, and framework boilerplate are noise. Ignore them.

## After a finding

Read **the line range it points at**, not the file. The report gives
`file:start-end` precisely so the read stays narrow — that is the entire economic
argument for using the tool.

Then classify. Three outcomes, and they are not equally common:

1. **Reuse what exists** — the semantics match. Most common.
2. **Keep them separate** — the code looks alike but changes for different reasons.
   Two validators for different entities may diverge tomorrow; unifying them now
   creates false coupling. This is a correct outcome, not a failure to refactor.
3. **Extract to a shared location** — only from the third occurrence onward, and
   only if a shared home already exists or clearly belongs.

## Report; do not refactor

State the finding and let the user choose. Refactoring both call sites into a
shared helper changes code you were not asked to touch, which violates the scope
rule that applies to every task.

Say it like this:

> This duplicates `FoodsTableReader.java:298-314`. That logic appears in 4 places
> and there is already a `shared/util/` package. Reuse the existing one, extract to
> shared, or keep this separate?

Then do what the answer says.
