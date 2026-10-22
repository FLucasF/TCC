package br.com.mare.pousada;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Um jeito de o hóspede se identificar: quais campos do corpo ele usa e como vira uma identificação. */
public record IdentityKind(String name, List<String> fieldNames, Function<Map<String, String>, GuestIdentity> factory) {
    public GuestIdentity create(Map<String, String> values) {
        return factory.apply(values);
    }
}
