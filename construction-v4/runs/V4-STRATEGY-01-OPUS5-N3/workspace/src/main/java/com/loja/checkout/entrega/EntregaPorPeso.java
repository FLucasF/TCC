package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Entrega que cobra um valor fixo mais um valor por quilo do pedido. */
public record EntregaPorPeso(String codigo, BigDecimal fixo, BigDecimal porKg, int prazoDias)
        implements ModalidadeEntrega {

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pedido.pesoKg())));
    }
}
