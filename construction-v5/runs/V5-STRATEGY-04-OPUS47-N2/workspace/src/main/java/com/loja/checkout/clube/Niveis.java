package com.loja.checkout.clube;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class Niveis {
    private static final Map<String, Supplier<NivelClube>> REGISTRY = Map.of(
            "BRONZE", Bronze::new,
            "PRATA", Prata::new,
            "OURO", Ouro::new
    );

    private Niveis() {}

    public static Optional<NivelClube> por(String codigo) {
        if (codigo == null) return Optional.empty();
        var factory = REGISTRY.get(codigo);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }
}
