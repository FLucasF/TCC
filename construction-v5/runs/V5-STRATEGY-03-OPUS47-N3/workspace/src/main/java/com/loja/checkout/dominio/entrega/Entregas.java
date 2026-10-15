package com.loja.checkout.dominio.entrega;

import java.util.List;
import java.util.Optional;

public final class Entregas {
    private static final List<Entrega> TODAS = List.of(
            new Economica(),
            new Expressa(),
            new RetiradaLoja(),
            new Motoboy()
    );

    private Entregas() {}

    public static Optional<Entrega> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return TODAS.stream().filter(e -> e.codigo().equals(codigo)).findFirst();
    }
}
