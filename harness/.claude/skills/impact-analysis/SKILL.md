---
name: impact-analysis
description: Use when answering "what breaks if I change this" — which callers, files or screens depend on a symbol, field, endpoint or component. Routes the question to the code graph or to search, because each one is wrong for what the other answers.
---

# What breaks if I change this

## The routing rule

Two tools, and neither subsumes the other. Pick by what the thing *is*, not by how
the question is phrased.

| the thing you are changing | tool |
|---|---|
| a declared symbol — function, class, method, component | `graphify affected "X"` |
| a field of a DTO or response body | search |
| an HTTP route, a query string, any string literal | search |
| a CSS class name | search |
| a method called on a value another call returned | search |

The split is not about size. It is about whether the thing is a node in the graph.
A field is one level below the smallest node, so the graph cannot see it at all.

## Why not always search

Search answers by name, so it misses the caller that renamed what it received and
it over-reports the caller that only shares a module.

Both failures measured on one project. Searching by the name a value is usually
bound to, `grep "feedback\."` returned 18 uses of a React hook's result; the real
number was 22, because two files bind it to `warnings` and `planWarnings` instead.
And asked what depends on `toIso`, the search named six files while only four
actually do — the other two import a sibling function that never calls it.

The graph fixes the second failure and not the first. `graphify affected "toIso"`
cost 1213 bytes against 4701, one invocation against four, and excluded the two
files that do not break. `graphify affected "useFeedback"` found all 18 sites where
the hook itself is called, each labelled with the enclosing component, in 2298 bytes
against 2520 — including the two the search's first pass missed. But the 22 calls on
the object the hook *returns* are invisible to it entirely; that is trap 2 below,
and it is why the routing table sends those to search.

## Why not always the graph

Because for anything that is not a node it returns nothing, and nothing looks like
an answer.

Measured on the same project, asked what breaks if a response field changes:

```
$ graphify affected "emitidoAt"
No unique node match for emitidoAt
```

The field crosses Java, JSON and TypeScript. One search found the whole chain — the
record component, the mirrored TypeScript type, the screen that renders it — in
285 bytes.

## The command

```
graphify affected "<symbol>" --depth 2
```

Start at 2 and raise until the count stops growing. Depth 1 is direct callers only,
which hides the real consumers whenever the symbol is wrapped: `toIso` reports 4 at
depth 1 — its own siblings in the same file — and 17 at depth 2, where the screens
that actually break appear. Measured stabilisation points: 19 at depth 4 for
`toIso`, 83 at depth 3 for a symbol every page touches.

If `graphify` is not installed, **say so and use search**, naming the limitation in
the answer. Do not present a search result for a symbol as if it were the graph's.
`sh scripts/install-graphify.sh` installs the pinned version — offer it and let the
user decide; never run it to close the gap on your own.

## Check freshness before trusting it

The graph is a file. It does not know the code moved.

```
find <source-roots> -type f -newer graphify-out/graph.json
```

Any output means the graph is behind. Rebuilding costs about 9.5 seconds:

```
graphify update . --no-cluster
```

`update` is not cheaper than a full build — measured 9468 ms after one file changed
against 9202 ms from cold, because the cache skips parsing and not the resolution
pass that follows. So rebuild once when the check says to, and never reflexively
between questions.

If the graph is stale and rebuilding is not appropriate right now, say the graph is
stale and answer with search instead. A stale graph is worse than no graph, because
it answers confidently.

The freshness check above catches changed code and nothing else. The cache is keyed
by file content and by the tool's version, so **adding or removing a language
grammar does not invalidate it** — measured: after uninstalling the SQL grammar,
two rebuilds reported "No code-graph changes detected" and kept 49 stale nodes,
`--force` included, because the unchanged `.sql` files still hit their cache
entries. Deleting `graphify-out/cache` and rebuilding dropped them. Do that
whenever the set of installed extractors changes.

## Three traps, all measured

**A line in the output is a declaration, not a call site.** A call inside a nested
function is attributed to every scope that encloses it, so the raw line count
overstates the number of places to edit. Measured: `explainError` reported 62
`[calls]` lines at depth 1 against 48 real call sites, because a page declares
`send()` inside a component and the call inside `send()` is emitted for both.

Deduplicate by `file:line` before reporting a count — that produced exactly 48,
matching the search. A symbol with no nested declarations is unaffected: another
gave 18 raw, 18 deduplicated, 18 by search.

**A missing edge is indistinguishable from a missing relationship.** The graph has
no way to say "I could not resolve this". Measured: 22 call sites of `.confirm()`
and `.warn()` on a hook's return value produced **zero** edges, and a 2277-node
graph of a Spring backend and a React frontend contained **zero** nodes carrying an
HTTP route — annotations and string literals are never read.

So never conclude *this endpoint has no consumer* or *nothing calls this* from an
empty result. Confirm with a search before reporting absence.

**The graph does not cross the HTTP boundary.** In a project where a frontend, a
test suite and an external client all reach the backend by URL, none of those three
edges exists in the graph. Impact that travels over HTTP has to be traced by
searching for the path segment on both sides.

## Report the impact; do not change it

State what depends on the thing and let the user decide. Say which tool produced
the answer and what it could not see:

> `toIso` has 19 impact points (graph, depth 4 — the count stops growing there),
> across `dates.ts` and 4 pages. The values leave over HTTP as `LocalDate` query
> params; that edge is not in the graph and I did not trace it.

The second sentence is the part that matters. An impact answer without its blind
spot named is a claim of completeness you cannot support.
