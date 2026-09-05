---
name: error-handling
description: Use when handling errors, exceptions, or failures — writing catch blocks, choosing error types, designing retries, or writing messages a user or another system will read.
---

# Errors

## Handle where there is context to decide

Low layers propagate; the boundary decides what to do — retry, fall back, or
surface to the user. A `catch` in the middle that logs and continues destroys the
information the caller needed.

Never swallow an error silently. An empty catch block is a delayed failure whose
symptom will appear somewhere with no connection to the cause.

## Preserve the root cause

When re-raising, chain the original: `raise ... from e`, `throw new X(msg, cause)`,
`fmt.Errorf("...: %w", err)`. A new exception that discards the original replaces
a diagnosis with a guess.

## Expected is not exceptional

"Invalid document number" is normal flow and deserves a return type — `Result`,
`Either`, an error value. "Disk full" is exceptional.

Using exceptions for expected outcomes makes ordinary paths invisible in the
signature and expensive at runtime.

## Messages

Say what failed, with which value, and what to do:

```
Failed to process order 8891: amount is negative (-5.00)
```

Not `"error"`, and not the raw stack.

Never leak internals to an end user — stack trace, SQL, file path, dependency
name. Log the full detail, return a short message plus a correlation id.

## Resilience

Retry only transient failures, only on idempotent operations, with exponential
backoff and jitter. Retrying a non-idempotent write duplicates the effect.

Operations that can be repeated — by retry, by message redelivery — need an
idempotency key.

Prefer graceful degradation: showing the cart without recommendations beats
failing the page.

## When to break these

A script you run once, in front of you, does not need chained causes and
correlation ids. Say that is the reason rather than applying the full treatment
by reflex.
