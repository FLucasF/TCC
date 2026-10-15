package com.loja.dominio.modalidade;

import com.loja.dominio.ErroCheckout;

import java.util.Map;
import java.util.function.Supplier;

public final class Modalidades {
    private static final Map<String, Supplier<ModalidadeEntrega>> REGISTRO = Map.of(
        "ECONOMICA", Economica::new,
        "EXPRESSA", Expressa::new,
        "RETIRADA_LOJA", RetiradaLoja::new,
        "MOTOBOY", Motoboy::new
    );

    private Modalidades() {}

    public static ModalidadeEntrega parse(String codigo) {
        if (codigo == null) throw new ErroCheckout("MODALIDADE_INVALIDA");
        var sup = REGISTRO.get(codigo);
        if (sup == null) throw new ErroCheckout("MODALIDADE_INVALIDA");
        return sup.get();
    }
}
