# Strategy

Put each variant of a rule in its own object behind one interface, so the code that uses the rule never names the
variants.

## Use it when

- several variants compute or decide the same thing in different ways (a route, a score, a compression, a sort order);
- the variant is chosen per use (by a field, a request, a setting) and does not change on its own afterwards;
- new variants are expected, and today each one would add a branch in more than one place.

## Do not use it when

- the variants differ only in data (a rate, a limit): keep the data in a table or an enum field;
- there are two variants and nothing suggests a third;
- the variants share a fixed sequence of steps and differ inside it: that is Template Method.

## Shape

```java
interface RoutePlanner {
    Route plan(Point from, Point to, MapData map);   // everything a variant needs comes in here
}

final class FastestRoute implements RoutePlanner { public Route plan(Point a, Point b, MapData m) { /* … */ } }
final class ShortestRoute implements RoutePlanner { public Route plan(Point a, Point b, MapData m) { /* … */ } }
final class ScenicRoute implements RoutePlanner { public Route plan(Point a, Point b, MapData m) { /* … */ } }

final class RoutePlanners {                          // the one place that knows the variants
    private final Map<String, RoutePlanner> byName = Map.of(
        "fastest", new FastestRoute(), "shortest", new ShortestRoute(), "scenic", new ScenicRoute());
    RoutePlanner named(String name) { /* look up, reject unknown names */ }
}

// the client only talks to the interface
Route route = planners.named(request.mode()).plan(request.from(), request.to(), map);
```

An `enum` whose constants each carry their own implementation (an abstract method overridden per constant, or a
lambda passed to the constructor) is the same pattern. A `switch` that only picks the implementation is fine; a
`switch` that holds the behavior is not.

## Done right when

- every variant, including the ones that existed before the new ones, is an implementation of the interface;
- the rule of each variant (its calculation, its validation, its exceptions) lives inside that variant;
- the client calls the interface and never checks which variant it holds;
- adding a variant means one new class and one registration.

## Usual mistakes

- Only the new variants become classes, and the old ones stay in a `switch` next to them.
- The interface is too narrow for one variant, so the client special-cases it (`if (planner instanceof ScenicRoute)`)
  to pass extra data. Widen what the variants receive instead.
- Each variant delegates back to a shared helper that still switches on the variant.
- An enum gains methods that `switch (this)`: the branching moved, it did not go away.

## Neighbors

- **State**: the object changes its own variant over time. Strategy is chosen from outside and stays.
- **Template Method**: the variants share a skeleton and differ in some steps.
- **Command**: packages a request to run later; Strategy packages a rule to run now.
