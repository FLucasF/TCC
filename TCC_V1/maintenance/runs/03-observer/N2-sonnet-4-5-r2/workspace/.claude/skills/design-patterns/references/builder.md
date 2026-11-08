# Builder

Assemble one complex object step by step, with names for each part, instead of a constructor with many parameters.

## Use it when

- an object has many parts, most of them optional, and constructors with long parameter lists are hard to read;
- the object must be valid as a whole, and the checks are easier to do once, when it is built;
- the same assembly steps produce different representations.

## Do not use it when

- the object has a few required fields: a constructor or a record is clearer;
- the object is plain data created in one place;
- the real question is which type to create: that is Factory Method.

## Shape

```java
final class HttpRequest {
    // fields, private constructor
    static Builder to(URI uri) { return new Builder(uri); }

    static final class Builder {
        private final URI uri;
        private String method = "GET";
        private final Map<String, String> headers = new LinkedHashMap<>();
        private Duration timeout = Duration.ofSeconds(30);

        Builder method(String method) { this.method = method; return this; }
        Builder header(String name, String value) { headers.put(name, value); return this; }
        Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        HttpRequest build() { /* validate the whole, then create */ }
    }
}

HttpRequest request = HttpRequest.to(uri).method("POST").header("Accept", "text/plain").build();
```

## Done right when

- callers name the parts they set and skip the rest;
- the built object is complete and valid, or `build()` refuses it;
- the object itself stays immutable.

## Usual mistakes

- A builder for an object with two or three fields: ceremony with no gain.
- A builder that only forwards to setters on a mutable object.
- Builders added "for consistency" to every data class.

## Neighbors

- **Factory Method**: decides which type; Builder decides how to assemble one.
- **Records with defaults**: often enough in modern Java for simple data.
