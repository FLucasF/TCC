package com.loja.checkout.pagamento;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class FormasPagamento {
    private static final Map<String, Supplier<FormaPagamento>> REGISTRY = Map.of(
            "PIX", Pix::new,
            "CARTAO", Cartao::new,
            "BOLETO", Boleto::new
    );

    private FormasPagamento() {}

    public static Optional<FormaPagamento> por(String codigo) {
        if (codigo == null) return Optional.empty();
        var factory = REGISTRY.get(codigo);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }
}
