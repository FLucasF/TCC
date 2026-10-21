package pousada.identity;

import java.util.List;
import java.util.Map;

/** Uma forma de identificação aceita na criação da reserva. */
public record IdentityKind(List<String> fields, java.util.function.Function<Map<String, String>, GuestIdentity> factory) {
    public GuestIdentity create(Map<String, String> values) {
        return factory.apply(values);
    }
}
