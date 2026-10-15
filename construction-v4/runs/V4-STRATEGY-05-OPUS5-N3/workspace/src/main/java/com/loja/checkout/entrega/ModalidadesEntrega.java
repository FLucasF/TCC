package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Registro;

/** As modalidades de entrega que a loja oferece hoje. */
public final class ModalidadesEntrega {

    public static final Registro<ModalidadeEntrega> REGISTRO = new Registro<>(
            new EntregaEconomica(),
            new EntregaExpressa(),
            new RetiradaLoja(),
            new Motoboy());

    private ModalidadesEntrega() {
    }
}
