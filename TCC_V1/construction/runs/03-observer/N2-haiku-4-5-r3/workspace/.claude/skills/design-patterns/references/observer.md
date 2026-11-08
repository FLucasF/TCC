# Observer

Let an object announce that something happened, and let any number of listeners react, without the announcing object
knowing who they are.

## Use it when

- one event must trigger several independent reactions (update a display, raise an alarm, write a log);
- new reactions to the same event are expected, and today each one would mean editing the code that raises it;
- the reactions belong to other parts of the system, and the source should not depend on them.

## Do not use it when

- there is exactly one reaction and nothing suggests more: a direct call is clearer;
- the reactions must happen in a strict order with results that feed each other: that is a sequence of calls, or a
  pipeline;
- the "listener list" would always contain the same single object.

## Shape

```java
interface ReadingListener {
    void onReading(SensorReading reading);
}

final class WeatherStation {
    private final List<ReadingListener> listeners;           // received from outside
    WeatherStation(List<ReadingListener> listeners) { this.listeners = List.copyOf(listeners); }

    void record(SensorReading reading) {
        // … store the reading …
        for (ReadingListener listener : listeners) listener.onReading(reading);
    }
}

final class Display implements ReadingListener { /* show it */ }
final class FrostAlarm implements ReadingListener { /* only reacts below zero: the filter is its own */ }
final class History implements ReadingListener { /* keep it */ }

// the wiring of the application decides who listens
WeatherStation station = new WeatherStation(List.of(new Display(), new FrostAlarm(), new History()));
```

## Done right when

- the source only announces the event; it builds no reaction and names no listener;
- each reaction is its own listener, with its own rules about when to act;
- the reactions that existed before are listeners too, not direct calls next to the new ones;
- the list of listeners is assembled outside the source, where the application is wired;
- adding a reaction means one new listener and one line where they are registered.

## Usual mistakes

- One central class that receives the event and performs every reaction itself: the code moved, the coupling did
  not.
- The source still builds the content of some reactions and only delegates the others.
- The listener list is created inside the class that dispatches the events, so adding a listener still means editing
  it.
- Only some events go through listeners, and others are still handled by direct calls.

## Neighbors

- **Mediator**: coordinates objects that talk to each other in both directions; Observer is one-way.
- **Command**: when the reaction must be queued, retried or undone.
