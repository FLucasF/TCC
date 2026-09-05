---
name: api-change
description: Use when changing anything another boundary consumes — an HTTP endpoint, a JSON field, a message format, a public function signature, or a shared type. Covers what breaks silently and how to evolve without breaking.
---

# Changing an API

API here means any promise your code makes to other code: an endpoint, a field
name, a queue message, a library signature, a shared type. The cost of breaking
one is paid by someone else, which is why it needs a rule.

## The definition that matters

> If the consumer has to change their code to keep working, it is breaking —
> even when no signature changed and no tool reports a difference.

The intuitive definition ("I changed the shape") is too narrow. Use the
consumer's point of view.

## What breaks silently

These are the dangerous ones, because nothing fails — the system just becomes
wrong.

**Unit or semantics.** `amount` returned `12.34` meaning 12 currency units and 34
cents; it now returns `1234` meaning cents. Same name, same type. The consumer
parses it without error and displays a value 100 times too large. No type check,
no contract test, and no grep catches this.

**Default ordering.** A list that always came newest-first now comes oldest-first.
The contract is byte-identical. Every consumer showing "latest items" is now wrong.

**Default values and page size.** Same class: the shape holds, the meaning moves.

**A new enum value.** Adding `REFUNDED` to a status is safe only if consumers are
documented to ignore values they do not recognize. Without that rule written into
the contract, a `switch` with no default branch throws on the new value.

## What is safe

Adding a new endpoint. Adding an optional request field with a default. Adding a
response field — provided the contract states that consumers must ignore unknown
fields. If that rule is not written down, it is not safe.

## Before changing

Say it is breaking, then ask whether to change both sides or preserve
compatibility. Do not choose silently.

Check who consumes the field before assuming nobody does. If the manifest declares
`consumedBy`, the consumer's validation runs too — a failing type check there is
the strongest signal available, because it tests real consumption rather than a
document.

## Evolving without breaking

Prefer additive change. Version only when you must break, and never let two
versions live forever — set a shutdown date when the second one is born.

Deprecating is not removing: mark it, keep it working, announce a window, measure
who still calls it, then remove.

## Generated types drift

When consumer types are generated from the producer's contract, they are only as
fresh as the last generation. Committed generated code that nobody regenerated
type-checks perfectly against a contract that no longer exists.

Never hand-edit a generated file to make a type check pass — fix the source and
regenerate. And remember that types erased at runtime prove nothing about the data
actually sent: validate at the boundary.
