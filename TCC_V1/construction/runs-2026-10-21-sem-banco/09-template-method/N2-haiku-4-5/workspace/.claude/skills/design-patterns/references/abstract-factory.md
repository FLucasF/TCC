# Abstract Factory

Create whole families of related objects through one interface, so the objects that must match each other always
come from the same family.

## Use it when

- several products must vary together (a button, a checkbox and a menu that all follow the same theme);
- mixing products from different families would be a bug;
- new families are expected.

## Do not use it when

- only one product varies: that is Factory Method;
- there is one family;
- the products are independent of each other.

## Shape

```java
interface WidgetKit {
    Button button(String label);
    Checkbox checkbox(String label);
    Menu menu(List<String> items);
}

final class LightKit implements WidgetKit { /* light versions of the three */ }
final class DarkKit implements WidgetKit { /* dark versions of the three */ }

// the application picks the kit once; screens only use the interface
Screen settings = new SettingsScreen(theme.isDark() ? new DarkKit() : new LightKit());
```

## Done right when

- the family is chosen once, and every product comes from that choice;
- clients never create a concrete product directly;
- adding a family means one new kit.

## Usual mistakes

- A kit for a single product: Factory Method would do.
- Clients still create some products directly, so families can get mixed.
- Adding a new product type to the families is expected often: then every kit changes each time; consider whether
  the families are the right axis.

## Neighbors

- **Factory Method**: one product varies.
- **Builder**: one complex product assembled step by step.
