package com.loja.checkout.dominio.entrega;

import java.util.Map;
import java.util.Optional;

public final class Modalidades {
    private static final Map<String, ModalidadeEntrega> MAP = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    private Modalidades() {}

    public static Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(MAP::get);
    }
}
