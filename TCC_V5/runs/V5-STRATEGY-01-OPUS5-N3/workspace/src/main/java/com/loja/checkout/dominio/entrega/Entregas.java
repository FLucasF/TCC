package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Catalogo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As opções de entrega que a loja oferece hoje. */
public final class Entregas {

    public static final Catalogo<ModalidadeEntrega> CATALOGO = new Catalogo<>(
            List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()).stream()
                    .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity())),
            "MODALIDADE_INVALIDA");

    private Entregas() {
    }
}
