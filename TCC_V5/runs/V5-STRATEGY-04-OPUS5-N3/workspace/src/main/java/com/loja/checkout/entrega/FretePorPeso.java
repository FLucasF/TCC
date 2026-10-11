package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Modalidade que cobra uma taxa fixa mais um valor por kg do pedido. */
record FretePorPeso(String codigo, BigDecimal fixo, BigDecimal porKg, int prazoDias)
        implements ModalidadeEntrega {

    FretePorPeso(String codigo, String fixo, String porKg, int prazoDias) {
        this(codigo, new BigDecimal(fixo), new BigDecimal(porKg), prazoDias);
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pedido.pesoKg())));
    }
}
