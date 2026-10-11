package com.loja.checkout.domain.entrega;

import java.util.Map;

public class Modalidades {
    private static final Map<String, CalculadoraFrete> REGISTRO = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    public static CalculadoraFrete buscar(String nome) {
        return REGISTRO.get(nome);
    }
}
