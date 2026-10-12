package com.loja.checkout.dominio.pagamento;

import java.util.Map;
import java.util.Optional;

public final class Pagamentos {
    private static final Map<String, FormaPagamento> MAP = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()
    );

    private Pagamentos() {}

    public static Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(MAP::get);
    }
}
