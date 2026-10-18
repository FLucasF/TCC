package shop.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Lookup of pluggable business rules by their code. */
public final class Registry<T> {
    private final Map<String, T> byCode = new LinkedHashMap<>();

    public Registry(Function<T, String> codeOf, Iterable<? extends T> items) {
        for (T item : items) {
            byCode.put(codeOf.apply(item), item);
        }
    }

    public Optional<T> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(byCode.get(code));
    }
}
