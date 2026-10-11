package com.loja.checkout.resumo.entrega;

import java.util.Map;
import java.util.Optional;

/** As opções de entrega que a loja oferece hoje, pelo código que o site envia. */
public final class Entregas {

    private static final Map<String, ModalidadeEntrega> POR_CODIGO = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy());

    private Entregas() {
    }

    public static Optional<ModalidadeEntrega> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(POR_CODIGO::get);
    }
}
