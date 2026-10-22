# Chain of Responsibility

Pass a request along a sequence of handlers; each one either handles it, or passes it on, or stops it.

## Use it when

- several handlers could deal with a request, and which one does depends on the request;
- the set of handlers or their order changes per configuration;
- new handlers are expected, and today each one would add a branch to a central method.

## Do not use it when

- there is always exactly one handler: call it;
- every handler must always run around the same operation and add something: that is closer to Decorator;
- the order and the set never change and fit in one short method.

## Shape

```java
interface ClickHandler {
    boolean handle(Click click);           // true when handled; false to let the next one try
}

final class HandlerChain {
    private final List<ClickHandler> handlers;      // the order is decided where the chain is built
    HandlerChain(List<ClickHandler> handlers) { this.handlers = List.copyOf(handlers); }

    void dispatch(Click click) {
        for (ClickHandler handler : handlers) {
            if (handler.handle(click)) return;
        }
    }
}

final class ButtonHandler implements ClickHandler { /* handles clicks inside the button */ }
final class PanelHandler implements ClickHandler { /* handles what the button did not */ }
final class WindowHandler implements ClickHandler { /* the last resort */ }
```

A linked form (each handler holds `next`) is the same pattern.

## Done right when

- each handler is its own piece and decides only about its own case;
- the list (or links) is built in one place, where the configuration is known;
- the dispatcher knows no handler by name;
- adding a handler means one new class and one change where the chain is built.

## Usual mistakes

- Handlers check a global setting to decide whether they are part of the chain, instead of the builder leaving them
  out.
- The dispatcher still has special cases for some requests before the chain.
- A handler depends on a flag set by a previous one in a hidden way.

## Neighbors

- **Decorator**: every wrapper runs and adds behavior; in a chain, usually one handler takes the request.
- **Strategy**: one rule chosen up front; a chain tries handlers in order.
