package com.loja.checkout.cupom;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class Cupons {
    private static final Map<String, Supplier<Cupom>> REGISTRY = Map.of(
            "BEMVINDO10", Bemvindo10::new,
            "MENOS50", Menos50::new,
            "FRETEGRATIS", FreteGratis::new,
            "LEVE3PAGUE2", Leve3Pague2::new
    );

    private Cupons() {}

    public static Optional<Cupom> por(String codigo) {
        if (codigo == null) return Optional.empty();
        var factory = REGISTRY.get(codigo);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }

    public static boolean existe(String codigo) {
        return codigo != null && REGISTRY.containsKey(codigo);
    }
}
