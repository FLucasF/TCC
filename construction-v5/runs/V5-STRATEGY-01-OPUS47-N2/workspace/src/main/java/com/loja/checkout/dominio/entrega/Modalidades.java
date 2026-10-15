package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.ErroPedido;
import java.util.Map;

public final class Modalidades {
    private static final Map<String, Modalidade> TODAS = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    private Modalidades() {}

    public static Modalidade resolver(String codigo) {
        Modalidade m = codigo == null ? null : TODAS.get(codigo);
        if (m == null) throw new ErroPedido("MODALIDADE_INVALIDA");
        return m;
    }
}
