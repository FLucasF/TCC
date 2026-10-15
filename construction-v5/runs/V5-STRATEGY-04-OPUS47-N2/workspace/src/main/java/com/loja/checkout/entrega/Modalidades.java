package com.loja.checkout.entrega;

import java.util.Map;
import java.util.Optional;

public final class Modalidades {
    private static final Map<String, java.util.function.Supplier<ModalidadeEntrega>> REGISTRY = Map.of(
            "ECONOMICA", Economica::new,
            "EXPRESSA", Expressa::new,
            "RETIRADA_LOJA", RetiradaLoja::new,
            "MOTOBOY", Motoboy::new
    );

    private Modalidades() {}

    public static Optional<ModalidadeEntrega> por(String codigo) {
        if (codigo == null) return Optional.empty();
        var factory = REGISTRY.get(codigo);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }
}
