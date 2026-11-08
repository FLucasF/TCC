# Adapter

Wrap an object whose interface does not match what your code expects, translating calls, names and units in one
place.

## Use it when

- you depend on a library, a legacy module or an external service with its own terms (names, units, error codes);
- the rest of your code should speak its own terms and not leak the other side's;
- the other side may be replaced, or several of them must look the same to your code.

## Do not use it when

- the external interface already matches yours;
- the translation is a single conversion used in one place.

## Shape

```java
interface Thermometer {                 // what our code expects
    double celsius();
}

final class LegacyProbeAdapter implements Thermometer {
    private final LegacyProbe probe;    // the other side, with its own terms
    LegacyProbeAdapter(LegacyProbe probe) { this.probe = probe; }

    public double celsius() {
        int raw = probe.readTenthsOfFahrenheit();         // their unit
        return (raw / 10.0 - 32) * 5 / 9;                 // translated here, and only here
    }
}
```

## Done right when

- the other side's names, units and errors appear only inside the adapter;
- the rest of the code uses your interface;
- replacing the other side means a new adapter, not edits across the code.

## Usual mistakes

- The adapter returns the other side's types, so the leak continues past it.
- Some calls go through the adapter and others call the external object directly.
- Translation of units or error codes scattered across callers.

## Neighbors

- **Facade**: simplifies a big interface; Adapter converts one interface into another.
- **Decorator**: keeps the interface and adds behavior.
