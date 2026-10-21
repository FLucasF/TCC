# Decorator

Add behavior around an operation by wrapping the object in other objects with the same interface, each adding one
thing, so the behaviors can be combined freely.

## Use it when

- several optional behaviors go around the same operation (buffering, compression, encryption, retry, logging);
- different configurations need different combinations of them;
- more such behaviors are expected, and today each one would add another `if` inside the operation.

## Do not use it when

- there is a single extra behavior that always applies: put it in the operation;
- the behaviors do not share the operation's interface;
- only the input or output format changes once: that may be an Adapter, or plain code.

## Shape

```java
interface DataSink {
    void write(byte[] data);
}

final class FileSink implements DataSink { /* the real operation */ }

final class Compressing implements DataSink {
    private final DataSink next;
    Compressing(DataSink next) { this.next = next; }
    public void write(byte[] data) { next.write(compress(data)); }
}

final class Encrypting implements DataSink { /* same shape: transform, then call next */ }
final class Counting implements DataSink { /* calls next, then counts the bytes */ }

// the assembly decides the combination, once
DataSink sink = settings.archive()
    ? new Counting(new Encrypting(new Compressing(new FileSink(path))))
    : new Counting(new FileSink(path));
```

## Done right when

- each behavior is a wrapper that does one thing and calls the next;
- the real operation knows nothing about the wrappers;
- the combination for each configuration is decided where the objects are assembled;
- adding a behavior means one new wrapper and a change in the assembly only.

## Usual mistakes

- A wrapper reads a configuration flag to decide whether it should act: the combination is back inside the pieces,
  and the assembly no longer says what runs.
- The innermost operation still checks the configuration.
- One of the behaviors stays as an `if` in the operation while the others are wrappers.
- The wrapper changes the interface (adds methods the clients must call): then it is not transparent anymore.

## Neighbors

- **Chain of Responsibility**: a sequence of handlers where one usually takes the request and stops. A chain or a
  list of filters is a fine alternative to Decorator when every piece is separate and the assembly decides which
  pieces are used.
- **Adapter**: changes the interface, keeps the behavior. Decorator keeps the interface, adds behavior.
- **Proxy**: same interface, but controls access to the object instead of adding behavior.
