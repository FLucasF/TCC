# Facade

Offer one simple entry point to a subsystem whose parts clients would otherwise have to call together, in the right
order, every time.

## Use it when

- clients repeat the same sequence of calls on several subsystem objects;
- the subsystem's details change, and clients should not;
- most clients need only a small part of what the subsystem can do.

## Do not use it when

- the "facade" would only forward each call to one object: it adds a layer and hides nothing;
- there is a single client and a single short sequence.

## Shape

```java
final class VideoConverter {                              // the facade
    private final Demuxer demuxer;
    private final Decoder decoder;
    private final Encoder encoder;
    private final Muxer muxer;

    Path convert(Path input, Format target) {
        var streams = demuxer.split(input);
        var frames = decoder.decode(streams.video());
        var encoded = encoder.encode(frames, target.codec());
        return muxer.join(encoded, streams.audio(), target);
    }
}
```

## Done right when

- clients call the facade and stop knowing the subsystem's order and parts;
- the subsystem can still be used directly by the few clients that need more.

## Usual mistakes

- A service layer that forwards every method one to one: it is a pass-through, not a facade.
- The facade grows into a class that does everything, including rules that belong in the subsystem.

## Neighbors

- **Adapter**: converts an interface; Facade simplifies one.
- **Mediator**: coordinates peers that talk to each other; a facade is called from outside.
