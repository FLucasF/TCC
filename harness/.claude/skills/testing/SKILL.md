---
name: testing
description: Use when writing, changing, or reviewing tests. Covers what to assert, coupling to implementation, fakes versus mocks, and which boundaries to cover.
---

# Tests

A test suite exists so the code can be changed safely. A test that breaks on every
refactor is not a safety net — it is a brake.

## Test behavior, not implementation

Assert on the public behavior. A test coupled to an internal detail — a private
method, a call order, an intermediate structure — fails when the implementation
improves, which trains people to delete tests instead of trusting them.

The check: if the implementation is rewritten and the behavior is unchanged, the
test must still pass.

## One behavior per test

The name describes the behavior, not the method:
`rejects_withdrawal_above_balance`, not `test_withdraw_2`.

When a test needs "and" in its name, it is two tests.

## Prefer fakes over mocks

A fake is a real but simplified implementation — an in-memory repository, a
deterministic clock. It tests through the same contract the production code uses.

Mocks assert on interactions, which couples the test to how the code calls its
collaborators rather than to what it produces. Heavy mocking gives coverage
numbers without the protection they imply.

## Determinism

No randomness, no current date, no network in a unit test. Inject the clock and
the generators — `now()` inside the code is a hidden dependency, and a test that
depends on it fails at midnight or in another timezone.

A test that fails intermittently must be fixed or quarantined. A tolerated flaky
test destroys confidence in the whole suite.

## Boundaries to cover

Empty, one, many. Zero, negative, maximum. Null and absent — which are different.
Special characters, timezone edges, and concurrent access where it applies.

## Every fixed bug gets a test

The test must fail before the fix and pass after. This is the only mechanism that
prevents the bug from returning, and it is the one most often skipped once the fix
is found.

## Coverage

Coverage locates untested areas. It is not a target: full coverage with weak
assertions protects nothing.
