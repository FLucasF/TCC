package com.loja.checkout.dominio.entrega;

import com.loja.checkout.infra.CheckoutException;

import java.util.Map;

public class ModalidadeEntregaRegistro {

    private static final Map<String, ModalidadeEntrega> REGISTRO = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    public static ModalidadeEntrega buscar(String codigo) {
        if (codigo == null || !REGISTRO.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return REGISTRO.get(codigo);
    }
}
