# Factory Method

Put the decision of which concrete class to create in one place, so every caller that needs the object asks that
place instead of choosing with its own `if`.

## Use it when

- several concrete types share an interface, and the right one depends on some input (a kind, a level, a format);
- more than one place in the code needs to create them, and each would otherwise repeat the choice;
- new types are expected.

## Do not use it when

- every caller knows exactly which class it needs: call the constructor;
- there is only one concrete type;
- the object is complex to assemble but the type is always the same: that is Builder.

## Shape

Three accepted forms; pick the one that fits the code.

```java
// 1. Classic: a creator whose subclasses override the creation step
abstract class Level {
    abstract Enemy spawnEnemy(Position where);          // the factory method
    void populate(List<Position> spots) { spots.forEach(p -> add(spawnEnemy(p))); }
}
final class ForestLevel extends Level { Enemy spawnEnemy(Position p) { return new Wolf(p); } }
final class CaveLevel extends Level { Enemy spawnEnemy(Position p) { return new Bat(p); } }

// 2. A concentrated factory: one class or method that maps the input to the type
final class Enemies {
    static Enemy spawn(Terrain terrain, Position p) {
        return switch (terrain) {                        // the only place that chooses
            case FOREST -> new Wolf(p);
            case CAVE -> new Bat(p);
            case SWAMP -> new Slime(p);
        };
    }
}

// 3. Polymorphic creation on the input itself
sealed interface Terrain permits Forest, Cave, Swamp { Enemy spawnEnemy(Position p); }
```

## Done right when

- the choice of the concrete type is made in exactly one place;
- every place that needs the object asks that place, including the places that existed before the new types;
- no caller builds one of the types by hand "just this once";
- adding a type means one new class and one entry in the factory.

## Usual mistakes

- The same `if`/`switch` on the input appears in each place that creates the object, even if each branch calls a
  helper: the choice is still repeated.
- A factory exists, but one caller still creates the object directly.
- The factory always returns the same type, and the real choice happens afterwards, outside it.

## Neighbors

- **Abstract Factory**: several related products that must vary together.
- **Builder**: how to assemble one complex object, not which type to create.
- **Strategy**: the created objects are often strategies; the factory is just where they are chosen.
